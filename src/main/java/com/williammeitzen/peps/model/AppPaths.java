package com.williammeitzen.peps.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Resolves the per-user data directory for PEPS (SQLite DB outside the install/unzip tree).
 *
 * <p>Override with env {@code PEPS_DATA_DIR} or system property {@code peps.data.dir}.
 * Defaults: {@code %APPDATA%\PostgreSQLExplainPlanStudio} on Windows;
 * {@code $XDG_DATA_HOME/postgresql-explain-plan-studio} or
 * {@code ~/.local/share/postgresql-explain-plan-studio} elsewhere.
 */
public final class AppPaths {

	public static final String SQLITE_FILE_NAME = "postgresql-explain-plan-studio.sqlite";

	private AppPaths() {
	}

	/** Directory that holds the SQLite database (created if missing). */
	public static Path dataDir() {
		Path dir = resolveDataDir();
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create PEPS data directory: " + dir, e);
		}
		return dir;
	}

	/**
	 * Absolute path to the SQLite file. If it does not exist yet but a legacy DB is present
	 * in {@code user.dir}, copies that file into the data directory once.
	 */
	public static Path sqliteDatabaseFile() {
		Path dest = dataDir().resolve(SQLITE_FILE_NAME);
		if (!Files.exists(dest)) {
			Path legacy = Paths.get(System.getProperty("user.dir", ".")).resolve(SQLITE_FILE_NAME);
			if (Files.isRegularFile(legacy) && !legacy.toAbsolutePath().normalize().equals(dest.toAbsolutePath().normalize())) {
				try {
					Files.copy(legacy, dest, StandardCopyOption.COPY_ATTRIBUTES);
					System.out.println("PostgreSQL Explain Plan Studio | Migrated SQLite database:");
					System.out.println("  from: " + legacy.toAbsolutePath());
					System.out.println("  to:   " + dest.toAbsolutePath());
				} catch (IOException e) {
					System.out.println("PostgreSQL Explain Plan Studio | Warning: could not migrate legacy SQLite DB from:");
					System.out.println("  " + legacy.toAbsolutePath());
					System.out.println("  " + e.getMessage());
					System.out.println("Will create a new database at: " + dest.toAbsolutePath());
				}
			}
		}
		return dest;
	}

	/** JDBC URL for the resolved SQLite file. */
	public static String sqliteJdbcUrl() {
		Path db = sqliteDatabaseFile().toAbsolutePath().normalize();
		// SQLite JDBC accepts forward slashes on all platforms.
		String path = db.toString().replace('\\', '/');
		return "jdbc:sqlite:" + path;
	}

	static Path resolveDataDir() {
		String prop = System.getProperty("peps.data.dir");
		if (prop != null && !prop.isBlank()) {
			return Paths.get(prop.trim()).toAbsolutePath().normalize();
		}
		String env = System.getenv("PEPS_DATA_DIR");
		if (env != null && !env.isBlank()) {
			return Paths.get(env.trim()).toAbsolutePath().normalize();
		}

		String os = System.getProperty("os.name", "").toLowerCase();
		if (os.contains("win")) {
			String appData = System.getenv("APPDATA");
			if (appData != null && !appData.isBlank()) {
				return Paths.get(appData, "PostgreSQLExplainPlanStudio");
			}
			return Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "PostgreSQLExplainPlanStudio");
		}

		String xdg = System.getenv("XDG_DATA_HOME");
		if (xdg != null && !xdg.isBlank()) {
			return Paths.get(xdg, "postgresql-explain-plan-studio");
		}
		return Paths.get(System.getProperty("user.home"), ".local", "share", "postgresql-explain-plan-studio");
	}
}
