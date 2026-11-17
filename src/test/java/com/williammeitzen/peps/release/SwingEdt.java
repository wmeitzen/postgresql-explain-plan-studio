package com.williammeitzen.peps.release;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

/** Small helpers so release smoke tests share one EDT pattern. */
public final class SwingEdt {

	private SwingEdt() {
	}

	public static void run(Runnable action) throws Exception {
		if (SwingUtilities.isEventDispatchThread()) {
			action.run();
			return;
		}
		try {
			SwingUtilities.invokeAndWait(action);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception) {
				throw (Exception) cause;
			}
			if (cause instanceof Error) {
				throw (Error) cause;
			}
			throw e;
		}
	}

	public static <T> T call(EdtCallable<T> action) throws Exception {
		AtomicReference<T> result = new AtomicReference<>();
		AtomicReference<Exception> error = new AtomicReference<>();
		run(() -> {
			try {
				result.set(action.call());
			} catch (Exception e) {
				error.set(e);
			}
		});
		if (error.get() != null) {
			throw error.get();
		}
		return result.get();
	}

	@FunctionalInterface
	public interface EdtCallable<T> {
		T call() throws Exception;
	}
}
