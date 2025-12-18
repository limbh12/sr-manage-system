-- OPEN API 현황조사에 작성상태(status) 컬럼 추가
-- 실행 방법: H2 Console 또는 각 DB 관리 툴에서 실행

-- status 컬럼 추가 (컬럼이 없는 경우에만 실행됨)
ALTER TABLE open_api_survey ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- 기존 데이터의 status 값 설정 (assignee가 있으면 IN_PROGRESS, 없으면 PENDING)
UPDATE open_api_survey
SET status = CASE
    WHEN assignee_id IS NOT NULL THEN 'IN_PROGRESS'
    ELSE 'PENDING'
END
WHERE status = 'PENDING';

-- 확인 쿼리
SELECT id, organization_code, system_name, status, assignee_id
FROM open_api_survey
ORDER BY id;
