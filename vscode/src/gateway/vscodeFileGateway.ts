import * as vscode from 'vscode';
import * as fs from 'node:fs';
import * as path from 'node:path';
import { spawnSync } from 'node:child_process';
import type * as c4 from "copy4lm-common";

type JsLogger = c4.io.github.ceracharlescc.copy4lm.JsLogger;
type JsFileRef = c4.io.github.ceracharlescc.copy4lm.JsFileRef;
type JsFileGateway = c4.io.github.ceracharlescc.copy4lm.JsFileGateway;

const BINARY_PROBE_BYTES = 8000;
const GIT_TIMEOUT_MS = 5000;

function toPosixPath(value: string): string {
  return value.replace(/\\/g, '/');
}

function normalizeForCompare(value: string): string {
  const normalized = path.normalize(value);
  return process.platform === 'win32' ? normalized.toLowerCase() : normalized;
}

function getTabUri(tab: vscode.Tab): vscode.Uri | undefined {
  if (tab.input instanceof vscode.TabInputText) {
    return tab.input.uri;
  }
  if (tab.input instanceof vscode.TabInputTextDiff) {
    return tab.input.modified;
  }
  return undefined;
}

export class VsCodeFileGateway implements JsFileGateway {
  private readonly logger: JsLogger | undefined;
  private readonly gitIgnoreCache = new Map<string, boolean>();
  private readonly gitIgnoreDisabledRoots = new Set<string>();
  private readonly gitIgnoreErrorLoggedRoots = new Set<string>();
  private readonly gitIgnoreInfoLoggedRoots = new Set<string>();
  private documentsByPath: Map<string, vscode.TextDocument> | undefined;
  private openPaths: Set<string> | undefined;
  private binaryProbe: Buffer | undefined;

  constructor(logger?: JsLogger, private readonly respectGitIgnore = true) {
    this.logger = logger;
  }

  childrenOf(dir: JsFileRef): JsFileRef[] {
    try {
      const entries = fs.readdirSync(dir.path, { withFileTypes: true });
      const children = entries.map((entry) => ({
        name: entry.name,
        path: path.join(dir.path, entry.name),
        isDirectory: entry.isDirectory()
      }));
      this.prepareGitIgnore(children);
      return children;
    } catch (error) {
      this.logger?.error(`Failed to list children for ${dir.path}`, String(error));
      return [];
    }
  }

  readText(file: JsFileRef, strictMemoryRead: boolean): string | null {
    try {
      // A gateway is scoped to one synchronous copy operation.
      this.documentsByPath ??= new Map(vscode.workspace.textDocuments.map((doc) =>
        [normalizeForCompare(doc.uri.fsPath), doc]
      ));
      const document = this.documentsByPath.get(normalizeForCompare(file.path));

      if (!strictMemoryRead && document) {
        return document.getText();
      }

      if (strictMemoryRead && document && this.isFileOpen(file.path)) {
        return document.getText();
      }

      return fs.readFileSync(file.path, 'utf8');
    } catch (error) {
      this.logger?.error(`Failed to read file contents for ${file.path}`, String(error));
      return null;
    }
  }

  isBinary(file: JsFileRef): boolean {
    try {
      const fd = fs.openSync(file.path, 'r');
      try {
        const buffer = this.binaryProbe ??= Buffer.allocUnsafe(BINARY_PROBE_BYTES);
        const bytesRead = fs.readSync(fd, buffer, 0, buffer.length, 0);
        return buffer.subarray(0, bytesRead).includes(0);
      } finally {
        fs.closeSync(fd);
      }
    } catch (error) {
      this.logger?.error(`Failed to detect binary file ${file.path}`, String(error));
      return false;
    }
  }

  sizeBytes(file: JsFileRef): number {
    try {
      return fs.statSync(file.path).size;
    } catch (error) {
      this.logger?.error(`Failed to stat file ${file.path}`, String(error));
      return 0;
    }
  }

  relativePath(file: JsFileRef): string {
    const workspaceFolders = vscode.workspace.workspaceFolders ?? [];
    const fileUri = vscode.Uri.file(file.path);
    const workspaceFolder = vscode.workspace.getWorkspaceFolder(fileUri)
      ?? workspaceFolders[0];

    if (!workspaceFolder) {
      return toPosixPath(file.path);
    }

    const relative = path.relative(workspaceFolder.uri.fsPath, file.path);
    const baseRelativePath = toPosixPath(relative || file.name);
    return workspaceFolders.length > 1
      ? `${workspaceFolder.name}/${baseRelativePath}`
      : baseRelativePath;
  }

  isGitIgnored(file: JsFileRef): boolean {
    const key = normalizeForCompare(file.path);
    const cached = this.gitIgnoreCache.get(key);
    if (cached !== undefined) return cached;
    this.prepareGitIgnore([file]);
    return this.gitIgnoreCache.get(key) ?? false;
  }

