import { apiClient } from './axiosClient';
import type { Gender } from '@/types/domain';

// Backs GET /api/v1/users/me (user-service's self-only profile endpoint) - the rest of the
// caller's own account beyond the JWT's minimal claims (see auth/types.ts's AuthUser). Used by
// the family registration wizard to prefill the head-of-family step from what was already
// captured at sign-up, instead of asking the user to retype their own name/DOB/gender/mobile/email.
export interface OwnProfile {
  id: string;
  chapterId: string;
  firstName: string | null;
  middleName: string | null;
  lastName: string | null;
  dateOfBirth: string | null; // YYYY-MM-DD
  gender: Gender | null;
  mobileNumber: string | null;
  email: string;
}

export const usersApi = {
  async getMe(): Promise<OwnProfile> {
    const { data } = await apiClient.get<OwnProfile>('/users/me');
    return data;
  },
};
