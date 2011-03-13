package com.restbackup;

public class RetryableException extends Exception {
	private static final long serialVersionUID = 1L;

	private final Exception _exception;

	RetryableException(Exception e) {
		super(e);
		_exception = e;
	}

	public Exception getOriginalException() {
		return _exception;
	}
}
