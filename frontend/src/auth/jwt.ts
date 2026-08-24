import { jwtDecode } from 'jwt-decode';
import type { AuthUser, JwtClaims } from './types';

export function decodeClaims(token: string): JwtClaims | null {
  try {
    return jwtDecode<JwtClaims>(token);
  } catch {
    return null;
  }
}

export function isExpired(claims: JwtClaims): boolean {
  return claims.exp * 1000 <= Date.now();
}

export function claimsToUser(claims: JwtClaims): AuthUser {
  return {
    id: claims.sub,
    email: claims.email,
    chapterId: claims.chapter_id,
    roles: claims.roles ?? [],
  };
}
