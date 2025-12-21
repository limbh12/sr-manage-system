package com.srmanagement.wiki.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfConversionService {

    private final StructureEnhancementService structureEnhancementService;

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
     * PDF를 마크다운으로 변환하고 이미지 위치 정보 반환
     *
     * @param filePath PDF 파일 경로
     * @param originalFileName 원본 파일명
     * @return 마크다운 텍스트와 페이지별 이미지 맵
     * @throws IOException PDF 읽기 실패
     * @throws TikaException Tika 파싱 실패
     * @throws SAXException XML 파싱 실패
     */
    public PdfConversionResult convertPdfToMarkdownWithImages(String filePath, String originalFileName)
            throws IOException, TikaException, SAXException {
        log.info("PDF 변환 시작 (이미지 포함): {}", filePath);

        // 페이지별 텍스트 추출
        List<String> pageTexts = extractTextByPages(filePath);

        // 마크다운 생성
        StringBuilder markdown = new StringBuilder();
        String title = originalFileName.replaceAll("\\.pdf$", "");
        markdown.append("# ").append(title).append("\n\n");

        // 페이지별 텍스트 추가 (이미지 삽입 위치 마커 포함)
        for (int i = 0; i < pageTexts.size(); i++) {
            int pageNum = i + 1;
            String pageText = pageTexts.get(i);

            // 페이지 구분자 추가 (2페이지부터)
            if (pageNum > 1) {
                markdown.append("\n\n---\n\n");
                markdown.append("## Page ").append(pageNum).append("\n\n");
            }

            // 페이지 텍스트 추가
            markdown.append(convertToMarkdown(pageText, ""));

            // 이미지 삽입 위치 마커 (WikiFileService에서 대체됨)
            markdown.append("\n\n{{IMAGES_PAGE_").append(pageNum).append("}}\n\n");
        }

        return new PdfConversionResult(markdown.toString(), pageTexts.size());
    }

    /**
     * PDF를 페이지별로 텍스트 추출
     */
    private List<String> extractTextByPages(String filePath) throws IOException {
        List<String> pageTexts = new ArrayList<>();

        try (PDDocument document = PDDocument.load(new File(filePath))) {
            for (PDPage page : document.getPages()) {
                try {
                    // 각 페이지를 별도 파일처럼 처리
                    PDDocument singlePageDoc = new PDDocument();
                    singlePageDoc.addPage(page);

                    // 임시 파일로 저장 후 Tika로 텍스트 추출
                    File tempFile = File.createTempFile("page_", ".pdf");
                    singlePageDoc.save(tempFile);
                    singlePageDoc.close();

                    String pageText = extractTextFromPdf(tempFile.getAbsolutePath());
                    pageTexts.add(pageText);

                    tempFile.delete();
                } catch (Exception e) {
                    log.warn("페이지 텍스트 추출 실패, 빈 텍스트로 대체: {}", e.getMessage());
                    pageTexts.add("");
                }
            }
        }

        log.info("페이지별 텍스트 추출 완료: {} 페이지", pageTexts.size());
        return pageTexts;
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

    /**
     * PDF에서 이미지 추출
     *
     * @param pdfFilePath PDF 파일 경로
     * @param outputDir 이미지 저장 디렉토리
     * @return 추출된 이미지 정보 리스트
     * @throws IOException 이미지 추출 실패
     */
    public List<ExtractedImage> extractImages(String pdfFilePath, String outputDir) throws IOException {
        log.info("PDF 이미지 추출 시작: {}", pdfFilePath);

        List<ExtractedImage> extractedImages = new ArrayList<>();
        File outputDirectory = new File(outputDir);

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        try (PDDocument document = PDDocument.load(new File(pdfFilePath))) {
            int pageNum = 0;
            int imageCounter = 0;

            for (PDPage page : document.getPages()) {
                pageNum++;
                PDResources resources = page.getResources();

                if (resources == null) {
                    continue;
                }

                for (org.apache.pdfbox.cos.COSName name : resources.getXObjectNames()) {
                    PDXObject xobject = resources.getXObject(name);

                    if (xobject instanceof PDImageXObject) {
                        PDImageXObject image = (PDImageXObject) xobject;
                        imageCounter++;

                        // 이미지 파일명 생성
                        String filename = String.format("page_%d_img_%d.png", pageNum, imageCounter);
                        String filepath = outputDir + File.separator + filename;

                        // 이미지 저장
                        BufferedImage bImage = image.getImage();
                        ImageIO.write(bImage, "PNG", new File(filepath));

                        log.info("이미지 추출 완료: {} ({}x{})", filename, bImage.getWidth(), bImage.getHeight());

                        extractedImages.add(new ExtractedImage(
                                filename,
                                filepath,
                                pageNum,
                                imageCounter,
                                bImage.getWidth(),
                                bImage.getHeight(),
                                (long) new File(filepath).length()
                        ));
                    }
                }
            }

            log.info("PDF 이미지 추출 완료: 총 {} 개", extractedImages.size());
            return extractedImages;

        } catch (Exception e) {
            log.error("PDF 이미지 추출 실패: {}", pdfFilePath, e);
            throw new IOException("PDF 이미지 추출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 추출된 이미지 정보
     */
    @Data
    @AllArgsConstructor
    public static class ExtractedImage {
        private String filename;
        private String filepath;
        private int pageNumber;
        private int imageNumber;
        private int width;
        private int height;
        private Long fileSize;
    }

    /**
     * PDF를 마크다운으로 변환하고 AI 구조 보정 적용
     *
     * @param filePath PDF 파일 경로
     * @param originalFileName 원본 파일명
     * @param enableAiEnhancement AI 구조 보정 활성화 여부
     * @return 변환 결과 (마크다운 + 페이지 수 + AI 보정 정보)
     * @throws IOException PDF 읽기 실패
     * @throws TikaException Tika 파싱 실패
     * @throws SAXException XML 파싱 실패
     */
    public PdfConversionResult convertPdfToMarkdownWithAiEnhancement(
            String filePath, String originalFileName, boolean enableAiEnhancement)
            throws IOException, TikaException, SAXException {

        log.info("PDF 변환 시작 (AI 보정: {}): {}", enableAiEnhancement, filePath);
        long startTime = System.currentTimeMillis();

        // 1. 기본 PDF → 마크다운 변환
        PdfConversionResult basicResult = convertPdfToMarkdownWithImages(filePath, originalFileName);
        String markdown = basicResult.getMarkdown();

        // 2. AI 구조 보정 적용 (선택적)
        StructureEnhancementService.EnhancementResult enhancementResult = null;
        if (enableAiEnhancement) {
            try {
                enhancementResult = structureEnhancementService.enhanceMarkdown(markdown, originalFileName);
                if (enhancementResult.isEnhanced()) {
                    markdown = enhancementResult.getEnhancedMarkdown();
                    log.info("AI 구조 보정 적용됨: 표 {}개, 수식 {}개",
                            enhancementResult.getTablesFound(),
                            enhancementResult.getFormulasFound());
                }
            } catch (Exception e) {
                log.warn("AI 구조 보정 실패, 기본 변환 결과 사용: {}", e.getMessage());
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("PDF 변환 완료: {}ms", elapsedTime);

        return PdfConversionResult.builder()
                .markdown(markdown)
                .totalPages(basicResult.getTotalPages())
                .aiEnhanced(enhancementResult != null && enhancementResult.isEnhanced())
                .tablesFound(enhancementResult != null ? enhancementResult.getTablesFound() : 0)
                .formulasFound(enhancementResult != null ? enhancementResult.getFormulasFound() : 0)
                .processingTimeMs(elapsedTime)
                .build();
    }

    /**
     * Pandoc을 사용한 PDF 변환 (고품질 표/수식 변환)
     *
     * @param filePath PDF 파일 경로
     * @param originalFileName 원본 파일명
     * @return 변환 결과
     */
    public PdfConversionResult convertPdfWithPandoc(String filePath, String originalFileName)
            throws IOException, TikaException, SAXException {

        log.info("Pandoc PDF 변환 시도: {}", filePath);

        // Pandoc 변환 시도
        StructureEnhancementService.PandocResult pandocResult =
                structureEnhancementService.convertWithPandoc(filePath);

        if (pandocResult.isSuccess() && pandocResult.getMarkdown() != null) {
            log.info("Pandoc 변환 성공");
            return PdfConversionResult.builder()
                    .markdown(pandocResult.getMarkdown())
                    .totalPages(0)  // Pandoc은 페이지 수를 제공하지 않음
                    .aiEnhanced(true)
                    .usedPandoc(true)
                    .processingTimeMs(pandocResult.getProcessingTimeMs())
                    .build();
        }

        // Pandoc 실패 시 기본 변환으로 폴백
        log.warn("Pandoc 변환 실패, 기본 변환 사용: {}", pandocResult.getMessage());
        return convertPdfToMarkdownWithAiEnhancement(filePath, originalFileName, true);
    }

    /**
     * PDF 변환 결과 (마크다운 + 페이지 수 + AI 보정 정보)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PdfConversionResult {
        private String markdown;
        private int totalPages;
        private boolean aiEnhanced;
        private boolean usedPandoc;
        private int tablesFound;
        private int formulasFound;
        private long processingTimeMs;

        // 기존 호환성을 위한 생성자
        public PdfConversionResult(String markdown, int totalPages) {
            this.markdown = markdown;
            this.totalPages = totalPages;
            this.aiEnhanced = false;
            this.usedPandoc = false;
            this.tablesFound = 0;
            this.formulasFound = 0;
            this.processingTimeMs = 0;
        }
    }
}
