const nx = require('@nx/eslint-plugin');
const tseslint = require('typescript-eslint');

module.exports = [
  ...nx.configs['flat/base'],
  ...tseslint.configs.recommended,
  {
    ignores: ['**/dist/**', '**/node_modules/**', '**/.angular/**', '**/target/**'],
  },
  {
    files: ['**/*.ts'],
    rules: {
      '@nx/enforce-module-boundaries': [
        'error',
        {
          enforceBuildableLibDependency: false,
          allow: [],
          depConstraints: [
            {
              sourceTag: 'scope:app',
              onlyDependOnLibsWithTags: [
                'scope:core',
                'scope:shared',
                'scope:public',
                'scope:academico',
                'scope:estudiante',
                'scope:apoderado',
                'scope:docente',
                'scope:admin',
              ],
            },
            {
              sourceTag: 'scope:public',
              onlyDependOnLibsWithTags: ['scope:core', 'scope:shared'],
            },
            {
              sourceTag: 'scope:estudiante',
              onlyDependOnLibsWithTags: ['scope:core', 'scope:shared', 'scope:academico'],
            },
            {
              sourceTag: 'scope:apoderado',
              onlyDependOnLibsWithTags: ['scope:core', 'scope:shared', 'scope:academico'],
            },
            {
              sourceTag: 'scope:docente',
              onlyDependOnLibsWithTags: ['scope:core', 'scope:shared', 'scope:academico'],
            },
            {
              sourceTag: 'scope:admin',
              onlyDependOnLibsWithTags: ['scope:core', 'scope:shared', 'scope:academico'],
            },
            {
              sourceTag: 'scope:academico',
              onlyDependOnLibsWithTags: ['scope:core', 'scope:shared'],
            },
            {
              sourceTag: 'scope:shared',
              onlyDependOnLibsWithTags: ['scope:core'],
            },
            {
              sourceTag: 'scope:core',
              onlyDependOnLibsWithTags: [],
            },
          ],
        },
      ],
    },
  },
];
