package com.nailsalon.backend.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nailsalon.backend.catalog.infrastructure.SalonServiceRepository;
import com.nailsalon.backend.shared.audit.AuditRecorder;
import com.nailsalon.backend.shared.error.ApiException;

import java.awt.image.BufferedImage;

/**
 * Image upload with defense-in-depth validation: declared MIME type, actual file
 * signature (magic bytes), size cap, and decodable dimensions. JPEG and PNG only in V1.
 */
@Service
public class MediaService {

	public record MediaResult(UUID id, String url, String contentType, long fileSize,
			Integer width, Integer height, String altText) {

		static MediaResult from(MediaAsset asset) {
			return new MediaResult(asset.getId(), "/api/v1/public/media/" + asset.getId(),
					asset.getContentType(), asset.getFileSize(), asset.getWidth(), asset.getHeight(),
					asset.getAltText());
		}
	}

	static final int MAX_BYTES = 5 * 1024 * 1024;
	static final int MAX_DIMENSION = 8000;

	private final MediaAssetRepository assets;
	private final SalonServiceRepository services;
	private final MediaStorage storage;
	private final AuditRecorder audit;

	public MediaService(MediaAssetRepository assets, SalonServiceRepository services,
			MediaStorage storage, AuditRecorder audit) {
		this.assets = assets;
		this.services = services;
		this.storage = storage;
		this.audit = audit;
	}

	@Transactional
	public MediaResult upload(MultipartFile file, String altText) {
		byte[] bytes = readBytes(file);
		if (bytes.length == 0) {
			throw ApiException.badRequest("Empty file");
		}
		if (bytes.length > MAX_BYTES) {
			throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE",
					"Image exceeds the " + (MAX_BYTES / (1024 * 1024)) + "MB limit");
		}
		String extension = detectImageType(file.getContentType(), bytes);
		BufferedImage image = decode(bytes);

		MediaAsset asset = new MediaAsset();
		asset.setStorageKey(UUID.randomUUID() + "." + extension);
		asset.setContentType("image/" + ("jpg".equals(extension) ? "jpeg" : extension));
		asset.setFileSize(bytes.length);
		asset.setWidth(image.getWidth());
		asset.setHeight(image.getHeight());
		asset.setAltText(altText);
		assets.saveAndFlush(asset);
		storage.store(asset.getStorageKey(), bytes);
		audit.record("MEDIA_UPLOADED", "MediaAsset", asset.getId(), asset.getStorageKey());
		return MediaResult.from(asset);
	}

	@Transactional
	public void delete(UUID id) {
		MediaAsset asset = assets.findById(id)
				.orElseThrow(() -> ApiException.notFound("Media not found"));
		if (services.existsByImageId(id)) {
			throw ApiException.conflict("Image is still referenced by a service");
		}
		storage.delete(asset.getStorageKey());
		assets.delete(asset);
		audit.record("MEDIA_DELETED", "MediaAsset", id, asset.getStorageKey());
	}

	@Transactional(readOnly = true)
	public MediaAsset requireReady(UUID id) {
		return assets.findById(id)
				.filter(asset -> asset.getStatus() == MediaAsset.Status.READY)
				.orElseThrow(() -> ApiException.notFound("Media not found"));
	}

	public byte[] content(MediaAsset asset) {
		return storage.read(asset.getStorageKey());
	}

	// --- validation helpers -------------------------------------------------------

	private static byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException ex) {
			throw ApiException.badRequest("Unreadable upload");
		}
	}

	/** Returns the file extension after verifying declared MIME type AND magic bytes agree. */
	private static String detectImageType(String declaredContentType, byte[] bytes) {
		boolean pngSignature = bytes.length >= 8
				&& (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;
		boolean jpegSignature = bytes.length >= 3
				&& (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;

		if ("image/png".equals(declaredContentType) && pngSignature) {
			return "png";
		}
		if ("image/jpeg".equals(declaredContentType) && jpegSignature) {
			return "jpg";
		}
		throw ApiException.badRequest("Only JPEG and PNG images are supported, "
				+ "and the file content must match its declared type");
	}

	private static BufferedImage decode(byte[] bytes) {
		BufferedImage image;
		try {
			image = ImageIO.read(new ByteArrayInputStream(bytes));
		} catch (IOException ex) {
			image = null;
		}
		if (image == null) {
			throw ApiException.badRequest("File is not a decodable image");
		}
		if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
			throw ApiException.badRequest("Image dimensions exceed " + MAX_DIMENSION + "px");
		}
		return image;
	}
}
