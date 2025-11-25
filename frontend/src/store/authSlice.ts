import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { AuthState, LoginRequest, RegisterRequest, User, TokenResponse } from '../types';
import * as authService from '../services/authService';
import { setAccessToken, setRefreshToken, clearTokens, getAccessToken } from '../utils/tokenUtils';

const initialState: AuthState = {
  user: null,
  accessToken: getAccessToken(),
  refreshToken: null,
  isAuthenticated: !!getAccessToken(),
  loading: false,
  error: null,
};

/**
 * 로그인 액션
 */
export const loginAsync = createAsyncThunk<
  { user: User; tokens: TokenResponse },
  LoginRequest,
  { rejectValue: string }
>('auth/login', async (credentials, { rejectWithValue }) => {
  try {
    const tokens = await authService.login(credentials);
    setAccessToken(tokens.accessToken);
    setRefreshToken(tokens.refreshToken);
    const user = await authService.getCurrentUser();
    return { user, tokens };
  } catch (error) {
    clearTokens();
    if (error instanceof Error) {
      return rejectWithValue(error.message);
    }
    return rejectWithValue('Login failed');
  }
});

/**
 * 회원가입 액션
 */
export const registerAsync = createAsyncThunk<
  User,
  RegisterRequest,
  { rejectValue: string }
>('auth/register', async (data, { rejectWithValue }) => {
  try {
    return await authService.register(data);
  } catch (error) {
    if (error instanceof Error) {
      return rejectWithValue(error.message);
    }
    return rejectWithValue('Registration failed');
  }
});

/**
 * 로그아웃 액션
 */
export const logoutAsync = createAsyncThunk<void, void, { rejectValue: string }>(
  'auth/logout',
  async (_, { rejectWithValue }) => {
    try {
      await authService.logout();
      clearTokens();
    } catch (error) {
      clearTokens();
      if (error instanceof Error) {
        return rejectWithValue(error.message);
      }
      return rejectWithValue('Logout failed');
    }
  }
);

/**
 * 현재 사용자 정보 조회 액션
 */
export const fetchCurrentUserAsync = createAsyncThunk<
  User,
  void,
  { rejectValue: string }
>('auth/fetchCurrentUser', async (_, { rejectWithValue }) => {
  try {
    return await authService.getCurrentUser();
  } catch (error) {
    clearTokens();
    if (error instanceof Error) {
      return rejectWithValue(error.message);
    }
    return rejectWithValue('Failed to fetch user');
  }
});

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    setUser: (state, action: PayloadAction<User | null>) => {
      state.user = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      // 로그인
      .addCase(loginAsync.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(loginAsync.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload.user;
        state.accessToken = action.payload.tokens.accessToken;
        state.refreshToken = action.payload.tokens.refreshToken;
        state.isAuthenticated = true;
      })
      .addCase(loginAsync.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload || 'Login failed';
        state.isAuthenticated = false;
      })
      // 회원가입
      .addCase(registerAsync.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(registerAsync.fulfilled, (state) => {
        state.loading = false;
      })
      .addCase(registerAsync.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload || 'Registration failed';
      })
      // 로그아웃
      .addCase(logoutAsync.fulfilled, (state) => {
        state.user = null;
        state.accessToken = null;
        state.refreshToken = null;
        state.isAuthenticated = false;
      })
      // 현재 사용자 조회
      .addCase(fetchCurrentUserAsync.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchCurrentUserAsync.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload;
        state.isAuthenticated = true;
      })
      .addCase(fetchCurrentUserAsync.rejected, (state) => {
        state.loading = false;
        state.user = null;
        state.isAuthenticated = false;
      });
  },
});

export const { clearError, setUser } = authSlice.actions;
export default authSlice.reducer;
