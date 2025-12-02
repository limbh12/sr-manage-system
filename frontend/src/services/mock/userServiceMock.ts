import { PageResponse, User, UserUpdateRequest, UserCreateRequest } from '../../types';
import { MOCK_USER, MOCK_ADMIN, delay } from './mockData';

const STORAGE_KEY = 'mock_users';

const getStoredUsers = (): User[] => {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored) {
    return JSON.parse(stored);
  }
  const initialUsers = [MOCK_ADMIN, MOCK_USER];
  localStorage.setItem(STORAGE_KEY, JSON.stringify(initialUsers));
  return initialUsers;
};

const saveUsers = (users: User[]) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(users));
};

export const getUsers = async (page = 0, size = 10): Promise<PageResponse<User>> => {
  await delay(500);
  const users = getStoredUsers();
  
  // 최신순 정렬 (ID 역순)
  const sortedUsers = [...users].sort((a, b) => b.id - a.id);
  
  const start = page * size;
  const end = start + size;
  const content = sortedUsers.slice(start, end);
  
  return {
    content,
    totalElements: users.length,
    totalPages: Math.ceil(users.length / size),
    size: size,
    number: page,
    first: page === 0,
    last: end >= users.length
  };
};

export const createUser = async (data: UserCreateRequest): Promise<User> => {
  await delay(500);
  const users = getStoredUsers();
  const newId = Math.max(...users.map(u => u.id)) + 1;
  
  const newUser: User = {
    id: newId,
    username: data.username,
    name: data.name,
    email: data.email,
    role: data.role,
    createdAt: new Date().toISOString()
  };
  
  users.push(newUser);
  saveUsers(users);
  return newUser;
};

export const updateUser = async (id: number, data: UserUpdateRequest): Promise<User> => {
  await delay(500);
  const users = getStoredUsers();
  const index = users.findIndex(u => u.id === id);
  
  if (index === -1) throw new Error('User not found');
  
  const updatedUser = {
    ...users[index],
    ...data,
  };
  
  users[index] = updatedUser;
  saveUsers(users);
  return updatedUser;
};

export const deleteUser = async (id: number): Promise<void> => {
  await delay(500);
  const users = getStoredUsers();
  const filteredUsers = users.filter(u => u.id !== id);
  saveUsers(filteredUsers);
};

export const updateMyProfile = async (data: UserUpdateRequest): Promise<User> => {
  await delay(500);
  // 현재 로그인한 사용자를 MOCK_USER 또는 MOCK_ADMIN으로 가정해야 함
  // 실제로는 토큰에서 ID를 가져오지만, Mock에서는 로컬스토리지의 role을 확인하거나
  // 간단히 MOCK_USER(ID: 1)를 업데이트하는 것으로 시뮬레이션
  // 여기서는 편의상 ID 1번 사용자를 업데이트한다고 가정 (또는 현재 로그인한 사용자 ID를 알 수 있다면 좋음)
  
  // Mock 환경의 한계로, 현재 로그인한 사용자의 ID를 정확히 알기 어려우므로
  // localStorage에 저장된 'mock_user_role'을 확인하여 추정
  const role = localStorage.getItem('mock_user_role') || 'USER';
  const targetId = role === 'ADMIN' ? 2 : 1; // MOCK_ADMIN: 2, MOCK_USER: 1
  
  return updateUser(targetId, data);
};

export const getUserOptions = async (): Promise<User[]> => {
  await delay(300);
  return getStoredUsers();
};
