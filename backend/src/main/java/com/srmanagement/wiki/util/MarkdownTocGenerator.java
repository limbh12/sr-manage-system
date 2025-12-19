package com.srmanagement.wiki.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 마크다운 문서에 목차(Table of Contents)를 자동 생성하는 유틸리티
 */
@Slf4j
public class MarkdownTocGenerator {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private static final String TOC_MARKER = "<!-- TOC -->";
    private static final String TOC_END_MARKER = "<!-- /TOC -->";

    /**
     * 마크다운 문서에 목차 생성
     *
     * @param markdown 원본 마크다운 문서
     * @param generateToc 목차 생성 여부
     * @return 목차가 추가된 마크다운 문서
     */
    public static String generateTableOfContents(String markdown, boolean generateToc) {
        if (!generateToc || markdown == null || markdown.trim().isEmpty()) {
            return markdown;
        }

        log.info("목차 자동 생성 시작");

        // 기존 목차 제거
        String content = removeExistingToc(markdown);

        // 제목 추출
        List<HeadingInfo> headings = extractHeadings(content);

        if (headings.isEmpty()) {
            log.info("제목이 없어 목차를 생성하지 않음");
            return markdown;
        }

        // 목차 생성
        String toc = buildToc(headings);

        // 목차 삽입 위치 결정 (첫 번째 제목 앞)
        String result = insertToc(content, toc, headings.get(0));

        log.info("목차 생성 완료: {} 개의 제목", headings.size());
        return result;
    }

    /**
     * 기존 목차 제거
     */
    private static String removeExistingToc(String markdown) {
        Pattern tocPattern = Pattern.compile(
            Pattern.quote(TOC_MARKER) + ".*?" + Pattern.quote(TOC_END_MARKER),
            Pattern.DOTALL
        );
        return tocPattern.matcher(markdown).replaceAll("").trim();
    }

    /**
     * 마크다운에서 제목 추출
     */
    private static List<HeadingInfo> extractHeadings(String markdown) {
        List<HeadingInfo> headings = new ArrayList<>();
        Matcher matcher = HEADING_PATTERN.matcher(markdown);

        while (matcher.find()) {
            int level = matcher.group(1).length();
            String text = matcher.group(2).trim();
            int position = matcher.start();

            // H1은 문서 제목이므로 목차에서 제외
            if (level > 1) {
                headings.add(new HeadingInfo(level, text, position));
            }
        }

        return headings;
    }

    /**
     * 목차 문자열 생성
     */
    private static String buildToc(List<HeadingInfo> headings) {
        StringBuilder toc = new StringBuilder();
        toc.append(TOC_MARKER).append("\n");
        toc.append("## 📑 목차\n\n");

        for (HeadingInfo heading : headings) {
            String indent = "  ".repeat(heading.level - 2); // H2는 들여쓰기 없음, H3부터 2칸씩
            String anchor = generateAnchor(heading.text);

            toc.append(indent)
               .append("- [")
               .append(heading.text)
               .append("](#")
               .append(anchor)
               .append(")\n");
        }

        toc.append("\n").append(TOC_END_MARKER).append("\n\n");
        return toc.toString();
    }

    /**
     * 앵커 링크 생성 (GitHub/rehype-slug 호환)
     *
     * rehype-slug는 github-slugger를 사용하며 다음 규칙을 따름:
     * 1. 소문자 변환
     * 2. 공백을 하이픈(-)으로 변환
     * 3. 특수문자 제거 (알파벳, 숫자, 한글, 중국어, 일본어, 하이픈, 언더스코어만 유지)
     * 4. 연속된 하이픈을 하나로 축약
     * 5. 앞뒤 하이픈 제거
     */
    private static String generateAnchor(String text) {
        return text.toLowerCase()
                   // 특수문자 제거 (알파벳, 숫자, 한글, 하이픈, 언더스코어, 공백만 유지)
                   .replaceAll("[^a-z0-9가-힣\\s_-]", "")
                   // 공백을 하이픈으로 변환
                   .replaceAll("\\s+", "-")
                   // 연속된 하이픈을 하나로
                   .replaceAll("-+", "-")
                   // 앞뒤 하이픈 제거
                   .replaceAll("^-|-$", "");
    }

    /**
     * 목차를 첫 번째 제목 앞에 삽입
     */
    private static String insertToc(String content, String toc, HeadingInfo firstHeading) {
        int insertPosition = firstHeading.position;

        return content.substring(0, insertPosition) +
               toc +
               content.substring(insertPosition);
    }

    /**
     * 제목 정보 클래스
     */
    private static class HeadingInfo {
        final int level;
        final String text;
        final int position;

        HeadingInfo(int level, String text, int position) {
            this.level = level;
            this.text = text;
            this.position = position;
        }
    }
}
