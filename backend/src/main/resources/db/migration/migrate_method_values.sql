-- OPEN API 현황조사 현재방식/희망전환방식 한글 → 영문 코드 변환
-- 실행 방법: H2 Console 또는 각 DB 관리 툴에서 실행

-- 현재방식(currentMethod) 변환
UPDATE open_api_survey
SET current_method = 'CENTRAL'
WHERE current_method IN ('중앙', '중앙형');

UPDATE open_api_survey
SET current_method = 'DISTRIBUTED'
WHERE current_method IN ('분산', '분산형');

UPDATE open_api_survey
SET current_method = 'NO_RESPONSE'
WHERE current_method = '미회신' OR current_method IS NULL OR current_method = '';

-- 희망전환방식(desiredMethod) 변환
UPDATE open_api_survey
SET desired_method = 'CENTRAL_IMPROVED'
WHERE desired_method IN ('중앙개선형', '중앙 개선형');

UPDATE open_api_survey
SET desired_method = 'DISTRIBUTED_IMPROVED'
WHERE desired_method IN ('분산개선형', '분산 개선형');

UPDATE open_api_survey
SET desired_method = 'NO_RESPONSE'
WHERE desired_method = '미회신' OR desired_method IS NULL OR desired_method = '';

-- 확인 쿼리
SELECT id, organization_code, system_name, current_method, desired_method
FROM open_api_survey
ORDER BY id;
