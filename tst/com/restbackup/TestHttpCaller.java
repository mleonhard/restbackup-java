/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.util.EntityUtils;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Depends on Mockito 1.8.5.
 * 
 * Thanks to Florin Duroiu for his helpful blog post:
 * http://blog.newsplore.com/2010/02/09/unit-testing-with-httpclient
 */
public class TestHttpCaller {

	@Test
	public void testEncodeBase64() {
		assertEquals(HttpCaller.encodeBase64(""), "");
		assertEquals(HttpCaller.encodeBase64("abcd"), "YWJjZA==");
		String input = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
				+ "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
				+ "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
				+ "aaaaaaaaaaaaaaaaaaaaa";
		String expected = "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFh"
				+ "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFh"
				+ "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFh"
				+ "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFh"
				+ "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYQ==";
		assertEquals(expected, HttpCaller.encodeBase64(input));
		input = "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
				+ "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
				+ "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
				+ "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
				+ "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
				+ "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
				+ "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
				+ "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
				+ "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
				+ "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
				+ "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
				+ "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000"
				+ "\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000\000";
		expected = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
				+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
				+ "AAAAAAAAAAAAAAAAAAAAAAA==";
		assertEquals(expected, HttpCaller.encodeBase64(input));
	}

	@Test
	public void testMakeFormUrlencodedEntity() throws IOException {
		HttpEntity entity;
		entity = HttpCaller.makeFormUrlencodedEntity(new String[] {});
		assertEquals("application/x-www-form-urlencoded; charset=UTF-8", entity.getContentType()
				.getValue());
		assertEquals("", EntityUtils.toString(entity));
		entity = HttpCaller.makeFormUrlencodedEntity(new String[] { "param1", "value1" });
		assertEquals("param1=value1", EntityUtils.toString(entity));
		entity = HttpCaller.makeFormUrlencodedEntity(new String[] { "param1", "value1", "param2",
				"value2" });
		assertEquals("param1=value1&param2=value2", EntityUtils.toString(entity));
		entity = HttpCaller.makeFormUrlencodedEntity(new String[] { "c", "\u5b57" });
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		InputStream input = entity.getContent();
		while (input.available() > 0) {
			bytes.write(input.read());
		}
		assertArrayEquals(new byte[] { 'c', '=', '%', 'E', '5', '%', 'A', 'D', '%', '9', '7' },
				bytes.toByteArray());
	}

	@Test
	public void testShutdown() throws IOException {
		new HttpCaller("https://u:p@h/").shutdown();
	}

	public static HttpClient fakeClient(String responseBody) throws ClientProtocolException,
			IOException {
		HttpClient client = Mockito.mock(HttpClient.class);
		Mockito.when(client.getParams()).thenReturn(new BasicHttpParams());
		HttpResponse response = TestUtils.fakeResponse(200, responseBody);
		Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
		return client;
	}

	/* EXECUTE REQUEST ********************************************** */

