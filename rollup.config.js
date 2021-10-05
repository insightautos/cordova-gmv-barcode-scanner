import commonjs from '@rollup/plugin-commonjs';
import { nodeResolve } from '@rollup/plugin-node-resolve';
import typescript from 'rollup-plugin-typescript2';

/** @type {import('rollup').RollupOptions} */
const options = {
  input: ['./src-typescript/BarcodeScanner.ts'],
  output: {
    dir: './www/',
    format: 'cjs',
    sourcemap: 'inline',
  },
  external: ['cordova'],
  plugins: [
    nodeResolve({ browser: true }),
    commonjs(),
    typescript({
      tsconfig: './tsconfig.dist.json',
    }),
  ],
};

export default options;
