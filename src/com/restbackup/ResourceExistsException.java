package com.restbackup;

import org.apache.http.HttpResponse;

public class ResourceExistsException extends RestBackupException {
	private static final long serialVersionUID = 1L;

	public ResourceExistsException(HttpResponse response) {
		super(response);
	}
}
