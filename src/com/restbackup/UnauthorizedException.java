package com.restbackup;

import org.apache.http.HttpResponse;

public class UnauthorizedException extends RestBackupException {
	private static final long serialVersionUID = 1L;

	public UnauthorizedException(HttpResponse response) {
		super(response);
	}
}
