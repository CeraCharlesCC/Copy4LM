import * as vscode from 'vscode';
import * as fs from 'node:fs';
import * as path from 'node:path';
import { JsFileGateway, JsFileRef, JsLogger } from 'copy4lm-common';

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
}
