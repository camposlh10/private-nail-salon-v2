package com.nailsalon.backend.media;

/** Blob storage abstraction — local disk now, object storage (S3 etc.) later. */
public interface MediaStorage {

	void store(String storageKey, byte[] bytes);

	byte[] read(String storageKey);

	void delete(String storageKey);
}
