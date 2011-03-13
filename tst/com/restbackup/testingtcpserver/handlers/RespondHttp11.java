package com.restbackup.testingtcpserver.handlers;

import java.io.UnsupportedEncodingException;
import java.net.Socket;

import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpDateGenerator;

public class RespondHttp11 extends Handler {
	public static final HttpVersion HTTP11 = HttpVersion.HTTP_1_1;
	public static final HttpDateGenerator dateGen = new HttpDateGenerator();

	private HttpResponse _response;

	public void handle(Socket socket) throws Exception {
		HttpServerConnection conn = bind(socket);
		conn.sendResponseHeader(_response);
		conn.sendResponseEntity(_response);
		conn.close();
	}

	public RespondHttp11() {
		this(200, "OK");
	}

	public RespondHttp11(HttpResponse response) {
		_response = response;
	}

	public RespondHttp11(int code, String reason) {
		BasicHttpResponse response = new BasicHttpResponse(HTTP11, 200, "Ok");
		response.setHeader("Date", dateGen.getCurrentDate());
		response.setHeader("Connection", "close");
		_response = response;
	}

	public RespondHttp11(int code, String reason, String body) throws UnsupportedEncodingException {
		this(code, reason);
		_response.setEntity(new StringEntity(body));
	}
}
