# Postgres Cache Spring Boot Starter

A lightweight Spring Boot starter that provides a high‑performance cache implementation backed by PostgreSQL.  
It uses **UNLOGGED tables** to reduce disk I/O and improve write performance, making it a good fit for ephemeral or easily rebuildable cache data.

---

## Features

- ✅ **Spring Boot auto‑configuration**  
  Plug-and-play integration via Spring Boot’s auto‑config mechanism.

- ✅ **PostgreSQL‑backed cache**  
  Stores cache entries in a PostgreSQL table with:
  - `key` as a primary identifier
  - `value` as JSON/JSONB for flexible payloads
  - optional TTL/expiration metadata

- ✅ **UNLOGGED table for speed**  
  Uses an `UNLOGGED` table to:
  - avoid WAL (Write-Ahead Log) writes
  - increase insert/update performance
  - accept that data is non‑durable across crashes/restarts (appropriate for cache semantics)

- ✅ **Configurable schema and table name**  
  Choose where the cache table lives in your database.

- ✅ **Works with existing Spring Data / JDBC configuration**  
  Reuses your existing `DataSource` and Spring Boot PostgreSQL setup.

---

## When to Use This

Use this starter when:

- You already run PostgreSQL and want to avoid introducing a separate cache infrastructure (like Redis) for simple use cases.
- Cache durability is **not** critical (it’s okay to lose cache data on crash/restart).
- You need **fast** cache writes and reads and can accept the trade-offs of UNLOGGED tables.
- You are using **Spring Boot 3.x or newer**.

Do **not** use this if:

- You need cache data to be durable across database restarts or crashes.
- You require distributed cache semantics beyond what PostgreSQL provides.
- You cannot enable UNLOGGED tables in your environment.
- You are on **Spring Boot 2.x or older** (this starter does **not** support those versions).

---

## Getting Started

### 1. Add Dependency

In your `pom.xml`:
```xml
<dependency>
  <groupId>info.dsandrade</groupId>
  <artifactId>spring-boot-starter-postgres-cache</artifactId>
</dependency>
```

Ensure you also have:

- Spring Boot 3.x
- PostgreSQL driver (`org.postgresql:postgresql`)

### 2. Enable PostgreSQL Connection

Configure your PostgreSQL `DataSource` as usual in `application.yml` or `application.properties`:


## Configuration

The starter exposes configuration properties to control the cache schema and table name, and potentially other behaviors (TTL handling, etc.).

Example (YAML):

```yaml
postgres-cache:
  schema: my_cache_schema
  table: my_cache_table
```

Typical properties (names may vary depending on your version):

- `postgres-cache.schema`  
  Schema where the cache table is created.  
  **Default:** `public` (or whatever default is defined in the project)

- `postgres-cache.table-name`  
  Name of the cache table.  
  **Default:** a sensible default like `postgres_cache` (check actual defaults in the project).

On application startup, if the schema/table does not exist, the starter will create an **UNLOGGED** table with the configured name.

---

## How It Works

1. **Auto‑configuration (Spring Boot 3+)**  
   When your Spring Boot 3 application starts and the starter is on the classpath:
    - It detects a PostgreSQL `DataSource`.
    - It loads the cache properties from the environment (`postgres-cache.*`).

2. **Cache table initialization**  
   An initializer runs at startup and ensures the cache table exists. It:
    - Builds a `CREATE UNLOGGED TABLE IF NOT EXISTS <schema>.<table>` statement.
    - Defines:
        - `key` as `TEXT PRIMARY KEY`
        - `value` as JSON/JSONB
        - `ttl` as a timestamp column for expiration.
    - Executes this statement using Spring’s JDBC support.

3. **Using the cache**  
   Your application interacts with a cache service (or Spring’s cache abstraction, if wired that way) that:
    - Persists entries into the PostgreSQL cache table.
    - Reads entries by key.
    - Optionally leverages the TTL column for expiration logic (depending on the rest of your setup).

> Note: This project is focused on creating and managing the underlying cache table in PostgreSQL. The actual higher-level cache API usage depends on how your application integrates with it (e.g., via a service bean, Spring Cache abstraction, etc.).

---

## Example Usage (Conceptual)

```java
import service.info.dsandrade.postgrescache.service.PostgresCacheService;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {
    private final PostgresCacheService cacheService; // example abstraction

    public UserProfileService(PostgresCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public UserProfile getUserProfile(String userId) {
        String cacheKey = "user-profile:" + userId;

        return cacheService.get(cacheKey, UserProfile.class)
                .orElseGet(() -> {
                    UserProfile profile = loadFromPrimaryStore(userId);
                    cacheService.put(cacheKey, profile, Duration.ofMinutes(10));
                    return profile;
                });
    }
}
```

This is only an illustration. The actual API may vary, but the underlying storage is the UNLOGGED PostgreSQL table provided by this starter.

---

## UNLOGGED Tables: Trade‑offs

**Pros:**

- Faster writes and updates due to bypassing WAL.
- Reduced disk usage for transient cache data.

**Cons:**

- Data is **not crash‑safe**:
    - On a PostgreSQL crash, UNLOGGED tables are truncated.
- Not replicated via standard physical replication methods in the same way as logged tables.
- Should only be used for data that can be recomputed or refetched (like cache).

This matches typical cache semantics: if the table is lost, the application can rebuild it on demand.

---

## Requirements

- **Spring Boot:** **3.x or newer** (not compatible with Spring Boot 2.x)
- **Java:** 17+
- **Database:** PostgreSQL 9.5+ (recommended newer version for better JSONB/UNLOGGED support)

---

## Roadmap / Ideas

Potential future enhancements:

- Pluggable eviction strategies (LRU, size-based, etc.).
- Scheduled cleanup for expired entries based on TTL.
- Metrics integration (e.g., Micrometer) for cache hits/misses.
- Multi-tenant schema/table support.

---

## License

This project is licensed under the MIT License (or your actual license).  
See `LICENSE` file for details.