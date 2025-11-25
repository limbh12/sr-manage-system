import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { SrState, Sr, SrCreateRequest, SrUpdateRequest, SrStatusUpdateRequest, Priority, SrStatus } from '../types';
import * as srService from '../services/srService';

const initialState: SrState = {
  srList: [],
  currentSr: null,
  totalElements: 0,
  totalPages: 0,
  currentPage: 0,
  loading: false,
  error: null,
};

interface FetchSrListParams {
  page?: number;
  size?: number;
  status?: SrStatus;
  priority?: Priority;
  search?: string;
}

/**
 * SR 목록 조회 액션
 */
export const fetchSrListAsync = createAsyncThunk<
  { srList: Sr[]; totalElements: number; totalPages: number; currentPage: number },
  FetchSrListParams,
  { rejectValue: string }
>('sr/fetchList', async (params, { rejectWithValue }) => {
  try {
    const response = await srService.getSrList(params);
    return {
      srList: response.content,
      totalElements: response.totalElements,
      totalPages: response.totalPages,
      currentPage: response.number,
    };
  } catch (error) {
    if (error instanceof Error) {
      return rejectWithValue(error.message);
    }
    return rejectWithValue('Failed to fetch SR list');
  }
});

/**
 * SR 상세 조회 액션
 */
export const fetchSrByIdAsync = createAsyncThunk<Sr, number, { rejectValue: string }>(
  'sr/fetchById',
  async (id, { rejectWithValue }) => {
    try {
      return await srService.getSrById(id);
    } catch (error) {
      if (error instanceof Error) {
        return rejectWithValue(error.message);
      }
      return rejectWithValue('Failed to fetch SR');
    }
  }
);

/**
 * SR 생성 액션
 */
export const createSrAsync = createAsyncThunk<Sr, SrCreateRequest, { rejectValue: string }>(
  'sr/create',
  async (data, { rejectWithValue }) => {
    try {
      return await srService.createSr(data);
    } catch (error) {
      if (error instanceof Error) {
        return rejectWithValue(error.message);
      }
      return rejectWithValue('Failed to create SR');
    }
  }
);

/**
 * SR 수정 액션
 */
export const updateSrAsync = createAsyncThunk<
  Sr,
  { id: number; data: SrUpdateRequest },
  { rejectValue: string }
>('sr/update', async ({ id, data }, { rejectWithValue }) => {
  try {
    return await srService.updateSr(id, data);
  } catch (error) {
    if (error instanceof Error) {
      return rejectWithValue(error.message);
    }
    return rejectWithValue('Failed to update SR');
  }
});

/**
 * SR 삭제 액션
 */
export const deleteSrAsync = createAsyncThunk<number, number, { rejectValue: string }>(
  'sr/delete',
  async (id, { rejectWithValue }) => {
    try {
      await srService.deleteSr(id);
      return id;
    } catch (error) {
      if (error instanceof Error) {
        return rejectWithValue(error.message);
      }
      return rejectWithValue('Failed to delete SR');
    }
  }
);

/**
 * SR 상태 변경 액션
 */
export const updateSrStatusAsync = createAsyncThunk<
  Sr,
  { id: number; data: SrStatusUpdateRequest },
  { rejectValue: string }
>('sr/updateStatus', async ({ id, data }, { rejectWithValue }) => {
  try {
    return await srService.updateSrStatus(id, data);
  } catch (error) {
    if (error instanceof Error) {
      return rejectWithValue(error.message);
    }
    return rejectWithValue('Failed to update SR status');
  }
});

const srSlice = createSlice({
  name: 'sr',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    setCurrentSr: (state, action: PayloadAction<Sr | null>) => {
      state.currentSr = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      // SR 목록 조회
      .addCase(fetchSrListAsync.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchSrListAsync.fulfilled, (state, action) => {
        state.loading = false;
        state.srList = action.payload.srList;
        state.totalElements = action.payload.totalElements;
        state.totalPages = action.payload.totalPages;
        state.currentPage = action.payload.currentPage;
      })
      .addCase(fetchSrListAsync.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload || 'Failed to fetch SR list';
      })
      // SR 상세 조회
      .addCase(fetchSrByIdAsync.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchSrByIdAsync.fulfilled, (state, action) => {
        state.loading = false;
        state.currentSr = action.payload;
      })
      .addCase(fetchSrByIdAsync.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload || 'Failed to fetch SR';
      })
      // SR 생성
      .addCase(createSrAsync.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(createSrAsync.fulfilled, (state, action) => {
        state.loading = false;
        state.srList.unshift(action.payload);
        state.totalElements += 1;
      })
      .addCase(createSrAsync.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload || 'Failed to create SR';
      })
      // SR 수정
      .addCase(updateSrAsync.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(updateSrAsync.fulfilled, (state, action) => {
        state.loading = false;
        const index = state.srList.findIndex((sr) => sr.id === action.payload.id);
        if (index !== -1) {
          state.srList[index] = action.payload;
        }
        if (state.currentSr?.id === action.payload.id) {
          state.currentSr = action.payload;
        }
      })
      .addCase(updateSrAsync.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload || 'Failed to update SR';
      })
      // SR 삭제
      .addCase(deleteSrAsync.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(deleteSrAsync.fulfilled, (state, action) => {
        state.loading = false;
        state.srList = state.srList.filter((sr) => sr.id !== action.payload);
        state.totalElements -= 1;
        if (state.currentSr?.id === action.payload) {
          state.currentSr = null;
        }
      })
      .addCase(deleteSrAsync.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload || 'Failed to delete SR';
      })
      // SR 상태 변경
      .addCase(updateSrStatusAsync.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(updateSrStatusAsync.fulfilled, (state, action) => {
        state.loading = false;
        const index = state.srList.findIndex((sr) => sr.id === action.payload.id);
        if (index !== -1) {
          state.srList[index] = action.payload;
        }
        if (state.currentSr?.id === action.payload.id) {
          state.currentSr = action.payload;
        }
      })
      .addCase(updateSrStatusAsync.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload || 'Failed to update SR status';
      });
  },
});

export const { clearError, setCurrentSr } = srSlice.actions;
export default srSlice.reducer;
