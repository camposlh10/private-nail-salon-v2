package com.nailsalon.backend.media;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nailsalon.backend.shared.error.ApiException;

/** Stores media on local disk under {@code app.media.local-dir}. */
@Component
public class LocalMediaStorage implements MediaStorage {

	private final Path root;

	public LocalMediaStorage(@Value("${app.media.local-dir}") String localDir) {
		this.root = Path.of(localDir).toAbsolutePath().normalize();
	}

	@Override
	public void store(String storageKey, byte[] bytes) {
		try {
			Files.createDirectories(root);
			Files.write(resolve(storageKey), bytes);
		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to store media " + storageKey, ex);
		}
	}

	@Override
	public byte[] read(String storageKey) {
		try {
			return Files.readAllBytes(resolve(storageKey));
		} catch (IOException ex) {
			throw ApiException.notFound("Media content not found");
		}
	}

	@Override
	public void delete(String storageKey) {
		try {
			Files.deleteIfExists(resolve(storageKey));
		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to delete media " + storageKey, ex);
		}
	}

	private Path resolve(String storageKey) {
		Path path = root.resolve(storageKey).normalize();
		if (!path.startsWith(root)) {
			// storageKeys are server-generated UUID names, but never trust a path join.
			throw ApiException.badRequest("Invalid storage key");
		}
		return path;
	}
}
