import { apiClient } from './axiosClient';
import type { AnalyticsSummary } from '@/types/domain';

export const analyticsApi = {
  async getSummary(): Promise<AnalyticsSummary> {
    const { data } = await apiClient.get<AnalyticsSummary>('/analytics/summary');
    return data;
  },
};
