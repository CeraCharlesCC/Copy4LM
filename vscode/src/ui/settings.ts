import * as vscode from 'vscode';
import * as c4 from 'copy4lm-common';

type JsCopyOptions = c4.io.github.ceracharlescc.copy4lm.JsCopyOptions;
type JsDirectoryStructureOptions = c4.io.github.ceracharlescc.copy4lm.JsDirectoryStructureOptions;

type CommonOptions = {
  fileCountLimit: number;
  setMaxFileCount: boolean;
  filenameFilters: string[];
  useFilenameFilters: boolean;
  respectGitIgnore: boolean;
  strictMemoryRead: boolean;
  maxFileSizeKB: number;
};

const DEFAULTS = {
  common: {
    fileCountLimit: 30,
    setMaxFileCount: true,
    filenameFilters: [] as string[],
    useFilenameFilters: false,
    respectGitIgnore: true,
    strictMemoryRead: true,
    maxFileSizeKB: 500
  },
  fileContent: {
    preText: '=====\n$PROJECT_NAME\n=====\n',
    headerFormat: '```$FILE_PATH',
    footerFormat: '```',
    addExtraLineBetweenFiles: true,
    postText: ''
  },
  directoryStructure: {
    preText: '',
    postText: ''
  }
} as const;

function clampMin(value: number, min: number): number {
  if (Number.isNaN(value)) return min;
  return Math.max(min, Math.floor(value));
}

function getStringArray(value: unknown, fallback: readonly string[]): string[] {
  if (!Array.isArray(value)) return [...fallback];
  return value.filter((item): item is string => typeof item === 'string');
}

function getConfig(): vscode.WorkspaceConfiguration {
  return vscode.workspace.getConfiguration('copy4lm');
}

function readCommonOptions(config: vscode.WorkspaceConfiguration): CommonOptions {
  return {
    fileCountLimit: clampMin(config.get('common.fileCountLimit', DEFAULTS.common.fileCountLimit), 1),
    setMaxFileCount: config.get('common.setMaxFileCount', DEFAULTS.common.setMaxFileCount),
    filenameFilters: getStringArray(config.get('common.filenameFilters', DEFAULTS.common.filenameFilters), DEFAULTS.common.filenameFilters),
    useFilenameFilters: config.get('common.useFilenameFilters', DEFAULTS.common.useFilenameFilters),
    respectGitIgnore: config.get('common.respectGitIgnore', DEFAULTS.common.respectGitIgnore),
    strictMemoryRead: config.get('common.strictMemoryRead', DEFAULTS.common.strictMemoryRead),
    maxFileSizeKB: clampMin(config.get('common.maxFileSizeKB', DEFAULTS.common.maxFileSizeKB), 1)
  };
}

function notImplemented(methodName: string): never {
  throw new Error(`${methodName} is not implemented for VS Code settings objects.`);
}

const interopMethodStubs = {
  copy(..._args: unknown[]): never {
    return notImplemented('copy');
  },
  toString(): never {
    return notImplemented('toString');
  },
  hashCode(): never {
    return notImplemented('hashCode');
  },
  equals(_other: c4.Nullable<any>): never {
    return notImplemented('equals');
  }
};

export function getCopyOptions(projectName: string): JsCopyOptions {
  const config = getConfig();
  const common = readCommonOptions(config);

  return {
    preText: config.get('fileContent.preText', DEFAULTS.fileContent.preText),
    headerFormat: config.get('fileContent.headerFormat', DEFAULTS.fileContent.headerFormat),
    footerFormat: config.get('fileContent.footerFormat', DEFAULTS.fileContent.footerFormat),
    addExtraLineBetweenFiles: config.get(
      'fileContent.addExtraLineBetweenFiles',
      DEFAULTS.fileContent.addExtraLineBetweenFiles
    ),
    postText: config.get('fileContent.postText', DEFAULTS.fileContent.postText),
    ...common,
    projectName,
    ...interopMethodStubs
  } as JsCopyOptions;
}

export function getDirectoryStructureOptions(projectName: string): JsDirectoryStructureOptions {
  const config = getConfig();
  const common = readCommonOptions(config);

  return {
    preText: config.get('directoryStructure.preText', DEFAULTS.directoryStructure.preText),
    postText: config.get('directoryStructure.postText', DEFAULTS.directoryStructure.postText),
    ...common,
    projectName,
    ...interopMethodStubs
  } as JsDirectoryStructureOptions;
}
