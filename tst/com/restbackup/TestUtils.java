/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;

public class TestUtils {
	public static HttpResponse fakeResponse(int code, String body) {
		try {
			ProtocolVersion ver = new ProtocolVersion("HTTP", 1, 1);
			HttpResponse response = new BasicHttpResponse(ver, code, "Test");
			response.setEntity(new StringEntity(body, "UTF-8"));
			return response;
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
