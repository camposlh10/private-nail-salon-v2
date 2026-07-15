package com.nailsalon.backend.shared;

import java.text.Normalizer;
import java.util.Locale;

/** URL-safe slug generation: lowercase ASCII, hyphen-separated. */
public final class Slugs {

	private Slugs() {
	}

	public static String slugify(String input) {
		String slug = Normalizer.normalize(input, Normalizer.Form.NFKD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-+|-+$", "");
		return slug.isBlank() ? "item" : slug;
	}
}
