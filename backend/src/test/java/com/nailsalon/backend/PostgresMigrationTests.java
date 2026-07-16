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
				"service_category", "service", "service_add_on", "audit_event",
				"client", "weekly_availability", "availability_override", "blocked_time",
				"slot_hold", "phone_verified_session", "appointment", "appointment_item",
				"appointment_event" }) {
			Integer exists = jdbc.queryForObject(
					"SELECT count(*) FROM information_schema.tables WHERE table_name = ?",
					Integer.class, table);
			assertThat(exists).as("table %s exists", table).isEqualTo(1);
		}
	}

	/**
	 * The V16 GiST exclusion constraint (PostgreSQL-only, so untestable on H2): no two
	 * blocking-status appointments may overlap, half-open [start, end) semantics.
	 */
	@Test
	void appointmentOverlapIsRejectedAtTheDatabaseLevel() {
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		java.util.UUID clientId = java.util.UUID.randomUUID();
		jdbc.update("""
				INSERT INTO client (id, name, phone_e164, created_at, updated_at)
				VALUES (?, 'Constraint Probe', '+15550109999', now(), now())""", clientId);

		insertAppointment(jdbc, clientId, "2030-05-06T14:00:00Z", "2030-05-06T15:00:00Z", "CONFIRMED");
		// Back-to-back is allowed: [14:00, 15:00) then [15:00, 16:00).
		insertAppointment(jdbc, clientId, "2030-05-06T15:00:00Z", "2030-05-06T16:00:00Z", "CONFIRMED");
		// Cancelled appointments do not block.
		insertAppointment(jdbc, clientId, "2030-05-06T14:30:00Z", "2030-05-06T15:30:00Z",
				"CANCELLED_BY_CLIENT");

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> insertAppointment(jdbc, clientId,
				"2030-05-06T14:30:00Z", "2030-05-06T15:30:00Z", "CONFIRMED"))
				.hasMessageContaining("ex_appointment_no_overlap");
	}

	private static void insertAppointment(JdbcTemplate jdbc, java.util.UUID clientId, String start,
			String end, String status) {
		jdbc.update("""
				INSERT INTO appointment (id, client_id, status, start_at, end_at, timezone,
						source, created_at, updated_at)
				VALUES (?, ?, ?, ?::timestamptz, ?::timestamptz, 'America/New_York', 'OWNER',
						now(), now())""",
				java.util.UUID.randomUUID(), clientId, status, start, end);
	}
}
