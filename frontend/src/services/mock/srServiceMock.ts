import { Sr, SrCreateRequest, SrUpdateRequest, SrStatusUpdateRequest, PageResponse, SrStatus, Priority, SrHistory, SrHistoryCreateRequest, SrHistoryType } from '../../types';
import { INITIAL_SR_LIST, MOCK_USER, STORAGE_KEYS, delay } from './mockData';

// 로컬 스토리지에서 SR 목록 가져오기
const getStoredSrList = (): Sr[] => {
  const stored = localStorage.getItem(STORAGE_KEYS.SR_LIST);
  if (!stored) {
    localStorage.setItem(STORAGE_KEYS.SR_LIST, JSON.stringify(INITIAL_SR_LIST));
    return INITIAL_SR_LIST;
  }
  return JSON.parse(stored);
};

// 로컬 스토리지에 SR 목록 저장하기
const saveSrList = (list: Sr[]) => {
  localStorage.setItem(STORAGE_KEYS.SR_LIST, JSON.stringify(list));
};

// 로컬 스토리지에서 SR 이력 목록 가져오기
const getStoredSrHistories = (): SrHistory[] => {
  const stored = localStorage.getItem(STORAGE_KEYS.SR_HISTORIES);
  return stored ? JSON.parse(stored) : [];
};

// 로컬 스토리지에 SR 이력 목록 저장하기
const saveSrHistories = (list: SrHistory[]) => {
  localStorage.setItem(STORAGE_KEYS.SR_HISTORIES, JSON.stringify(list));
};

// 우선순위 한글 변환
const getPriorityLabel = (priority: Priority): string => {
  switch (priority) {
    case 'LOW': return '낮음';
    case 'MEDIUM': return '보통';
    case 'HIGH': return '높음';
    case 'CRITICAL': return '긴급';
    default: return priority;
  }
};

// 상태 한글 변환
const getStatusLabel = (status: SrStatus): string => {
  switch (status) {
    case 'OPEN': return '신규';
    case 'IN_PROGRESS': return '처리중';
    case 'RESOLVED': return '해결됨';
    case 'CLOSED': return '종료';
    default: return status;
  }
};

// 이력 생성 헬퍼 함수
const addHistory = (srId: number, historyType: SrHistoryType, content: string, previousValue?: string, newValue?: string) => {
  const histories = getStoredSrHistories();
  const newId = histories.length > 0 ? Math.max(...histories.map(h => h.id)) + 1 : 1;
  
  const newHistory: SrHistory = {
    id: newId,
    srId,
    historyType,
    content,
    previousValue,
    newValue,
    createdBy: MOCK_USER,
    createdAt: new Date().toISOString(),
  };
  
  histories.push(newHistory);
  saveSrHistories(histories);
  return newHistory;
};


interface GetSrListParams {
  page?: number;
  size?: number;
  status?: SrStatus;
  priority?: Priority;
  search?: string;
}

// 사용자 목록 가져오기 (userServiceMock와 키 공유)
const getUserList = (): any[] => {
  const stored = localStorage.getItem('mock_users');
  return stored ? JSON.parse(stored) : [MOCK_USER];
};

export const getSrList = async (params: GetSrListParams = {}): Promise<PageResponse<Sr>> => {
  await delay(500); // 네트워크 지연 시뮬레이션

  let list = getStoredSrList();

  // 필터링
  if (params.status) {
    list = list.filter((sr) => sr.status === params.status);
  }
  if (params.priority) {
    list = list.filter((sr) => sr.priority === params.priority);
  }
  if (params.search) {
    const searchLower = params.search.toLowerCase();
    list = list.filter(
      (sr) =>
        sr.title.toLowerCase().includes(searchLower) ||
        sr.description.toLowerCase().includes(searchLower)
    );
  }

  // 정렬 (최신순)
  list.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  // 페이지네이션
  const page = params.page || 0;
  const size = params.size || 10;
  const totalElements = list.length;
  const totalPages = Math.ceil(totalElements / size);
  const content = list.slice(page * size, (page + 1) * size);

  return {
    content,
    totalElements,
    totalPages,
    size,
    number: page,
    first: page === 0,
    last: page === totalPages - 1,
  };
};

export const getSrById = async (id: number): Promise<Sr> => {
  await delay(300);
  const list = getStoredSrList();
  const sr = list.find((item) => item.id === id);
  if (!sr) {
    throw new Error('SR not found');
  }
  return sr;
};

