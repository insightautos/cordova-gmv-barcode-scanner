/** @type {import('@teckel.ts-internal/eslint-config/Types').IEslintConfig} */
const config = {
  env: { node: true },
  extends: '@teckel.ts-internal/eslint-config',
  parserOptions: {
    project: './tsconfig.lint.json',
    tsconfigRootDir: __dirname,
  },
};

module.exports = config;
