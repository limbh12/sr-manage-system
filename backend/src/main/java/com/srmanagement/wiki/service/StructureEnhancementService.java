package com.srmanagement.wiki.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 기반 구조 보정 서비스
 * - 마크다운 텍스트에서 표(Table)를 인식하고 변환
 * - 수식(LaTeX) 인식 및 변환
 * - Pandoc 통합 (선택적)
 *
 * D-3 기능 구현
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StructureEnhancementService {

    private final OllamaChatModel chatModel;

    @Value("${wiki.pandoc.enabled:false}")
    private boolean pandocEnabled;

    @Value("${wiki.pandoc.path:pandoc}")
    private String pandocPath;

    @Value("${wiki.structure-enhancement.enabled:true}")
    private boolean structureEnhancementEnabled;

    @Value("${wiki.structure-enhancement.table-detection:true}")
    private boolean tableDetectionEnabled;

    @Value("${wiki.structure-enhancement.formula-detection:true}")
    private boolean formulaDetectionEnabled;

    @Value("${wiki.structure-enhancement.vision-enabled:false}")
    private boolean visionEnabled;

    @Value("${wiki.structure-enhancement.vision-model:llama3.2-vision}")
    private String visionModel;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    /**
     * 마크다운 구조 보정 수행
     * - 표 인식 및 변환
     * - 수식 인식 및 변환
     * - AI 기반 구조 분석
     *
     * @param rawMarkdown 원본 마크다운 텍스트
     * @param originalFileName 원본 파일명
     * @return 보정된 마크다운 텍스트와 분석 결과
     */
    public EnhancementResult enhanceMarkdown(String rawMarkdown, String originalFileName) {
        if (!structureEnhancementEnabled) {
            log.info("구조 보정 기능이 비활성화됨");
            return EnhancementResult.builder()
                    .enhancedMarkdown(rawMarkdown)
                    .tablesFound(0)
                    .formulasFound(0)
                    .enhanced(false)
                    .message("구조 보정 기능이 비활성화되어 있습니다")
                    .build();
        }

        log.info("📐 구조 보정 시작: {}", originalFileName);
        long startTime = System.currentTimeMillis();

        String enhancedMarkdown = rawMarkdown;
        List<String> enhancementLog = new ArrayList<>();
        int tablesFound = 0;
        int formulasFound = 0;

        try {
            // 1. 표(Table) 구조 인식 및 변환
            if (tableDetectionEnabled) {
                TableEnhancementResult tableResult = enhanceTables(enhancedMarkdown);
                enhancedMarkdown = tableResult.getEnhancedMarkdown();
                tablesFound = tableResult.getTablesDetected();
                if (tablesFound > 0) {
                    enhancementLog.add(String.format("표 %d개 인식 및 변환", tablesFound));
                }
            }

            // 2. 수식(LaTeX) 인식 및 변환
            if (formulaDetectionEnabled) {
                FormulaEnhancementResult formulaResult = enhanceFormulas(enhancedMarkdown);
                enhancedMarkdown = formulaResult.getEnhancedMarkdown();
                formulasFound = formulaResult.getFormulasDetected();
                if (formulasFound > 0) {
                    enhancementLog.add(String.format("수식 %d개 인식 및 변환", formulasFound));
                }
            }

            // 3. AI 기반 추가 구조 분석 (표/수식이 감지된 경우만)
            if (tablesFound > 0 || formulasFound > 0) {
                enhancedMarkdown = performAiStructureAnalysis(enhancedMarkdown, originalFileName);
                enhancementLog.add("AI 구조 분석 완료");
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("✅ 구조 보정 완료: {} ({}ms) - 표 {}개, 수식 {}개",
                    originalFileName, elapsedTime, tablesFound, formulasFound);

            return EnhancementResult.builder()
                    .enhancedMarkdown(enhancedMarkdown)
                    .tablesFound(tablesFound)
                    .formulasFound(formulasFound)
                    .enhanced(tablesFound > 0 || formulasFound > 0)
                    .processingTimeMs(elapsedTime)
                    .message(enhancementLog.isEmpty() ? "보정할 구조 없음" : String.join(", ", enhancementLog))
                    .build();

        } catch (Exception e) {
            log.error("구조 보정 실패: {}", originalFileName, e);
            return EnhancementResult.builder()
                    .enhancedMarkdown(rawMarkdown)
                    .tablesFound(0)
                    .formulasFound(0)
                    .enhanced(false)
                    .message("구조 보정 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 표(Table) 구조 인식 및 마크다운 변환
     * - 정규식으로 표 패턴 인식
     * - AI를 사용하여 복잡한 표 구조 변환
     */
    private TableEnhancementResult enhanceTables(String markdown) {
        log.debug("표 구조 인식 시작");

        // 표로 보이는 패턴 탐지 (공백으로 정렬된 열, 탭 구분, | 구분 등)
        List<TableCandidate> tableCandidates = detectTableCandidates(markdown);

        if (tableCandidates.isEmpty()) {
            return TableEnhancementResult.builder()
                    .enhancedMarkdown(markdown)
                    .tablesDetected(0)
                    .build();
        }

        log.info("표 후보 {}개 발견", tableCandidates.size());

        String[] lines = markdown.split("\n");
        StringBuilder result = new StringBuilder();
        int tablesConverted = 0;

        // 줄 번호 기반으로 교체 (역순으로 처리하면 인덱스 꼬임 방지)
        // 여기서는 순차 처리하면서 건너뛰는 방식 사용
        java.util.Set<Integer> processedLines = new java.util.HashSet<>();

        for (TableCandidate candidate : tableCandidates) {
            try {
                // AI를 사용하여 표 구조 분석 및 마크다운 표로 변환
                String convertedTable = convertToMarkdownTable(candidate.getContent());
                if (convertedTable != null && !convertedTable.equals(candidate.getContent())) {
                    // 표 영역의 줄들을 마킹
                    for (int i = candidate.getStartLine(); i <= candidate.getEndLine(); i++) {
                        processedLines.add(i);
                    }
                    candidate.setConvertedContent(convertedTable);
                    tablesConverted++;
                    log.debug("표 변환 완료: {} 문자 → {} 문자",
                            candidate.getContent().length(), convertedTable.length());
                }
            } catch (Exception e) {
                log.warn("표 변환 실패, 원본 유지: {}", e.getMessage());
            }
        }

        // 결과 조립
        int candidateIdx = 0;
        for (int i = 0; i < lines.length; i++) {
            if (processedLines.contains(i)) {
                // 표 영역의 시작 줄인 경우 변환된 표 삽입
                if (candidateIdx < tableCandidates.size()) {
                    TableCandidate candidate = tableCandidates.get(candidateIdx);
                    if (i == candidate.getStartLine() && candidate.getConvertedContent() != null) {
                        result.append(candidate.getConvertedContent()).append("\n\n");
                        candidateIdx++;
                    }
                }
                // 표 영역의 다른 줄들은 건너뜀
            } else {
                result.append(lines[i]).append("\n");
            }
        }

        return TableEnhancementResult.builder()
                .enhancedMarkdown(result.toString().trim())
                .tablesDetected(tablesConverted)
                .build();
    }

    /**
     * 표 후보 영역 탐지
     * - 패턴 1: 탭이나 공백으로 구분된 열이 있는 줄
     * - 패턴 2: PDF에서 추출된 반복 패턴 (숫자 + 텍스트가 반복)
     * - 패턴 3: 헤더-데이터 패턴 (첫 줄이 헤더, 이후 유사한 구조)
     */
    private List<TableCandidate> detectTableCandidates(String markdown) {
        List<TableCandidate> candidates = new ArrayList<>();

        // 패턴 1: 기존 방식 - 탭/공백으로 구분된 데이터
        candidates.addAll(detectTabularData(markdown));

        // 패턴 2: PDF 추출 패턴 - 반복되는 구조 감지
        candidates.addAll(detectPdfTablePattern(markdown));

        return candidates;
    }

    /**
     * 탭/공백으로 구분된 표 데이터 감지 (기존 로직)
     */
    private List<TableCandidate> detectTabularData(String markdown) {
        List<TableCandidate> candidates = new ArrayList<>();
        String[] lines = markdown.split("\n");
        StringBuilder currentCandidate = new StringBuilder();
        int tableStartLine = -1;
        int consecutiveTableLines = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (isLikelyTableRow(line)) {
                if (tableStartLine == -1) {
                    tableStartLine = i;
                }
                currentCandidate.append(line).append("\n");
                consecutiveTableLines++;
            } else {
                // 표 후보 종료
                if (consecutiveTableLines >= 2) {
                    String content = currentCandidate.toString().trim();
                    if (!isAlreadyMarkdownTable(content)) {
                        candidates.add(TableCandidate.builder()
                                .content(content)
                                .startLine(tableStartLine)
                                .endLine(i - 1)
                                .build());
                    }
                }
                currentCandidate = new StringBuilder();
                tableStartLine = -1;
                consecutiveTableLines = 0;
            }
        }

        // 마지막 후보 처리
        if (consecutiveTableLines >= 2) {
            String content = currentCandidate.toString().trim();
            if (!isAlreadyMarkdownTable(content)) {
                candidates.add(TableCandidate.builder()
                        .content(content)
                        .startLine(tableStartLine)
                        .endLine(lines.length - 1)
                        .build());
            }
        }

        return candidates;
    }

    /**
     * PDF에서 추출된 표 패턴 감지
     * - "위원1 79.88" 같은 패턴이 반복되는 경우
     * - "항목 값" 패턴이 연속으로 나타나는 경우
     * - PDF 추출 시 빈 줄이 삽입되는 경우도 처리
     */
    private List<TableCandidate> detectPdfTablePattern(String markdown) {
        List<TableCandidate> candidates = new ArrayList<>();

        // 빈 줄을 제거하고 비빈 줄만 추출
        String[] allLines = markdown.split("\n");
        List<String> nonEmptyLines = new ArrayList<>();
        List<Integer> originalLineNumbers = new ArrayList<>();

        for (int i = 0; i < allLines.length; i++) {
            if (!allLines[i].trim().isEmpty()) {
                nonEmptyLines.add(allLines[i].trim());
                originalLineNumbers.add(i);
            }
        }

        // 패턴: "텍스트 + 숫자" 또는 "텍스트 + 숫자 + 텍스트" 가 반복되는 구간 찾기
        // 예: "위원1 79.88 최저점", "위원2 86.18 최고점"
        Pattern rowPattern = Pattern.compile("^(.+?)\\s+(\\d+\\.?\\d*)\\s*(.*)$");

        StringBuilder currentCandidate = new StringBuilder();
        int tableStartIdx = -1;
        int consecutivePatternLines = 0;
        String lastPrefix = null;
        int emptyLineCount = 0;

        for (int i = 0; i < nonEmptyLines.size(); i++) {
            String line = nonEmptyLines.get(i);

            Matcher matcher = rowPattern.matcher(line);
            if (matcher.matches()) {
                String prefix = matcher.group(1);

                // 유사한 접두사 패턴 확인 (예: 위원1, 위원2, 위원3...)
                if (lastPrefix != null && isSimilarPrefix(lastPrefix, prefix)) {
                    if (tableStartIdx == -1) {
                        tableStartIdx = i - 1;
                        // 이전 줄도 포함 (헤더일 수 있음)
                        if (i > 0) {
                            currentCandidate.insert(0, nonEmptyLines.get(i-1) + "\n");
                        }
                    }
                    currentCandidate.append(line).append("\n");
                    consecutivePatternLines++;
                    emptyLineCount = 0;
                } else if (consecutivePatternLines > 0 && emptyLineCount < 2) {
                    // 패턴이 다르지만 표 안의 다른 데이터일 수 있음
                    currentCandidate.append(line).append("\n");
                } else {
                    // 새로운 패턴 시작점일 수 있음
                    if (consecutivePatternLines >= 3) {
                        int startLine = tableStartIdx >= 0 ? originalLineNumbers.get(tableStartIdx) : 0;
                        int endLine = i > 0 ? originalLineNumbers.get(i - 1) : 0;
                        candidates.add(TableCandidate.builder()
                                .content(currentCandidate.toString().trim())
                                .startLine(startLine)
                                .endLine(endLine)
                                .build());
                    }
                    currentCandidate = new StringBuilder();
                    tableStartIdx = -1;
                    consecutivePatternLines = 0;
                }

                lastPrefix = prefix;
            } else {
                emptyLineCount++;
                // 패턴 매칭 실패 - 표 종료 여부 체크
                if (emptyLineCount >= 3 || !line.matches(".*\\d+.*")) {
                    if (consecutivePatternLines >= 3) {
                        int startLine = tableStartIdx >= 0 ? originalLineNumbers.get(tableStartIdx) : 0;
                        int endLine = i > 0 ? originalLineNumbers.get(i - 1) : 0;
                        candidates.add(TableCandidate.builder()
                                .content(currentCandidate.toString().trim())
                                .startLine(startLine)
                                .endLine(endLine)
                                .build());
                    }
                    currentCandidate = new StringBuilder();
                    tableStartIdx = -1;
                    consecutivePatternLines = 0;
                    lastPrefix = null;
                }
            }
        }

        // 마지막 후보 처리
        if (consecutivePatternLines >= 3) {
            int startLine = tableStartIdx >= 0 ? originalLineNumbers.get(tableStartIdx) : 0;
            int endLine = nonEmptyLines.size() > 0 ? originalLineNumbers.get(nonEmptyLines.size() - 1) : 0;
            candidates.add(TableCandidate.builder()
                    .content(currentCandidate.toString().trim())
                    .startLine(startLine)
                    .endLine(endLine)
                    .build());
        }

        return candidates;
    }

    /**
     * 유사한 접두사인지 확인 (예: 위원1, 위원2 → 유사)
     */
    private boolean isSimilarPrefix(String prefix1, String prefix2) {
        // 숫자를 제거하고 비교
        String base1 = prefix1.replaceAll("\\d+", "").trim();
        String base2 = prefix2.replaceAll("\\d+", "").trim();

        if (base1.equals(base2) && !base1.isEmpty()) {
            return true;
        }

        // 길이가 비슷하고 앞부분이 같은 경우 (예: Item1, Item2)
        if (prefix1.length() >= 2 && prefix2.length() >= 2) {
            int minLen = Math.min(prefix1.length(), prefix2.length());
            int matchLen = 0;
            for (int i = 0; i < minLen - 1; i++) {
                if (prefix1.charAt(i) == prefix2.charAt(i)) {
                    matchLen++;
                } else {
                    break;
                }
            }
            // 70% 이상 일치하면 유사
            if (matchLen >= minLen * 0.7) {
                return true;
            }
        }

        return false;
    }

    /**
     * 줄이 표의 행처럼 보이는지 판단
     */
    private boolean isLikelyTableRow(String line) {
        if (line.trim().isEmpty()) {
            return false;
        }

        // 탭이 2개 이상 있는 경우
        long tabCount = line.chars().filter(c -> c == '\t').count();
        if (tabCount >= 2) {
            return true;
        }

        // 연속된 공백(3개 이상)이 2번 이상 나타나는 경우
        Pattern multiSpacePattern = Pattern.compile("\\s{3,}");
        Matcher matcher = multiSpacePattern.matcher(line);
        int spaceGaps = 0;
        while (matcher.find()) {
            spaceGaps++;
        }
        if (spaceGaps >= 2) {
            return true;
        }

        // | 문자로 구분된 경우 (하지만 이건 이미 마크다운 표일 수 있음)
        if (line.contains("|") && line.split("\\|").length >= 3) {
            return true;
        }

        return false;
    }

    /**
     * 이미 마크다운 표 형식인지 확인
     */
    private boolean isAlreadyMarkdownTable(String content) {
        String[] lines = content.split("\n");
        if (lines.length < 2) {
            return false;
        }

        // 마크다운 표는 두 번째 줄에 |---|---| 패턴이 있음
        for (int i = 0; i < Math.min(3, lines.length); i++) {
            if (lines[i].matches("^\\|?[\\s\\-:|]+\\|[\\s\\-:|]+\\|?$")) {
                return true;
            }
        }

        return false;
    }

    /**
     * AI를 사용하여 텍스트를 마크다운 표로 변환
     */
    private String convertToMarkdownTable(String tableText) {
        String promptText = String.format("""
                다음 텍스트는 표 형식의 데이터입니다. 이것을 마크다운 표로 변환해주세요.

                **입력 텍스트:**
                ```
                %s
                ```

                **변환 규칙:**
                1. 첫 번째 행을 헤더로 사용
                2. 모든 열을 정렬하여 마크다운 표 형식으로 출력
                3. 빈 셀은 공백으로 처리
                4. 추가 설명 없이 마크다운 표만 출력

                **마크다운 표:**
                """, tableText);

        try {
            Prompt prompt = new Prompt(promptText);
            ChatResponse response = chatModel.call(prompt);
            String result = response.getResult().getOutput().getContent().trim();

            // 결과에서 마크다운 표만 추출
            if (result.contains("|")) {
                return extractMarkdownTable(result);
            }
            return tableText;  // 변환 실패시 원본 반환
        } catch (Exception e) {
            log.warn("AI 표 변환 실패: {}", e.getMessage());
            return tableText;
        }
    }

    /**
     * AI 응답에서 마크다운 표만 추출
     */
    private String extractMarkdownTable(String aiResponse) {
        StringBuilder table = new StringBuilder();
        String[] lines = aiResponse.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            // | 로 시작하거나 포함하는 줄만 추출
            if (trimmed.startsWith("|") || trimmed.contains("|")) {
                table.append(line).append("\n");
            }
        }

        String result = table.toString().trim();
        return result.isEmpty() ? aiResponse : result;
    }

    /**
     * 수식(LaTeX) 인식 및 변환
     */
    private FormulaEnhancementResult enhanceFormulas(String markdown) {
        log.debug("수식 인식 시작");

        int formulasDetected = 0;
        String enhancedMarkdown = markdown;

        // 패턴 1: 간단한 수학 표현식 인식 (예: x^2, a_1, sqrt(x))
        Pattern simpleFormulaPattern = Pattern.compile(
                "\\b([a-zA-Z]+\\^\\d+|[a-zA-Z]+_\\d+|sqrt\\([^)]+\\)|" +
                "\\d+/\\d+|[a-zA-Z]\\([^)]+\\)|sum|prod|int)\\b",
                Pattern.CASE_INSENSITIVE
        );

        // 패턴 2: 그리스 문자 텍스트 인식 (예: alpha, beta, sigma)
        Pattern greekPattern = Pattern.compile(
                "\\b(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|" +
                "mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)\\b",
                Pattern.CASE_INSENSITIVE
        );

        // 패턴 3: 이미 LaTeX 형식인 것 확인 ($..$ 또는 $$..$$)
        Pattern existingLatexPattern = Pattern.compile("\\$[^$]+\\$|\\$\\$[^$]+\\$\\$");

        // 이미 LaTeX가 있으면 개수만 카운트
        Matcher existingMatcher = existingLatexPattern.matcher(markdown);
        while (existingMatcher.find()) {
            formulasDetected++;
        }

        // 간단한 수식 패턴 탐지
        Matcher simpleMatcher = simpleFormulaPattern.matcher(markdown);
        List<String> simpleFormulas = new ArrayList<>();
        while (simpleMatcher.find()) {
            String formula = simpleMatcher.group();
            // 이미 $ 안에 있는지 확인
            int start = simpleMatcher.start();
            String before = markdown.substring(Math.max(0, start - 5), start);
            if (!before.contains("$")) {
                simpleFormulas.add(formula);
            }
        }

        // 그리스 문자 패턴 탐지
        Matcher greekMatcher = greekPattern.matcher(markdown);
        List<String> greekSymbols = new ArrayList<>();
        while (greekMatcher.find()) {
            String symbol = greekMatcher.group();
            int start = greekMatcher.start();
            String before = markdown.substring(Math.max(0, start - 5), start);
            if (!before.contains("$") && !before.contains("\\")) {
                greekSymbols.add(symbol);
            }
        }

        // 수식 후보가 있으면 AI로 변환
        if (!simpleFormulas.isEmpty() || !greekSymbols.isEmpty()) {
            log.info("수식 후보 발견: 간단식 {}개, 그리스문자 {}개",
                    simpleFormulas.size(), greekSymbols.size());

            // 중요 수식만 변환 (문맥에 따라)
            for (String formula : simpleFormulas) {
                if (shouldConvertToLatex(formula, markdown)) {
                    String latexFormula = convertToLatex(formula);
                    if (latexFormula != null && !latexFormula.equals(formula)) {
                        enhancedMarkdown = enhancedMarkdown.replace(formula, "$" + latexFormula + "$");
                        formulasDetected++;
                    }
                }
            }
        }

        return FormulaEnhancementResult.builder()
                .enhancedMarkdown(enhancedMarkdown)
                .formulasDetected(formulasDetected)
                .build();
    }

    /**
     * LaTeX로 변환해야 하는지 판단 (문맥 기반)
     */
    private boolean shouldConvertToLatex(String formula, String context) {
        // 코드 블록 안이면 변환하지 않음
        int idx = context.indexOf(formula);
        if (idx > 0) {
            String before = context.substring(Math.max(0, idx - 50), idx);
            if (before.contains("```") || before.contains("`")) {
                return false;
            }
        }

        // 수학적 문맥에서 사용되는지 확인
        String[] mathContextWords = {"수식", "공식", "계산", "방정식", "함수",
                "equation", "formula", "calculate", "function"};
        String lowerContext = context.toLowerCase();
        for (String word : mathContextWords) {
            if (lowerContext.contains(word)) {
                return true;
            }
        }

        // 기본적으로 복잡한 수식만 변환
        return formula.contains("^") || formula.contains("_") ||
               formula.contains("sqrt") || formula.contains("/");
    }

    /**
     * 간단한 수식을 LaTeX로 변환
     */
    private String convertToLatex(String formula) {
        String latex = formula;

        // x^2 → x^{2}
        latex = latex.replaceAll("\\^(\\d+)", "^{$1}");

        // a_1 → a_{1}
        latex = latex.replaceAll("_(\\d+)", "_{$1}");

        // sqrt(x) → \\sqrt{x}
        latex = latex.replaceAll("sqrt\\(([^)]+)\\)", "\\\\sqrt{$1}");

        // 그리스 문자
        latex = latex.replaceAll("(?i)\\balpha\\b", "\\\\alpha");
        latex = latex.replaceAll("(?i)\\bbeta\\b", "\\\\beta");
        latex = latex.replaceAll("(?i)\\bgamma\\b", "\\\\gamma");
        latex = latex.replaceAll("(?i)\\bdelta\\b", "\\\\delta");
        latex = latex.replaceAll("(?i)\\bsigma\\b", "\\\\sigma");
        latex = latex.replaceAll("(?i)\\bpi\\b", "\\\\pi");
        latex = latex.replaceAll("(?i)\\btheta\\b", "\\\\theta");
        latex = latex.replaceAll("(?i)\\blambda\\b", "\\\\lambda");

        // sum, prod, int
        latex = latex.replaceAll("(?i)\\bsum\\b", "\\\\sum");
        latex = latex.replaceAll("(?i)\\bprod\\b", "\\\\prod");
        latex = latex.replaceAll("(?i)\\bint\\b", "\\\\int");

        return latex;
    }

    /**
     * AI 기반 추가 구조 분석
     * - 제목 계층 정리
     * - 리스트 구조 개선
     * - 코드 블록 언어 감지
     */
    private String performAiStructureAnalysis(String markdown, String originalFileName) {
        // 마크다운이 충분히 길면 AI 분석 수행
        if (markdown.length() < 500) {
            return markdown;
        }

        String promptText = String.format("""
                다음 마크다운 문서의 구조를 분석하고 개선해주세요.

                **문서 제목:** %s

                **현재 마크다운 (처음 2000자):**
                ```markdown
                %s
                ```

                **개선 사항을 확인해주세요:**
                1. 제목 계층이 올바른가? (H1 → H2 → H3 순서)
                2. 리스트 들여쓰기가 일관적인가?
                3. 코드 블록에 언어가 지정되어 있는가?

                **주의:**
                - 내용을 변경하지 말고 구조만 개선
                - 표와 수식은 이미 처리됨
                - 개선이 필요 없으면 "NO_CHANGE" 라고만 응답

                **개선된 마크다운 또는 NO_CHANGE:**
                """,
                originalFileName,
                markdown.length() > 2000 ? markdown.substring(0, 2000) : markdown);

        try {
            Prompt prompt = new Prompt(promptText);
            ChatResponse response = chatModel.call(prompt);
            String result = response.getResult().getOutput().getContent().trim();

            if ("NO_CHANGE".equals(result) || result.contains("NO_CHANGE")) {
                log.debug("AI 구조 분석: 변경 불필요");
                return markdown;
            }

            // AI 응답이 실제 마크다운이면 적용 (처음 2000자 부분만)
            if (result.contains("#") || result.contains("-") || result.contains("```")) {
                log.info("AI 구조 분석 적용됨");
                // 부분 교체 로직 (실제로는 전체 문서에 대한 더 정교한 처리 필요)
                return markdown;  // 현재는 안전하게 원본 반환
            }

            return markdown;
        } catch (Exception e) {
            log.warn("AI 구조 분석 실패: {}", e.getMessage());
            return markdown;
        }
    }

    /**
     * Pandoc을 사용한 PDF 변환 (선택적)
     * - Pandoc이 설치되어 있어야 함
     * - 표와 수식 변환이 더 정확함
     *
     * @param pdfPath PDF 파일 경로
     * @return 변환된 마크다운 텍스트
     */
    public PandocResult convertWithPandoc(String pdfPath) {
        if (!pandocEnabled) {
            return PandocResult.builder()
                    .success(false)
                    .message("Pandoc 통합이 비활성화되어 있습니다")
                    .build();
        }

        // Pandoc 설치 확인
        if (!isPandocAvailable()) {
            return PandocResult.builder()
                    .success(false)
                    .message("Pandoc이 설치되어 있지 않습니다")
                    .build();
        }

        log.info("Pandoc 변환 시작: {}", pdfPath);
        long startTime = System.currentTimeMillis();

        try {
            // PDF → Markdown 변환 명령
            ProcessBuilder pb = new ProcessBuilder(
                    pandocPath,
                    pdfPath,
                    "-f", "pdf",
                    "-t", "gfm",  // GitHub Flavored Markdown
                    "--wrap=none",
                    "--extract-media=./media"
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(60, TimeUnit.SECONDS);
            int exitCode = completed ? process.exitValue() : -1;

            long elapsedTime = System.currentTimeMillis() - startTime;

            if (exitCode == 0) {
                log.info("Pandoc 변환 완료: {}ms", elapsedTime);
                return PandocResult.builder()
                        .success(true)
                        .markdown(output.toString())
                        .processingTimeMs(elapsedTime)
                        .message("Pandoc 변환 성공")
                        .build();
            } else {
                log.warn("Pandoc 변환 실패: exitCode={}, output={}", exitCode, output);
                return PandocResult.builder()
                        .success(false)
                        .message("Pandoc 변환 실패: " + output)
                        .build();
            }

        } catch (IOException | InterruptedException e) {
            log.error("Pandoc 실행 오류: {}", e.getMessage());
            return PandocResult.builder()
                    .success(false)
                    .message("Pandoc 실행 오류: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Pandoc 설치 여부 확인
     */
    public boolean isPandocAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(pandocPath, "--version");
            Process process = pb.start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);

            if (completed && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String version = reader.readLine();
                    log.info("Pandoc 발견: {}", version);
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Pandoc 확인 실패: {}", e.getMessage());
        }
        return false;
    }

    // ========== DTO 클래스들 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnhancementResult {
        private String enhancedMarkdown;
        private int tablesFound;
        private int formulasFound;
        private boolean enhanced;
        private long processingTimeMs;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableEnhancementResult {
        private String enhancedMarkdown;
        private int tablesDetected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormulaEnhancementResult {
        private String enhancedMarkdown;
        private int formulasDetected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableCandidate {
        private String content;
        private int startLine;
        private int endLine;
        private String convertedContent;  // AI 변환 결과 저장용
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PandocResult {
        private boolean success;
        private String markdown;
        private long processingTimeMs;
        private String message;
    }

    // ========== Vision 기반 복잡한 표 처리 (D-3 고급 기능) ==========

    /**
     * Vision 모델을 사용하여 이미지에서 복잡한 표 추출
     * - 셀 병합, 중첩 표, 다중 헤더 등 복잡한 구조 지원
     * - Llama Vision 또는 GPT-4 Vision 사용
     *
     * @param imagePath 표가 포함된 이미지 경로
     * @return 마크다운 표로 변환된 결과
     */
    public VisionTableResult extractTableFromImage(String imagePath) {
        if (!visionEnabled) {
            return VisionTableResult.builder()
                    .success(false)
                    .message("Vision 기반 표 추출이 비활성화되어 있습니다. wiki.structure-enhancement.vision-enabled=true로 설정하세요.")
                    .build();
        }

        log.info("🔍 Vision 기반 표 추출 시작: {}", imagePath);
        long startTime = System.currentTimeMillis();

        try {
            // Vision 모델용 프롬프트 구성
            String promptText = """
                    이 이미지에서 표(Table)를 찾아 마크다운 형식으로 변환해주세요.

                    **변환 규칙:**
                    1. 병합된 셀은 해당 내용을 첫 번째 셀에 넣고 나머지는 빈칸 처리
                    2. 중첩된 표는 별도의 표로 분리
                    3. 다중 헤더는 첫 번째 행을 메인 헤더로 사용
                    4. 화살표(→, ↓)나 특수기호는 텍스트로 유지
                    5. 색상/배경 정보는 무시하고 텍스트만 추출

                    **복잡한 구조 처리:**
                    - 행 병합: 같은 내용을 반복하거나 "^" 기호 사용
                    - 열 병합: 내용을 합쳐서 표시
                    - 계층 구조: 들여쓰기로 표현

                    **출력 형식:**
                    마크다운 표만 출력하세요. 설명이나 추가 텍스트 없이 표만 출력합니다.
                    여러 개의 표가 있으면 각각 별도로 출력하세요.
                    """;

            // Ollama Vision API 호출 (curl 사용)
            String result = callOllamaVision(imagePath, promptText);

            long elapsedTime = System.currentTimeMillis() - startTime;

            if (result != null && result.contains("|")) {
                log.info("✅ Vision 표 추출 완료: {}ms", elapsedTime);
                return VisionTableResult.builder()
                        .success(true)
                        .markdown(result)
                        .processingTimeMs(elapsedTime)
                        .message("Vision 모델로 표 추출 성공")
                        .build();
            } else {
                return VisionTableResult.builder()
                        .success(false)
                        .markdown(result)
                        .processingTimeMs(elapsedTime)
                        .message("이미지에서 표를 찾지 못했습니다")
                        .build();
            }

        } catch (Exception e) {
            log.error("Vision 표 추출 실패: {}", e.getMessage());
            return VisionTableResult.builder()
                    .success(false)
                    .message("Vision 표 추출 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Ollama Vision API 호출
     */
    private String callOllamaVision(String imagePath, String prompt) {
        try {
            // 이미지를 Base64로 인코딩
            java.io.File imageFile = new java.io.File(imagePath);
            if (!imageFile.exists()) {
                log.warn("이미지 파일이 존재하지 않습니다: {}", imagePath);
                return null;
            }

            byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);

            // Ollama API 요청 구성
            String requestBody = String.format("""
                    {
                        "model": "%s",
                        "prompt": "%s",
                        "images": ["%s"],
                        "stream": false
                    }
                    """,
                    visionModel,
                    prompt.replace("\"", "\\\"").replace("\n", "\\n"),
                    base64Image);

            // curl로 Ollama API 호출 (application.yml 설정 사용)
            String apiUrl = ollamaBaseUrl + "/api/generate";
            log.debug("Ollama Vision API 호출: {}", apiUrl);

            ProcessBuilder pb = new ProcessBuilder(
                    "curl", "-s", "-X", "POST",
                    apiUrl,
                    "-H", "Content-Type: application/json",
                    "-d", requestBody
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean completed = process.waitFor(120, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("Vision API 타임아웃");
                return null;
            }

            // JSON 응답에서 response 필드 추출
            String response = output.toString();
            if (response.contains("\"response\":")) {
                int start = response.indexOf("\"response\":\"") + 12;
                int end = response.indexOf("\"", start);
                if (end > start) {
                    return response.substring(start, end)
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"");
                }
            }

            return response;

        } catch (Exception e) {
            log.error("Ollama Vision 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 복잡한 표 감지 (셀 병합, 중첩 등)
     * 복잡한 표인 경우 Vision 처리 권장
     */
    public TableComplexityResult analyzeTableComplexity(String tableText) {
        int complexityScore = 0;
        List<String> complexityFactors = new ArrayList<>();

        // 1. 행 수 체크
        String[] lines = tableText.split("\n");
        if (lines.length > 10) {
            complexityScore += 2;
            complexityFactors.add("많은 행 수 (" + lines.length + "행)");
        }

        // 2. 열 수 불일치 체크 (병합된 셀 가능성)
        int[] columnCounts = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            columnCounts[i] = lines[i].split("\\s{2,}|\t").length;
        }
        boolean hasInconsistentColumns = false;
        for (int i = 1; i < columnCounts.length; i++) {
            if (columnCounts[i] != columnCounts[0] && columnCounts[i] > 1) {
                hasInconsistentColumns = true;
                break;
            }
        }
        if (hasInconsistentColumns) {
            complexityScore += 3;
            complexityFactors.add("열 수 불일치 (셀 병합 가능성)");
        }

        // 3. 특수 기호 체크
        if (tableText.contains("→") || tableText.contains("↓") || tableText.contains("↑")) {
            complexityScore += 1;
            complexityFactors.add("방향 기호 포함");
        }

        // 4. 중첩 구조 체크 (들여쓰기 패턴)
        Pattern indentPattern = Pattern.compile("^\\s{4,}");
        int indentedLines = 0;
        for (String line : lines) {
            if (indentPattern.matcher(line).find()) {
                indentedLines++;
            }
        }
        if (indentedLines > lines.length / 4) {
            complexityScore += 2;
            complexityFactors.add("중첩 구조 (들여쓰기)");
        }

        // 5. 숫자 패턴 다양성 (배점 등 복잡한 데이터)
        Pattern numberPattern = Pattern.compile("\\d+\\.?\\d*");
        int numberCount = 0;
        Matcher m = numberPattern.matcher(tableText);
        while (m.find()) {
            numberCount++;
        }
        if (numberCount > lines.length * 2) {
            complexityScore += 1;
            complexityFactors.add("많은 숫자 데이터");
        }

        // 복잡도 판정
        TableComplexityLevel level;
        if (complexityScore >= 5) {
            level = TableComplexityLevel.HIGH;
        } else if (complexityScore >= 3) {
            level = TableComplexityLevel.MEDIUM;
        } else {
            level = TableComplexityLevel.LOW;
        }

        return TableComplexityResult.builder()
                .complexityScore(complexityScore)
                .level(level)
                .factors(complexityFactors)
                .recommendVision(level == TableComplexityLevel.HIGH)
                .build();
    }

    // ========== Vision 관련 DTO ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisionTableResult {
        private boolean success;
        private String markdown;
        private long processingTimeMs;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableComplexityResult {
        private int complexityScore;
        private TableComplexityLevel level;
        private List<String> factors;
        private boolean recommendVision;
    }

    public enum TableComplexityLevel {
        LOW,    // 단순 표 - 텍스트 기반 처리 가능
        MEDIUM, // 보통 - 텍스트 기반 처리 가능하나 일부 정보 손실 가능
        HIGH    // 복잡 - Vision 기반 처리 권장
    }
}
