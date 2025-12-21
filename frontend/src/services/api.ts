import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { getAccessToken, getRefreshToken, setAccessToken, setRefreshToken, clearTokens } from '../utils/tokenUtils';
import { toast } from '../utils/toast';

// API 기본 URL
const API_BASE_URL = '/api';

// Axios 인스턴스 생성
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: Access Token 추가
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터: 토큰 갱신 처리
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // 401 에러이고 재시도하지 않은 경우
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = getRefreshToken();
      if (refreshToken) {
        try {
          // 토큰 갱신 요청
          const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken,
          });

          const { accessToken, refreshToken: newRefreshToken } = response.data;
          setAccessToken(accessToken);
          setRefreshToken(newRefreshToken);

          // 원래 요청 재시도
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          }
          return api(originalRequest);
        } catch {
          // 갱신 실패 시 로그아웃
          toast.warning('세션이 만료되었습니다. 다시 로그인해주세요.');
          setTimeout(() => {
            clearTokens();
            window.location.href = '/login';
          }, 1500);
          return new Promise(() => {}); // 리다이렉트 전까지 대기
        }
      } else {
        toast.warning('로그인이 필요합니다.');
        setTimeout(() => {
          clearTokens();
          window.location.href = '/login';
        }, 1500);
        return new Promise(() => {}); // 리다이렉트 전까지 대기
      }
    }

    return Promise.reject(error);
  }
);

export default api;
