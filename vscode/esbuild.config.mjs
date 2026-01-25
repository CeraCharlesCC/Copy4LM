import { build, context } from 'esbuild';

const watch = process.argv.includes('--watch');

const options = {
  entryPoints: ['src/extension.ts'],
  bundle: true,
  platform: 'node',
  format: 'cjs',
  sourcemap: true,
  outfile: 'dist/extension.js',
  external: ['vscode', 'copy4lm-common'],
  tsconfig: 'tsconfig.json',
  logLevel: 'info'
};

if (watch) {
  const ctx = await context(options);
  await ctx.watch();
  console.log('Watching for changes...');
} else {
  await build(options);
}
