import { apiClient } from './axiosClient';
import type { CreateFamilyMemberRequest, CreateFamilyRequest, Family, FamilyMember } from '@/types/domain';

export const familiesApi = {
  async list(): Promise<Family[]> {
    const { data } = await apiClient.get<Family[]>('/families');
    return data;
  },
  async get(familyId: string): Promise<Family> {
    const { data } = await apiClient.get<Family>(`/families/${familyId}`);
    return data;
  },
  async register(request: CreateFamilyRequest): Promise<Family> {
    const { data } = await apiClient.post<Family>('/families', request);
    return data;
  },
  async listMembers(familyId: string): Promise<FamilyMember[]> {
    const { data } = await apiClient.get<FamilyMember[]>(`/families/${familyId}/members`);
    return data;
  },
  async addMember(familyId: string, request: CreateFamilyMemberRequest): Promise<FamilyMember> {
    const { data } = await apiClient.post<FamilyMember>(`/families/${familyId}/members`, request);
    return data;
  },
  async uploadPhoto(familyId: string, file: File): Promise<void> {
    const formData = new FormData();
    formData.append('file', file);
    // apiClient's instance default Content-Type is application/json - a FormData body needs the
    // multipart type set explicitly, axios won't switch it automatically for us.
    await apiClient.post(`/families/${familyId}/photo`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  async getPhotoUrl(familyId: string): Promise<string | null> {
    try {
      const { data } = await apiClient.get(`/families/${familyId}/photo`, { responseType: 'blob' });
      return URL.createObjectURL(data as Blob);
    } catch {
      // No photo uploaded (404) or otherwise unavailable - callers show a placeholder instead.
      return null;
    }
  },
};