	@Test
	public void testExecuteRequest_success() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		assertEquals("body", EntityUtils.toString(caller.executeRequestWithRetries(new HttpGet()).getEntity()));
	}

	/* CONSTRUCTOR ************************************************ */

	@Test
	public void testHttpCaller_https() {
		HttpCaller caller;
		String accessUrl = "https://USER:PASS@host/";
		caller = new HttpCaller(accessUrl);
		assertEquals("https", caller.getScheme());
		assertEquals("USER", caller.getUsername());
		assertEquals("PASS", caller.getPassword());
		assertEquals("host", caller.getHost());
		assertEquals(443, caller.getPort());
		assertEquals(accessUrl, caller.getAccessUrl());

		@SuppressWarnings("unchecked")
		Collection<Header> headersList = (Collection<Header>) caller._httpClient.getParams()
				.getParameter(ClientPNames.DEFAULT_HEADERS);
		Map<String, Header> headers = new HashMap<String, Header>();
		for (Header header : headersList) {
			assertFalse(headers.containsKey(header.getName()));
			headers.put(header.getName(), header);
		}
		assertNotNull(headers.get("Authorization"));
		assertEquals("Basic VVNFUjpQQVNT", headers.get("Authorization").getValue());
	}

	@Test
	public void testHttpCaller_http() {
		HttpCaller caller;
		String accessUrl = "http://USER:PASS@host/";
		caller = new HttpCaller(accessUrl);
		assertEquals("http", caller.getScheme());
		assertEquals("USER", caller.getUsername());
		assertEquals("PASS", caller.getPassword());
		assertEquals("host", caller.getHost());
		assertEquals(80, caller.getPort());
		assertEquals(accessUrl, caller.getAccessUrl());
	}

	@Test
	public void testHttpCaller_https_with_port() {
		HttpCaller caller;
		String accessUrl = "https://USER:PASS@host:8443/";
		caller = new HttpCaller(accessUrl);
		assertEquals("https", caller.getScheme());
		assertEquals("USER", caller.getUsername());
		assertEquals("PASS", caller.getPassword());
		assertEquals("host", caller.getHost());
		assertEquals(8443, caller.getPort());
		assertEquals(accessUrl, caller.getAccessUrl());
	}

	@Test
	public void testHttpCaller_http_with_port() {
		HttpCaller caller;
		String accessUrl = "http://USER:PASS@host:8080/";
		caller = new HttpCaller(accessUrl);
		assertEquals("http", caller.getScheme());
		assertEquals("USER", caller.getUsername());
		assertEquals("PASS", caller.getPassword());
		assertEquals("host", caller.getHost());
		assertEquals(8080, caller.getPort());
		assertEquals(accessUrl, caller.getAccessUrl());
	}

	@Test
	public void testHttpCaller_longValues() {
		HttpCaller caller;
		String alnum = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		String host = alnum + "-a.x";
		String accessUrl = String.format("https://%s:%s@%s:%s/", alnum, alnum, host, "0123456789");
		caller = new HttpCaller(accessUrl);
		assertEquals("https", caller.getScheme());
		assertEquals(alnum, caller.getUsername());
		assertEquals(alnum, caller.getPassword());
		assertEquals(host, caller.getHost());
		assertEquals(123456789, caller.getPort());
		assertEquals(accessUrl, caller.getAccessUrl());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_emptyAccessUrl() {
		new HttpCaller("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_emptyScheme() {
		new HttpCaller("://USER:PASS@host/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_missingScheme() {
		new HttpCaller("USER:PASS@host/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_badScheme() {
		new HttpCaller("htt://USER:PASS@host/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_emptyUsername() {
		new HttpCaller("https://:PASS@host/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_badUsername() {
		new HttpCaller("https://USE*R:PASS@host/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_missingPassword() {
		new HttpCaller("https://USER@host/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_emptyPassword() {
		new HttpCaller("https://USER:@host/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_badPassword() {
		new HttpCaller("https://USER:PA#S@host/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_missingHost() {
		new HttpCaller("https://USER:PASS/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_emptyHost() {
		new HttpCaller("https://USER:PASS@/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_badHost() {
		new HttpCaller("https://USER:PASS@host%com/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_missingTrailingSlash() {
		new HttpCaller("https://USER:PASS@host");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_missingTrailingSlash_withPort() {
		new HttpCaller("https://USER:PASS@host:80");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_emptyPort() {
		new HttpCaller("https://USER:PASS@host:/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_badPort() {
		new HttpCaller("https://USER:PASS@host%com:abc/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHttpCaller_withResourceName() {
		new HttpCaller("https://USER:PASS@host/abc");
	}

	@Test
	public void testHttpCaller_withHttpClient() throws IOException {
		HttpClient client = new DefaultHttpClient();
		HttpCaller caller = new HttpCaller("https://u:p@h/", client);
		assertSame(client, caller._httpClient);
	}

	/* GET ************************************************************ */

	@Test
	public void testGet_success() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		HttpResponse response = caller.doGet("/", new BasicHeader("name", "value"));
		assertEquals("body", EntityUtils.toString(response.getEntity()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGet_nullUri() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		caller.doGet(null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGet_emptyUri() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		caller.doGet("", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGet_badUri() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		caller.doGet("x", null);
	}

	@Test
	public void testGet_extraHeader() throws ClientProtocolException, IOException {
		HttpClient client = Mockito.mock(HttpClient.class);
		Mockito.when(client.getParams()).thenReturn(new BasicHttpParams());
		Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(
				new Answer<Object>() {
					public Object answer(InvocationOnMock invocation) {
						Object[] args = invocation.getArguments();
						assertThat(args[0], new IsInstanceOf(HttpGet.class));
						HttpGet request = (HttpGet) args[0];
						assertEquals(1, request.getHeaders("name").length);
						assertEquals("value", request.getHeaders("name")[0].getValue());
						// HttpClient mock = (HttpClient) invocation.getMock();
						return TestUtils.fakeResponse(200, "body");
					}
				});
		HttpCaller caller = new HttpCaller("https://u:p@h/", client);
		HttpResponse response = caller.doGet("/", new BasicHeader("name", "value"));
		assertEquals("body", EntityUtils.toString(response.getEntity()));
	}

	/* PUT ************************************************************** */

	@Test
	public void testPut_success() throws ClientProtocolException, IOException {
		HttpClient client = Mockito.mock(HttpClient.class);
		Mockito.when(client.getParams()).thenReturn(new BasicHttpParams());
		Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(
				new Answer<Object>() {
					public Object answer(InvocationOnMock invocation) {
						Object[] args = invocation.getArguments();
						assertThat(args[0], new IsInstanceOf(HttpPut.class));
						return TestUtils.fakeResponse(200, "body");
					}
				});
		HttpCaller caller = new HttpCaller("https://u:p@h/", client);
		HttpResponse response = caller.doPut("/", new StringEntity("e"));
		assertEquals("body", EntityUtils.toString(response.getEntity()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPut_nullUri() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		caller.doPut(null, new StringEntity("e"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPut_emptyUri() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		caller.doPut("", new StringEntity("e"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPut_badUri() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		caller.doPut("x", new StringEntity("e"));
	}

	@Test
	public void testPut_emptyEntity() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		HttpResponse response = caller.doPut("/", new StringEntity(""));
		assertEquals("body", EntityUtils.toString(response.getEntity()));
	}

	/* POST ************************************************************** */

	@Test
	public void testPost_success() throws ClientProtocolException, IOException {
		HttpClient client = Mockito.mock(HttpClient.class);
		Mockito.when(client.getParams()).thenReturn(new BasicHttpParams());
		Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(
				new Answer<Object>() {
					public Object answer(InvocationOnMock invocation) {
						Object[] args = invocation.getArguments();
						assertThat(args[0], new IsInstanceOf(HttpPost.class));
						HttpPost request = (HttpPost) args[0];
						try {
							assertEquals("a=b&c=d", EntityUtils.toString(request.getEntity()));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
						return TestUtils.fakeResponse(200, "body");
					}
				});

		HttpCaller caller = new HttpCaller("https://u:p@h/", client);
		HttpResponse response = caller.doPost("/", new String[] { "a", "b", "c", "d" });
		assertEquals("body", EntityUtils.toString(response.getEntity()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPost_nullUri() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		caller.doPost(null, new String[] { "a", "b" });
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPost_emptyUri() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		caller.doPost("", new String[] { "a", "b" });
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPost_badUri() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		caller.doPost("x", new String[] { "a", "b" });
	}

	@Test
	public void testPost_nullParams() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		HttpResponse response = caller.doPost("/", null);
		assertEquals("body", EntityUtils.toString(response.getEntity()));
	}

	@Test
	public void testPost_emptyParams() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		HttpResponse response = caller.doPost("/", new String[] {});
		assertEquals("body", EntityUtils.toString(response.getEntity()));
	}

	/* DELETE ************************************************************ */

	@Test
	public void testDelete_success() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		HttpResponse response = caller.doDelete("/");
		assertEquals("body", EntityUtils.toString(response.getEntity()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDelete_nullUri() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		caller.doDelete(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDelete_emptyUri() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		caller.doDelete("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDelete_badUri() throws ClientProtocolException, IOException {
		HttpCaller caller = new HttpCaller("https://u:p@h/", TestHttpCaller.fakeClient("body"));
		caller.doDelete("x");
	}
}
