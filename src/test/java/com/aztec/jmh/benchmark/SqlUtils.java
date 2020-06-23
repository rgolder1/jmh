package com.aztec.jmh.benchmark;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.jdbc.core.JdbcTemplate;

public class SqlUtils {

    /**
     * Drops the specified index.  Examines the databaseUrl to determine the database type in order to use the correct
     * syntax.
     */
    public static void dropIndexes(final JdbcTemplate jdbcTemplate, final String databaseUrl, final String... indexes) {
        if(databaseUrl.contains("mysql")) {
            // MySQL.
            List<Map<String, Object>> results = jdbcTemplate.queryForList("SHOW INDEX FROM event");

            Stream.of(indexes).forEach ( index -> {
                    if (results.stream().anyMatch(r -> r.get("Key_name").equals(index))) {
                        jdbcTemplate.execute("ALTER TABLE event DROP INDEX " + index);
                    }
                }
            );
        } else if(databaseUrl.contains("sqlserver")) {
            // SQL Server.
            Stream.of(indexes).forEach ( index -> {
                    jdbcTemplate.execute("DROP INDEX IF EXISTS jmh.event." + index);
                }
            );
        } else {
            // H2 and Postgres.
            Stream.of(indexes).forEach ( index -> {
                        jdbcTemplate.execute("DROP INDEX IF EXISTS " + index);
                    }
            );
        }
    }
}
