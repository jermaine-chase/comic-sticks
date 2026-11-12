package com.comicsticks.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.comicsticks.persistence.SavedComicRepository;
import com.jayway.jsonpath.JsonPath;

/** Saving, listing, opening, updating and deleting comics, against the demo profile's H2 database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class SavedComicControllerTest {

    private static final String COMIC = """
            {"title":"The Attic Map","panels":[
              {"panelNumber":1,"scene":"indoor","caption":"","characters":[
                {"name":"Mia","pose":"pointing","expression":"surprised","facing":"right","feeling":"surprised"}],
               "dialogue":[{"speaker":"Mia","text":"Look!","type":"shout"}],
               "sourceText":"Mia found a map. \\"Look!\\" she shouted.","notes":[],"place":"The attic",
               "things":[{"thing":"map","holder":"Mia"}]},
              {"panelNumber":2,"scene":"indoor","caption":"","characters":[],"dialogue":[],
               "sourceText":"","notes":[],"place":""}],
             "characters":[{"name":"Mia","description":"Mia: a girl.","kind":"person","color":"#1b1b1f",
               "hat":"cap","hair":"none","accessory":"none","ears":"none","tail":"none","snout":"none",
               "added":false,"species":"none","stance":"upright"}],
             "places":[{"name":"The attic","description":"A dusty room.","background":"room"}],
             "bookends":null}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SavedComicRepository repository;

    @BeforeEach
    void empty() {
        repository.deleteAll();
    }

    private static String body(String comic) {
        return "{\"comic\":" + comic + ",\"options\":{\"supportLevel\":2,\"feelingLabels\":true,\"notes\":false,"
                + "\"questions\":false,\"things\":true}}";
    }

    private String save(String comic) throws Exception {
        String reply = mvc.perform(post("/api/saved-comics").contentType(MediaType.APPLICATION_JSON).content(body(comic)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/saved-comics/")))
                .andExpect(jsonPath("$.title").value("The Attic Map"))
                .andExpect(jsonPath("$.panelCount").value(2))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(reply, "$.id");
    }

    @Test
    void savesAComicAndOpensItExactlyAsSaved() throws Exception {
        String id = save(COMIC);

        mvc.perform(get("/api/saved-comics/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comic.panels[0].dialogue[0].text").value("Look!"))
                .andExpect(jsonPath("$.comic.panels[0].things[0].holder").value("Mia"))
                .andExpect(jsonPath("$.comic.characters[0].hat").value("cap"))
                .andExpect(jsonPath("$.comic.places[0].background").value("room"))
                .andExpect(jsonPath("$.options.supportLevel").value(2))
                .andExpect(jsonPath("$.options.notes").value(false));
    }

    @Test
    void listsSavedComicsMostRecentFirst() throws Exception {
        save(COMIC);
        String second = save(COMIC.replace("The Attic Map", "The Attic Map"));
        mvc.perform(put("/api/saved-comics/" + second).contentType(MediaType.APPLICATION_JSON)
                        .content(body(COMIC.replace("The Attic Map", "Back to the Attic"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Back to the Attic"));

        mvc.perform(get("/api/saved-comics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Back to the Attic"))
                .andExpect(jsonPath("$[0].panelCount").value(2))
                .andExpect(jsonPath("$[0].comic").doesNotExist());
    }

    @Test
    void savesEditsOverTheSavedComic() throws Exception {
        String id = save(COMIC);

        mvc.perform(put("/api/saved-comics/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(body(COMIC.replace("\"Look!\",\"type\"", "\"Look at this!\",\"type\""))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/saved-comics/" + id))
                .andExpect(jsonPath("$.comic.panels[0].dialogue[0].text").value("Look at this!"));
    }

    @Test
    void deletesAComic() throws Exception {
        String id = save(COMIC);

        mvc.perform(delete("/api/saved-comics/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/saved-comics/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("That comic was not found. It may have been deleted."));
        mvc.perform(delete("/api/saved-comics/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void refusesIdsThatAreNotUuids() throws Exception {
        mvc.perform(get("/api/saved-comics/..%2F..%2Fetc")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/saved-comics/not-an-id")).andExpect(status().isBadRequest());
    }

    @Test
    void needsAComicToSave() throws Exception {
        mvc.perform(post("/api/saved-comics").contentType(MediaType.APPLICATION_JSON).content("{\"options\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void namesAnUntitledComic() throws Exception {
        String reply = mvc.perform(post("/api/saved-comics").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comic\":{\"title\":\"  \",\"panels\":[]}}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/saved-comics/" + JsonPath.read(reply, "$.id")))
                .andExpect(jsonPath("$.title").value("Untitled comic"))
                .andExpect(jsonPath("$.panelCount").value(0));
    }
}
