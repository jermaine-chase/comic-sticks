package com.comicsticks.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
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
                .andExpect(jsonPath("$.panels[0].sourceText").value(""))
                // A two-sentence ending gets two panels, with its text under the last.
                .andExpect(jsonPath("$.panels[1].dialogue[0].type").value("shout"))
                .andExpect(jsonPath("$.panels[1].sourceText").value("Mia waved. \"Hello there!\" she said."));
    }

    @Test
    void estimatesFromJsonText() throws Exception {
        mvc.perform(post("/api/comics/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Mia waved. Leo ran.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.panels").value(2))
                .andExpect(jsonPath("$.parts").value(1))
                .andExpect(jsonPath("$.estimatedSeconds").value(1));
    }

    @Test
    void supportLevelChangesHowTextIsGrouped() throws Exception {
        String story = "Mia ran.\\n\\nLeo hid.\\n\\nMia found him.\\n\\nThey laughed.";
        mvc.perform(post("/api/comics/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + story + "\"}"))
                .andExpect(jsonPath("$.parts").value(4));
        mvc.perform(post("/api/comics/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + story + "\",\"supportLevel\":2}"))
                .andExpect(jsonPath("$.parts").value(1));
        var file = new MockMultipartFile("file", "story.txt", "text/plain",
                story.replace("\\n", "\n").getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/comics/generate").file(file).param("supportLevel", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.panels.length()").value(2))
                .andExpect(jsonPath("$.panels[1].sourceText").value(story.replace("\\n", "\n")));
    }

    @Test
    void findsTheCharacterSheetAndDrawsFromAnEditedOne() throws Exception {
        String story = "Mole met Ratty by the river. \\\"Hello!\\\" said Mole. Ratty waved.";
        mvc.perform(post("/api/comics/characters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + story + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characters[0].name").value("Mole"))
                .andExpect(jsonPath("$.characters[0].kind").value("animal"))
                .andExpect(jsonPath("$.places[0].background").value("riverbank"));

        String edited = """
                {"characters":[{"name":"Mole","description":"Mole: shy.","kind":"animal","color":"#123456",
                  "hat":"top_hat","hair":"none","accessory":"none","ears":"round","tail":"short","snout":"short"}],
                 "places":[]}""";
        mvc.perform(post("/api/comics/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + story + "\",\"characterSheet\":" + edited + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characters[0].hat").value("top_hat"))
                .andExpect(jsonPath("$.characters[0].color").value("#123456"))
                .andExpect(jsonPath("$.panels[0].characters[0].name").value("Mole"));
        var file = new MockMultipartFile("file", "story.txt", "text/plain",
                story.replace("\\\"", "\"").getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/comics/generate").file(file).param("characterSheet", edited))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characters[0].hat").value("top_hat"));
    }

    @Test
    void endsEveryComicWithBookendsAndAddsQuestionsWhenAsked() throws Exception {
        String story = "Mole met Ratty by the river. \\\"Hello!\\\" said Mole.\\n\\nRatty waved back.\\n\\n"
                + "They rowed home.";
        mvc.perform(post("/api/comics/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + story + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookends.whosWho.characters[0]").value("Mole"))
                .andExpect(jsonPath("$.bookends.whosWho.whereWeAre").value("We are at the river bank."))
                .andExpect(jsonPath("$.bookends.recap[*].sentence", hasItem(startsWith("At the end, "))))
                .andExpect(jsonPath("$.bookends.questions").isEmpty());
        var file = new MockMultipartFile("file", "story.txt", "text/plain",
                story.replace("\\n", "\n").replace("\\\"", "\"").getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/comics/generate").file(file).param("questions", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookends.questions[0].question").value("Who said \"Hello!\"?"))
                .andExpect(jsonPath("$.bookends.questions[0].choices[0].character").value("Mole"));
    }

    @Test
    void anUnknownSupportLevelIsRejected() throws Exception {
        mvc.perform(post("/api/comics/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Mia ran.\",\"supportLevel\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The support level must be 1, 2 or 3."));
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
