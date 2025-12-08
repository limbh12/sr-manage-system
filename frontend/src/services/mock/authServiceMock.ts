import { LoginRequest, TokenResponse, User } from '../../types';
import { MOCK_USER, MOCK_ADMIN, delay } from './mockData';

export const login = async (data: LoginRequest): Promise<TokenResponse> => {
  await delay(500);
  
  // Mock 로그인 상태 저장
  // 1. 먼저 저장된 사용자 목록에서 일치하는 사용자를 찾음
  const storedUsers = localStorage.getItem('mock_users');
  let foundUser: User | undefined;
  
  if (storedUsers) {
    const users: User[] = JSON.parse(storedUsers);
    foundUser = users.find(u => u.username === data.username);
  }

  if (foundUser) {
    // 찾은 사용자의 ID와 Role을 저장
    localStorage.setItem('mock_user_id', foundUser.id.toString());
    localStorage.setItem('mock_user_role', foundUser.role);
  } else {
    // 기존 로직 유지 (admin/tester 기본 계정)
    if (data.username === 'admin') {
      localStorage.setItem('mock_user_role', 'ADMIN');
      localStorage.setItem('mock_user_id', '2');
    } else {
      localStorage.setItem('mock_user_role', 'USER');
      localStorage.setItem('mock_user_id', '1');
    }
  }

  return {
    accessToken: 'mock-access-token',
    refreshToken: 'mock-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 3600,
  };
};

export const refreshToken = async (_refreshToken: string): Promise<TokenResponse> => {
  await delay(300);
  return {
    accessToken: 'new-mock-access-token',
    refreshToken: 'new-mock-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 3600,
  };
};

export const logout = async (): Promise<void> => {
  await delay(200);
  localStorage.removeItem('mock_user_role');
  localStorage.removeItem('mock_user_id');
};

export const getCurrentUser = async (): Promise<User> => {
  await delay(300);
  const role = localStorage.getItem('mock_user_role');
  const userIdStr = localStorage.getItem('mock_user_id');
  
  // userServiceMock에서 관리하는 사용자 목록에서 최신 정보를 가져와야 함
  const storedUsers = localStorage.getItem('mock_users');
  if (storedUsers) {
    const users: User[] = JSON.parse(storedUsers);
    
    // 저장된 ID가 있으면 해당 ID로 검색
    if (userIdStr) {
      const targetId = parseInt(userIdStr);
      const foundUser = users.find(u => u.id === targetId);
      if (foundUser) return foundUser;
    }
    
    // ID가 없으면 기존 로직대로 Role 기반 추정 (하위 호환성)
    const targetId = role === 'ADMIN' ? 2 : 1; // MOCK_ADMIN: 2, MOCK_USER: 1
    const foundUser = users.find(u => u.id === targetId);
    if (foundUser) {
      return foundUser;
    }
  }

  // 저장된 정보가 없거나 찾지 못한 경우 기본값 반환
  if (role === 'ADMIN') {
    return MOCK_ADMIN;
  }
  return MOCK_USER;
};
