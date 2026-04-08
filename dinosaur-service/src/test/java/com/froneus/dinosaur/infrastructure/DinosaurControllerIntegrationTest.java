package com.froneus.dinosaur.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.CreateDinosaurRequest;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.UpdateDinosaurRequest;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class DinosaurControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("froneus_db")
            .withUsername("froneus")
            .withPassword("froneus_pass")
            .withInitScript("db/test-init.sql");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ── POST ──────────────────────────────────────────────────────────────────

    @Test
    void post_shouldReturn201() throws Exception {
        var req = new CreateDinosaurRequest("T-Rex Integration", "Theropod",
                LocalDateTime.of(1902, 1, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 59));

        mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("ALIVE"));
    }

    @Test
    void post_shouldReturn400WhenDuplicate() throws Exception {
        var req = new CreateDinosaurRequest("Duplicate Dino", "Theropod",
                LocalDateTime.of(1902, 1, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 59));

        mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre del dinosaurio ya existe"));
    }

    // ── GET list ──────────────────────────────────────────────────────────────

    @Test
    void getAll_shouldReturnPagedResponse() throws Exception {
        mockMvc.perform(get("/v1/dinosaur")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(10));
    }

    // ── GET by id ─────────────────────────────────────────────────────────────

    @Test
    void getById_shouldReturn204WhenNotFound() throws Exception {
        mockMvc.perform(get("/v1/dinosaur/99999"))
                .andExpect(status().isNoContent());
    }

    // ── PUT ───────────────────────────────────────────────────────────────────

    @Test
    void put_shouldReturn400WhenExtinct() throws Exception {
        // Primero creamos
        var createReq = new CreateDinosaurRequest("Extinct Dino", "Sauropod",
                LocalDateTime.of(1900, 1, 1, 0, 0),
                LocalDateTime.of(2020, 1, 1, 0, 0));
        String createResp = mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResp).get("id").asLong();

        // Intentamos actualizar con status EXTINCT — primero lo ponemos extinct via update
        var updateToExtinct = new UpdateDinosaurRequest(null, null, null, null, DinosaurStatus.EXTINCT);
        mockMvc.perform(put("/v1/dinosaur/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateToExtinct)))
                .andExpect(status().isOk());

        // Ahora intentamos modificar el extinct
        var updateAgain = new UpdateDinosaurRequest("New Name", null, null, null, null);
        mockMvc.perform(put("/v1/dinosaur/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateAgain)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot modify an EXTINCT dinosaur"));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void delete_shouldReturn204() throws Exception {
        var createReq = new CreateDinosaurRequest("Delete Me Dino", "Sauropod",
                LocalDateTime.of(1900, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 1, 0, 0));
        String createResp = mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResp).get("id").asLong();

        mockMvc.perform(delete("/v1/dinosaur/" + id))
                .andExpect(status().isNoContent());

        // Ya no debe aparecer en GET
        mockMvc.perform(get("/v1/dinosaur/" + id))
                .andExpect(status().isNoContent());
    }
}
