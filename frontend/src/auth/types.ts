import type { Role } from '@/types/domain';

/**
 * Matches the claim shape the backend's local-auth token issuer (and Cognito, in the
 * dev/staging/prod profiles) actually produces - see
 * backend/common/security/local/LocalTokenController.java. Deliberately does NOT include
 * `name`/`chapter_name`: earlier versions of this type assumed those claims existed and they
 * never have (only sub, chapter_id, roles, email, iat, exp are ever issued) - AuthUser derives
 * a display name from email instead, and chapter/branch display info is resolved separately via
 * branchesApi where needed (e.g. AppBarTop), not trusted from the token.
 */
export interface JwtClaims {
  sub: string;
  email: string;
  chapter_id: string;
  roles: Role[];
  role_id: string;
  role_name: string;
  iat: number;
  exp: number;
}

export interface AuthUser {
  id: string;
  email: string;
  chapterId: string;
  roles: Role[];
}

// Matches backend/common's LocalTokenRequest/LocalTokenResponse exactly (POST
// /api/v1/local-auth/token) - the "local" profile's stand-in for Cognito. identifier/password are
// verified for real against app_users (BCrypt compare - see DbLocalCredentialAuthenticator); this
// is no longer passwordless. In dev/staging/prod this whole request/response shape is replaced by
// the Cognito Hosted UI/SDK flow.
export interface LocalLoginRequest {
  identifier: string;
  password: string;
}

export interface LocalLoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}
