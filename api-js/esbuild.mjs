import * as esbuild from 'esbuild'
import { nodeModulesPolyfillPlugin } from 'esbuild-plugins-node-modules-polyfill';

await esbuild.build({
    entryPoints: ['src/core.ts'],
    bundle: true,
    minify: true,
    sourcemap: true,
    target: 'es6',
    format: 'iife',
    globalName: 'Sepajs',
    outfile: 'web/sepa.js',
    plugins: [nodeModulesPolyfillPlugin()],
})