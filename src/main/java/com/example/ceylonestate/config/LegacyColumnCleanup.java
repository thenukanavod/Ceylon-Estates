package com.example.ceylonestate.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The old link-only version of articles had a NOT NULL "url" column. Hibernate's
 * ddl-auto=update never drops columns, so databases created back then still have it
 * and every insert into "articles" fails. This removes it once, before DataSeeder runs.
 */
@Component
@Order(0)
public class LegacyColumnCleanup implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public LegacyColumnCleanup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        Integer n = jdbc.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = database() and table_name = 'articles' and column_name = 'url'",
                Integer.class);
        if (n != null && n > 0) {
            jdbc.execute("alter table articles drop column url");
        }
    }
}
