import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { authApi } from '@/api/authApi';
import { meApi } from '@/api/meApi';
import { AUTH_LOGOUT_EVENT } from '@/api/axiosClient';
import { claimsToUser, decodeClaims, isExpired } from './jwt';
import { tokenStorage } from './tokenStorage';
import type { AuthUser } from './types';
import type { MenuItem, Permission, Role } from '@/types/domain';

export interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  /** Permission codes for the signed-in user, from GET /me. Empty until that resolves. */
  permissions: Permission[];
  /** Menus this user's role may see, ordered. Empty until /me resolves. */
  menus: MenuItem[];
  /** True once /me has been attempted, so callers can distinguish "loading" from "denied". */
  isProfileLoaded: boolean;
  /**
   * Signs in with an identifier (email or mobile number) and password, verified server-side
   * against app_users (see DbLocalCredentialAuthenticator). Chapter and role come from the
   * account record, so there is nothing else for a sign-in screen to ask for.
   */
  login: (identifier: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (role: Role | Role[]) => boolean;
  /** The authorization check. Prefer this over hasRole everywhere. */
  can: (permission: Permission | Permission[]) => boolean;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function loadUserFromStorage(): AuthUser | null {
  const token = tokenStorage.get();
  if (!token) return null;
  const claims = decodeClaims(token);
  if (!claims || isExpired(claims)) {
    tokenStorage.clear();
    return null;
  }
  return claimsToUser(claims);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [menus, setMenus] = useState<MenuItem[]>([]);
  const [isProfileLoaded, setIsProfileLoaded] = useState(false);

  useEffect(() => {
    setUser(loadUserFromStorage());
    setIsInitializing(false);
  }, []);

  // Whenever there is a signed-in user (fresh login, or a token restored from storage on
  // refresh), pull their menus/permissions. Deliberately keyed on the user id rather than the
  // user object so a re-render doesn't refetch.
  useEffect(() => {
    if (!user) {
      setPermissions([]);
      setMenus([]);
      setIsProfileLoaded(false);
      return;
    }

    let cancelled = false;
    meApi
      .get()
      .then((me) => {
        if (cancelled) return;
        setPermissions(me.permissions ?? []);
        setMenus(me.menus ?? []);
      })
      .catch(() => {
        // A failed /me leaves the user authenticated but with no permissions, so the UI degrades
        // to showing nothing rather than wrongly showing everything. The JWT still governs what
        // the API will actually accept.
        if (cancelled) return;
        setPermissions([]);
        setMenus([]);
      })
      .finally(() => {
        if (!cancelled) setIsProfileLoaded(true);
      });

    return () => {
      cancelled = true;
    };
  }, [user?.id]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const onForcedLogout = () => setUser(null);
    window.addEventListener(AUTH_LOGOUT_EVENT, onForcedLogout);
    return () => window.removeEventListener(AUTH_LOGOUT_EVENT, onForcedLogout);
  }, []);

  const login = useCallback(async (identifier: string, password: string) => {
    const { accessToken } = await authApi.login({ identifier, password });
    const claims = decodeClaims(accessToken);
    if (!claims) {
      throw new Error('Received an unreadable token from the server.');
    }
    tokenStorage.set(accessToken);
    setUser(claimsToUser(claims));
  }, []);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
  }, []);

  const hasRole = useCallback(
    (role: Role | Role[]) => {
      if (!user) return false;
      const required = Array.isArray(role) ? role : [role];
      return required.some((r) => user.roles.includes(r));
    },
    [user],
  );

  const can = useCallback(
    (permission: Permission | Permission[]) => {
      const required = Array.isArray(permission) ? permission : [permission];
      // Any-of, not all-of: call sites that need every one of a set should call can() per item.
      return required.some((code) => permissions.includes(code));
    },
    [permissions],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      isInitializing,
      permissions,
      menus,
      isProfileLoaded,
      login,
      logout,
      hasRole,
      can,
    }),
    [user, isInitializing, permissions, menus, isProfileLoaded, login, logout, hasRole, can],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
