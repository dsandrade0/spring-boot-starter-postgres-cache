package info.dsandrade.postgrescache.config;

import info.dsandrade.postgrescache.properties.PostgresCacheProperties;
import info.dsandrade.postgrescache.service.PostgresCacheService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@AutoConfiguration
@EnableConfigurationProperties(PostgresCacheProperties.class)
@ConditionalOnClass(DataSource.class)
public class PostgresCacheAutoConfiguration {

    @Bean
    public PostgresCacheInitializer postgresCacheInitializer(JdbcTemplate jdbcTemplate, PostgresCacheProperties props) {
        return new PostgresCacheInitializer(jdbcTemplate, props);
    }

    @Bean
    public PostgresCacheService postgresCacgeService(JdbcTemplate jdbcTemplate, PostgresCacheProperties props) {
        return new PostgresCacheService(jdbcTemplate, props);
    }
}
