package info.dsandrade.postgrescache.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "postgres-cache")
public class PostgresCacheProperties {
    private String tableName = "mem_cache";
    private String schema = "public";
    private boolean h2 = false;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public boolean isH2() {
        return h2;
    }

    public void setH2(boolean h2) {
        this.h2 = h2;
    }
}
