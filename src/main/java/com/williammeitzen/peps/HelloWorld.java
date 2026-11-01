package com.williammeitzen.peps;

/**
 * Bootstrap entry point for PostgreSQL Explain Plan Studio.
 */
public final class HelloWorld {

	private HelloWorld() {
	}

	public static void main(String[] args) {
		System.out.println("Hello, PostgreSQL Explain Plan Studio!");
		System.out.println();
		System.out.println("Press Enter to exit...");
		try {
			System.in.read();
		} catch (Exception ignored) {
			// Keep Hello World resilient if stdin is unavailable.
		}
	}
}
