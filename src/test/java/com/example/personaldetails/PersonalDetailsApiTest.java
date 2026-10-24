package com.example.personaldetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PersonalDetailsApiTest {

    private static final String VALID = """
            {"firstName":"Asha","lastName":"Rao","email":"asha@example.com",
             "phone":"+91 98765 43210","dateOfBirth":"1992-04-18",
             "address":"12 Marine Drive","city":"Mumbai","country":"India"}
            """;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createReadUpdateDelete() throws Exception {
        MockHttpSession session = new MockHttpSession();

        MvcResult created = mvc.perform(post("/api/people").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        String location = created.getResponse().getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);

        mvc.perform(get("/api/people").session(session))
                .andExpect(status().isOk())
                .andExpect(bodyContains("Asha"));

        mvc.perform(put("/api/people/" + id).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID.replace("Mumbai", "Pune")))
                .andExpect(status().isOk())
                .andExpect(bodyContains("Pune"));

        mvc.perform(delete("/api/people/" + id).session(session))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/people/" + id).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void eachSessionSeesOnlyItsOwnRecords() throws Exception {
        MockHttpSession alice = new MockHttpSession();
        MockHttpSession bob = new MockHttpSession();

        mvc.perform(post("/api/people").session(alice)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/people").session(bob))
                .andExpect(status().isOk())
                .andExpect(bodyEquals("[]"));
    }

    @Test
    void rejectsInvalidPayload() throws Exception {
        mvc.perform(post("/api/people").session(new MockHttpSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"Rao\",\"email\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(bodyContains("errors"))
                .andExpect(bodyContains("firstName"))
                .andExpect(bodyContains("email"));
    }

    @Test
    void infoEndpointReportsCommit() throws Exception {
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(bodyContains("\"commit\""));
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(bodyContains("UP"));
    }

    private static ResultMatcher bodyContains(String text) {
        return result -> {
            String body = result.getResponse().getContentAsString();
            assertTrue(body.contains(text), "Expected body to contain <" + text + "> but was: " + body);
        };
    }

    private static ResultMatcher bodyEquals(String text) {
        return result -> assertEquals(text, result.getResponse().getContentAsString().trim());
    }
}
