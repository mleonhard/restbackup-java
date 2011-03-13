package com.restbackup.testingtcpserver.handlers;

import java.io.UnsupportedEncodingException;
import java.net.Socket;

public class Respond extends Handler {
	private final byte[] bytes;
	private final Handler _nextHandler;

	public Respond(String data) throws UnsupportedEncodingException {
		bytes = data.getBytes("UTF-8");
		_nextHandler = null;
	}

	public Respond(String data, Handler handler) throws UnsupportedEncodingException {
		bytes = data.getBytes("UTF-8");
		_nextHandler = handler;
	}

	public void handle(Socket socket) throws Exception {
		socket.getOutputStream().write(bytes);
		if (_nextHandler != null) {
			_nextHandler.handle(socket);
		}
	}
}