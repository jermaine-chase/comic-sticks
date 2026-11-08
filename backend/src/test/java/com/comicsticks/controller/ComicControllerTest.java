package com.comicsticks.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Exercises the HTTP layer end to end, using the demo generator in place of the LLM. */
@SpringBootTest(properties = "comic.story.max-input-chars=200")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class ComicControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void generatesFromJsonText() throws Exception {
        mvc.perform(post("/api/comics/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Mia waved. \\\"Hello there!\\\" she said.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.panels[0].panelNumber").value(1))
                .andExpect(jsonPath("$.panels[0].characters[0].name").value("Mia"))
                .andExpect(jsonPath("$.panels[0].characters[0].pose").value("standing"))
                .andExpect(jsonPath("$.panels[0].dialogue[0].type").value("shout"))
                .andExpect(jsonPath("$.panels[0].sourceText").value("Mia waved. \"Hello there!\" she said."));
    }

    @Test
    void estimatesFromJsonText() throws Exception {
        mvc.perform(post("/api/comics/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Mia waved. Leo ran.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.panels").value(1))
                .andExpect(jsonPath("$.parts").value(1))
                .andExpect(jsonPath("$.estimatedSeconds").value(1));
    }

    @Test
    void estimatesFromFile() throws Exception {
        var file = new MockMultipartFile("file", "story.pdf", "application/pdf", pdfContaining("Mia ran home."));

        mvc.perform(multipart("/api/comics/estimate").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.panels").value(1));
    }

    @Test
    void estimateAppliesTheSameLengthLimit() throws Exception {
        mvc.perform(post("/api/comics/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + "a".repeat(201) + "\"}"))
                .andExpect(status().isContentTooLarge());
    }

    @Test
    void generatesFromTextFile() throws Exception {
        var file = new MockMultipartFile("file", "story.txt", "text/plain",
                "Mia ran home.".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/comics/generate").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.panels[0].sourceText").value("Mia ran home."));
    }

    @Test
    void generatesFromPdfFile() throws Exception {
        var file = new MockMultipartFile("file", "story.pdf", "application/pdf", pdfContaining("Mia ran home."));

        mvc.perform(multipart("/api/comics/generate").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.panels[0].sourceText").value("Mia ran home."));
    }

    @Test
    void blankTextIsRejected() throws Exception {
        mvc.perform(post("/api/comics/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Story text is required."));
    }

    @Test
    void overlongTextIsRejected() throws Exception {
        mvc.perform(post("/api/comics/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + "a".repeat(201) + "\"}"))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.detail", containsString("limit")));
    }

    @Test
    void binaryFileIsRejected() throws Exception {
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 0, 0});

        mvc.perform(multipart("/api/comics/generate").file(file))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail", containsString(".pdf")));
    }

    @Test
    void pdfWithoutTextIsRejected() throws Exception {
        var file = new MockMultipartFile("file", "scan.pdf", "application/pdf", pdfContaining(null));

        mvc.perform(multipart("/api/comics/generate").file(file))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail", containsString("OCR")));
    }

    private static byte[] pdfContaining(String text) throws Exception {
        try (PDDocument document = new PDDocument(); var out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            if (text != null) {
                try (var stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(50, 700);
                    stream.showText(text);
                    stream.endText();
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
