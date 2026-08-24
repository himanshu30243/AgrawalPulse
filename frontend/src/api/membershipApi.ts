import { apiClient } from './axiosClient';
import type {
  ChapterCollectionSummary,
  MembershipPayment,
  MembershipSummary,
} from '@/types/domain';

export const membershipApi = {
  async getStatus(familyId: string): Promise<MembershipSummary> {
    const { data } = await apiClient.get<MembershipSummary>(
      `/memberships/${familyId}`,
    );
    return data;
  },
  async getPaymentHistory(familyId: string): Promise<MembershipPayment[]> {
    const { data } = await apiClient.get<MembershipPayment[]>(
      `/memberships/${familyId}/payments`,
    );
    return data;
  },
  async renew(familyId: string): Promise<MembershipSummary> {
    const { data } = await apiClient.post<MembershipSummary>(
      `/memberships/${familyId}/renew`,
    );
    return data;
  },
  async getChapterCollection(): Promise<ChapterCollectionSummary[]> {
    const { data } = await apiClient.get<ChapterCollectionSummary[]>(
      '/memberships/collection',
    );
    return data;
  },
};
