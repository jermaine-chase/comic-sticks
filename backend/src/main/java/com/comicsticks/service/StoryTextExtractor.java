package com.comicsticks.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.comicsticks.exception.ComicGenerationException;

/** Reads story text out of an uploaded .txt or .pdf file. */
@Component
public class StoryTextExtractor {

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    public String extract(MultipartFile file) {
        byte[] bytes = readBytes(file);
        if (bytes.length == 0) {
            throw badFile("That file is empty.");
        }
        // Go by content: the browser-supplied name and content type are only hints.
        return looksLikePdf(bytes) ? extractPdf(bytes) : decodeText(bytes);
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ComicGenerationException(HttpStatus.BAD_REQUEST, "The upload could not be read.", e);
        }
    }

    private static boolean looksLikePdf(byte[] bytes) {
        return bytes.length >= PDF_MAGIC.length
                && Arrays.equals(Arrays.copyOf(bytes, PDF_MAGIC.length), PDF_MAGIC);
    }

    private static String extractPdf(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setParagraphEnd("\n\n"); // Keeps paragraphs apart for StorySegmenter.
            // A PDF's spacing is layout, not a scene break, so runs of blank lines become one.
            String text = stripper.getText(document).replace("\r\n", "\n").replaceAll("\\n{3,}", "\n\n").strip();
            if (text.isEmpty()) {
                throw new ComicGenerationException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "No text found in that PDF. It may be a scan, which would need OCR first.");
            }
            return text;
        } catch (InvalidPasswordException e) {
            throw badFile("That PDF is password protected.");
        } catch (IOException e) {
            throw new ComicGenerationException(HttpStatus.UNPROCESSABLE_CONTENT, "That PDF could not be read.", e);
        }
    }

    private static String decodeText(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            if (text.indexOf('\0') >= 0) {
                throw badFile("Only .txt and .pdf files are supported.");
            }
            return text.startsWith("﻿") ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            throw badFile("Only .txt (UTF-8) and .pdf files are supported.");
        }
    }

    private static ComicGenerationException badFile(String message) {
        return new ComicGenerationException(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
