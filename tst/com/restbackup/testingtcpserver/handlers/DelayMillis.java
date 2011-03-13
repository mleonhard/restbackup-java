package com.restbackup.testingtcpserver.handlers;

import java.net.Socket;

public class DelayMillis extends Handler {
	private final long _millisToDelay;
	private final Handler _nextHandler;

	public DelayMillis(long milliSeconds) {
		_millisToDelay = milliSeconds;
		_nextHandler = null;
	}

	public DelayMillis(long milliSeconds, Handler handler) {
		_millisToDelay = milliSeconds;
		_nextHandler = handler;
	}

	public void handle(Socket socket) throws Exception {
		Thread.sleep(_millisToDelay);
		if (_nextHandler != null) {
			_nextHandler.handle(socket);
		}
	}
}