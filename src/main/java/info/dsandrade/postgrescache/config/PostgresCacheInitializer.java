package info.dsandrade.postgrescache.config;

import info.dsandrade.postgrescache.properties.PostgresCacheProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresCacheInitializer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JdbcTemplate jdbcTemplate;
    private final PostgresCacheProperties props;

    public PostgresCacheInitializer(JdbcTemplate jdbcTemplate, PostgresCacheProperties props) {
        this.jdbcTemplate = jdbcTemplate;
        this.props = props;
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing Postgres Redis table");
        String sql = String.format("""
            CREATE UNLOGGED TABLE IF NOT EXISTS %s.%s (
                key TEXT PRIMARY KEY,
                value jsonb,
                ttl TIMESTAMP
            )
        """, props.getSchema(), props.getTableName());
        jdbcTemplate.execute(sql);
    }
}
