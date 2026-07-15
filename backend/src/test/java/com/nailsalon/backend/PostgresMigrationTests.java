package com.nailsalon.backend;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the full application against a real PostgreSQL container so Flyway migrations are
 * validated with production fidelity (types, constraints, indexes) from an empty database.
 *
 * <p>{@code disabledWithoutDocker = true} makes the whole class skip on machines without a
 * Docker daemon (e.g. local dev without Docker Desktop); it runs in CI where Docker is present.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private DataSource dataSource;

	@Test
	void migrationsApplyCleanlyAndCreateCatalogTables() {
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);

		Integer applied = jdbc.queryForObject(
				"SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
		assertThat(applied).isNotNull().isGreaterThanOrEqualTo(7);

		for (String table : new String[] { "business_profile", "owner_user", "media_asset",
				"service_category", "service", "service_add_on", "audit_event" }) {
			Integer exists = jdbc.queryForObject(
					"SELECT count(*) FROM information_schema.tables WHERE table_name = ?",
					Integer.class, table);
			assertThat(exists).as("table %s exists", table).isEqualTo(1);
		}
	}
}
