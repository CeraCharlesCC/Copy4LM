const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const Module = require('node:module');
const { spawnSync } = require('node:child_process');
const { test, beforeEach, afterEach } = require('node:test');
const { buildSync } = require('esbuild');

let root;
let calls;
let infos;
let errors;
let failProbe;
let closeCalls;
let gitFailure;
class TabInputText { constructor(uri) { this.uri = uri; } }
class TabInputTextDiff { constructor(modified) { this.modified = modified; } }
const uri = (fsPath) => ({ fsPath });
const ref = (filePath, isDirectory = false) => ({
  name: path.basename(filePath), path: filePath, isDirectory
});
const vscode = {
  Uri: { file: uri }, TabInputText, TabInputTextDiff,
  workspace: {
    getWorkspaceFolder: (entry) => vscode.workspace.workspaceFolders.find((folder) => {
      const relative = path.relative(folder.uri.fsPath, entry.fsPath);
      return relative === '' || (relative !== '..' && !relative.startsWith(`..${path.sep}`)
        && !path.isAbsolute(relative));
    })
  },
  window: { tabGroups: {} }
};
const logger = { info: (message) => infos.push(message), error: (...args) => errors.push(args) };
const code = buildSync({
  entryPoints: [path.join(__dirname, '../src/gateway/vscodeFileGateway.ts')],
  bundle: true, platform: 'node', format: 'cjs', external: ['vscode'], write: false
}).outputFiles[0].text;
const loaded = new Module(__filename, module);
loaded.filename = __filename;
loaded.paths = module.paths;
const originalLoad = Module._load;
try {
  Module._load = function (name, ...args) {
    if (name === 'vscode') return vscode;
    if (name === 'node:child_process') return { spawnSync: (...args) => {
      calls.push(args);
      return gitFailure ?? spawnSync(...args);
    } };
    if (name === 'node:fs') return { ...fs,
      readSync: (...args) => {
        if (failProbe) throw new Error('probe failed');
        return fs.readSync(...args);
      },
      closeSync: (...args) => { closeCalls++; return fs.closeSync(...args); }
    };
    return originalLoad.call(this, name, ...args);
  };
  loaded._compile(code, __filename);
} finally { Module._load = originalLoad; }
const { VsCodeFileGateway } = loaded.exports;

