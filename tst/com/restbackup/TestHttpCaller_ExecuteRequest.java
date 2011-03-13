package com.restbackup;

import static org.junit.Assert.*;

import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.restbackup.testingtcpserver.TestingTcpServer;
import com.restbackup.testingtcpserver.handlers.ImmediatelyCloseSocket;
import com.restbackup.testingtcpserver.handlers.ReadAndCloseSocket;
import com.restbackup.testingtcpserver.handlers.DelayMillis;
import com.restbackup.testingtcpserver.handlers.ReadHttp11;
import com.restbackup.testingtcpserver.handlers.ReadAndRespondHttp11;
import com.restbackup.testingtcpserver.handlers.Respond;
import com.restbackup.testingtcpserver.handlers.RespondHttp11;

public class TestHttpCaller_ExecuteRequest {

	private static TestingTcpServer _server;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.out.println("Creating new TestingHttpServer");
		_server = new TestingTcpServer();
		System.out.println("Done creating new TestingHttpServer");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		System.out.println("Shutting down TestingHttpServer");
		_server.shutdown();
	}

	@Before
	public void setUp() throws Exception {
		_server.reset();
	}

	@After
	public void tearDown() throws Exception {
	}

	public HttpResponse executeAndCheck(HttpCaller caller, int requestCount, long minMillis,
			long maxMillis) throws RestBackupException {
		long startTime = System.currentTimeMillis();
		HttpResponse response = caller.executeRequestWithRetries(new HttpGet("/"));
		if (requestCount > 0) {
			assertEquals(requestCount, _server.requestCount());
		}
		long elapsedMillis = System.currentTimeMillis() - startTime;
		if (minMillis > 0) {
			assertTrue("elapsedMillis is " + elapsedMillis + ", should be > " + minMillis,
					elapsedMillis > minMillis);
		}
		if (maxMillis > 0) {
			assertTrue("elapsedMillis is " + elapsedMillis + ", should be < " + maxMillis,
					elapsedMillis < maxMillis);
		}
		return response;
	}

	public HttpResponse tryExecuteAndCheck(HttpCaller caller, int requestCount, long minMillis,
			long maxMillis) throws RestBackupException {
		long startTime = System.currentTimeMillis();
		RestBackupException exception;
		try {
			return caller.executeRequestWithRetries(new HttpGet("/"));
		} catch (RestBackupException e) {
			exception = e;
		}
		if (requestCount > 0) {
			assertEquals(requestCount, _server.requestCount());
		}
		long elapsedMillis = System.currentTimeMillis() - startTime;
		if (minMillis > 0) {
			assertTrue("elapsedMillis is " + elapsedMillis + ", should be > " + minMillis,
					elapsedMillis > minMillis);
		}
		if (maxMillis > 0) {
			assertTrue("elapsedMillis is " + elapsedMillis + ", should be < " + maxMillis,
					elapsedMillis < maxMillis);
		}
		throw exception;
	}

	@Test
	public void test_ImmediateSuccess() throws Exception {
		_server.add(new ReadAndRespondHttp11(200, "OK", "body"));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		HttpResponse response = executeAndCheck(caller, 1, 0, 1000);
		assertEquals("body", EntityUtils.toString(response.getEntity()));
	}

	@Test(expected = RestBackupException.class)
	public void test_NotListening() throws Exception {
		HttpCaller caller = new HttpCaller("http://USER:PASS@localhost:1/", 4);
		tryExecuteAndCheck(caller, 0, 12000, 13000);
	}

	@Test(expected = RestBackupException.class)
	public void test_ConnectBacklogFull() throws Exception {
		ServerSocket serverSocket = new ServerSocket(0, 1);
		Socket blocker = new Socket("localhost", serverSocket.getLocalPort());
		try {
			String url = "http://USER:PASS@127.0.0.1:" + serverSocket.getLocalPort() + "/";
			tryExecuteAndCheck(new HttpCaller(url, 4), 0, 8000, 9000);
		} finally {
			blocker.close();
			serverSocket.close();
		}
	}

	@Test(expected = RestBackupException.class)
	public void test_TimeoutThenBacklogFull() throws Exception {
		ServerSocket serverSocket = new ServerSocket(0, 2);
		Socket blocker = new Socket("localhost", serverSocket.getLocalPort());
		try {
			String url = "http://USER:PASS@127.0.0.1:" + serverSocket.getLocalPort() + "/";
			HttpCaller caller = new HttpCaller(url, 4);
			HttpParams params = caller.getHttpClient().getParams();
			params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 1000);
			tryExecuteAndCheck(caller, 0, 8000, 9000);
		} finally {
			blocker.close();
			serverSocket.close();
		}
	}

	@Test(expected = RestBackupException.class)
	public void test_ImmediateDisconnect_RetryFailure() throws Exception {
		_server.add(new ImmediatelyCloseSocket());
		_server.add(new ImmediatelyCloseSocket());
		_server.add(new ImmediatelyCloseSocket());
		_server.add(new ImmediatelyCloseSocket());
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		tryExecuteAndCheck(caller, 4, 4000, 5000);
	}

	@Test(expected = RestBackupException.class)
	public void test_ReadAndDisconnect_RetryFailure() throws Exception {
		_server.add(new ReadAndCloseSocket());
		_server.add(new ReadAndCloseSocket());
		_server.add(new ReadAndCloseSocket());
		_server.add(new ReadAndCloseSocket());
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		tryExecuteAndCheck(caller, 4, 4000, 5000);
	}

	@Test
	public void test_ImmediateDisconnect_RetrySuccess() throws Exception {
		_server.add(new ImmediatelyCloseSocket());
		_server.add(new ImmediatelyCloseSocket());
		_server.add(new ImmediatelyCloseSocket());
		_server.add(new ReadAndRespondHttp11());
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		executeAndCheck(caller, 4, 4000, 5000).getEntity().getContent().close();
	}

	@Test
	public void test_ReadAndDisconnect_RetrySuccess() throws Exception {
		_server.add(new ReadAndCloseSocket());
		_server.add(new ReadAndCloseSocket());
		_server.add(new ReadAndCloseSocket());
		_server.add(new ReadAndRespondHttp11());
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		executeAndCheck(caller, 4, 4000, 5000).getEntity().getContent().close();
	}

	@Test(expected = RestBackupException.class)
	public void test_ConnectTimeout_RetryFailure() throws Exception {
		String url = "http://USER:PASS@restbackup.com:1/"; // discards packets
		HttpCaller caller = new HttpCaller(url, 4);
		HttpParams params = caller.getHttpClient().getParams();
		params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
		tryExecuteAndCheck(caller, 0, 16000, 17000);
	}

	@Test(expected = RestBackupException.class)
	public void test_ReadTimeout_RetryFailure() throws Exception {
		_server.add(new ReadHttp11(new DelayMillis(10000, new RespondHttp11())));
		_server.add(new ReadHttp11(new DelayMillis(10000, new RespondHttp11())));
		_server.add(new ReadHttp11(new DelayMillis(10000, new RespondHttp11())));
		_server.add(new ReadHttp11(new DelayMillis(10000, new RespondHttp11())));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		caller.getHttpClient().getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 1000);
		tryExecuteAndCheck(caller, 4, 8000, 9000); // 4.1 + 1 x 4
	}

	@Test
	public void test_ReadTimeout_RetrySuccess() throws Exception {
		_server.add(new ReadHttp11(new DelayMillis(10000, new RespondHttp11())));
		_server.add(new ReadHttp11(new DelayMillis(10000, new RespondHttp11())));
		_server.add(new ReadHttp11(new DelayMillis(10000, new RespondHttp11())));
		_server.add(new ReadAndRespondHttp11());
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		caller.getHttpClient().getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);
		executeAndCheck(caller, 4, 13000, 14000).getEntity().getContent().close();
	}

	// Write request line timeout
	// Write headers timeout
	// Write body timeout

	/* Partial Status Line *************************************************** */

	@Test(expected = RestBackupException.class)
	public void test_PartialRequestLine_RetryFailure() throws Exception {
		String response = "HTTP/1.1 20";
		_server.add(new ReadHttp11(new Respond(response)));
		_server.add(new ReadHttp11(new Respond(response)));
		_server.add(new ReadHttp11(new Respond(response)));
		_server.add(new ReadHttp11(new Respond(response)));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		tryExecuteAndCheck(caller, 4, 8000, 9000);
	}

	@Test
	public void test_PartialRequestLine_RetrySuccess() throws Exception {
		String response = "HTTP/1.1 20";
		_server.add(new ReadHttp11(new Respond(response)));
		_server.add(new ReadHttp11(new Respond(response)));
		_server.add(new ReadHttp11(new Respond(response)));
		_server.add(new ReadAndRespondHttp11());
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		executeAndCheck(caller, 4, 7000, 8000).getEntity().getContent().close();
	}

	@Test(expected = RestBackupException.class)
	public void test_PartialRequestLine_RetryTimeoutFailure() throws Exception {
		String response = "HTTP/1.1 20";
		_server.add(new ReadHttp11(new Respond(response, new DelayMillis(10000))));
		_server.add(new ReadHttp11(new Respond(response, new DelayMillis(10000))));
		_server.add(new ReadHttp11(new Respond(response, new DelayMillis(10000))));
		_server.add(new ReadHttp11(new Respond(response, new DelayMillis(10000))));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		caller.getHttpClient().getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);
		tryExecuteAndCheck(caller, 4, 20000, 21000);
	}

	@Test
	public void test_PartialRequestLine_RetryTimeoutSuccess() throws Exception {
		String response = "HTTP/1.1 20";
		_server.add(new ReadHttp11(new Respond(response, new DelayMillis(10000))));
		_server.add(new ReadHttp11(new Respond(response, new DelayMillis(10000))));
		_server.add(new ReadHttp11(new Respond(response, new DelayMillis(10000))));
		_server.add(new ReadAndRespondHttp11());
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		caller.getHttpClient().getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);
		executeAndCheck(caller, 4, 16000, 17000).getEntity().getContent().close();
	}

	/* Malformed Head ************ */

	@Test(expected = RestBackupException.class)
	public void test_MalformedHeader_RetryFailure() throws Exception {
		String response = "HTTP/1.1 200 OK\r\nXX";
		_server.add(new ReadHttp11(new Respond(response)));
		_server.add(new ReadHttp11(new Respond(response)));
		_server.add(new ReadHttp11(new Respond(response)));
		_server.add(new ReadHttp11(new Respond(response)));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		tryExecuteAndCheck(caller, 4, 8000, 9000);
	}

	/* 5xx **************************************** */

	@Test(expected = RestBackupException.class)
	public void test_501InternalServerError_RetryFailure() throws Exception {
		_server.add(new ReadAndRespondHttp11(501, "Internal Server Error"));
		_server.add(new ReadAndRespondHttp11(501, "Internal Server Error"));
		_server.add(new ReadAndRespondHttp11(501, "Internal Server Error"));
		_server.add(new ReadAndRespondHttp11(501, "Internal Server Error"));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		tryExecuteAndCheck(caller, 4, 4000, 5000);
	}

	@Test
	public void test_501InternalServerError_RetrySuccess() throws Exception {
		_server.add(new ReadAndRespondHttp11(501, "Internal Server Error"));
		_server.add(new ReadAndRespondHttp11(501, "Internal Server Error"));
		_server.add(new ReadAndRespondHttp11(501, "Internal Server Error"));
		_server.add(new ReadAndRespondHttp11());
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		executeAndCheck(caller, 4, 4000, 5000).getEntity().getContent().close();
	}

	@Test(expected = RestBackupException.class)
	public void test_503ServiceUnavailable_RetryFailure() throws Exception {
		_server.add(new ReadAndRespondHttp11(503, "Service Unavailable"));
		_server.add(new ReadAndRespondHttp11(503, "Service Unavailable"));
		_server.add(new ReadAndRespondHttp11(503, "Service Unavailable"));
		_server.add(new ReadAndRespondHttp11(503, "Service Unavailable"));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		tryExecuteAndCheck(caller, 4, 4000, 5000);
	}

	@Test
	public void test_503InternalServerError_RetrySuccess() throws Exception {
		_server.add(new ReadAndRespondHttp11(503, "Service Unavailable"));
		_server.add(new ReadAndRespondHttp11(503, "Service Unavailable"));
		_server.add(new ReadAndRespondHttp11(503, "Service Unavailable"));
		_server.add(new ReadAndRespondHttp11());
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		executeAndCheck(caller, 4, 4000, 5000).getEntity().getContent().close();
	}

