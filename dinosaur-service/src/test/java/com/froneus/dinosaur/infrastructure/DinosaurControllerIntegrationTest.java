package com.froneus.dinosaur.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.froneus.dinosaur.domain.model.DinosaurStatus;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.CreateDinosaurRequest;
import com.froneus.dinosaur.infrastructure.adapter.in.web.dto.UpdateDinosaurRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración del Controller.
 *
 * - PostgreSQL real via Testcontainers (requiere Docker Desktop activo)
 * - RabbitMQ mockeado con @MockBean
 * - Redis mockeado con @MockBean
 *
 * Si Docker no está disponible, correr solo los tests unitarios:
 *   mvn test -Dtest="!DinosaurControllerIntegrationTest"
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

    // Mockear Redis y RabbitMQ para no necesitar contenedores adicionales
    @MockBean RabbitTemplate rabbitTemplate;
    @MockBean StringRedisTemplate stringRedisTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled",      () -> "false");
        registry.add("spring.rabbitmq.host",       () -> "localhost");
        registry.add("spring.data.redis.host",     () -> "localhost");
    }

    @Autowired private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Mock Redis operations para que no fallen las llamadas de idempotencia y outbox
        ListOperations<String, String>  listOps  = mock(ListOperations.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForList()).thenReturn(listOps);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(listOps.rightPush(anyString(), anyString())).thenReturn(1L);
        when(listOps.leftPop(anyString())).thenReturn(null);
        when(valueOps.get(anyString())).thenReturn(null);
    }

    // ── POST ──────────────────────────────────────────────────────────────────

    @Test
    void post_shouldReturn201WithAliveStatus() throws Exception {
        var req = new CreateDinosaurRequest("T-Rex Integration", "Theropod",
                LocalDateTime.of(1902, 1, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 59));

        mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("T-Rex Integration"))
                .andExpect(jsonPath("$.status").value("ALIVE"));
    }

    @Test
    void post_shouldReturn400WhenDuplicateName() throws Exception {
        var req = new CreateDinosaurRequest("Duplicate Test Dino", "Theropod",
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
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Dinosaur name already exists"));
    }

    @Test
    void post_shouldReturn400WhenInvalidDates() throws Exception {
        var req = new CreateDinosaurRequest("Invalid Dates Dino", "Theropod",
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2020, 1, 1, 0, 0));

        mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── GET ───────────────────────────────────────────────────────────────────

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

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/v1/dinosaur/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Dinosaur not found"));
    }

    @Test
    void getById_shouldReturnDinosaurWhenExists() throws Exception {
        var req = new CreateDinosaurRequest("GetById Test", "Sauropod",
                LocalDateTime.of(1900, 1, 1, 0, 0),
                LocalDateTime.of(2030, 1, 1, 0, 0));
        String resp = mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).get("id").asLong();

        mockMvc.perform(get("/v1/dinosaur/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("GetById Test"));
    }

    // ── PUT ───────────────────────────────────────────────────────────────────

    @Test
    void put_shouldReturn400WhenModifyingExtinct() throws Exception {
        var req = new CreateDinosaurRequest("Extinct Test Dino", "Sauropod",
                LocalDateTime.of(1900, 1, 1, 0, 0),
                LocalDateTime.of(2020, 1, 1, 0, 0));
        String resp = mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).get("id").asLong();

        // Cambiar a EXTINCT
        var toExtinct = new UpdateDinosaurRequest(null, null, null, null, DinosaurStatus.EXTINCT);
        mockMvc.perform(put("/v1/dinosaur/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toExtinct)))
                .andExpect(status().isOk());

        // Intentar modificar → 400
        var update = new UpdateDinosaurRequest("New Name", null, null, null, null);
        mockMvc.perform(put("/v1/dinosaur/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot modify an EXTINCT dinosaur"));
    }

    @Test
    void put_shouldReturn404WhenNotFound() throws Exception {
        var req = new UpdateDinosaurRequest("Name", null, null, null, DinosaurStatus.ALIVE);
        mockMvc.perform(put("/v1/dinosaur/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Dinosaur not found"));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void delete_shouldReturn200WithMessage() throws Exception {
        var req = new CreateDinosaurRequest("Delete Test Dino", "Sauropod",
                LocalDateTime.of(1900, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 1, 0, 0));
        String resp = mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).get("id").asLong();

        mockMvc.perform(delete("/v1/dinosaur/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.message").value(containsString("successfully deleted")));
    }

    @Test
    void delete_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(delete("/v1/dinosaur/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Dinosaur not found"));
    }

    @Test
    void delete_shouldMakeDinosaurInvisibleAfterDeletion() throws Exception {
        var req = new CreateDinosaurRequest("Soft Delete Dino", "Sauropod",
                LocalDateTime.of(1900, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 1, 0, 0));
        String resp = mockMvc.perform(post("/v1/dinosaur")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).get("id").asLong();

        mockMvc.perform(delete("/v1/dinosaur/" + id))
                .andExpect(status().isOk());

        // Ya no aparece en GET
        mockMvc.perform(get("/v1/dinosaur/" + id))
                .andExpect(status().isNotFound());
    }
}