export const createSr = async (data: SrCreateRequest): Promise<Sr> => {
  await delay(500);
  const list = getStoredSrList();
  
  const newId = list.length > 0 ? Math.max(...list.map((sr) => sr.id)) + 1 : 1;
  const now = new Date();
  const nowIso = now.toISOString();
  
  // SR ID 생성 (SR-YYMM-XXXX)
  const yy = now.getFullYear().toString().slice(2);
  const mm = (now.getMonth() + 1).toString().padStart(2, '0');
  const prefix = `SR-${yy}${mm}-`;
  
  // 같은 달의 마지막 번호 찾기
  const currentMonthSrs = list.filter(sr => sr.srId && sr.srId.startsWith(prefix));
  let sequence = 1;
  if (currentMonthSrs.length > 0) {
    const lastSrId = currentMonthSrs.map(sr => sr.srId).sort().pop();
    if (lastSrId) {
      const lastSeq = parseInt(lastSrId.split('-')[2]);
      sequence = lastSeq + 1;
    }
  }
  const newSrId = `${prefix}${sequence.toString().padStart(4, '0')}`;

  let assignee = null;
  if (data.assigneeId) {
    const users = getUserList();
    const found = users.find((u: any) => u.id === data.assigneeId);
    if (found) assignee = found;
  }

  const newSr: Sr = {
    id: newId,
    srId: newSrId,
    title: data.title,
    description: data.description || '',
    status: 'OPEN',
    priority: data.priority || 'MEDIUM',
    requester: MOCK_USER, // 현재 로그인한 사용자로 가정
    assignee,
    applicantName: data.applicantName,
    applicantPhone: data.applicantPhone,
    openApiSurveyId: data.openApiSurveyId,
    createdAt: nowIso,
    updatedAt: nowIso,
  };

  list.unshift(newSr);
  saveSrList(list);
  return newSr;
};

export const updateSr = async (id: number, data: SrUpdateRequest): Promise<Sr> => {
  await delay(500);
  const list = getStoredSrList();
  const index = list.findIndex((item) => item.id === id);
  
  if (index === -1) {
    throw new Error('SR not found');
  }

  const oldSr = list[index];
  
  // 변경 사항 감지 및 이력 생성
  if (data.priority && data.priority !== oldSr.priority) {
    addHistory(id, 'PRIORITY_CHANGE', '우선순위가 변경되었습니다.', getPriorityLabel(oldSr.priority), getPriorityLabel(data.priority));
  }

  if (data.description !== undefined && data.description !== oldSr.description) {
    addHistory(id, 'INFO_CHANGE', '요청사항이 변경되었습니다.', oldSr.description, data.description);
  }
  
  if (data.processingDetails !== undefined && data.processingDetails !== oldSr.processingDetails) {
    addHistory(id, 'INFO_CHANGE', '처리내용이 변경되었습니다.', oldSr.processingDetails, data.processingDetails);
  }

  if (data.applicantName !== undefined && data.applicantName !== oldSr.applicantName) {
    addHistory(id, 'INFO_CHANGE', '요청자 이름이 변경되었습니다.', oldSr.applicantName || '없음', data.applicantName);
  }

  if (data.applicantPhone !== undefined && data.applicantPhone !== oldSr.applicantPhone) {
    addHistory(id, 'INFO_CHANGE', '요청자 연락처가 변경되었습니다.', oldSr.applicantPhone || '없음', data.applicantPhone);
  }
  
  // 담당자 변경 처리
  let assignee = oldSr.assignee;
  if (data.assigneeId !== undefined) {
    // assigneeId가 null이거나 undefined가 아닌 경우 (여기서는 undefined 체크는 위에서 함)
    // 하지만 data.assigneeId가 number | undefined 이므로
    // null 처리를 위해 타입을 확인하거나, 0이나 -1 같은 값을 쓰지 않는 이상 null을 보낼 수 있어야 함.
    // SrUpdateRequest에서 assigneeId는 number | undefined.
    // 만약 unassign을 지원하려면 null도 허용해야 함.
    // 여기서는 값이 있으면 변경하는 것으로 처리.
    
    const users = getUserList();
    const newAssignee = users.find((u: any) => u.id === data.assigneeId);
    
    // 기존 담당자와 다른 경우에만 변경 및 이력 생성
    if (oldSr.assignee?.id !== data.assigneeId) {
       const oldName = oldSr.assignee ? oldSr.assignee.name : '없음';
       const newName = newAssignee ? newAssignee.name : '없음';
       addHistory(id, 'ASSIGNEE_CHANGE', '담당자가 변경되었습니다.', oldName, newName);
       assignee = newAssignee || null;
    }
  }

  const updatedSr = {
    ...oldSr,
    ...data,
    assignee,
    updatedAt: new Date().toISOString(),
  };

  list[index] = updatedSr;
  saveSrList(list);
  return updatedSr;
};

export const deleteSr = async (id: number): Promise<void> => {
  await delay(500);
  const list = getStoredSrList();
  const newList = list.filter((item) => item.id !== id);
  saveSrList(newList);
};

export const updateSrStatus = async (id: number, data: SrStatusUpdateRequest): Promise<Sr> => {
  await delay(300);
  const list = getStoredSrList();
  const index = list.findIndex((item) => item.id === id);
  
  if (index === -1) {
    throw new Error('SR not found');
  }

  const oldSr = list[index];
  
  if (data.status !== oldSr.status) {
    addHistory(id, 'STATUS_CHANGE', '상태가 변경되었습니다.', getStatusLabel(oldSr.status), getStatusLabel(data.status));
  }

  const updatedSr = {
    ...oldSr,
    status: data.status,
    updatedAt: new Date().toISOString(),
  };

  list[index] = updatedSr;
  saveSrList(list);
  return updatedSr;
};

export const getSrHistories = async (id: number): Promise<SrHistory[]> => {
  await delay(300);
  const histories = getStoredSrHistories();
  return histories
    .filter(h => h.srId === id)
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
};

export const createSrHistory = async (id: number, data: SrHistoryCreateRequest): Promise<SrHistory> => {
  await delay(300);
  // 댓글 생성
  return addHistory(id, 'COMMENT', data.content);
};
