import { useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { RootState, AppDispatch } from '../store';
import {
  fetchSrListAsync,
  fetchSrByIdAsync,
  createSrAsync,
  updateSrAsync,
  deleteSrAsync,
  updateSrStatusAsync,
  fetchSrHistoriesAsync,
  createSrHistoryAsync,
  clearError,
  setCurrentSr,
} from '../store/srSlice';
import { SrCreateRequest, SrUpdateRequest, SrStatusUpdateRequest, Sr, Priority, SrStatus, SrHistoryCreateRequest } from '../types';

interface FetchSrListParams {
  page?: number;
  size?: number;
  status?: SrStatus;
  priority?: Priority;
  search?: string;
}

/**
 * SR 관련 커스텀 훅
 */
export const useSr = () => {
  const dispatch = useDispatch<AppDispatch>();
  const { srList, currentSr, srHistories, totalElements, totalPages, currentPage, loading, error } =
    useSelector((state: RootState) => state.sr);

  /**
   * SR 목록 조회
   */
  const fetchSrList = useCallback(
    (params: FetchSrListParams = {}) => {
      dispatch(fetchSrListAsync(params));
    },
    [dispatch]
  );

  /**
   * SR 상세 조회
   */
  const fetchSrById = useCallback(
    (id: number) => {
      dispatch(fetchSrByIdAsync(id));
    },
    [dispatch]
  );

  /**
   * SR 이력 조회
   */
  const fetchSrHistories = useCallback(
    (id: number) => {
      dispatch(fetchSrHistoriesAsync(id));
    },
    [dispatch]
  );

  /**
   * SR 이력(댓글) 생성
   */
  const createSrHistory = useCallback(
    async (id: number, data: SrHistoryCreateRequest) => {
      const result = await dispatch(createSrHistoryAsync({ id, data }));
      return createSrHistoryAsync.fulfilled.match(result);
    },
    [dispatch]
  );

  /**
   * SR 생성
   */
  const createSr = useCallback(
    async (data: SrCreateRequest) => {
      const result = await dispatch(createSrAsync(data));
      return createSrAsync.fulfilled.match(result);
    },
    [dispatch]
  );

  /**
   * SR 수정
   */
  const updateSr = useCallback(
    async (id: number, data: SrUpdateRequest) => {
      const result = await dispatch(updateSrAsync({ id, data }));
      return updateSrAsync.fulfilled.match(result);
    },
    [dispatch]
  );

  /**
   * SR 삭제
   */
  const deleteSr = useCallback(
    async (id: number) => {
      const result = await dispatch(deleteSrAsync(id));
      return deleteSrAsync.fulfilled.match(result);
    },
    [dispatch]
  );

  /**
   * SR 상태 변경
   */
  const updateSrStatus = useCallback(
    async (id: number, data: SrStatusUpdateRequest) => {
      const result = await dispatch(updateSrStatusAsync({ id, data }));
      return updateSrStatusAsync.fulfilled.match(result);
    },
    [dispatch]
  );

  /**
   * 현재 SR 설정
   */
  const selectSr = useCallback(
    (sr: Sr | null) => {
      dispatch(setCurrentSr(sr));
    },
    [dispatch]
  );

  /**
   * 에러 초기화
   */
  const resetError = useCallback(() => {
    dispatch(clearError());
  }, [dispatch]);

  return {
    srList,
    currentSr,
    srHistories,
    totalElements,
    totalPages,
    currentPage,
    loading,
    error,
    fetchSrList,
    fetchSrById,
    fetchSrHistories,
    createSrHistory,
    createSr,
    updateSr,
    deleteSr,
    updateSrStatus,
    selectSr,
    resetError,
  };
};
