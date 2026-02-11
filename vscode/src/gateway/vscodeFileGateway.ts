import * as vscode from 'vscode';
import * as fs from 'node:fs';
import * as path from 'node:path';
import { spawnSync } from 'node:child_process';
import * as c4 from "copy4lm-common";

type JsLogger = c4.io.github.ceracharlescc.copy4lm.JsLogger;
type JsFileRef = c4.io.github.ceracharlescc.copy4lm.JsFileRef;
type JsFileGateway = c4.io.github.ceracharlescc.copy4lm.JsFileGateway;

const BINARY_PROBE_BYTES = 8000;

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

  constructor(logger?: JsLogger) {
    this.logger = logger;
  }

  childrenOf(dir: JsFileRef): JsFileRef[] {
    try {
      const entries = fs.readdirSync(dir.path, { withFileTypes: true });
      return entries.map((entry) => ({
        name: entry.name,
        path: path.join(dir.path, entry.name),
        isDirectory: entry.isDirectory()
      }));
    } catch (error) {
      this.logger?.error(`Failed to list children for ${dir.path}`, String(error));
      return [];
    }
  }

  readText(file: JsFileRef, strictMemoryRead: boolean): string {
    try {
      const document = vscode.workspace.textDocuments.find((doc) =>
        normalizeForCompare(doc.uri.fsPath) === normalizeForCompare(file.path)
      );

      if (!strictMemoryRead && document) {
        return document.getText();
      }

      if (strictMemoryRead && document && this.isFileOpen(file.path)) {
        return document.getText();
      }

      return fs.readFileSync(file.path, 'utf8');
    } catch (error) {
      this.logger?.error(`Failed to read file contents for ${file.path}`, String(error));
      return '';
    }
  }

  isBinary(file: JsFileRef): boolean {
    try {
      const fd = fs.openSync(file.path, 'r');
      const buffer = Buffer.alloc(BINARY_PROBE_BYTES);
      const bytesRead = fs.readSync(fd, buffer, 0, buffer.length, 0);
      fs.closeSync(fd);
      for (let i = 0; i < bytesRead; i += 1) {
        if (buffer[i] === 0) {
          return true;
        }
      }
      return false;
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
    const fileUri = vscode.Uri.file(file.path);
    const workspaceFolder = vscode.workspace.getWorkspaceFolder(fileUri)
      ?? vscode.workspace.workspaceFolders?.[0];

    if (!workspaceFolder) {
      return toPosixPath(file.path);
    }

    const relative = path.relative(workspaceFolder.uri.fsPath, file.path);
    return toPosixPath(relative || file.name);
  }

  isGitIgnored(file: JsFileRef): boolean {
    const cacheKey = normalizeForCompare(file.path);
    const cached = this.gitIgnoreCache.get(cacheKey);
    if (cached !== undefined) {
      return cached;
    }

    const ignored = this.resolveGitIgnore(file);
    this.gitIgnoreCache.set(cacheKey, ignored);
    return ignored;
  }

  private isFileOpen(filePath: string): boolean {
    const normalized = normalizeForCompare(filePath);
    for (const group of vscode.window.tabGroups.all) {
      for (const tab of group.tabs) {
        const uri = getTabUri(tab);
        if (uri && normalizeForCompare(uri.fsPath) === normalized) {
          return true;
        }
      }
    }
    return false;
  }

  private resolveGitIgnore(file: JsFileRef): boolean {
    const fileUri = vscode.Uri.file(file.path);
    const workspaceFolder = vscode.workspace.getWorkspaceFolder(fileUri)
      ?? vscode.workspace.workspaceFolders?.[0];

    if (!workspaceFolder) {
      return false;
    }

    const workspaceRoot = workspaceFolder.uri.fsPath;
    const workspaceKey = normalizeForCompare(workspaceRoot);

    if (!vscode.workspace.isTrusted) {
      this.logGitIgnoreInfoOnce(
        workspaceRoot,
        'Workspace is untrusted; skipping VCS ignore evaluation and including files.'
      );
      return false;
    }

    if (this.gitIgnoreDisabledRoots.has(workspaceKey)) {
      return false;
    }

    const relativePath = path.relative(workspaceRoot, file.path);
    if (relativePath.startsWith('..') || path.isAbsolute(relativePath)) {
      return false;
    }

    const pathForGit = toPosixPath(relativePath || '.');
    const result = spawnSync(
      'git',
      ['check-ignore', '--quiet', '--', pathForGit],
      { cwd: workspaceRoot, windowsHide: true }
    );

    if (result.status === 0) {
      return true;
    }

    if (result.status === 1) {
      return false;
    }

    const expectedFallbackDetail = this.resolveExpectedGitFallbackDetail(result);
    if (expectedFallbackDetail) {
      this.gitIgnoreDisabledRoots.add(workspaceKey);
      this.logGitIgnoreInfoOnce(
        workspaceRoot,
        'VCS ignore evaluation unavailable for this workspace root; including files.',
        expectedFallbackDetail
      );
      return false;
    }

    const detail = result.error?.message
      ?? result.stderr?.toString().trim()
      ?? `Exit code: ${String(result.status)}`;
    this.logGitIgnoreErrorOnce(workspaceRoot, detail);
    return false;
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
