package com.nailsalon.backend.media.api;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nailsalon.backend.media.MediaAsset;
import com.nailsalon.backend.media.MediaService;

/**
 * Serves uploaded images. Media ids are immutable (re-uploading creates a new asset),
 * so responses are cacheable for a year with an ETag for revalidation.
 */
@RestController
@RequestMapping("/api/v1/public/media")
public class PublicMediaController {

	private final MediaService media;

	public PublicMediaController(MediaService media) {
		this.media = media;
	}

	@GetMapping("/{id}")
	public ResponseEntity<byte[]> get(@PathVariable UUID id) {
		MediaAsset asset = media.requireReady(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(asset.getContentType()))
				.cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
				.eTag("\"" + asset.getId() + "\"")
				.body(media.content(asset));
	}
}
