package com.nailsalon.backend.booking.api.admin;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.AdminAppointmentCreate;
import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.AdminAppointmentDetail;
import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.AdminAppointmentView;
import com.nailsalon.backend.booking.api.admin.AdminAppointmentDtos.StatusChangeRequest;
import com.nailsalon.backend.booking.application.AppointmentAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/appointments")
public class AdminAppointmentController {

	private final AppointmentAdminService appointmentService;

	public AdminAppointmentController(AppointmentAdminService appointmentService) {
		this.appointmentService = appointmentService;
	}

	@GetMapping
	public List<AdminAppointmentView> list(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return appointmentService.list(from, to);
	}

	@GetMapping("/{id}")
	public AdminAppointmentDetail get(@PathVariable UUID id) {
		return appointmentService.get(id);
	}

	@PostMapping
	public ResponseEntity<AdminAppointmentDetail> create(@Valid @RequestBody AdminAppointmentCreate request) {
		AdminAppointmentDetail created = appointmentService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/admin/appointments/" + created.id())).body(created);
	}

	@PatchMapping("/{id}/status")
	public AdminAppointmentDetail changeStatus(@PathVariable UUID id,
			@Valid @RequestBody StatusChangeRequest request) {
		return appointmentService.changeStatus(id, request.status());
	}
}
