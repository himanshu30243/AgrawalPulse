import { apiClient } from './axiosClient';
import type { BranchSummary } from '@/types/domain';

// Calls user-service's /chapters endpoint (unchanged path there - see BranchSummary's comment
// in types/domain.ts for why the display name differs from the underlying entity name).
export const branchesApi = {
  async list(): Promise<BranchSummary[]> {
    const { data } = await apiClient.get<BranchSummary[]>('/chapters');
    return data;
  },
};
