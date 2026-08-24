import { apiClient } from './axiosClient';
import type { Gender } from '@/types/domain';

// Backs POST /api/v1/users/register (user-service's UserController.selfRegister - permitAll,
// see SecurityConfig, since a brand-new account has no JWT yet). No role field: the server always
// assigns USER (see RegisterUserRequest's comment) - a registrant cannot elect their own role.
export interface SelfRegisterRequest {
  firstName: string;
  middleName?: string;
  lastName: string;
  dateOfBirth: string; // YYYY-MM-DD
  gender: Gender;
  mobileNumber: string;
  emailAddress: string;
  password: string;
}

export interface SelfRegisterResponse {
  id: string;
  chapterId: string;
  firstName: string | null;
  middleName: string | null;
  lastName: string | null;
  email: string;
  createdAt: string;
}

export const registrationApi = {
  async register(request: SelfRegisterRequest): Promise<SelfRegisterResponse> {
    const { data } = await apiClient.post<SelfRegisterResponse>('/users/register', request);
    return data;
  },
};
