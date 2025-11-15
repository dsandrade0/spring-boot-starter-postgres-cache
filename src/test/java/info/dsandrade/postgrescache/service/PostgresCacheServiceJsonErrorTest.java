package info.dsandrade.postgrescache.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.dsandrade.postgrescache.properties.PostgresCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class PostgresCacheServiceJsonErrorTest {

    private JdbcTemplate jdbcTemplate;
    private PostgresCacheProperties props;
    private ObjectMapper objectMapper;
    private PostgresCacheService cacheService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        props = new PostgresCacheProperties();
        props.setSchema("public");
        props.setTableName("postgres_cache");
        objectMapper = Mockito.mock(ObjectMapper.class);

        cacheService = new PostgresCacheService(jdbcTemplate, props, objectMapper);
    }

    @Test
    void putShouldWrapJsonProcessingExceptionInIllegalStateException() throws Exception {
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") {});

        assertThrows(IllegalStateException.class,
                () -> cacheService.put("key", new Object(), Duration.ofMinutes(1)));
    }

    @Test
    void genericGetShouldWrapJsonProcessingExceptionInIllegalStateException() throws Exception {
        // Simulate that DB returns some JSON string
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), any()))
                .thenReturn("{ invalid json }");
        // And ObjectMapper fails to parse it
        when(objectMapper.readValue(anyString(), eq(Object.class)))
                .thenThrow(new JsonProcessingException("boom") {});

        assertThrows(IllegalStateException.class,
                () -> cacheService.get("key", Object.class));
    }
}
