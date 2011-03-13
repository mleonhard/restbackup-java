package com.restbackup.testingtcpserver.handlers;

import java.io.InputStream;
import java.net.Socket;

/**
 * This class implements the "proper" way to close a TCP socket
 */
public class ReadAndCloseSocket extends Handler {
	public void handle(Socket socket) throws Exception {
		// Close output channel of socket
		socket.shutdownOutput();
		InputStream input = socket.getInputStream();
		byte[] buffer = new byte[8192];
		// Read until other side closes other channel
		while (input.read(buffer) != -1) {
		}
		socket.shutdownInput();
		socket.close();
	}
}