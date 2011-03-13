package com.restbackup.testingtcpserver.handlers;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpDateGenerator;

public class ReadAndRespondHttp11 extends Handler {
	public static final HttpVersion HTTP11 = HttpVersion.HTTP_1_1;
	public static final HttpDateGenerator dateGen = new HttpDateGenerator();

	private HttpResponse _response;

	public HttpResponse handle(HttpRequest request) {
		return _response;
	}

	public ReadAndRespondHttp11() {
		this(200, "OK");
	}

	public ReadAndRespondHttp11(String body) throws UnsupportedEncodingException {
		this(200, "OK", body);
	}

	public ReadAndRespondHttp11(HttpResponse response) {
		_response = response;
	}

	public ReadAndRespondHttp11(int code, String reason) {
		BasicHttpResponse response = new BasicHttpResponse(HTTP11, code, reason);
		response.setHeader("Date", dateGen.getCurrentDate());
		response.setHeader("Content-Length", "0");
		response.setHeader("Connection", "close");
		_response = response;
	}

	public ReadAndRespondHttp11(int code, String reason, String body)
			throws UnsupportedEncodingException {
		BasicHttpResponse response = new BasicHttpResponse(HTTP11, code, reason);
		response.setHeader("Date", dateGen.getCurrentDate());
		response.setHeader("Content-Length", String.valueOf(body.length()));
		response.setHeader("Connection", "close");
		response.setEntity(new StringEntity(body));
		_response = response;
	}
}
