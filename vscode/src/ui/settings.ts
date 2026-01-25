import * as vscode from 'vscode';
import { JsCopyOptions, JsDirectoryStructureOptions } from 'copy4lm-common';

const DEFAULTS = {
  headerFormat: '```$FILE_PATH',
  footerFormat: '```',
  preText: '=====\n$PROJECT_NAME\n=====\n',
  postText: '',
  fileCountLimit: 30,
  setMaxFileCount: true,
  filenameFilters: [] as string[],
  useFilenameFilters: false,
  addExtraLineBetweenFiles: true,
  strictMemoryRead: true,
  maxFileSizeKB: 500,
  directoryPreText: '',
  directoryPostText: ''
};

function clampMin(value: number, min: number): number {
  if (Number.isNaN(value)) return min;
  return Math.max(min, Math.floor(value));
}

function getStringArray(value: unknown, fallback: string[]): string[] {
  if (!Array.isArray(value)) return fallback;
  return value.filter((item) => typeof item === 'string') as string[];
}

export function getCopyOptions(projectName: string): JsCopyOptions {
  const config = vscode.workspace.getConfiguration('copy4lm');
  const fileCountLimit = clampMin(config.get<number>('common.fileCountLimit', DEFAULTS.fileCountLimit), 1);
  const maxFileSizeKB = clampMin(config.get<number>('common.maxFileSizeKB', DEFAULTS.maxFileSizeKB), 1);
  const filenameFilters = getStringArray(
    config.get('common.filenameFilters', DEFAULTS.filenameFilters),
    DEFAULTS.filenameFilters
  );

  return {
    headerFormat: config.get('fileContent.headerFormat', DEFAULTS.headerFormat),
    footerFormat: config.get('fileContent.footerFormat', DEFAULTS.footerFormat),
    preText: config.get('fileContent.preText', DEFAULTS.preText),
    postText: config.get('fileContent.postText', DEFAULTS.postText),
    fileCountLimit,
    setMaxFileCount: config.get('common.setMaxFileCount', DEFAULTS.setMaxFileCount),
    filenameFilters,
    useFilenameFilters: config.get('common.useFilenameFilters', DEFAULTS.useFilenameFilters),
    addExtraLineBetweenFiles: config.get(
      'fileContent.addExtraLineBetweenFiles',
      DEFAULTS.addExtraLineBetweenFiles
    ),
    strictMemoryRead: config.get('common.strictMemoryRead', DEFAULTS.strictMemoryRead),
    maxFileSizeKB,
    projectName
  };
}

export function getDirectoryStructureOptions(projectName: string): JsDirectoryStructureOptions {
  const config = vscode.workspace.getConfiguration('copy4lm');
  const fileCountLimit = clampMin(config.get<number>('common.fileCountLimit', DEFAULTS.fileCountLimit), 1);
  const maxFileSizeKB = clampMin(config.get<number>('common.maxFileSizeKB', DEFAULTS.maxFileSizeKB), 1);
  const filenameFilters = getStringArray(
    config.get('common.filenameFilters', DEFAULTS.filenameFilters),
    DEFAULTS.filenameFilters
  );

  return {
    preText: config.get('directoryStructure.preText', DEFAULTS.directoryPreText),
    postText: config.get('directoryStructure.postText', DEFAULTS.directoryPostText),
    fileCountLimit,
    setMaxFileCount: config.get('common.setMaxFileCount', DEFAULTS.setMaxFileCount),
    filenameFilters,
    useFilenameFilters: config.get('common.useFilenameFilters', DEFAULTS.useFilenameFilters),
    maxFileSizeKB,
    projectName
  };
}