function git(...args) {
  const result = spawnSync('git', args, { cwd: root, encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr || result.error?.message);
}
beforeEach(() => {
  root = fs.mkdtempSync(path.join(os.tmpdir(), 'copy4lm-gateway-'));
  calls = []; infos = []; errors = []; closeCalls = 0; failProbe = false; gitFailure = undefined;
  vscode.workspace.isTrusted = true;
  vscode.workspace.workspaceFolders = [{ name: 'repo', uri: uri(root) }];
  Object.defineProperty(vscode.workspace, 'textDocuments', { configurable: true, value: [] });
  Object.defineProperty(vscode.window.tabGroups, 'all', { configurable: true, value: [] });
});
afterEach(() => fs.rmSync(root, { recursive: true, force: true }));

test('directory batching matches Git including negation, tracked files, newlines and leading dashes', () => {
  git('init', '--quiet');
  fs.writeFileSync(path.join(root, '.gitignore'), '*.log\n!negated.log\n..cache/\n');
  const names = [...Array.from({ length: 180 }, (_, i) => `file-${i}.txt`),
    'ignored.log', 'line\nbreak.log', '--option.log', 'negated.log', 'tracked.log'];
  names.forEach((name) => fs.writeFileSync(path.join(root, name), name));
  fs.mkdirSync(path.join(root, '..cache'));
  git('add', '-f', 'tracked.log');
  const gateway = new VsCodeFileGateway(logger);
  const entries = gateway.childrenOf(ref(root, true));

  for (const entry of entries) {
    const expected = spawnSync('git', ['check-ignore', '--quiet', '--', entry.name], { cwd: root });
    assert.ok(expected.status === 0 || expected.status === 1);
    assert.equal(gateway.isGitIgnored(entry), expected.status === 0, entry.name);
  }
  gateway.childrenOf(ref(root, true));
  assert.equal(calls.length, 1, 'one subprocess per directory, cached on repeated lookup');
  assert.equal(gateway.isGitIgnored(ref(path.join(root, 'tracked.log'))), false);
  assert.equal(gateway.isGitIgnored(ref(path.join(root, 'negated.log'))), false);
  assert.equal(gateway.isGitIgnored(ref(path.join(root, '..cache'), true)), true);
  assert.deepEqual(errors, []);
});

test('nested ignore rules and multiple workspace roots use separate batches', () => {
  const firstRoot = root;
  git('init', '--quiet');
  fs.writeFileSync(path.join(root, '.gitignore'), '*.log\n');
  const nested = path.join(root, 'nested');
  fs.mkdirSync(nested);
  fs.writeFileSync(path.join(nested, '.gitignore'), '!keep.log\n');
  fs.writeFileSync(path.join(nested, 'keep.log'), 'keep');
  fs.writeFileSync(path.join(nested, 'skip.log'), 'skip');
  const secondRoot = path.join(root, 'second');
  fs.mkdirSync(secondRoot);
  root = secondRoot;
  git('init', '--quiet');
  fs.writeFileSync(path.join(root, '.gitignore'), 'skip.txt\n');
  fs.writeFileSync(path.join(root, 'skip.txt'), 'skip');
  root = firstRoot;
  vscode.workspace.workspaceFolders.unshift({ name: 'second', uri: uri(secondRoot) });
  const gateway = new VsCodeFileGateway(logger);
  const files = [ref(path.join(nested, 'keep.log')), ref(path.join(nested, 'skip.log')),
    ref(path.join(secondRoot, 'skip.txt'))];

  gateway.prepareGitIgnore(files);

  assert.deepEqual(files.map((file) => gateway.isGitIgnored(file)), [false, true, true]);
  assert.equal(calls.length, 2);
});

test('disabled and untrusted ignore evaluation never spawn Git', () => {
  const file = ref(path.join(root, 'a.txt'));
  fs.writeFileSync(file.path, 'a');
  const disabled = new VsCodeFileGateway(logger, false);
  disabled.childrenOf(ref(root, true));
  assert.equal(disabled.isGitIgnored(file), false);
  vscode.workspace.isTrusted = false;
  const untrusted = new VsCodeFileGateway(logger);
  untrusted.childrenOf(ref(root, true));
  assert.equal(untrusted.isGitIgnored(file), false);
  assert.equal(calls.length, 0);
  assert.equal(infos.length, 1);
});

test('Git failure falls back once per root and ignores partial stdout', () => {
  const file = ref(path.join(root, 'a.txt'));
  const another = ref(path.join(root, 'b.txt'));
  gitFailure = { status: null, error: Object.assign(new Error('timeout'), { code: 'ETIMEDOUT' }),
    stdout: 'a.txt\0', stderr: '' };
  const gateway = new VsCodeFileGateway(logger);
  gateway.prepareGitIgnore([file]);
  assert.equal(gateway.isGitIgnored(file), false);
  assert.equal(gateway.isGitIgnored(another), false);
  assert.equal(calls.length, 1);
  assert.equal(errors.length, 1);
});

test('paths outside a workspace are not passed to Git', () => {
  const gateway = new VsCodeFileGateway(logger);
  assert.equal(gateway.isGitIgnored(ref(path.join(root, '..', 'outside.txt'))), false);
  assert.equal(calls.length, 0);
});

test('binary probes reuse storage without inspecting stale bytes and close descriptors after failure', () => {
  const binary = ref(path.join(root, 'binary'));
  const text = ref(path.join(root, 'text'));
  fs.writeFileSync(binary.path, Buffer.from([65, 65, 0]));
  fs.writeFileSync(text.path, 'A');
  const gateway = new VsCodeFileGateway(logger, false);
  assert.equal(gateway.isBinary(binary), true);
  assert.equal(gateway.isBinary(text), false);
  failProbe = true;
  assert.equal(gateway.isBinary(text), false);
  assert.equal(closeCalls, 3);
  assert.equal(errors.length, 1);
});

test('editor lookup snapshots preserve strict memory reads and diff editor contents', () => {
  const open = ref(path.join(root, 'open.txt'));
  const closed = ref(path.join(root, 'closed.txt'));
  fs.writeFileSync(open.path, 'open disk');
  fs.writeFileSync(closed.path, 'closed disk');
  let documentScans = 0;
  let tabScans = 0;
  Object.defineProperty(vscode.workspace, 'textDocuments', { configurable: true, get() {
    documentScans++;
    return [open, closed].map((file) => ({ uri: uri(file.path), getText: () => `${file.name} editor` }));
  } });
  Object.defineProperty(vscode.window.tabGroups, 'all', { configurable: true, get() {
    tabScans++;
    return [{ tabs: [{ input: new TabInputTextDiff(uri(open.path)) }] }];
  } });
  const gateway = new VsCodeFileGateway(logger, false);
  assert.equal(gateway.readText(open, true), 'open.txt editor');
  assert.equal(gateway.readText(closed, true), 'closed disk');
  assert.equal(gateway.readText(closed, false), 'closed.txt editor');
  assert.equal(documentScans, 1);
  assert.equal(tabScans, 1);
});
