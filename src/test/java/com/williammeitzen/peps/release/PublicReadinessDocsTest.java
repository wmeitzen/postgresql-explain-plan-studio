package com.williammeitzen.peps.release;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * README / packaging docs must be clean UTF-8 before going public — mojibake
 * in the first screen of the repo is a trust killer.
 */
class PublicReadinessDocsTest {

	@Test
	void readmeHasNoMojibakeOrReplacementChars() throws Exception {
		Path readme = Path.of("README.md");
		assertTrue(Files.isRegularFile(readme), "README.md missing at repo root");
		String text = Files.readString(readme, StandardCharsets.UTF_8);
		assertFalse(text.contains("\uFFFD"), "README contains Unicode replacement char");
		assertFalse(text.contains("Ã¢"), "README looks mojibaked (Ã¢…)");
		assertFalse(text.contains("Ã¢â‚¬â€"), "README em-dash mojibake");
		assertFalse(text.contains("Ã¢â€"), "README arrow mojibake");
		// macOS Gatekeeper hint should use a clear separator, not a bare "?"
		assertFalse(text.contains("right-click the app ? Open"),
				"README Gatekeeper tip uses '?' instead of an em dash / arrow");
	}
}
