import { apiClient } from './axiosClient';
import type {
  CreateFamilyMemberRequest,
  CreateFamilyRequest,
  Family,
  FamilyMember,
  PincodeLookupResult,
  UpdateFamilyRequest,
} from '@/types/domain';

export interface FamilySearchFilters {
  headOfFamilyName?: string;
  mobileNumber?: string;
  areaLocality?: string;
}

export const familiesApi = {
  // filters are optional query params applied server-side within the caller's own read scope
  // (see family-service's FamilyController#listFamilies) - omitting them keeps the original
  // "every visible family" behavior every existing caller relies on.
  async list(filters?: FamilySearchFilters): Promise<Family[]> {
    const { data } = await apiClient.get<Family[]>('/families', { params: filters });
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
  async update(familyId: string, request: UpdateFamilyRequest): Promise<Family> {
    const { data } = await apiClient.put<Family>(`/families/${familyId}`, request);
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
  // 404 (unknown PIN code) and any other failure (upstream down, network error) both resolve to
  // null here rather than throwing - callers fall back to the manual State/District dropdowns in
  // either case, so there's no reason to distinguish them at the call site.
  async lookupPincode(pincode: string): Promise<PincodeLookupResult | null> {
    try {
      const { data } = await apiClient.get<PincodeLookupResult>(`/families/pincode/${pincode}`);
      return data;
    } catch {
      return null;
    }
  },
};
