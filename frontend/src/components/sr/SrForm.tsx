import { useState, FormEvent, useEffect } from 'react';
import { Sr, SrCreateRequest, SrUpdateRequest, Priority, SrStatus, OpenApiSurvey, User, CommonCode } from '../../types';
import SurveySearchModal from './SurveySearchModal';
import * as surveyService from '../../services/surveyService';
import * as userService from '../../services/userService';
import { commonCodeService } from '../../services/commonCodeService';
import OpenApiSurveyInfoCard from './OpenApiSurveyInfoCard';
import { formatPhoneNumber } from '../../utils/formatUtils';

interface SrFormProps {
  sr?: Sr | null;
  onSubmit: (data: SrCreateRequest | SrUpdateRequest) => void;
  onCancel: () => void;
  loading?: boolean;
}

/**
 * SR 생성/수정 폼 컴포넌트
 */
function SrForm({ sr, onSubmit, onCancel, loading = false }: SrFormProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [processingDetails, setProcessingDetails] = useState('');
  const [priority, setPriority] = useState<Priority>('MEDIUM');
  const [status, setStatus] = useState<SrStatus>('OPEN');
  const [category, setCategory] = useState('OPEN_API');
  const [requestType, setRequestType] = useState('');
  const [applicantName, setApplicantName] = useState('');
  const [applicantPhone, setApplicantPhone] = useState('');
  const [assigneeId, setAssigneeId] = useState<number | undefined>(undefined);
  const [assigneeOptions, setAssigneeOptions] = useState<User[]>([]);
  const [categoryOptions, setCategoryOptions] = useState<CommonCode[]>([]);
  const [requestTypeOptions, setRequestTypeOptions] = useState<CommonCode[]>([]);
  const [showSurveyModal, setShowSurveyModal] = useState(false);
  const [modalInitialKeyword, setModalInitialKeyword] = useState('');
  const [selectedSurvey, setSelectedSurvey] = useState<OpenApiSurvey | null>(null);
  const [openApiSurveyId, setOpenApiSurveyId] = useState<number | undefined>(undefined);

  const isEditMode = !!sr;

  useEffect(() => {
    userService.getUserOptions().then(setAssigneeOptions).catch(console.error);
    commonCodeService.getActiveCodesByGroup('SR_CATEGORY').then(setCategoryOptions).catch(console.error);
    commonCodeService.getActiveCodesByGroup('SR_REQUEST_TYPE').then(setRequestTypeOptions).catch(console.error);
  }, []);

  useEffect(() => {
    if (sr) {
      setTitle(sr.title);
      setDescription(sr.description || '');
      setProcessingDetails(sr.processingDetails || '');
      setPriority(sr.priority);
      setStatus(sr.status);
      setCategory(sr.category || '');
      setRequestType(sr.requestType || '');
      setAssigneeId(sr.assignee?.id);
      setApplicantName(sr.applicantName || '');
      setApplicantPhone(formatPhoneNumber(sr.applicantPhone || ''));

      if (sr.openApiSurveyId) {
        setOpenApiSurveyId(sr.openApiSurveyId);
        // 기존 연결된 설문 정보 로드
        surveyService.getSurveyById(sr.openApiSurveyId).then(setSelectedSurvey).catch(console.error);
      }
    }
  }, [sr]);

  useEffect(() => {
    const handleEsc = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        if (showSurveyModal) return;
        onCancel();
      }
    };
    window.addEventListener('keydown', handleEsc);
    return () => {
      window.removeEventListener('keydown', handleEsc);
    };
  }, [onCancel, showSurveyModal]);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    
    if (!applicantName) {
      alert('요청자 이름을 입력해주세요.');
      return;
    }

    if (!applicantPhone) {
      alert('요청자 전화번호를 입력해주세요.');
      return;
    }

    if (!category) {
      alert('분류를 선택해주세요.');
      return;
    }

    if (!requestType) {
      alert('요청구분을 선택해주세요.');
      return;
    }

    if (!description) {
      alert('요청사항을 입력해주세요.');
      return;
    }
    
    const commonData = {
      title,
      description,
      priority,
      category,
      requestType,
      openApiSurveyId,
      assigneeId,
      applicantName,
      applicantPhone: applicantPhone.replace(/-/g, ''), // 하이픈 제거
    };

    if (isEditMode) {
      onSubmit({
        ...commonData,
        processingDetails,
        status,
      } as SrUpdateRequest);
    } else {
      onSubmit(commonData as SrCreateRequest);
    }
  };

  const handleSurveySelect = (survey: OpenApiSurvey) => {
    setSelectedSurvey(survey);
    setOpenApiSurveyId(survey.id);
    
    // 자동 입력 (값이 없을 때만)
    if (!applicantName) {
      setApplicantName(survey.contactName);
    }
    if (!applicantPhone) {
      setApplicantPhone(formatPhoneNumber(survey.contactPhone));
    }
    
    if (!title) {
      setTitle(`[${survey.organization.name}] ${survey.systemName} 관련 요청`);
    }
    
    setShowSurveyModal(false);
  };

  const handleRequesterSearch = async () => {
    const keyword = applicantName || applicantPhone;
    if (!keyword) {
      alert('이름 또는 전화번호를 입력해주세요.');
      return;
    }

    try {
      const response = await surveyService.getSurveyList(0, 10, { keyword });
      if (response.content.length === 0) {
        alert('검색 결과가 없습니다.');
      } else if (response.content.length === 1) {
        handleSurveySelect(response.content[0]);
      } else {
        // 여러개면 모달 열기 (여러개일 경우 모달에서 다시 검색하게 됨)
        setModalInitialKeyword(keyword);
        setShowSurveyModal(true);
      }
    } catch (error) {
      console.error('Search failed', error);
      alert('검색 중 오류가 발생했습니다.');
    }
  };

  const handleRemoveSurvey = () => {
    setSelectedSurvey(null);
    setOpenApiSurveyId(undefined);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
    }
  };

  const handleRequesterKeyDown = (e: React.KeyboardEvent) => {
    if (e.nativeEvent.isComposing) return;
    if (e.key === 'Enter') {
      e.preventDefault();
      handleRequesterSearch();
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '1000px', width: '90%' }}>
        <div className="modal-header">
          <h2 className="modal-title">{isEditMode ? 'SR 수정' : 'SR 등록'}</h2>
          <button className="modal-close" onClick={onCancel}>
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">요청자 정보 *</label>
            <div style={{ display: 'flex', gap: '16px' }}>
              <div style={{ flex: 1 }}>
                <input
                  type="text"
                  className="form-input"
                  value={applicantName}
                  onChange={(e) => setApplicantName(e.target.value)}
                  onKeyDown={handleRequesterKeyDown}
                  placeholder="이름"
                  disabled={loading}
                  required
                />
              </div>
              <div style={{ flex: 1 }}>
                <input
                  type="text"
                  className="form-input"
                  value={applicantPhone}
                  onChange={(e) => setApplicantPhone(formatPhoneNumber(e.target.value))}
                  onKeyDown={handleRequesterKeyDown}
                  placeholder="전화번호"
                  disabled={loading}
                  required
                />
              </div>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={handleRequesterSearch}
                disabled={loading}
              >
                검색 및 자동입력
              </button>
            </div>
            <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
              * 이름 또는 전화번호 입력 후 검색 시 OPEN API 현황정보가 자동 연동됩니다.
            </div>
          </div>

          <div className="form-group">
            <div style={{ display: 'flex', gap: '16px' }}>
              <div style={{ flex: 1 }}>
                <label htmlFor="category" className="form-label">
                  분류 *
                </label>
                <select
                  id="category"
                  className="form-select"
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  disabled={loading}
                  required
                >
                  <option value="">선택하세요</option>
                  {categoryOptions.map((option) => (
                    <option key={option.id} value={option.codeValue}>
                      {option.codeName}
                    </option>
                  ))}
                </select>
              </div>
              <div style={{ flex: 1 }}>
                <label htmlFor="requestType" className="form-label">
                  요청구분 *
                </label>
                <select
                  id="requestType"
                  className="form-select"
                  value={requestType}
                  onChange={(e) => setRequestType(e.target.value)}
                  disabled={loading}
                  required
                >
                  <option value="">선택하세요</option>
                  {requestTypeOptions.map((option) => (
                    <option key={option.id} value={option.codeValue}>
                      {option.codeName}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="title" className="form-label">
              제목 *
            </label>
            <input
              type="text"
              id="title"
              className="form-input"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="SR 제목을 입력하세요"
              required
              disabled={loading}
            />
          </div>

          {/* OPEN API 현황조사 정보 표시 영역 */}
          <div className="form-group">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
              <label className="form-label" style={{ marginBottom: 0 }}>
                OPEN API 현황조사 정보
              </label>
              {!selectedSurvey ? (
                <button
                  type="button"
                  className="btn btn-secondary"
                  style={{ fontSize: '12px', padding: '4px 8px' }}
                  onClick={() => {
                    setModalInitialKeyword('');
                    setShowSurveyModal(true);
                  }}
                >
                  불러오기
                </button>
              ) : (
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    style={{ fontSize: '12px', padding: '4px 8px' }}
                    onClick={() => {
                      setModalInitialKeyword('');
                      setShowSurveyModal(true);
                    }}
                  >
                    변경
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger"
                    style={{ fontSize: '12px', padding: '4px 8px' }}
                    onClick={handleRemoveSurvey}
                  >
                    삭제
                  </button>
                </div>
              )}
            </div>
            
            {selectedSurvey && (
              <OpenApiSurveyInfoCard survey={selectedSurvey} />
            )}
          </div>

          <div className="form-group">
            <label htmlFor="description" className="form-label">
              요청사항 *
            </label>
            <textarea
              id="description"
              className="form-input"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="SR 요청사항을 입력하세요 (Markdown 지원)"
              rows={10}
              disabled={loading}
              style={{ resize: 'vertical' }}
              required
            />
          </div>

          {isEditMode && (
            <div className="form-group">
              <label htmlFor="processingDetails" className="form-label">
                처리내용
              </label>
              <textarea
                id="processingDetails"
                className="form-input"
                value={processingDetails}
                onChange={(e) => setProcessingDetails(e.target.value)}
                placeholder="처리내용을 입력하세요 (Markdown 지원)"
                rows={10}
                disabled={loading}
                style={{ resize: 'vertical' }}
              />
            </div>
          )}

          <div className="form-group">
            <div style={{ display: 'flex', gap: '16px' }}>
              <div style={{ flex: 1 }}>
                <label htmlFor="priority" className="form-label">
                  우선순위
                </label>
                <select
                  id="priority"
                  className="form-select"
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as Priority)}
                  disabled={loading}
                >
                  <option value="LOW">낮음</option>
                  <option value="MEDIUM">보통</option>
                  <option value="HIGH">높음</option>
                  <option value="CRITICAL">긴급</option>
                </select>
              </div>

              {isEditMode && (
                <div style={{ flex: 1 }}>
                  <label htmlFor="status" className="form-label">
                    처리상태
                  </label>
                  <select
                    id="status"
                    className="form-select"
                    value={status}
                    onChange={(e) => setStatus(e.target.value as SrStatus)}
                    disabled={loading}
                  >
                    <option value="OPEN">접수</option>
                    <option value="IN_PROGRESS">진행중</option>
                    <option value="RESOLVED">해결됨</option>
                    <option value="CLOSED">종료</option>
                  </select>
                </div>
              )}
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="assignee" className="form-label">
              담당자
            </label>
            <select
              id="assignee"
              className="form-select"
              value={assigneeId || ''}
              onChange={(e) => setAssigneeId(e.target.value ? Number(e.target.value) : undefined)}
              disabled={loading}
            >
              <option value="">미지정</option>
              {assigneeOptions.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.name} ({user.email})
                </option>
              ))}
            </select>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={loading}>
              취소
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? '저장 중...' : isEditMode ? '수정' : '등록'}
            </button>
          </div>
        </form>
      </div>

      {showSurveyModal && (
        <SurveySearchModal
          onSelect={handleSurveySelect}
          onClose={() => {
            setShowSurveyModal(false);
            setModalInitialKeyword('');
          }}
          initialKeyword={modalInitialKeyword}
        />
      )}
    </div>
  );
}

export default SrForm;
