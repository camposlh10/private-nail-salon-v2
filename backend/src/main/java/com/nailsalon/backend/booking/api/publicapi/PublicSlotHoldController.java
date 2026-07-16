package com.nailsalon.backend.booking.api.publicapi;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.SlotHoldCreate;
import com.nailsalon.backend.booking.api.publicapi.PublicBookingDtos.SlotHoldView;
import com.nailsalon.backend.booking.application.SlotHoldService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/public/slot-holds")
public class PublicSlotHoldController {

	private final SlotHoldService slotHoldService;

	public PublicSlotHoldController(SlotHoldService slotHoldService) {
		this.slotHoldService = slotHoldService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SlotHoldView create(@Valid @RequestBody SlotHoldCreate request) {
		return slotHoldService.create(request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void release(@PathVariable UUID id) {
		slotHoldService.release(id);
	}
}
