---

# 📑 Project: AI-Powered Wiki for Air-Gapped Network

본 프로젝트는 폐쇄망 환경에서 지식의 체계적 축적과 AI를 활용한 지능형 검색 및 분석을 제공하는 위키 시스템 구축을 목표로 합니다.

---

## 🛠 1. 기술 스택 (Tech Stack)

| 구분 | 상세 기술 | 비고 |
| --- | --- | --- |
| **Frontend** | React 18, Tailwind CSS | UI/UX |
| **Backend** | Spring Boot 3.x, Spring Data JPA, Spring AI | API & Logic |
| **Database** | H2 Database (File Mode) | 데이터 저장 |
| **AI Engine** | Llama 3.2 (via Local Ollama) | LLM/RAG |
| **Document** | Pandoc, Apache PDFBox | PDF 파싱 및 변환 |
| **Security** | Spring Security | 권한 및 인증 |

---

## 📋 2. 기능 요구사항 (Functional Requirements)

### Epic 1. 코어 위키 시스템 (Core Wiki)

* [ ] **W-1: 마크다운 에디터 구현**
* Toast UI Editor 연동 및 실시간 미리보기 지원.


* [ ] **W-2: 문서 버전 관리 및 이력**
* 수정 시점별 스냅샷 저장 및 특정 버전으로 복구(Rollback) 기능.


* [ ] **W-3: 파일 및 이미지 서버**
* 폐쇄망 서버 로컬 스토리지 연동 및 본문 삽입용 URL 생성.


* [ ] **W-4: 계층형 카테고리 관리**
* 폴더 구조의 문서 분류 및 사이드바 트리 내비게이션.



### Epic 2. PDF 지능형 변환 (PDF Processing)

* [ ] **D-1: PDF to Markdown 자동 변환**
* Pandoc 엔진을 활용한 업로드 즉시 위키 문서화.


* [ ] **D-2: 멀티미디어 자산 추출**
* PDF 내 이미지를 자동 추출하여 서버 저장 및 마크다운 매핑.


* [ ] **D-3: AI 기반 구조 보정**
* 복잡한 표나 수식을 Llama Vision 모델을 통해 정교한 마크다운으로 재구성.



### Epic 3. AI 지능형 검색 및 분석 (AI Intelligence)

* [ ] **A-1: RAG 기반 자연어 검색**
* 사용자 질문에 대해 위키 본문 내용을 참고하여 답변 생성.


* [ ] **A-2: 근거 문서 하이라이팅**
* AI 답변 생성 시 참고한 위키 문서의 링크와 단락 표시.


* [ ] **A-3: 이미지/OCR 검색**
* 이미지 내 텍스트 및 캡셔닝 정보를 포함한 통합 검색 기능.


* [ ] **A-4: 자동 요약 기능**
* 긴 문서 진입 시 AI가 3줄 요약을 자동 생성하여 상단 배치.



---

## 🏗 3. 시스템 아키텍처 (System Architecture)

---

## ⚙️ 4. 비기능 및 인프라 (Non-Functional)

* **배포 방식:** 모든 라이브러리를 포함한 단일 실행 파일(Fat JAR) 배포.
* **데이터 백업:** 매일 정해진 시간에 DB 파일 및 미디어 폴더 자동 압축 백업.
* **보안:** 폐쇄망 내 사용자별 읽기/쓰기 권한 제어(ACL).
* **성능:** LLM 추론 시 GPU 가속 지원 여부에 따른 처리 모드 분리(CPU/GPU).

---

## 🚀 5. 단계별 개발 로드맵 (Roadmap)

1. **Phase 1 (MVP):** 기초 위키 CRUD 및 H2 파일 모드 연동.
2. **Phase 2 (Convert):** Pandoc 엔진 연동 및 PDF 변환 기능 안정화.
3. **Phase 3 (AI):** Ollama 연동 및 RAG 시스템(벡터 검색) 구축.
4. **Phase 4 (Final):** 권한 관리 추가 및 폐쇄망 실환경 최적화.

---
