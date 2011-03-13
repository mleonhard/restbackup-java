package com.restbackup.testingtcpserver.handlers;

import java.io.IOException;
import java.net.Socket;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;

public class Handler {
	public void handle(Socket socket) throws Exception {
		HttpServerConnection conn = bind(socket);
		HttpRequest request = conn.receiveRequestHeader();
		HttpResponse response = handle(request);
		conn.sendResponseHeader(response);
		conn.sendResponseEntity(response);
		conn.close();
	}

	public HttpServerConnection bind(Socket socket) throws IOException {
		DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
		conn.bind(socket, new BasicHttpParams());
		return conn;
	}

	public HttpResponse handle(HttpRequest request) {
		assert (false);
		return null;
	}
}
