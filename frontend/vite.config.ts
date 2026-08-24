import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { fileURLToPath, URL } from 'node:url';

// __dirname isn't available here — this file runs as ESM (package.json
// "type": "module"), so the alias is resolved from import.meta.url instead.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: true,
    // Default 5000ms is too tight here: userEvent.type() simulates real keystrokes one at a time,
    // and a form with several fields plus a real async submit-and-wait can legitimately exceed it
    // on a loaded machine (this isn't masking a hang - MSW handlers respond immediately).
    testTimeout: 15000,
    // Vite does not load .env.local when mode is "test" (vitest's default) - without this,
    // apiClient's baseURL is undefined during tests, so every request is missing "/api/v1"
    // entirely and silently never matches an MSW handler (hangs until the test times out,
    // rather than failing with a clear error). Set explicitly here instead of depending on an
    // env file that's deliberately excluded from this mode.
    env: {
      VITE_API_BASE_URL: '/api/v1',
    },
  },
  server: {
    port: 5173,
    // Mirrors nginx/local-gateway.conf's routing table (used by docker-compose) so `npm run dev`
    // works identically without Docker: each service still runs on its own port, this just
    // avoids needing an actual gateway in front of them for local frontend development. Keep
    // this in sync with nginx/local-gateway.conf and docs/microservices-contract.md if a new
    // service/route is added.
    proxy: {
      '/api/v1/local-auth': 'http://localhost:8081',
      '/api/v1/me': 'http://localhost:8081',
      '/api/v1/users': 'http://localhost:8081',
      '/api/v1/chapters': 'http://localhost:8081',
      '/api/v1/roles': 'http://localhost:8081',
      '/api/v1/permissions': 'http://localhost:8081',
      '/api/v1/menus': 'http://localhost:8081',
      '/api/v1/families': 'http://localhost:8082',
      '/api/v1/memberships': 'http://localhost:8083',
      '/api/v1/matrimony': 'http://localhost:8084',
      '/api/v1/events': 'http://localhost:8085',
      '/api/v1/analytics': 'http://localhost:8086',
    },
  },
});
