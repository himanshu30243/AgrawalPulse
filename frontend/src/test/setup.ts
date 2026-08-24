import '@testing-library/jest-dom/vitest';
import { afterAll, afterEach, beforeAll, beforeEach } from 'vitest';
import { server } from '@/mocks/server';
import '@/i18n/i18n';

// jsdom's built-in localStorage is broken under this Node/jsdom combination (Node's own
// experimental webstorage implementation takes over and throws "getItem is not a function"
// without a --localstorage-file path configured) - a minimal in-memory polyfill sidesteps the
// version-compatibility issue entirely rather than depending on it being fixed upstream.
class MemoryStorage implements Storage {
  private store = new Map<string, string>();
  get length() {
    return this.store.size;
  }
  clear(): void {
    this.store.clear();
  }
  getItem(key: string): string | null {
    return this.store.has(key) ? (this.store.get(key) as string) : null;
  }
  key(index: number): string | null {
    return Array.from(this.store.keys())[index] ?? null;
  }
  removeItem(key: string): void {
    this.store.delete(key);
  }
  setItem(key: string, value: string): void {
    this.store.set(key, String(value));
  }
}

Object.defineProperty(globalThis, 'localStorage', {
  value: new MemoryStorage(),
  writable: true,
});

beforeEach(() => localStorage.clear());

// Every component test runs against the same MSW handlers/fixtures used by dev:mock (see
// src/mocks/handlers.ts) - a component test is really "does the GUI render/behave correctly
// against a realistic fake backend", not "does it render with props I hand-crafted", so it
// catches things like a wrong field name in an API response mapping.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
