const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const Module = require('node:module');
const { test, beforeEach, afterEach } = require('node:test');

const commands = new Map();
const defaults = Object.assign({}, ...require('../package.json').contributes.configuration
  .map((section) => section.properties));
let root;
let folders;
let config;
let clipboard;
let errors;

const uri = (fsPath) => ({ fsPath });
const vscode = {
  Uri: { file: uri },
  commands: { registerCommand: (name, handler) => {
    commands.set(name, handler);
    return { dispose() {} };
  } },
  workspace: {
    getConfiguration: () => ({ get: (key, fallback) => config[key]
      ?? defaults[`copy4lm.${key}`]?.default ?? fallback }),
    getWorkspaceFolder: (entry) => folders.find((folder) => {
      const relative = path.relative(folder.uri.fsPath, entry.fsPath);
      return relative === '' || (!relative.startsWith(`..${path.sep}`)
        && relative !== '..' && !path.isAbsolute(relative));
    })
  },
  window: {
    showInformationMessage() {},
    showErrorMessage: (message) => errors.push(message),
    createOutputChannel: () => ({ appendLine() {}, dispose() {} })
  },
  env: { clipboard: { writeText: async (text) => { clipboard = text; } } }
};

const originalLoad = Module._load;
try {
  Module._load = function (name, ...args) {
    return name === 'vscode' ? vscode : originalLoad.call(this, name, ...args);
  };
  require('../dist/extension.js').activate({ subscriptions: [] });
} finally {
  Module._load = originalLoad;
}

beforeEach(() => {
  root = fs.mkdtempSync(path.join(os.tmpdir(), 'copy4lm-paths-'));
  folders = [{ name: 'workspace', uri: uri(root) }];
  config = {};
  clipboard = 'existing clipboard';
  errors = [];
  vscode.window.activeTextEditor = undefined;
});
afterEach(() => fs.rmSync(root, { recursive: true, force: true }));

test('Explorer multi-selection copies entries, including binary files and parent/child selections', async () => {
  const dirs = ['Copy4LM', 'ContextTools-extension', 'repozstd'];
  dirs.forEach((dir) => fs.mkdirSync(path.join(root, dir)));
  const binary = path.join(root, 'Copy4LM', 'image.png');
  fs.writeFileSync(binary, Buffer.from([0, 1, 2]));
  config = { 'common.fileCountLimit': 1, 'common.useFilenameFilters': true,
    'common.filenameFilters': ['.kt'] };
  const selected = [...dirs.map((dir) => uri(path.join(root, dir))), uri(binary)];

  await commands.get('copy4lm.copyRelativePaths')(selected[1], [...selected, selected[0]]);

  assert.equal(clipboard, '[Copy4LM/, ContextTools-extension/, repozstd/, Copy4LM/image.png]');
  assert.deepEqual(errors, []);
});

test('absolute paths use literal start/end and multiline delimiter settings', async () => {
  const file = path.join(root, 'example.txt');
  fs.writeFileSync(file, 'text');
  config = { 'pathList.start': 'START\n', 'pathList.end': '\nEND', 'pathList.delimiter': '\n' };

  await commands.get('copy4lm.copyAbsolutePaths')(uri(root), [uri(root), uri(file)]);

  const normalized = root.replace(/\\/g, '/');
  assert.equal(clipboard, `START\n${normalized}/\n${normalized}/example.txt\nEND`);
});

test('each workspace root is used independently without dropping matching relative paths', async () => {
  const roots = ['one', 'two'].map((name) => path.join(root, name));
  roots.forEach((dir) => {
    fs.mkdirSync(dir);
    fs.writeFileSync(path.join(dir, 'main.ts'), '');
  });
  folders = roots.map((dir) => ({ name: path.basename(dir), uri: uri(dir) }));
  await commands.get('copy4lm.copyRelativePaths')(undefined,
    roots.map((dir) => uri(path.join(dir, 'main.ts'))));
  assert.equal(clipboard, '[main.ts, main.ts]');
});

test('relative mode handles the selected workspace root and entries outside workspace roots', async () => {
  folders = [{ name: 'nested', uri: uri(path.join(root, 'nested')) }];
  fs.mkdirSync(folders[0].uri.fsPath);
  await commands.get('copy4lm.copyRelativePaths')(undefined, [folders[0].uri, uri(root)]);
  assert.equal(clipboard, `[./, ${root.replace(/\\/g, '/')}/]`);
});

test('Command Palette falls back to the active file and supports empty formatting strings', async () => {
  const file = path.join(root, 'active.txt');
  fs.writeFileSync(file, '');
  vscode.window.activeTextEditor = { document: { uri: uri(file) } };
  config = { 'pathList.start': '', 'pathList.end': '', 'pathList.delimiter': '' };
  await commands.get('copy4lm.copyRelativePaths')();
  assert.equal(clipboard, 'active.txt');
});

test('missing selection or inaccessible entry leaves the clipboard unchanged and reports an error', async () => {
  await commands.get('copy4lm.copyRelativePaths')();
  assert.equal(clipboard, 'existing clipboard');
  assert.match(errors[0], /No files or folders selected/);
  await commands.get('copy4lm.copyAbsolutePaths')(uri(root),
    [uri(root), uri(path.join(root, 'missing'))]);
  assert.equal(clipboard, 'existing clipboard');
  assert.match(errors[1], /Path list copy failed/);
});
