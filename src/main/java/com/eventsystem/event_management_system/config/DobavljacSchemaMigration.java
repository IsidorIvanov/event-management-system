package com.eventsystem.event_management_system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DobavljacSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureRejtingNullable() {
        try {
            Integer notNullable = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'dobavljac'
                      AND COLUMN_NAME = 'rejting'
                      AND IS_NULLABLE = 'NO'
                    """, Integer.class);
            if (notNullable != null && notNullable > 0) {
                jdbcTemplate.execute("ALTER TABLE dobavljac MODIFY COLUMN rejting DECIMAL(3,2) NULL");
                log.info("Kolona dobavljac.rejting je postavljena na NULLABLE.");
            }
        } catch (Exception ex) {
            log.warn("Migracija dobavljac.rejting nije izvršena: {}", ex.getMessage());
        }
    }
}
