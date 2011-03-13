package com.restbackup.testingtcpserver.handlers;

import java.io.InputStream;
import java.net.Socket;

public class ReadHttp11 extends Handler {
	private final Handler _nextHandler;

	public void handle(Socket socket) throws Exception {
		InputStream input = socket.getInputStream();
		byte[] buffer = new byte[8192];
		Thread.sleep(500);
		while (input.available() > 0) {
			int bytesRead = input.read(buffer);
			System.out.println("Read and discarded " + bytesRead + " bytes");
			if (bytesRead == -1) {
				break;
			}
			Thread.sleep(500);
		}
		_nextHandler.handle(socket);
	}

	public ReadHttp11(Handler handler) {
		_nextHandler = handler;
	}
}