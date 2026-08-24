import { apiClient } from './axiosClient';
import type { MenuItem, Permission, Role } from '@/types/domain';

// Administration APIs backed by user-service's RbacController / UserController / ChapterController.
// Every one of these is gated server-side on a permission (MANAGE_ROLES / MANAGE_USERS /
// MANAGE_BRANCHES); the UI hides what the caller can't use, but the server is the actual gate.

export interface RoleDetail {
  roleId: string;
  roleCode: Role;
  roleName: string;
  description: string | null;
  active: boolean;
  permissionCodes: Permission[];
  menuKeys: string[];
  createdAt: string;
  updatedAt: string;
}

export interface PermissionDetail {
  permissionId: string;
  permissionCode: Permission;
  permissionName: string;
  description: string | null;
}

export interface AdminUser {
  id: string;
  chapterId: string;
  firstName: string | null;
  middleName: string | null;
  lastName: string | null;
  dateOfBirth: string | null;
  gender: 'MALE' | 'FEMALE' | 'OTHER' | null;
  mobileNumber: string | null;
  email: string;
  cognitoSub: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  role: { roleId: string; roleCode: Role; roleName: string } | null;
  createdAt: string;
  updatedAt: string;
}

export interface Chapter {
  id: string;
  name: string;
  city: string;
  state: string;
  createdAt: string;
}

export const rolesApi = {
  async list(): Promise<RoleDetail[]> {
    const { data } = await apiClient.get<RoleDetail[]>('/roles');
    return data;
  },
  async create(request: { roleCode: string; roleName: string; description?: string }): Promise<RoleDetail> {
    const { data } = await apiClient.post<RoleDetail>('/roles', request);
    return data;
  },
  async update(roleId: string, request: { roleName: string; description?: string }): Promise<RoleDetail> {
    const { data } = await apiClient.put<RoleDetail>(`/roles/${roleId}`, request);
    return data;
  },
  async setActive(roleId: string, active: boolean): Promise<RoleDetail> {
    const { data } = await apiClient.put<RoleDetail>(`/roles/${roleId}/active`, { active });
    return data;
  },
  // Wholesale replacement, matching the backend - send the full desired set, not a delta.
  async setPermissions(roleId: string, permissionCodes: Permission[]): Promise<RoleDetail> {
    const { data } = await apiClient.put<RoleDetail>(`/roles/${roleId}/permissions`, { permissionCodes });
    return data;
  },
  async setMenus(roleId: string, menuKeys: string[]): Promise<RoleDetail> {
    const { data } = await apiClient.put<RoleDetail>(`/roles/${roleId}/menus`, { menuKeys });
    return data;
  },
};

export const permissionsApi = {
  async list(): Promise<PermissionDetail[]> {
    const { data } = await apiClient.get<PermissionDetail[]>('/permissions');
    return data;
  },
};

export const menusApi = {
  async list(): Promise<MenuItem[]> {
    const { data } = await apiClient.get<MenuItem[]>('/menus');
    return data;
  },
};

export const adminUsersApi = {
  async list(): Promise<AdminUser[]> {
    const { data } = await apiClient.get<AdminUser[]>('/users');
    return data;
  },
  // A user holds exactly one role since user-service's V2 migration - this replaces the previous
  // PUT /users/{id}/roles that took a set.
  async setRole(userId: string, roleCode: string): Promise<AdminUser> {
    const { data } = await apiClient.put<AdminUser>(`/users/${userId}/role`, { roleCode });
    return data;
  },
};

export const chaptersApi = {
  async list(): Promise<Chapter[]> {
    const { data } = await apiClient.get<Chapter[]>('/chapters');
    return data;
  },
  async create(request: { name: string; city: string; state: string }): Promise<Chapter> {
    const { data } = await apiClient.post<Chapter>('/chapters', request);
    return data;
  },
  async update(chapterId: string, request: { name: string; city: string; state: string }): Promise<Chapter> {
    const { data } = await apiClient.put<Chapter>(`/chapters/${chapterId}`, request);
    return data;
  },
  // No delete: chapter_id is the tenant boundary five other services key off, so removing one
  // would orphan their rows (see ChapterController's comment).
};
