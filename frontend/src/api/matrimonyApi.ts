import { apiClient } from './axiosClient';
import type { ConsentScope, MatrimonyConsent, MatrimonyProfile } from '@/types/domain';

export const matrimonyApi = {
  async getMyConsent(): Promise<MatrimonyConsent> {
    const { data } = await apiClient.get<MatrimonyConsent>('/matrimony/consent/me');
    return data;
  },
  async setConsent(
    consentGiven: boolean,
    consentScope: ConsentScope | null,
  ): Promise<MatrimonyConsent> {
    const { data } = await apiClient.put<MatrimonyConsent>('/matrimony/consent/me', {
      consentGiven,
      consentScope,
    });
    return data;
  },
  async requestErasure(): Promise<void> {
    await apiClient.post('/matrimony/consent/me/erasure-request');
  },
  async getDirectory(): Promise<MatrimonyProfile[]> {
    const { data } = await apiClient.get<MatrimonyProfile[]>('/matrimony/directory');
    return data;
  },
};
