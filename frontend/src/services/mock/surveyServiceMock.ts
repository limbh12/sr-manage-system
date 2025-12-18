import { OpenApiSurvey, OpenApiSurveyCreateRequest, PageResponse } from '../../types';
import { delay, INITIAL_SURVEY_LIST } from './mockData';

// 로컬 스토리지 키 추가
const SURVEY_STORAGE_KEY = 'mock_survey_list';

const getStoredSurveyList = (): OpenApiSurvey[] => {
  const stored = localStorage.getItem(SURVEY_STORAGE_KEY);
  if (stored) {
    return JSON.parse(stored);
  }
  // 초기 데이터가 없으면 초기값 저장 후 반환
  saveSurveyList(INITIAL_SURVEY_LIST);
  return INITIAL_SURVEY_LIST;
};

const saveSurveyList = (list: OpenApiSurvey[]) => {
  localStorage.setItem(SURVEY_STORAGE_KEY, JSON.stringify(list));
};

export const getSurveyList = async (page = 0, size = 10, search?: { keyword?: string; currentMethod?: string; desiredMethod?: string }): Promise<PageResponse<OpenApiSurvey>> => {
  await delay(500);
  let list = getStoredSurveyList();
  
  // 검색 필터링
  if (search) {
    if (search.keyword) {
      const keyword = search.keyword.toLowerCase();
      list = list.filter(item => 
        item.organization.name.toLowerCase().includes(keyword) ||
        item.department.toLowerCase().includes(keyword) ||
        item.systemName.toLowerCase().includes(keyword) ||
        item.contactName.toLowerCase().includes(keyword) ||
        item.contactPhone.includes(keyword)
      );
    }
    if (search.currentMethod) {
      list = list.filter(item => item.currentMethod === search.currentMethod);
    }
    if (search.desiredMethod) {
      list = list.filter(item => item.desiredMethod === search.desiredMethod);
    }
  }

  // 최신순 정렬
  list.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

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

export const getSurveyById = async (id: number): Promise<OpenApiSurvey> => {
  await delay(300);
  const list = getStoredSurveyList();
  const survey = list.find(item => item.id === id);
  if (!survey) throw new Error('Survey not found');
  return survey;
};

export const createSurvey = async (data: OpenApiSurveyCreateRequest): Promise<OpenApiSurvey> => {
  await delay(500);
  const list = getStoredSurveyList();
  
  const newId = list.length > 0 ? Math.max(...list.map(s => s.id)) + 1 : 1;
  const now = new Date().toISOString();

  const newSurvey: OpenApiSurvey = {
    id: newId,
    organization: { code: data.organizationCode, name: 'Mock Organization' }, // Mock organization
    department: data.department,
    contactName: data.contactName,
    contactPhone: data.contactPhone,
    contactEmail: data.contactEmail,
    assignee: null, // Mock: assignee would be resolved from assigneeId
    status: data.status,
    receivedDate: data.receivedDate,
    systemName: data.systemName,
    operationStatus: data.operationStatus || 'OPERATING',
    currentMethod: data.currentMethod,
    desiredMethod: data.desiredMethod,
    reasonForDistributed: data.reasonForDistributed,
    maintenanceOperation: data.maintenanceOperation,
    maintenanceLocation: data.maintenanceLocation,
    maintenanceAddress: data.maintenanceAddress,
    maintenanceNote: data.maintenanceNote,
    operationEnv: data.operationEnv,
    serverLocation: data.serverLocation,
    webServerOs: data.webServerOs,
    webServerOsType: data.webServerOsType,
    webServerOsVersion: data.webServerOsVersion,
    webServerType: data.webServerType,
    webServerTypeOther: data.webServerTypeOther,
    webServerVersion: data.webServerVersion,
    wasServerOs: data.wasServerOs,
    wasServerOsType: data.wasServerOsType,
    wasServerOsVersion: data.wasServerOsVersion,
    wasServerType: data.wasServerType,
    wasServerTypeOther: data.wasServerTypeOther,
    wasServerVersion: data.wasServerVersion,
    dbServerOs: data.dbServerOs,
    dbServerOsType: data.dbServerOsType,
    dbServerOsVersion: data.dbServerOsVersion,
    dbServerType: data.dbServerType,
    dbServerTypeOther: data.dbServerTypeOther,
    dbServerVersion: data.dbServerVersion,
    devLanguage: data.devLanguage,
    devLanguageOther: data.devLanguageOther,
    devLanguageVersion: data.devLanguageVersion,
    devFramework: data.devFramework,
    devFrameworkOther: data.devFrameworkOther,
    devFrameworkVersion: data.devFrameworkVersion,
    otherRequests: data.otherRequests,
    note: data.note,
    createdAt: now,
    updatedAt: now,
  };

  list.unshift(newSurvey);
  saveSurveyList(list);
  return newSurvey;
};

export const updateSurvey = async (id: number, data: OpenApiSurveyCreateRequest): Promise<OpenApiSurvey> => {
  await delay(500);
  const list = getStoredSurveyList();
  const index = list.findIndex(item => item.id === id);
  
  if (index === -1) throw new Error('Survey not found');
  
  const updatedSurvey: OpenApiSurvey = {
    ...list[index],
    ...data,
    updatedAt: new Date().toISOString(),
  };
  
  list[index] = updatedSurvey;
  saveSurveyList(list);
  return updatedSurvey;
};

export const downloadSurveyFile = async (id: number): Promise<void> => {
  await delay(500);
  const list = getStoredSurveyList();
  const survey = list.find(item => item.id === id);
  
  if (!survey) throw new Error('Survey not found');
  
  // 가짜 파일 다운로드
  const content = `Mock file content for survey ${id}\nOrganization: ${survey.organization.name}`;
  const blob = new Blob([content], { type: 'text/plain' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', survey.receivedFileName || `survey_${id}_file.txt`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const searchOrganizations = async (keyword: string): Promise<{ code: string; name: string }[]> => {
  await delay(300);
  // Mock data for organization search
  const organizations = [
    { code: '1741000', name: '행정안전부' },
    { code: '3000000', name: '서울특별시' },
    { code: '3010000', name: '서울특별시 종로구' },
    { code: '3020000', name: '서울특별시 중구' },
    { code: '3030000', name: '서울특별시 용산구' },
    { code: '3040000', name: '서울특별시 성동구' },
    { code: '3050000', name: '서울특별시 광진구' },
    { code: '3060000', name: '서울특별시 동대문구' },
    { code: '3070000', name: '서울특별시 중랑구' },
    { code: '3080000', name: '서울특별시 성북구' },
    { code: '3090000', name: '서울특별시 강북구' },
    { code: '3100000', name: '서울특별시 도봉구' },
    { code: '3110000', name: '서울특별시 노원구' },
    { code: '3120000', name: '서울특별시 은평구' },
    { code: '3130000', name: '서울특별시 서대문구' },
    { code: '3140000', name: '서울특별시 마포구' },
    { code: '3150000', name: '서울특별시 양천구' },
    { code: '3160000', name: '서울특별시 강서구' },
    { code: '3170000', name: '서울특별시 구로구' },
    { code: '3180000', name: '서울특별시 금천구' },
    { code: '3190000', name: '서울특별시 영등포구' },
    { code: '3200000', name: '서울특별시 동작구' },
    { code: '3210000', name: '서울특별시 관악구' },
    { code: '3220000', name: '서울특별시 서초구' },
    { code: '3230000', name: '서울특별시 강남구' },
    { code: '3240000', name: '서울특별시 송파구' },
    { code: '3250000', name: '서울특별시 강동구' },
    { code: '6110000', name: '부산광역시' },
    { code: '6270000', name: '대구광역시' },
    { code: '6280000', name: '인천광역시' },
    { code: '6290000', name: '광주광역시' },
    { code: '6300000', name: '대전광역시' },
    { code: '6310000', name: '울산광역시' },
    { code: '6410000', name: '경기도' },
    { code: '6420000', name: '강원도' },
    { code: '6430000', name: '충청북도' },
    { code: '6440000', name: '충청남도' },
    { code: '6450000', name: '전라북도' },
    { code: '6460000', name: '전라남도' },
    { code: '6470000', name: '경상북도' },
    { code: '6480000', name: '경상남도' },
    { code: '6500000', name: '제주특별자치도' },
  ];

  if (!keyword) return [];
  
  return organizations.filter(org => 
    org.name.includes(keyword) || org.code.includes(keyword)
  );
};
