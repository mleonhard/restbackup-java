/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

public class RestBackupException extends IOException {
	private static final long serialVersionUID = 1L;
	private HttpResponse _response = null;

	public RestBackupException(String s) {
		super(s);
	}

	public RestBackupException(String s, Throwable t) {
		super(s, t);
	}

	public RestBackupException(Throwable t, HttpResponse response) {
		super(t);
		_response = response;
	}

	public RestBackupException(String s, Throwable t, HttpResponse response) {
		super(s, t);
		_response = response;
	}

	public RestBackupException(String s, HttpResponse response) {
		super(s + ". " + extractReason(response));
		_response = response;
	}

	public RestBackupException(HttpResponse response) {
		super(extractReason(response));
		_response = response;
	}

	private static String extractReason(HttpResponse response) {
		try {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				return response.getStatusLine().toString() + ". " + EntityUtils.toString(entity);
			} else {
				return response.getStatusLine().toString();
			}
		} catch (Exception e) {
			return response.getStatusLine().toString() + ". (Error reading response body: "
					+ e.toString() + ")";
		}
	}

	/**
	 * Returns the HttpResponse object that triggered this exception, or null
	 */
	public HttpResponse getResponse() {
		return _response;
	}

	public RestBackupException(Throwable e) {
		super(e);
	}
}
