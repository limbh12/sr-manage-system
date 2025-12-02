import { OpenApiSurvey } from '../../types';
import { formatPhoneNumber } from '../../utils/formatUtils';

interface Props {
  survey: OpenApiSurvey;
}

function OpenApiSurveyInfoCard({ survey }: Props) {
  const renderField = (label: string, value?: string | React.ReactNode) => {
    if (!value) return null;
    return (
      <div>
        <span style={{ color: '#666', display: 'block', fontSize: '12px' }}>{label}</span>
        <span style={{ fontWeight: 500 }}>{value}</span>
      </div>
    );
  };

  const formatServerInfo = (
    os?: string, 
    osType?: string,
    osVersion?: string,
    type?: string, 
    typeOther?: string, 
    version?: string
  ) => {
    const parts = [];
    
    // OS Info
    let osStr = os;
    if (os === 'OTHER' && osType) osStr = osType;
    if (osStr) {
      let fullOs = `OS: ${osStr}`;
      if (osVersion) fullOs += ` ${osVersion}`;
      parts.push(fullOs);
    }

    // Middleware/DB Info
    let typeStr = type;
    if (type === 'OTHER' && typeOther) typeStr = typeOther;
    if (typeStr) {
      let fullType = `Type: ${typeStr}`;
      if (version) fullType += ` ${version}`;
      parts.push(fullType);
    }

    return parts.length > 0 ? parts.join(' / ') : '-';
  };

  const formatDevInfo = () => {
    const lang = survey.devLanguage === 'OTHER' ? survey.devLanguageOther : survey.devLanguage;
    const fw = survey.devFramework === 'OTHER' ? survey.devFrameworkOther : survey.devFramework;
    
    const parts = [];
    if (lang) parts.push(`언어: ${lang} ${survey.devLanguageVersion || ''}`);
    if (fw) parts.push(`프레임워크: ${fw} ${survey.devFrameworkVersion || ''}`);
    
    return parts.length > 0 ? parts.join(' / ') : '-';
  };

  return (
    <div style={{ 
      padding: '16px', 
      backgroundColor: '#f8f9fa', 
      borderRadius: '4px', 
      border: '1px solid #e9ecef',
      fontSize: '14px'
    }}>
      <h4 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '12px', color: '#333' }}>기본 정보</h4>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '16px' }}>
        {renderField('기관명', survey.organizationName)}
        {renderField('부서', survey.department)}
        {renderField('담당자', survey.contactName)}
        {renderField('연락처', `${formatPhoneNumber(survey.contactPhone)} / ${survey.contactEmail}`)}
        {renderField('시스템명', survey.systemName)}
        {renderField('전환방식', `${survey.currentMethod === 'CENTRAL' ? '중앙형' : '분산형'} → ${survey.desiredMethod === 'CENTRAL_IMPROVED' ? '중앙개선형' : '분산개선형'}`)}
      </div>

      <h4 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '12px', color: '#333', borderTop: '1px solid #eee', paddingTop: '12px' }}>개발 및 운영환경</h4>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
        {renderField('WEB Server', formatServerInfo(
          survey.webServerOs, survey.webServerOsType, survey.webServerOsVersion,
          survey.webServerType, survey.webServerTypeOther, survey.webServerVersion
        ))}
        {renderField('WAS Server', formatServerInfo(
          survey.wasServerOs, survey.wasServerOsType, survey.wasServerOsVersion,
          survey.wasServerType, survey.wasServerTypeOther, survey.wasServerVersion
        ))}
        {renderField('DB Server', formatServerInfo(
          survey.dbServerOs, survey.dbServerOsType, survey.dbServerOsVersion,
          survey.dbServerType, survey.dbServerTypeOther, survey.dbServerVersion
        ))}
        {renderField('개발환경', formatDevInfo())}
      </div>
    </div>
  );
}

export default OpenApiSurveyInfoCard;
