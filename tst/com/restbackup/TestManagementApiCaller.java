/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.util.EntityUtils;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class TestManagementApiCaller {

	@Test
	public void testManagementApiCaller() {
		String accessUrl = "https://USER:PASS@host/";
		ManagementApiCaller caller = new ManagementApiCaller(accessUrl);
		assertEquals(accessUrl, caller.getAccessUrl());
	}

	public HttpClient fakeClient_CreateBackupAccount() throws Exception {
		HttpClient client = Mockito.mock(HttpClient.class);
		Mockito.when(client.getParams()).thenReturn(new BasicHttpParams());
		Answer<Object> answer = new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				assertThat(args[0], new IsInstanceOf(HttpPost.class));
				HttpPost request = (HttpPost) args[0];
				try {
					assertEquals("description=A+B&retaindays=8",
							EntityUtils.toString(request.getEntity()));
					assertEquals("application/x-www-form-urlencoded; charset=UTF-8", request
							.getEntity().getContentType().getValue());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				String body = "{\r\n\"account\":\"/acct123\",\r\n" + "\"retaindays\":8,\r\n"
						+ "\"description\":\"A B\",\r\n\"access-url\":\"https://x:y@h:7/\"\r\n"
						+ "}\r\n";
				return TestUtils.fakeResponse(201, body);
			}
		};
		Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(answer);
		return client;
	}

	@Test
	public void testCreateBackupAccountStringIntInt() throws Exception {
		HttpClient client = fakeClient_CreateBackupAccount();
		ManagementApiCaller caller = new ManagementApiCaller("https://u:p@h/", client);
		long start = System.currentTimeMillis();
		BackupAccountDetails account = caller.createBackupAccount("A B", 8, 0);
		long elapsedMillis = System.currentTimeMillis() - start;
		assertTrue(elapsedMillis < 1000);
		assertEquals("A B", account.getDescription());
		assertEquals("/acct123", account.getAccountId());
		assertEquals(8, account.getRetainUploadsDays());
		assertEquals("https://x:y@h:7/", account.getAccessUrl());
	}

	@Test
	public void testCreateBackupAccountStringInt() throws Exception {
		HttpClient client = fakeClient_CreateBackupAccount();
		ManagementApiCaller caller = new ManagementApiCaller("https://u:p@h/", client);
		long start = System.currentTimeMillis();
		BackupAccountDetails account = caller.createBackupAccount("A B", 8);
		long elapsedMillis = System.currentTimeMillis() - start;
		assertTrue(elapsedMillis > 4500);
		assertTrue(elapsedMillis < 6000);
		assertEquals("A B", account.getDescription());
		assertEquals("/acct123", account.getAccountId());
		assertEquals(8, account.getRetainUploadsDays());
		assertEquals("https://x:y@h:7/", account.getAccessUrl());
	}

	@Test
	public void testGetBackupAccount() throws Exception {
		HttpClient client = Mockito.mock(HttpClient.class);
		Mockito.when(client.getParams()).thenReturn(new BasicHttpParams());
		Answer<Object> answer = new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				assertThat(args[0], new IsInstanceOf(HttpGet.class));
				HttpGet request = (HttpGet) args[0];
				assertEquals("/f1093e71-60d0-41ae-9179-a82bd232a45a", request.getURI().toString());
				String body = "{\r\n\"account\":\"/f1093e71-60d0-41ae-9179-a82bd232a45a\",\r\n"
						+ "\"retaindays\":8,\r\n\"description\":\"A B\",\r\n"
						+ "\"access-url\":\"https://x:y@h:7/\"\r\n}\r\n";
				return TestUtils.fakeResponse(200, body);
			}
		};
		Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(answer);
		ManagementApiCaller caller = new ManagementApiCaller("https://u:p@h/", client);
		BackupAccountDetails account = caller
				.getBackupAccount("/f1093e71-60d0-41ae-9179-a82bd232a45a");
		assertEquals("A B", account.getDescription());
		assertEquals("/f1093e71-60d0-41ae-9179-a82bd232a45a", account.getAccountId());
		assertEquals(8, account.getRetainUploadsDays());
		assertEquals("https://x:y@h:7/", account.getAccessUrl());
	}

	@Test
	public void testDeleteBackupAccount() throws Exception {
		HttpClient client = Mockito.mock(HttpClient.class);
		Mockito.when(client.getParams()).thenReturn(new BasicHttpParams());
		Answer<Object> answer = new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				assertThat(args[0], new IsInstanceOf(HttpDelete.class));
				HttpDelete request = (HttpDelete) args[0];
				assertEquals("/f1093e71-60d0-41ae-9179-a82bd232a45a", request.getURI().toString());
				return TestUtils.fakeResponse(200, "deleted");
			}
		};
		Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(answer);
		ManagementApiCaller caller = new ManagementApiCaller("https://u:p@h/", client);
		String result = caller.deleteBackupAccount("/f1093e71-60d0-41ae-9179-a82bd232a45a");
		assertEquals("deleted", result);
	}

	@Test
	public void testListBackupAccounts() throws Exception {
		HttpClient client = Mockito.mock(HttpClient.class);
		Mockito.when(client.getParams()).thenReturn(new BasicHttpParams());
		Answer<Object> answer = new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				assertThat(args[0], new IsInstanceOf(HttpGet.class));
				HttpGet request = (HttpGet) args[0];
				assertEquals("/", request.getURI().toString());
				String body = "[\r\n"
						+ "{\"account\":\"/b1f1e86b\",\"retaindays\":7,\"description\":\"d1\"},\r\n"
						+ "{\"account\":\"/8b6e153a\",\"retaindays\":77,\"description\":\"d2\"}\r\n"
						+ "]\r\n";
				return TestUtils.fakeResponse(200, body);
			}
		};
		Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(answer);
		ManagementApiCaller caller = new ManagementApiCaller("https://u:p@h/", client);
		Collection<BackupAccount> accounts = caller.listBackupAccounts();
		assertEquals(2, accounts.size());
		Iterator<BackupAccount> iter = accounts.iterator();
		BackupAccount account = iter.next();
		assertEquals("/b1f1e86b", account.getAccountId());
		assertEquals(7, account.getRetainUploadsDays());
		assertEquals("d1", account.getDescription());
		account = iter.next();
		assertEquals("/8b6e153a", account.getAccountId());
		assertEquals(77, account.getRetainUploadsDays());
		assertEquals("d2", account.getDescription());
	}

	@Test
	public void testToString() {
		ManagementApiCaller caller = new ManagementApiCaller("https://u:p@h/");
		assertEquals("ManagementApiCaller(\"https://u:p@h/\")", caller.toString());
	}
}
