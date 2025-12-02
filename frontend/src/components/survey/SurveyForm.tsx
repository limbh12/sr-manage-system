import { useState, useRef, FormEvent, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { 
  OpenApiSurveyCreateRequest
} from '../../types';
import * as surveyService from '../../services/surveyService';
import OrganizationSearchModal from './OrganizationSearchModal';
import { formatPhoneNumber } from '../../utils/formatUtils';

function SurveyForm() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [isOrgModalOpen, setIsOrgModalOpen] = useState(false);
  const [formData, setFormData] = useState<OpenApiSurveyCreateRequest>({
    organizationName: '',
    department: '',
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    receivedDate: new Date().toISOString().split('T')[0],
    systemName: '',
    currentMethod: 'CENTRAL',
    desiredMethod: 'CENTRAL_IMPROVED',
    reasonForDistributed: '',
    maintenanceOperation: 'INTERNAL',
    maintenanceLocation: 'INTERNAL',
    maintenanceAddress: '',
    maintenanceNote: '',
    operationEnv: 'OPS',
    serverLocation: '',
    
    // 4. 개발 및 운영환경 초기값
    webServerOs: 'LINUX',
    webServerOsType: '',
    webServerOsVersion: '',
    webServerType: 'APACHE',
    webServerTypeOther: '',
    webServerVersion: '',

    wasServerOs: 'LINUX',
    wasServerOsType: '',
    wasServerOsVersion: '',
    wasServerType: 'TOMCAT',
    wasServerTypeOther: '',
    wasServerVersion: '',

    dbServerOs: 'LINUX',
    dbServerOsType: '',
    dbServerOsVersion: '',
    dbServerType: 'POSTGRESQL',
    dbServerTypeOther: '',
    dbServerVersion: '',

    devLanguage: 'JAVA',
    devLanguageOther: '',
    devLanguageVersion: '',
    devFramework: 'SPRING_BOOT',
    devFrameworkOther: '',
    devFrameworkVersion: '',

    otherRequests: '',
    note: '',
  });

  useEffect(() => {
    if (id) {
      loadSurvey(Number(id));
    }
  }, [id]);

  const loadSurvey = async (surveyId: number) => {
    setLoading(true);
    try {
      const data = await surveyService.getSurveyById(surveyId);
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { id, createdAt, updatedAt, ...rest } = data;
      setFormData(rest);
    } catch (error) {
      console.error(error);
      alert('데이터를 불러오는데 실패했습니다.');
      navigate('/survey');
    } finally {
      setLoading(false);
    }
  };

  // File Upload State
  const [dragActive, setDragActive] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFile(e.dataTransfer.files[0]);
    }
  };

  const handleChangeFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    e.preventDefault();
    if (e.target.files && e.target.files[0]) {
      handleFile(e.target.files[0]);
    }
  };

  const handleFile = (file: File) => {
    setFile(file);
    setFormData(prev => ({ ...prev, receivedFileName: file.name }));
  };
  
  const onButtonClick = () => {
    fileInputRef.current?.click();
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    if (name === 'contactPhone') {
      setFormData(prev => ({ ...prev, [name]: formatPhoneNumber(value) }));
    } else {
      setFormData(prev => ({ ...prev, [name]: value }));
    }
  };

  const handleOrgSelect = (org: { code: string; name: string }) => {
    setFormData(prev => ({ ...prev, organizationName: org.name }));
    setIsOrgModalOpen(false);
  };

  const handleServerLocationSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value;
    if (value === 'OTHER') {
      setFormData(prev => ({ ...prev, serverLocation: '' }));
    } else {
      setFormData(prev => ({ ...prev, serverLocation: value }));
    }
  };

  const handleDownload = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!id) return;
    
    try {
      await surveyService.downloadSurveyFile(Number(id));
    } catch (error) {
      console.error(error);
      alert('파일 다운로드에 실패했습니다.');
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (id) {
        await surveyService.updateSurvey(Number(id), formData);
        alert('수정되었습니다.');
      } else {
        await surveyService.createSurvey(formData);
        alert('저장되었습니다.');
      }
      navigate('/survey'); // 목록 페이지로 이동
    } catch (error) {
      console.error(error);
      alert(id ? '수정에 실패했습니다.' : '저장에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const SERVER_LOCATION_PRESETS = ['NIRS_B', 'NIRS_S', 'INTERNAL'];
  const serverLocationSelectValue = SERVER_LOCATION_PRESETS.includes(formData.serverLocation || '') ? formData.serverLocation : 'OTHER';

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">{id ? 'OPEN API 현황조사 수정' : 'OPEN API 현황조사 등록'}</h2>
      </div>
      <form onSubmit={handleSubmit} className="card">
        
        {/* 1. 기본 정보 */}
        <section className="mb-4">
          <h3 className="section-title">1. 기본 정보</h3>
          <div className="grid-2">
            <div className="form-group">
              <label className="form-label">기관명 *</label>
              <div style={{ display: 'flex', gap: '8px' }}>
                <input 
                  type="text" 
                  name="organizationName" 
                  required 
                  className="form-input" 
                  value={formData.organizationName} 
                  onChange={handleChange} 
                  placeholder="기관 검색을 이용하세요"
                />
                <button 
                  type="button" 
                  className="btn btn-secondary" 
                  onClick={() => setIsOrgModalOpen(true)}
                  style={{ whiteSpace: 'nowrap' }}
                >
                  검색
                </button>
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">부서 *</label>
              <input type="text" name="department" required className="form-input" value={formData.department} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label className="form-label">담당자명 *</label>
              <input type="text" name="contactName" required className="form-input" value={formData.contactName} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label className="form-label">연락처 *</label>
              <input type="text" name="contactPhone" required className="form-input" value={formData.contactPhone} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label className="form-label">이메일 *</label>
              <input type="email" name="contactEmail" required className="form-input" value={formData.contactEmail} onChange={handleChange} />
            </div>
          </div>
        </section>

        {/* 2. 수신 파일 정보 */}
        <section className="mb-4">
          <h3 className="section-title">2. 수신 파일 정보</h3>
          <div className="grid-2">
            <div className="form-group">
              <label className="form-label">수신파일 (업로드)</label>
              <div 
                className={`file-upload-box ${dragActive ? 'active' : ''}`}
                onDragEnter={handleDrag}
                onDragLeave={handleDrag}
                onDragOver={handleDrag}
                onDrop={handleDrop}
                onClick={onButtonClick}
                style={{
                  border: '1px solid #d1d5db',
                  borderRadius: '4px',
                  padding: '8px 12px',
                  textAlign: 'center',
                  cursor: 'pointer',
                  backgroundColor: dragActive ? '#f0f8ff' : '#fff',
                  transition: 'all 0.2s ease',
                  height: '42px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                <input 
                  ref={fileInputRef}
                  type="file" 
                  className="file-input" 
                  style={{ display: 'none' }} 
                  onChange={handleChangeFile}
                />
                {file || formData.receivedFileName ? (
                  <div style={{ fontSize: '0.9em', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      <span style={{ fontWeight: '500', marginRight: '8px' }}>
                        {file ? file.name : formData.receivedFileName}
                      </span>
                      {file ? (
                        <span style={{ color: '#666', fontSize: '0.85em' }}>({(file.size / 1024).toFixed(1)} KB)</span>
                      ) : (
                        <span style={{ color: '#666', fontSize: '0.85em' }}>(기존 파일)</span>
                      )}
                    </div>
                    {!file && id && (
                      <button 
                        type="button" 
                        className="btn btn-secondary" 
                        onClick={handleDownload}
                        style={{ marginLeft: '8px', padding: '2px 8px', fontSize: '0.8em', height: '24px', lineHeight: '1' }}
                      >
                        다운로드
                      </button>
                    )}
                  </div>
                ) : (
                  <div style={{ color: '#9ca3af', fontSize: '0.9em' }}>
                    <span>클릭 또는 드래그하여 파일 업로드</span>
                  </div>
                )}
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">파일수신일자 *</label>
              <input type="date" name="receivedDate" required className="form-input" value={formData.receivedDate} onChange={handleChange} />
            </div>
          </div>
        </section>

        {/* 3. API 시스템 현황 */}
        <section className="mb-4">
          <h3 className="section-title">3. API 시스템 현황</h3>
          <div className="space-y-4">
            <div className="form-group">
              <label className="form-label">시스템명 *</label>
              <input type="text" name="systemName" required className="form-input" value={formData.systemName} onChange={handleChange} />
            </div>
            
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">현재방식 *</label>
                <select name="currentMethod" className="form-select" value={formData.currentMethod} onChange={handleChange}>
                  <option value="CENTRAL">중앙형</option>
                  <option value="DISTRIBUTED">분산형</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">희망전환방식 *</label>
                <select name="desiredMethod" className="form-select" value={formData.desiredMethod} onChange={handleChange}>
                  <option value="CENTRAL_IMPROVED">중앙개선형</option>
                  <option value="DISTRIBUTED_IMPROVED">분산개선형</option>
                </select>
              </div>
            </div>

            {formData.desiredMethod === 'DISTRIBUTED_IMPROVED' && (
              <div className="form-group">
                <label className="form-label">분산개선형 선택 사유 *</label>
                <textarea name="reasonForDistributed" required className="form-input" rows={3} value={formData.reasonForDistributed} onChange={handleChange} />
              </div>
            )}
          </div>
        </section>

        {/* 4. 시스템 운영팀 현황 */}
        <section className="mb-4">
          <h3 className="section-title">4. 시스템 운영팀 현황</h3>
          <div className="space-y-4">
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">운영인력보유 *</label>
                <select name="maintenanceOperation" className="form-select" value={formData.maintenanceOperation} onChange={handleChange}>
                  <option value="INTERNAL">자체운영</option>
                  <option value="PROFESSIONAL_RESIDENT">전문인력 보유(상주)</option>
                  <option value="PROFESSIONAL_NON_RESIDENT">전문인력 보유(비상주)</option>
                  <option value="OTHER">기타</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">수행장소 *</label>
                <select name="maintenanceLocation" className="form-select" value={formData.maintenanceLocation} onChange={handleChange}>
                  <option value="INTERNAL">기관내부</option>
                  <option value="EXTERNAL">기관외부</option>
                  <option value="REMOTE">원격지-온라인</option>
                </select>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">소재지</label>
              <input type="text" name="maintenanceAddress" className="form-input" value={formData.maintenanceAddress} onChange={handleChange} />
            </div>
            
            <div className="form-group">
              <label className="form-label">유지보수 담당자 정보</label>
              <input 
                type="text" 
                name="maintenanceNote" 
                className="form-input" 
                value={formData.maintenanceNote} 
                onChange={handleChange} 
                placeholder="담당자명, 연락처, 이메일, 비상주의 경우 방문횟수 등"
              />
            </div>
          </div>
        </section>

        {/* 5. 개발 및 운영환경 */}
        <section className="mb-4">
          <h3 className="section-title">5. 개발 및 운영환경</h3>
          
          <div className="grid-2 mb-4">
            <div className="form-group">
              <label className="form-label">운영환경 구분 *</label>
              <select name="operationEnv" className="form-select" value={formData.operationEnv} onChange={handleChange}>
                <option value="OPS">운영</option>
                <option value="DEV_OPS">개발/운영</option>
                <option value="TEST_OPS">테스트/운영</option>
                <option value="DEV_TEST_OPS">개발/테스트/운영</option>
                <option value="OTHER">기타</option>
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">서버위치</label>
              <div className="grid-2" style={{ gap: '8px' }}>
                <select className="form-select" value={serverLocationSelectValue} onChange={handleServerLocationSelect}>
                  <option value="NIRS_B">국가정보자원관리원(B존)</option>
                  <option value="NIRS_S">국가정보자원관리원(S존)</option>
                  <option value="INTERNAL">기관내부</option>
                  <option value="OTHER">기타</option>
                </select>
                {serverLocationSelectValue === 'OTHER' && (
                  <input 
                    type="text" 
                    name="serverLocation" 
                    className="form-input" 
                    placeholder="직접 입력" 
                    value={formData.serverLocation} 
                    onChange={handleChange} 
                  />
                )}
              </div>
            </div>
          </div>

          {/* WEB Server */}
          <div className="mb-4 p-4 bg-gray-50 rounded" style={{ backgroundColor: '#f9fafb', padding: '16px', borderRadius: '8px' }}>
            <h4 className="font-medium mb-3" style={{ fontWeight: 500, marginBottom: '12px' }}>WEB 서버</h4>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">OS</label>
                <div className="grid-2" style={{ gap: '8px' }}>
                  <select name="webServerOs" className="form-select" value={formData.webServerOs} onChange={handleChange}>
                    <option value="LINUX">리눅스</option>
                    <option value="WINDOWS">윈도우</option>
                    <option value="UNIX">유닉스</option>
                    <option value="OTHER">기타</option>
                  </select>
                  <input type="text" name="webServerOsType" className="form-input" placeholder="종류 (예: CentOS)" value={formData.webServerOsType} onChange={handleChange} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">OS 버전</label>
                <input type="text" name="webServerOsVersion" className="form-input" value={formData.webServerOsVersion} onChange={handleChange} />
              </div>
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">WEB</label>
                <div className="grid-2" style={{ gap: '8px' }}>
                  <select name="webServerType" className="form-select" value={formData.webServerType} onChange={handleChange}>
                    <option value="APACHE">Apache</option>
                    <option value="NGINX">NginX</option>
                    <option value="WEBTOB">WebtoB</option>
                    <option value="IIS">IIS</option>
                    <option value="OTHER">기타</option>
                  </select>
                  {formData.webServerType === 'OTHER' && (
                    <input type="text" name="webServerTypeOther" className="form-input" placeholder="기타 종류 입력" value={formData.webServerTypeOther} onChange={handleChange} />
                  )}
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">WEB 버전</label>
                <input type="text" name="webServerVersion" className="form-input" value={formData.webServerVersion} onChange={handleChange} />
              </div>
            </div>
          </div>

          {/* WAS Server */}
          <div className="mb-4 p-4 bg-gray-50 rounded" style={{ backgroundColor: '#f9fafb', padding: '16px', borderRadius: '8px' }}>
            <h4 className="font-medium mb-3" style={{ fontWeight: 500, marginBottom: '12px' }}>WAS 서버</h4>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">OS</label>
                <div className="grid-2" style={{ gap: '8px' }}>
                  <select name="wasServerOs" className="form-select" value={formData.wasServerOs} onChange={handleChange}>
                    <option value="LINUX">리눅스</option>
                    <option value="WINDOWS">윈도우</option>
                    <option value="UNIX">유닉스</option>
                    <option value="OTHER">기타</option>
                  </select>
                  <input type="text" name="wasServerOsType" className="form-input" placeholder="종류 (예: CentOS)" value={formData.wasServerOsType} onChange={handleChange} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">OS 버전</label>
                <input type="text" name="wasServerOsVersion" className="form-input" value={formData.wasServerOsVersion} onChange={handleChange} />
              </div>
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">WAS</label>
                <div className="grid-2" style={{ gap: '8px' }}>
                  <select name="wasServerType" className="form-select" value={formData.wasServerType} onChange={handleChange}>
                    <option value="JBOSS_EAP">Jboss EAP</option>
                    <option value="TOMCAT">Apache Tomcat</option>
                    <option value="WILDFLY">WildFly</option>
                    <option value="WEBLOGIC">WebLogic</option>
                    <option value="JEUS">JEUS</option>
                    <option value="JETTY">Jetty</option>
                    <option value="OTHER">기타</option>
                  </select>
                  {formData.wasServerType === 'OTHER' && (
                    <input type="text" name="wasServerTypeOther" className="form-input" placeholder="기타 종류 입력" value={formData.wasServerTypeOther} onChange={handleChange} />
                  )}
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">WAS 버전</label>
                <input type="text" name="wasServerVersion" className="form-input" value={formData.wasServerVersion} onChange={handleChange} />
              </div>
            </div>
          </div>

          {/* DB Server */}
          <div className="mb-4 p-4 bg-gray-50 rounded" style={{ backgroundColor: '#f9fafb', padding: '16px', borderRadius: '8px' }}>
            <h4 className="font-medium mb-3" style={{ fontWeight: 500, marginBottom: '12px' }}>DB 서버</h4>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">OS</label>
                <div className="grid-2" style={{ gap: '8px' }}>
                  <select name="dbServerOs" className="form-select" value={formData.dbServerOs} onChange={handleChange}>
                    <option value="LINUX">리눅스</option>
                    <option value="WINDOWS">윈도우</option>
                    <option value="UNIX">유닉스</option>
                    <option value="OTHER">기타</option>
                  </select>
                  <input type="text" name="dbServerOsType" className="form-input" placeholder="종류 (예: CentOS)" value={formData.dbServerOsType} onChange={handleChange} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">OS 버전</label>
                <input type="text" name="dbServerOsVersion" className="form-input" value={formData.dbServerOsVersion} onChange={handleChange} />
              </div>
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">DB</label>
                <div className="grid-2" style={{ gap: '8px' }}>
                  <select name="dbServerType" className="form-select" value={formData.dbServerType} onChange={handleChange}>
                    <option value="TIBERO">Tibero</option>
                    <option value="POSTGRESQL">PostgreSQL</option>
                    <option value="CUBRID">Cubrid</option>
                    <option value="MYSQL">My-Sql</option>
                    <option value="ORACLE">오라클</option>
                    <option value="MSSQL">MS-Sql</option>
                    <option value="OTHER">기타</option>
                  </select>
                  {formData.dbServerType === 'OTHER' && (
                    <input type="text" name="dbServerTypeOther" className="form-input" placeholder="기타 종류 입력" value={formData.dbServerTypeOther} onChange={handleChange} />
                  )}
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">DB 버전</label>
                <input type="text" name="dbServerVersion" className="form-input" value={formData.dbServerVersion} onChange={handleChange} />
              </div>
            </div>
          </div>

          {/* 개발 및 운영환경 */}
          <div className="mb-4 p-4 bg-gray-50 rounded" style={{ backgroundColor: '#f9fafb', padding: '16px', borderRadius: '8px' }}>
            <h4 className="font-medium mb-3" style={{ fontWeight: 500, marginBottom: '12px' }}>개발 및 운영환경</h4>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">언어</label>
                <div className="grid-2" style={{ gap: '8px' }}>
                  <select name="devLanguage" className="form-select" value={formData.devLanguage} onChange={handleChange}>
                    <option value="JAVA">JAVA</option>
                    <option value="PHP">PHP</option>
                    <option value="PYTHON">파이썬</option>
                    <option value="CSHARP">C#</option>
                    <option value="OTHER">기타</option>
                  </select>
                  {formData.devLanguage === 'OTHER' && (
                    <input type="text" name="devLanguageOther" className="form-input" placeholder="기타 언어 입력" value={formData.devLanguageOther} onChange={handleChange} />
                  )}
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">언어 버전</label>
                <input type="text" name="devLanguageVersion" className="form-input" value={formData.devLanguageVersion} onChange={handleChange} />
              </div>
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">프레임워크</label>
                <div className="grid-2" style={{ gap: '8px' }}>
                  <select name="devFramework" className="form-select" value={formData.devFramework} onChange={handleChange}>
                    <option value="EGOV">전자정부프레임워크</option>
                    <option value="SPRING">Spring</option>
                    <option value="SPRING_BOOT">Spring Boot</option>
                    <option value="OTHER">기타</option>
                  </select>
                  {formData.devFramework === 'OTHER' && (
                    <input type="text" name="devFrameworkOther" className="form-input" placeholder="기타 프레임워크 입력" value={formData.devFrameworkOther} onChange={handleChange} />
                  )}
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">프레임워크 버전</label>
                <input type="text" name="devFrameworkVersion" className="form-input" value={formData.devFrameworkVersion} onChange={handleChange} />
              </div>
            </div>
          </div>
        </section>

        {/* 6. 기타 */}
        <section className="mb-4">
          <h3 className="section-title">6. 기타</h3>
          <div className="space-y-4">
            <div className="form-group">
              <label className="form-label">기타 요청사항</label>
              <textarea name="otherRequests" className="form-input" rows={3} value={formData.otherRequests} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label className="form-label">비고</label>
              <textarea name="note" className="form-input" rows={3} value={formData.note} onChange={handleChange} />
            </div>
          </div>
        </section>

        <div className="flex-end pt-4">
          <button type="button" onClick={() => navigate('/survey')} className="btn btn-secondary">취소</button>
          <button type="submit" disabled={loading} className="btn btn-primary">
            {loading ? (id ? '수정 중...' : '저장 중...') : (id ? '수정' : '저장')}
          </button>
        </div>
      </form>

      <OrganizationSearchModal
        isOpen={isOrgModalOpen}
        onClose={() => setIsOrgModalOpen(false)}
        onSelect={handleOrgSelect}
      />
    </div>
  );
}

export default SurveyForm;