//	@Test(expected = RestBackupException.class)
//	public void test_HalfReply222_RetryFailure() throws Exception {
//		String response = "HTTP/1.1 200 OK\r\nDate: Mon, 23 May 2005 22:38:34 GMT\r\nConnection: close\r\nContent-Length: 0\r\n\r\n";
//		_server.add(new ReadHttp11(new Respond(response)));
//		_server.add(new ReadAndRespondHttp11());
//		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
//		caller.getHttpClient().getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);
//		executeAndCheck(caller, 4, 13000, 14000).getEntity().getContent().close();
//	}

	/* 1xx, 3xx, 4xx, etc. **************************************** */

	@Test
	public void test_100Continue() throws Exception {
		_server.add(new ReadHttp11(new Respond("HTTP/1.1 100 Continue\r\n\r\n", new DelayMillis(
				1000, new RespondHttp11(100, "Continue", "100Continue body")))));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		HttpResponse response = executeAndCheck(caller, 1, 1000, 3000);
		assertEquals("100Continue body", EntityUtils.toString(response.getEntity()));
	}

	@Test(expected = RestBackupException.class)
	public void test_301MovedPermanently() throws Exception {
		_server.add(new ReadAndRespondHttp11(301, "Moved Permanently"));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		tryExecuteAndCheck(caller, 1, 0, 1000);
	}

	@Test(expected = RestBackupException.class)
	public void test_302Found() throws Exception {
		_server.add(new ReadAndRespondHttp11(302, "Found"));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		tryExecuteAndCheck(caller, 1, 0, 1000);
	}

	@Test(expected = RestBackupException.class)
	public void test_303SeeOther() throws Exception {
		_server.add(new ReadAndRespondHttp11(303, "See Other"));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		tryExecuteAndCheck(caller, 1, 0, 1000);
	}

	@Test(expected = RestBackupException.class)
	public void test_307TemporaryRedirect() throws Exception {
		_server.add(new ReadAndRespondHttp11(303, "Temporary Redirect"));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		tryExecuteAndCheck(caller, 1, 0, 1000);
	}

	@Test(expected = UnauthorizedException.class)
	public void test_401Unauthorized() throws Exception {
		_server.add(new ReadAndRespondHttp11(401, "Unauthorized"));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		tryExecuteAndCheck(caller, 1, 0, 1000);
	}

	@Test(expected = RestBackupException.class)
	public void test_404NotFound() throws Exception {
		_server.add(new ReadAndRespondHttp11(404, "Not Found"));
		HttpCaller caller = new HttpCaller(_server.getUrl(), 4);
		tryExecuteAndCheck(caller, 1, 0, 1000);
	}
}
