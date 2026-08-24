const TOKEN_KEY = 'agrawalpulse_token';

/**
 * Isolated from AuthContext so the axios interceptor can read the token
 * without importing React context code (avoids a circular import between
 * the API layer and the auth layer).
 */
export const tokenStorage = {
  get(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },
  set(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
  },
  clear(): void {
    localStorage.removeItem(TOKEN_KEY);
  },
};
