const assert = require('node:assert/strict');
const { test } = require('node:test');
const { io: { github: { ceracharlescc: { copy4lm: api } } } } = require('copy4lm-common');

const options = {
  headerFormat: 'H:$FILE_PATH', footerFormat: 'F', preText: '', postText: '',
  fileCountLimit: 30, setMaxFileCount: true, filenameFilters: [], useFilenameFilters: false,
  respectGitIgnore: true, addExtraLineBetweenFiles: false, strictMemoryRead: true,
  maxFileSizeKB: 500, projectName: 'project'
};
const files = ['a.txt', 'b.txt'].map((name) => ({ name, path: `/repo/${name}`, isDirectory: false }));
const gateway = {
  childrenOf: () => [], readText: (file) => file.name === 'a.txt' ? 'one\ntwo;' : null,
  isBinary: () => false, sizeBytes: () => 10,
  relativePath: (file) => file.name, isGitIgnored: () => false
};

test('facade returns compiler-exported domain results with content-only statistics', () => {
  const result = api.copyFiles(files, options, gateway);
  assert.ok(result instanceof api.domain.vo.CopyResult);
  assert.ok(result.stats instanceof api.domain.vo.CopyStats);
  assert.equal(result.clipboardText, 'H:a.txt\none\ntwo;\nF');
  assert.equal(result.copiedFileCount, 1);
  assert.equal(result.failedFileCount, 1);
  assert.equal(result.stats.totalWords, 2);
  assert.equal(result.stats.totalTokens, 3);
  assert.equal(result.stats.totalLines, 2);
});

test('tree templates exclude unreadable files and leave literal file contents untouched', () => {
  const result = api.copyFiles(files, { ...options, preText: '$DIRECTORY_STRUCTURE' }, gateway);
  assert.ok(result.clipboardText.includes('a.txt'));
  assert.equal(result.clipboardText.includes('b.txt'), false);
  const literal = api.copyFiles([files[0]], options,
    { ...gateway, readText: () => '$PROJECT_NAME $DIRECTORY_STRUCTURE' });
  assert.equal(literal.clipboardText, 'H:a.txt\n$PROJECT_NAME $DIRECTORY_STRUCTURE\nF');
});

test('directory facade returns the exported result type and applies filters', () => {
  const result = api.copyDirectoryStructure(files,
    { ...options, useFilenameFilters: true, filenameFilters: ['a.txt'] }, gateway);
  assert.ok(result instanceof api.domain.vo.DirectoryStructureResult);
  assert.equal(result.collectedFileCount, 1);
  assert.ok(result.clipboardText.includes('a.txt'));
  assert.equal(result.clipboardText.includes('b.txt'), false);
});

test('JS statistics agree on Unicode whitespace and punctuation', () => {
  const text = ' \talpha\r\nbeta\u00a0gamma\u3000delta;{()}[],\n';
  const stats = new api.domain.vo.CopyStats().plus(text);
  assert.equal(stats.totalChars, text.length);
  assert.equal(stats.totalLines, 3);
  assert.equal(stats.totalWords, 4);
  assert.equal(stats.totalTokens, 12);
});
