import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import { useAuth } from '@/auth/useAuth';
import type { Permission, Role } from '@/types/domain';

interface ProtectedRouteProps {
  children: ReactNode;
  /**
   * Permission codes required to enter (any-of). This is the preferred gate: it survives new
   * roles being added in the database, because it never names a role.
   */
  permission?: Permission | Permission[];
  /**
   * Role codes required to enter (any-of). Escape hatch for the rare gate that genuinely is about
   * identity rather than capability - prefer `permission`.
   */
  roles?: Role[];
}

export function ProtectedRoute({ children, permission, roles }: ProtectedRouteProps) {
  const { isAuthenticated, isInitializing, isProfileLoaded, hasRole, can } = useAuth();
  const location = useLocation();

  if (isInitializing) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Permissions arrive from GET /me a moment after the token does. Without this wait a
  // permission-gated route would bounce the user to /dashboard on every hard refresh, which is
  // exactly the "clicking does nothing" class of bug this route guard caused before.
  if (permission && !isProfileLoaded) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (permission && !can(permission)) {
    return <Navigate to="/dashboard" replace />;
  }

  if (roles && !hasRole(roles)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}
