import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    setupFiles: ['./src/test-setup.ts'],
    pool: 'threads',
    poolOptions: {
      threads: {
        singleThread: true,
        maxThreads: 1,
        minThreads: 1,
      },
    },
    testTimeout: 15000,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        '**/node_modules/**',
        '**/src/app/core/api/generated/**',
        '**/*.spec.ts',
        '**/test-setup.ts',
      ],
      thresholds: {
        lines: 75,
        branches: 65,
        functions: 70,
        statements: 65,
      },
    },
  },
});
