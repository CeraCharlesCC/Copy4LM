import * as vscode from 'vscode';
import * as fs from 'node:fs';
import * as path from 'node:path';
import {
  copyDirectoryStructure,
  copyFiles,
  JsFileRef,
  JsLogger
} from 'copy4lm-common';

import { VsCodeFileGateway } from './gateway/vscodeFileGateway';
import { getCopyOptions, getDirectoryStructureOptions } from './ui/settings';
import {
  showCopyResult,
  showDirectoryStructureResult,
  showError,
  showFileLimitWarning
} from './ui/notifications';

let outputChannel: vscode.OutputChannel | undefined;
let cachedLogger: JsLogger | undefined;

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

function uniqueUris(uris: vscode.Uri[]): vscode.Uri[] {
  const seen = new Set<string>();
  const unique: vscode.Uri[] = [];
  for (const uri of uris) {
    const key = normalizeForCompare(uri.fsPath);
    if (!seen.has(key)) {
      seen.add(key);
      unique.push(uri);
    }
  }
  return unique;
}

function resolveSelectionUris(uri?: vscode.Uri, selectedUris?: vscode.Uri[]): vscode.Uri[] {
  if (selectedUris && selectedUris.length > 0) {
    return uniqueUris(selectedUris);
  }
  if (uri) {
    return [uri];
  }
  const active = vscode.window.activeTextEditor?.document.uri;
  return active ? [active] : [];
}

function getOpenEditorUris(): vscode.Uri[] {
  const uris: vscode.Uri[] = [];
  for (const group of vscode.window.tabGroups.all) {
    for (const tab of group.tabs) {
      const uri = getTabUri(tab);
      if (uri) {
        uris.push(uri);
      }
    }
  }
  return uniqueUris(uris);
}

function resolveProjectName(uris: vscode.Uri[]): string {
  const workspaceFolders = vscode.workspace.workspaceFolders ?? [];
  for (const uri of uris) {
    const folder = vscode.workspace.getWorkspaceFolder(uri);
    if (folder) {
      return folder.name;
    }
  }
  if (workspaceFolders.length > 0) {
    return workspaceFolders[0].name;
  }
  if (uris.length > 0) {
    return path.basename(uris[0].fsPath);
  }
  return 'workspace';
}

function toFileRef(uri: vscode.Uri): JsFileRef | null {
  try {
    const stat = fs.statSync(uri.fsPath);
    return {
      name: path.basename(uri.fsPath),
      path: uri.fsPath,
      isDirectory: stat.isDirectory()
    };
  } catch {
    return null;
  }
}

function createLogger(outputChannel: vscode.OutputChannel): JsLogger {
  return {
    info(message: string) {
      outputChannel.appendLine(`[info] ${message}`);
    },
    error(message: string, throwable?: string) {
      const suffix = throwable ? ` (${throwable})` : '';
      outputChannel.appendLine(`[error] ${message}${suffix}`);
    }
  };
}

function getLogger(): JsLogger {
  if (!outputChannel) {
    outputChannel = vscode.window.createOutputChannel('Copy 4 LM');
  }
  if (!cachedLogger) {
    cachedLogger = createLogger(outputChannel);
  }
  return cachedLogger;
}

async function copySelection(uri?: vscode.Uri, selectedUris?: vscode.Uri[]): Promise<void> {
  const selection = resolveSelectionUris(uri, selectedUris);
  if (selection.length === 0) {
    showError('No files or folders selected.');
    return;
  }

  const projectName = resolveProjectName(selection);
  const options = getCopyOptions(projectName);
  const fileRefs = selection
    .map(toFileRef)
    .filter((ref): ref is JsFileRef => Boolean(ref));

  if (fileRefs.length === 0) {
    showError('No readable files or folders found in the selection.');
    return;
  }

  const logger = getLogger();
  const gateway = new VsCodeFileGateway(logger);

  try {
    const result = copyFiles(fileRefs, options, gateway, logger);
    await vscode.env.clipboard.writeText(result.clipboardText);
    showCopyResult(result);
    if (result.fileLimitReached) {
      showFileLimitWarning(options.fileCountLimit);
    }
  } catch (error) {
    logger.error('Copy failed', String(error));
    showError('Copy failed. See output for details.');
  }
}

async function copyOpenEditors(): Promise<void> {
  const uris = getOpenEditorUris();
  if (uris.length === 0) {
    showError('No open editors found.');
    return;
  }
  await copySelection(undefined, uris);
}

async function copyCurrentFile(): Promise<void> {
  const uri = vscode.window.activeTextEditor?.document.uri;
  if (!uri) {
    showError('No active editor.');
    return;
  }
  await copySelection(uri, [uri]);
}

async function copyDirectoryStructureCommand(uri?: vscode.Uri, selectedUris?: vscode.Uri[]): Promise<void> {
  const selection = resolveSelectionUris(uri, selectedUris);
  if (selection.length === 0) {
    showError('No files or folders selected.');
    return;
  }

  const projectName = resolveProjectName(selection);
  const options = getDirectoryStructureOptions(projectName);
  const fileRefs = selection
    .map(toFileRef)
    .filter((ref): ref is JsFileRef => Boolean(ref));

  if (fileRefs.length === 0) {
    showError('No readable files or folders found in the selection.');
    return;
  }

  const logger = getLogger();
  const gateway = new VsCodeFileGateway(logger);

  try {
    const result = copyDirectoryStructure(fileRefs, options, gateway, logger);
    await vscode.env.clipboard.writeText(result.clipboardText);
    showDirectoryStructureResult(result);
    if (result.fileLimitReached) {
      showFileLimitWarning(options.fileCountLimit);
    }
  } catch (error) {
    logger.error('Directory structure copy failed', String(error));
    showError('Directory structure copy failed. See output for details.');
  }
}

export function activate(context: vscode.ExtensionContext): void {
  context.subscriptions.push(
    vscode.commands.registerCommand('copy4lm.copySelection', copySelection),
    vscode.commands.registerCommand('copy4lm.copyOpenEditors', copyOpenEditors),
    vscode.commands.registerCommand('copy4lm.copyDirectoryStructure', copyDirectoryStructureCommand),
    vscode.commands.registerCommand('copy4lm.copyCurrentFile', copyCurrentFile),
    {
      dispose() {
        outputChannel?.dispose();
      }
    }
  );
}

export function deactivate(): void {}
