package com.knowledge.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class DocumentParser {
    
    public static String parseFile(File file, String fileType) throws IOException {
        switch (fileType.toLowerCase()) {
            case "pdf":
                return parsePdf(file);
            case "docx":
            case "doc":
                return parseWord(file);
            case "txt":
                return parseTxt(file);
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
    }
    
    private static String parsePdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    private static String parseWord(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            StringBuilder content = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                content.append(paragraph.getText()).append("\n");
            }
            return content.toString();
        }
    }
    
    private static String parseTxt(File file) throws IOException {
        return new String(Files.readAllBytes(Paths.get(file.getPath())), "UTF-8");
    }
}