  /** Batch selections and directory entries, preserving Git's tracked-file/negation semantics. */
  prepareGitIgnore(files: readonly JsFileRef[]): void {
    if (!this.respectGitIgnore) return;

    const batches = new Map<string, { root: string; paths: Map<string, string> }>();
    for (const file of files) {
      const cacheKey = normalizeForCompare(file.path);
      if (this.gitIgnoreCache.has(cacheKey)) continue;
      const workspaceFolder = vscode.workspace.getWorkspaceFolder(vscode.Uri.file(file.path))
        ?? vscode.workspace.workspaceFolders?.[0];
      this.gitIgnoreCache.set(cacheKey, false);
      if (!workspaceFolder) continue;

      const root = workspaceFolder.uri.fsPath;
      const rootKey = normalizeForCompare(root);
      if (!vscode.workspace.isTrusted) {
        this.logGitIgnoreInfoOnce(root,
          'Workspace is untrusted; skipping VCS ignore evaluation and including files.');
        continue;
      }
      if (this.gitIgnoreDisabledRoots.has(rootKey)) continue;

      const relative = path.relative(root, file.path);
      if (relative === '..' || relative.startsWith(`..${path.sep}`) || path.isAbsolute(relative)) continue;
      let batch = batches.get(rootKey);
      if (!batch) {
        batch = { root, paths: new Map() };
        batches.set(rootKey, batch);
      }
      batch.paths.set(cacheKey, toPosixPath(relative || '.'));
    }

    for (const [rootKey, { root, paths }] of batches) {
      // NUL delimiters handle whitespace, newlines and option-like filenames literally.
      const input = [...paths.values()].join('\0') + '\0';
      const result = spawnSync('git', ['check-ignore', '--stdin', '-z'], {
        cwd: root,
        windowsHide: true,
        timeout: GIT_TIMEOUT_MS,
        input,
        encoding: 'utf8',
        maxBuffer: Math.max(1024 * 1024, Buffer.byteLength(input) + 65536)
      });
      if (!result.error && (result.status === 0 || result.status === 1)) {
        const ignored = new Set(result.stdout.split('\0'));
        for (const [cacheKey, relative] of paths) {
          this.gitIgnoreCache.set(cacheKey, ignored.has(relative));
        }
        continue;
      }

      this.gitIgnoreDisabledRoots.add(rootKey);
      const expectedDetail = this.resolveExpectedGitFallbackDetail(result);
      if (expectedDetail) {
        this.logGitIgnoreInfoOnce(root,
          'VCS ignore evaluation unavailable for this workspace root; including files.', expectedDetail);
      } else {
        const detail = result.error?.message || result.stderr?.trim() || `Exit code: ${String(result.status)}`;
        this.logGitIgnoreErrorOnce(root, detail);
      }
    }
  }

  private isFileOpen(filePath: string): boolean {
    if (!this.openPaths) {
      this.openPaths = new Set();
      for (const group of vscode.window.tabGroups.all) {
        for (const tab of group.tabs) {
          const uri = getTabUri(tab);
          if (uri) this.openPaths.add(normalizeForCompare(uri.fsPath));
        }
      }
    }
    return this.openPaths.has(normalizeForCompare(filePath));
  }

  private resolveExpectedGitFallbackDetail(result: ReturnType<typeof spawnSync>): string | undefined {
    const errorCode = this.getErrnoCode(result.error);
    if (errorCode === 'ENOENT') {
      return 'Git executable was not found in PATH.';
    }

    const stderr = result.stderr?.toString().trim() ?? '';
    const normalizedStderr = stderr.toLowerCase();
    if (
      normalizedStderr.includes('not a git repository')
      || normalizedStderr.includes('not in a git directory')
      || normalizedStderr.includes('outside repository')
    ) {
      return stderr || 'Current workspace root is not part of a Git repository.';
    }

    return undefined;
  }

  private getErrnoCode(error: Error | undefined): string | undefined {
    const errnoError = error as NodeJS.ErrnoException | undefined;
    return typeof errnoError?.code === 'string' ? errnoError.code : undefined;
  }

  private logGitIgnoreErrorOnce(workspaceRoot: string, detail: string): void {
    const key = normalizeForCompare(workspaceRoot);
    if (this.gitIgnoreErrorLoggedRoots.has(key)) {
      return;
    }
    this.gitIgnoreErrorLoggedRoots.add(key);
    this.logger?.error(
      `Failed to evaluate VCS ignore rules under ${workspaceRoot}; falling back to include files.`,
      detail
    );
  }

  private logGitIgnoreInfoOnce(workspaceRoot: string, message: string, detail?: string): void {
    const key = normalizeForCompare(workspaceRoot);
    if (this.gitIgnoreInfoLoggedRoots.has(key)) {
      return;
    }
    this.gitIgnoreInfoLoggedRoots.add(key);
    const suffix = detail ? ` (${detail})` : '';
    this.logger?.info(`${message} [root=${workspaceRoot}]${suffix}`);
  }
}
