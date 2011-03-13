package com.restbackup.testingtcpserver;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.restbackup.testingtcpserver.handlers.Handler;

public class TestingTcpServer implements Runnable {
	private ServerSocket _serverSocket;
	private final Thread _serverThread;
	private final ConcurrentLinkedQueue<Handler> _handlerQueue = new ConcurrentLinkedQueue<Handler>();
	private volatile int _requestCount = 0;

	public TestingTcpServer() throws IOException {
		_serverSocket = new ServerSocket(0);
		_serverThread = new Thread(this);
		_serverThread.setDaemon(true);
		_serverThread.start();
	}

	public void shutdown() throws IOException {
		_serverThread.interrupt();
		_serverSocket.close();
	}

	public InetSocketAddress getAddress() {
		return new InetSocketAddress(_serverSocket.getInetAddress(), _serverSocket.getLocalPort());
	}

	public String getUrl() {
		return "http://USER:PASS@localhost:" + _serverSocket.getLocalPort() + "/";
	}

	public void add(Handler handler) {
		this._handlerQueue.add(handler);
	}

	public int requestCount() {
		return _requestCount;
	}

	public void reset() {
		_handlerQueue.clear();
		_requestCount = 0;
	}

	@Override
	public void run() {
		System.out.println("Listening on port " + _serverSocket.getLocalPort());
		while (!Thread.interrupted()) {
			try {
				Socket socket = _serverSocket.accept();
				this._requestCount++;
				System.out.println("Accepted connection from " + socket.getInetAddress());
				Handler handler = this._handlerQueue.poll();
				if (handler == null) {
					System.out.println("No handlers queued.  Closing connection.");
					socket.close();
				} else {
					HandlerThread handlerThread = new HandlerThread(socket, handler);
					handlerThread.start();
				}
			} catch (InterruptedIOException ex) {
				break;
			} catch (IOException e) {
				if (!Thread.interrupted()) {
					System.err.println("IOException accepting connection: " + e.getMessage());
				}
				break;
			}
		}
	}

	class HandlerThread extends Thread {
		private final Socket _socket;
		private final Handler _handler;

		public HandlerThread(Socket socket, Handler handler) {
			_socket = socket;
			_handler = handler;
			setDaemon(true);
		}

		public void run() {
			try {
				_handler.handle(_socket);
			} catch (Exception ex) {
				System.err.println("Handler error: " + ex.getMessage());
			} finally {
				try {
					_socket.close();
				} catch (IOException ignore) {
				}
			}
		}
	}
}