package com.srmanagement.wiki.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfConversionService {

    /**
     * PDF 파일을 텍스트로 추출
     *
     * @param filePath PDF 파일 경로
     * @return 추출된 텍스트
     * @throws IOException PDF 읽기 실패
     * @throws TikaException Tika 파싱 실패
     * @throws SAXException XML 파싱 실패
     */
    public String extractTextFromPdf(String filePath) throws IOException, TikaException, SAXException {
        log.info("PDF 텍스트 추출 시작: {}", filePath);

        try (InputStream stream = new FileInputStream(filePath)) {
            // Tika 파서 설정
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1); // 무제한 문자 추출
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            // PDF 파싱
            parser.parse(stream, handler, metadata, context);

            String text = handler.toString();
            log.info("PDF 텍스트 추출 완료: {} 문자", text.length());

            return text;
        } catch (Exception e) {
            log.error("PDF 텍스트 추출 실패: {}", filePath, e);
            throw e;
        }
    }

    /**
     * 추출된 텍스트를 마크다운 형식으로 변환
     *
     * @param text 추출된 텍스트
     * @param originalFileName 원본 파일명 (제목으로 사용)
     * @return 마크다운 형식의 텍스트
     */
    public String convertToMarkdown(String text, String originalFileName) {
        log.info("마크다운 변환 시작: {} 문자", text.length());

        StringBuilder markdown = new StringBuilder();

        // 파일명을 제목으로 추가
        String title = originalFileName.replaceAll("\\.pdf$", "");
        markdown.append("# ").append(title).append("\n\n");

        // 텍스트 정규화 및 구조화
        String[] lines = text.split("\\r?\\n");
        boolean inCodeBlock = false;
        boolean inList = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // 빈 줄
            if (trimmed.isEmpty()) {
                if (inList) {
                    inList = false;
                }
                markdown.append("\n");
                continue;
            }

            // 제목 인식 (대문자로만 구성된 짧은 줄)
            if (isLikelyHeading(trimmed)) {
                markdown.append("\n## ").append(trimmed).append("\n\n");
                continue;
            }

            // 리스트 항목 인식
            if (isListItem(trimmed)) {
                if (!inList) {
                    inList = true;
                }
                markdown.append("- ").append(trimmed.replaceFirst("^[•\\-\\*]\\s*", "")).append("\n");
                continue;
            }

            // 코드 블록 인식 (들여쓰기된 줄)
            if (trimmed.startsWith("    ") || trimmed.startsWith("\t")) {
                if (!inCodeBlock) {
                    markdown.append("\n```\n");
                    inCodeBlock = true;
                }
                markdown.append(trimmed).append("\n");
                continue;
            } else if (inCodeBlock) {
                markdown.append("```\n\n");
                inCodeBlock = false;
            }

            // 일반 텍스트
            markdown.append(trimmed).append("\n");
        }

        // 코드 블록이 닫히지 않았으면 닫기
        if (inCodeBlock) {
            markdown.append("```\n");
        }

        String result = markdown.toString();
        log.info("마크다운 변환 완료: {} 문자", result.length());

        return result;
    }

    /**
     * PDF를 마크다운으로 변환 (통합 메서드)
     *
     * @param filePath PDF 파일 경로
     * @param originalFileName 원본 파일명
     * @return 마크다운 텍스트
     * @throws IOException PDF 읽기 실패
     * @throws TikaException Tika 파싱 실패
     * @throws SAXException XML 파싱 실패
     */
    public String convertPdfToMarkdown(String filePath, String originalFileName) throws IOException, TikaException, SAXException {
        String text = extractTextFromPdf(filePath);
        return convertToMarkdown(text, originalFileName);
    }

    /**
     * 제목처럼 보이는 줄인지 판단
     */
    private boolean isLikelyHeading(String line) {
        // 짧은 줄 (3~50자)
        if (line.length() < 3 || line.length() > 50) {
            return false;
        }

        // 대부분 대문자로 구성
        long uppercaseCount = line.chars().filter(Character::isUpperCase).count();
        long letterCount = line.chars().filter(Character::isLetter).count();

        return letterCount > 0 && (uppercaseCount / (double) letterCount) > 0.7;
    }

    /**
     * 리스트 항목인지 판단
     */
    private boolean isListItem(String line) {
        return line.matches("^[•\\-\\*]\\s+.+") || line.matches("^\\d+\\.\\s+.+");
    }

    /**
     * PDF 메타데이터 추출
     */
    public Metadata extractMetadata(String filePath) throws IOException, TikaException, SAXException {
        try (InputStream stream = new FileInputStream(filePath)) {
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            parser.parse(stream, handler, metadata, context);

            log.info("PDF 메타데이터: 제목={}, 작성자={}, 페이지수={}",
                    metadata.get("title"),
                    metadata.get("author"),
                    metadata.get("xmpTPg:NPages"));

            return metadata;
        }
    }
}
