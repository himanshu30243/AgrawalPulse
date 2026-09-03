import { Navigate, Route, Routes } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import LoginPage from '@/pages/auth/LoginPage';
import RegistrationPage from '@/pages/auth/RegistrationPage';
import DashboardPage from '@/pages/DashboardPage';
import FamiliesListPage from '@/pages/families/FamiliesListPage';
import FamilyRegistrationWizardPage from '@/pages/families/registration/FamilyRegistrationWizardPage';
import MemberRegistrationPage from '@/pages/families/MemberRegistrationPage';
import MembershipPage from '@/pages/membership/MembershipPage';
import MyConsentPage from '@/pages/matrimony/MyConsentPage';
import MatrimonyDirectoryPage from '@/pages/matrimony/MatrimonyDirectoryPage';
import EventsPage from '@/pages/events/EventsPage';
import NotFoundPage from '@/pages/NotFoundPage';
import ComingSoonPage from '@/pages/ComingSoonPage';
import { AppLayout } from '@/layout/AppLayout';
import RoleManagementPage from '@/pages/admin/RoleManagementPage';
import UserManagementPage from '@/pages/admin/UserManagementPage';
import BranchManagementPage from '@/pages/admin/BranchManagementPage';
import { PERMISSIONS } from '@/types/domain';
import { ProtectedRoute } from './ProtectedRoute';

export function AppRoutes() {
  const { t } = useTranslation();

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegistrationPage />} />

      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<DashboardPage />} />
        {/*
          Admin pages live in the same shell as everything else - the app has exactly one
          layout/sidebar (see NavDrawer, driven by the real menus from GET /me). There used
          to be a second, hand-built "Administration" shell (AdminLayout) with its own
          hardcoded fake signed-in user and a static, non-DB-driven menu list - it rendered
          User/Role/Branch Management behind a jarring separate header instead of alongside
          Families/Membership/Events like every other page. Removed along with the fake
          "Welcome to Admin Panel" dashboard (AdminDashboardPage), which showed hardcoded
          stat numbers (1250 users, 8 roles, ...) that never reflected real data.
        */}
        <Route
          path="/admin/roles"
          element={
            <ProtectedRoute permission={PERMISSIONS.manageRoles}>
              <RoleManagementPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute permission={PERMISSIONS.manageUsers}>
              <UserManagementPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/branches"
          element={
            <ProtectedRoute permission={PERMISSIONS.manageBranches}>
              <BranchManagementPage />
            </ProtectedRoute>
          }
        />
        {/*
          Backend read access for both families and membership is
          hasAnyRole('ADMIN','CHAPTER_ADMIN','TREASURER','MEMBER') - effectively every
          authenticated role - so neither route is role-restricted here. The previous
          CENSUS_VOLUNTEER/MEMBERSHIP_COLLECTOR role gates named roles that don't exist
          anywhere in the backend (see types/domain.ts's Role type). Write-side actions
          within these pages remain gated server-side to ADMIN/CHAPTER_ADMIN/TREASURER.
        */}
        <Route path="/families" element={<FamiliesListPage />} />
        {/*
          Derived from the permission table (auth/permissions.ts) rather than a hand-written role
          array. This route was previously pinned to ['ADMIN','CHAPTER_ADMIN'], which silently
          broke "Register New Family" for members once the product rule changed to let them
          register their own family: the button navigated here, ProtectedRoute rejected the role
          and redirected straight back to /dashboard, so the click looked like a no-op.

          NOTE: family-service still gates POST /families to hasAnyRole('ADMIN','CHAPTER_ADMIN'),
          so a MEMBER reaching this wizard will get a 403 on submit until that @PreAuthorize is
          widened and the one-family cap is enforced server-side.
        */}
        <Route
          path="/families/register"
          element={
            <ProtectedRoute permission={PERMISSIONS.createFamily}>
              <FamilyRegistrationWizardPage />
            </ProtectedRoute>
          }
        />
        {/*
          The registration wizard redirects here on success, so this has to admit everyone who
          can reach the wizard - gating it to ADMIN/CHAPTER_ADMIN while members could create a
          family would dead-end them at /dashboard right after their family was created. Adding
          members to a family you own is an edit of that family, hence 'family:edit:own'.
        */}
        <Route
          path="/member-registration"
          element={
            <ProtectedRoute permission={PERMISSIONS.editFamily}>
              <MemberRegistrationPage />
            </ProtectedRoute>
          }
        />
        {/*
          Previously ungated (unlike every other permission-sensitive route) despite the page
          itself gating admin-only tabs internally via useMembershipPermissions - the route guard
          now matches that same base permission (VIEW_MEMBERSHIP, held by every authenticated
          role) so an unauthenticated deep-link redirects to /login instead of rendering a blank
          "my status" tab with no data.
        */}
        <Route
          path="/membership"
          element={
            <ProtectedRoute permission={PERMISSIONS.viewMembership}>
              <MembershipPage />
            </ProtectedRoute>
          }
        />
        {/*
          Per docs/api-specifications.md, consent (POST/DELETE
          /matrimony/consent) is "self or guardian (MEMBER)" — any
          authenticated user, not MATRIMONY_VIEWER-gated. Only the
          eligible-profiles directory (GET /matrimony/eligible) requires the
          viewer role, so it gets its own ProtectedRoute below.
        */}
        <Route path="/matrimony/consent" element={<MyConsentPage />} />
        <Route
          path="/matrimony/directory"
          element={
            <ProtectedRoute permission={PERMISSIONS.viewMatrimonyDirectory}>
              <MatrimonyDirectoryPage />
            </ProtectedRoute>
          }
        />
        <Route path="/events" element={<EventsPage />} />
        {/*
          "Reports" and "Settings" are seeded menu entries (user-service's V2 migration) with no
          page built behind them yet - without a matching route, clicking either from the sidebar
          fell through to the catch-all NotFoundPage below, which reads as a bug rather than an
          acknowledged gap. ComingSoonPage marks that distinction honestly until each is built.
        */}
        <Route
          path="/reports"
          element={
            <ProtectedRoute permission={PERMISSIONS.viewReports}>
              <ComingSoonPage title={t('nav.reports')} />
            </ProtectedRoute>
          }
        />
        <Route path="/settings" element={<ComingSoonPage title={t('nav.settings')} />} />
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
