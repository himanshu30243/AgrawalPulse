import { apiClient } from './axiosClient';
import type {
  MembershipCollectionSummary,
  MembershipReportRow,
  MembershipStatus,
  MembershipStatusSummary,
  MembershipTransaction,
  RecordTransactionRequest,
  UpdateTransactionRequest,
} from '@/types/domain';

export interface PendingPaymentReportFilters {
  financialYear?: number;
  familyId?: string;
  headOfFamilyName?: string;
  mobileNumber?: string;
  areaLocality?: string;
}

export const membershipApi = {
  async getStatus(familyId: string): Promise<MembershipStatusSummary> {
    const { data } = await apiClient.get<MembershipStatusSummary>(
      `/memberships/family/${familyId}/status`,
    );
    return data;
  },
  async getTransactionHistory(familyId: string): Promise<MembershipTransaction[]> {
    const { data } = await apiClient.get<MembershipTransaction[]>(
      `/memberships/family/${familyId}/transactions`,
    );
    return data;
  },
  async recordTransaction(request: RecordTransactionRequest): Promise<MembershipTransaction> {
    const { data } = await apiClient.post<MembershipTransaction>('/memberships/transactions', request);
    return data;
  },
  async updateTransaction(
    transactionId: string,
    request: UpdateTransactionRequest,
  ): Promise<MembershipTransaction> {
    const { data } = await apiClient.put<MembershipTransaction>(
      `/memberships/transactions/${transactionId}`,
      request,
    );
    return data;
  },
  async listMembers(financialYear?: number, status?: MembershipStatus): Promise<MembershipStatusSummary[]> {
    const { data } = await apiClient.get<MembershipStatusSummary[]>('/memberships/members', {
      params: { financialYear, status },
    });
    return data;
  },
  async pendingPaymentReport(filters: PendingPaymentReportFilters): Promise<MembershipReportRow[]> {
    const { data } = await apiClient.get<MembershipReportRow[]>('/memberships/reports/pending', {
      params: filters,
    });
    return data;
  },
  async getCollectionSummary(financialYear?: number): Promise<MembershipCollectionSummary> {
    const { data } = await apiClient.get<MembershipCollectionSummary>(
      '/memberships/reports/collection-summary',
      { params: { financialYear } },
    );
    return data;
  },
};
