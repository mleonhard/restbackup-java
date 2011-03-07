package com.restbackup;

import org.apache.http.HttpResponse;

public class ResourceNotFoundException extends RestBackupException {
	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(HttpResponse response) {
		super(response);
	}
}
