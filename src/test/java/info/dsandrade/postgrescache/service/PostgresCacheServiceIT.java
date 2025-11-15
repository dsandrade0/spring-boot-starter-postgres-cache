package info.dsandrade.postgrescache.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.dsandrade.postgrescache.properties.PostgresCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresCacheServiceIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("cache_db")
                    .withUsername("test")
                    .withPassword("test");

    private JdbcTemplate jdbcTemplate;
    private PostgresCacheService cacheService;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
        jdbcTemplate = new JdbcTemplate(dataSource);

        // Ensure schema + table exist (matches your starter behaviour)
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS public");
        jdbcTemplate.execute("""
                CREATE UNLOGGED TABLE IF NOT EXISTS public.postgres_cache (
                    key TEXT PRIMARY KEY,
                    value jsonb,
                    ttl TIMESTAMP
                )
                """);

        PostgresCacheProperties props = new PostgresCacheProperties();
        props.setSchema("public");
        props.setTableName("postgres_cache");

        cacheService = new PostgresCacheService(jdbcTemplate, props, new ObjectMapper());
    }

    @Test
    void putAndGetShouldRoundTripObjectAgainstRealPostgres() {
        String key = "user-profile:123";
        UserProfile profile = new UserProfile("123", "Alice");

        cacheService.put(key, profile, Duration.ofMinutes(5));

        Optional<UserProfile> cached = cacheService.get(key, UserProfile.class);

        assertThat(cached).isPresent();
        assertThat(cached.get().getId()).isEqualTo("123");
        assertThat(cached.get().getName()).isEqualTo("Alice");
    }

    @Test
    void getShouldReturnEmptyWhenKeyDoesNotExist() {
        Optional<UserProfile> cached = cacheService.get("missing-key", UserProfile.class);

        assertThat(cached).isEmpty();
    }

    @Test
    void getGenericShouldReturnEmptyWhenKeyDoesNotExist() {
        Optional<UserProfile> cached = cacheService.get("missing-generic-key", UserProfile.class);

        assertThat(cached).isEmpty();
    }

    @Test
    void expiredEntriesShouldNotBeReturnedFromGenericGet() {
        String key = "expiring-generic-key";
        UserProfile profile = new UserProfile("999", "Temp User");

        cacheService.put(key, profile, Duration.ofMinutes(1));

        // Force TTL to be in the past
        LocalDateTime past = LocalDateTime.now().minusMinutes(10);
        jdbcTemplate.update("UPDATE public.postgres_cache SET ttl = ? WHERE key = ?", past, key);

        Optional<UserProfile> cached = cacheService.get(key, UserProfile.class);

        assertThat(cached).isEmpty();
    }

    @Test
    void deleteShouldRemoveEntryFromRealPostgres() {
        String key = "delete-key";
        UserProfile profile = new UserProfile("777", "To Be Deleted");

        cacheService.put(key, profile, Duration.ofMinutes(5));
        assertThat(cacheService.get(key, UserProfile.class)).isPresent();

        cacheService.delete(key);

        assertThat(cacheService.get(key, UserProfile.class)).isEmpty();
    }
}
