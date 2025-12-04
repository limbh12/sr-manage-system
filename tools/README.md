# Tools & Scripts

이 디렉토리는 프로젝트 개발 및 운영에 필요한 유틸리티 스크립트를 포함합니다.

## Python Scripts

### 1. `convert_excel_to_csv.py`
- **기능**: `docs/openapi_survey.xlsx` 엑셀 파일을 읽어 시스템 업로드용 CSV 파일(`openapi_survey_result.csv`)로 변환합니다.
- **사용법**:
  ```bash
  python tools/convert_excel_to_csv.py
  ```
- **요구사항**: `pandas`, `openpyxl` 라이브러리 필요.

### 2. `inspect_excel.py`
- **기능**: `docs/openapi_survey.xlsx` 파일의 컬럼 구조와 초기 데이터를 검사하여 출력합니다.
- **사용법**:
  ```bash
  python tools/inspect_excel.py
  ```
