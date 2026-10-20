package com.example.ceylonestate.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Removes long dashes (em dash and en dash) from text that is already stored in the database,
 * for example listing titles, article text and broker descriptions that were saved by an earlier version.
 * (The source files themselves no longer contain any.)
 *
 * <p>An em dash between words becomes a comma, an en dash between words becomes a plain hyphen.
 * It runs first on every start-up, touches only the rows that contain one of those characters, and never
 * stops the application from starting if a column cannot be updated.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DashCleaner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DashCleaner.class);

    private static final String EM_DASH = "\u2014";
    private static final String EN_DASH = "\u2013";
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9_]+");

    private final JdbcTemplate jdbc;

    public DashCleaner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        List<Map<String, Object>> columns;
        try {
            columns = jdbc.queryForList(
                    "SELECT table_name AS t, column_name AS c FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() "
                            + "AND data_type IN ('varchar', 'char', 'tinytext', 'text', 'mediumtext', 'longtext')");
        } catch (Exception e) {
            log.warn("Dash clean-up skipped (could not read the table list): {}", e.getMessage());
            return;
        }

        int rows = 0;
        for (Map<String, Object> column : columns) {
            String table = String.valueOf(column.get("t"));
            String name = String.valueOf(column.get("c"));
            if (!SAFE_NAME.matcher(table).matches() || !SAFE_NAME.matcher(name).matches()) {
                continue;
            }
            String sql = "UPDATE `" + table + "` SET `" + name + "` = "
                    + "REPLACE(REPLACE(REPLACE(REPLACE(`" + name + "`, ?, ?), ?, ?), ?, ?), ?, ?) "
                    + "WHERE `" + name + "` LIKE ? OR `" + name + "` LIKE ?";
            try {
                rows += jdbc.update(sql,
                        " " + EM_DASH + " ", ", ",
                        EM_DASH, ", ",
                        " " + EN_DASH + " ", " - ",
                        EN_DASH, "-",
                        "%" + EM_DASH + "%", "%" + EN_DASH + "%");
            } catch (Exception e) {
                log.warn("Dash clean-up skipped for {}.{}: {}", table, name, e.getMessage());
            }
        }
        if (rows > 0) {
            log.info("Removed long dashes from {} stored text values.", rows);
        }
    }
}
