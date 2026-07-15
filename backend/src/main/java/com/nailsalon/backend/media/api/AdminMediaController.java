package com.nailsalon.backend.media.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nailsalon.backend.media.MediaService;
import com.nailsalon.backend.media.MediaService.MediaResult;

@RestController
@RequestMapping("/api/v1/admin/media")
public class AdminMediaController {

	private final MediaService media;

	public AdminMediaController(MediaService media) {
		this.media = media;
	}

	@PostMapping
	public ResponseEntity<MediaResult> upload(@RequestParam("file") MultipartFile file,
			@RequestParam(value = "altText", required = false) String altText) {
		MediaResult result = media.upload(file, altText);
		return ResponseEntity.created(URI.create(result.url())).body(result);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		media.delete(id);
		return ResponseEntity.noContent().build();
	}
}
