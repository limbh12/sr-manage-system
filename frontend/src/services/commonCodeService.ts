import api from './api';
import { CommonCode } from '../types';

export const commonCodeService = {
  getCodeGroups: async (): Promise<string[]> => {
    const response = await api.get<string[]>('/common-codes/groups');
    return response.data;
  },

  getCodesByGroup: async (codeGroup: string): Promise<CommonCode[]> => {
    const response = await api.get<CommonCode[]>(`/common-codes/${codeGroup}`);
    return response.data;
  },

  getActiveCodesByGroup: async (codeGroup: string): Promise<CommonCode[]> => {
    const response = await api.get<CommonCode[]>(`/common-codes/${codeGroup}/active`);
    return response.data;
  },

  createCode: async (code: Partial<CommonCode>): Promise<CommonCode> => {
    const response = await api.post<CommonCode>('/common-codes', code);
    return response.data;
  },

  updateCode: async (id: number, code: Partial<CommonCode>): Promise<CommonCode> => {
    const response = await api.put<CommonCode>(`/common-codes/${id}`, code);
    return response.data;
  },

  deleteCode: async (id: number): Promise<void> => {
    await api.delete(`/common-codes/${id}`);
  },

  reorderCodes: async (codes: CommonCode[]): Promise<void> => {
    await api.put('/common-codes/reorder', codes);
  },
};
