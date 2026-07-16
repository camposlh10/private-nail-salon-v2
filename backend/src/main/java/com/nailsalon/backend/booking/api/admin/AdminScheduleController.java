package com.nailsalon.backend.booking.api.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nailsalon.backend.booking.api.admin.ScheduleDtos.AdminBlock;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.AdminOverride;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.BlockWrite;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.OverrideWrite;
import com.nailsalon.backend.booking.api.admin.ScheduleDtos.WeeklySchedule;
import com.nailsalon.backend.booking.application.ScheduleAdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/schedule")
public class AdminScheduleController {

	private final ScheduleAdminService scheduleService;

	public AdminScheduleController(ScheduleAdminService scheduleService) {
		this.scheduleService = scheduleService;
	}

	@GetMapping("/weekly")
	public WeeklySchedule weekly() {
		return scheduleService.weekly();
	}

	@PutMapping("/weekly")
	public WeeklySchedule replaceWeekly(@Valid @RequestBody WeeklySchedule request) {
		return scheduleService.replaceWeekly(request);
	}

	@GetMapping("/overrides")
	public List<AdminOverride> overrides(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return scheduleService.overrides(from, to);
	}

	@PostMapping("/overrides")
	@ResponseStatus(HttpStatus.CREATED)
	public AdminOverride createOverride(@Valid @RequestBody OverrideWrite request) {
		return scheduleService.createOverride(request);
	}

	@DeleteMapping("/overrides/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteOverride(@PathVariable UUID id) {
		scheduleService.deleteOverride(id);
	}

	@GetMapping("/blocks")
	public List<AdminBlock> blocks(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return scheduleService.blocks(from, to);
	}

	@PostMapping("/blocks")
	@ResponseStatus(HttpStatus.CREATED)
	public AdminBlock createBlock(@Valid @RequestBody BlockWrite request) {
		return scheduleService.createBlock(request);
	}

	@DeleteMapping("/blocks/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteBlock(@PathVariable UUID id) {
		scheduleService.deleteBlock(id);
	}
}
