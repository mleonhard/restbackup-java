package com.restbackup.testingtcpserver.handlers;

import java.net.Socket;

public class ImmediatelyCloseSocket extends Handler {
	public void handle(Socket socket) throws Exception {
		socket.close();
	}
}