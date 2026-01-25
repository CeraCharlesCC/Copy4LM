import * as vscode from 'vscode';
import { JsCopyResult, JsDirectoryStructureResult } from 'copy4lm-common';

export function showCopyResult(result: JsCopyResult): void {
  const fileCountLabel = result.copiedFileCount === 1 ? '1 file' : `${result.copiedFileCount} files`;
  const stats = result.stats;
  const message = `Copy 4 LM: ${fileCountLabel} copied. Chars ${stats.totalChars}, lines ${stats.totalLines}, words ${stats.totalWords}, tokens ${stats.totalTokens}.`;
  void vscode.window.showInformationMessage(message);
}

export function showDirectoryStructureResult(_result: JsDirectoryStructureResult): void {
  void vscode.window.showInformationMessage('Copy 4 LM: Directory structure copied.');
}

export function showFileLimitWarning(fileCountLimit: number): void {
  void vscode.window.showWarningMessage(
    `Copy 4 LM: File limit of ${fileCountLimit} files was reached.`
  );
}

export function showError(message: string): void {
  void vscode.window.showErrorMessage(`Copy 4 LM: ${message}`);
}
