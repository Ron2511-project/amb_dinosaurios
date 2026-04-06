package com.froneus.dinosaur.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.CreateDinosaurRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración con Testcontainers.
 * Levanta Postgres y Redis en contenedores efímeros.
 * El schema se crea con ddl-auto=create-drop para el entorno de test.
 */
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
        // En test usamos update para que Hibernate cree las tablas
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createDinosaur_shouldReturn201WithStatusAlive() throws Exception {
        var request = new CreateDinosaurRequest(
                "Tyrannosaurus Rex", "Theropod",
                LocalDateTime.of(1902, 1, 1, 23, 59, 59),
                LocalDateTime.of(2023, 12, 31, 23, 59, 59)
        );

        mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Tyrannosaurus Rex"))
                .andExpect(jsonPath("$.status").value("ALIVE"));
    }

    @Test
    void createDinosaur_shouldReturn400WhenNameIsDuplicated() throws Exception {
        var request = new CreateDinosaurRequest(
                "Triceratops", "Ceratopsid",
                LocalDateTime.of(1889, 1, 1, 0, 0),
                LocalDateTime.of(2023, 6, 1, 0, 0)
        );

        mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El nombre del dinosaurio ya existe"));
    }

    @Test
    void createDinosaur_shouldReturn400WhenDatesInvalid() throws Exception {
        var request = new CreateDinosaurRequest(
                "InvalidDino", "Theropod",
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2020, 1, 1, 0, 0)
        );

        mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "La fecha de descubrimiento debe ser menor a la fecha de extinción"));
    }

    @Test
    void createDinosaur_shouldReturn400WhenBodyMalformed() throws Exception {
        mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la petición es inválido"));
    }

    @Test
    void createDinosaur_withIdempotencyKey_shouldNotCreateDuplicate() throws Exception {
        var request = new CreateDinosaurRequest(
                "Brachiosaurus", "Sauropod",
                LocalDateTime.of(1900, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 1, 0, 0)
        );
        String key = "idem-test-001";

        String body1 = mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String body2 = mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body1).isEqualTo(body2);
    }
}
