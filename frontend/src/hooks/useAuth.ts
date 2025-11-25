import { useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { RootState, AppDispatch } from '../store';
import {
  loginAsync,
  registerAsync,
  logoutAsync,
  fetchCurrentUserAsync,
  clearError,
} from '../store/authSlice';
import { LoginRequest, RegisterRequest } from '../types';
import { getAccessToken } from '../utils/tokenUtils';

/**
 * 인증 관련 커스텀 훅
 */
export const useAuth = () => {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const { user, isAuthenticated, loading, error } = useSelector(
    (state: RootState) => state.auth
  );

  // 초기 로드 시 사용자 정보 가져오기
  useEffect(() => {
    const token = getAccessToken();
    if (token && !user && !loading) {
      dispatch(fetchCurrentUserAsync());
    }
  }, [dispatch, user, loading]);

  /**
   * 로그인
   */
  const login = useCallback(
    async (credentials: LoginRequest) => {
      const result = await dispatch(loginAsync(credentials));
      if (loginAsync.fulfilled.match(result)) {
        navigate('/dashboard');
      }
    },
    [dispatch, navigate]
  );

  /**
   * 회원가입
   */
  const register = useCallback(
    async (data: RegisterRequest) => {
      const result = await dispatch(registerAsync(data));
      if (registerAsync.fulfilled.match(result)) {
        navigate('/login');
      }
    },
    [dispatch, navigate]
  );

  /**
   * 로그아웃
   */
  const logout = useCallback(async () => {
    await dispatch(logoutAsync());
    navigate('/login');
  }, [dispatch, navigate]);

  /**
   * 에러 초기화
   */
  const resetError = useCallback(() => {
    dispatch(clearError());
  }, [dispatch]);

  return {
    user,
    isAuthenticated,
    loading,
    error,
    login,
    register,
    logout,
    resetError,
  };
};
