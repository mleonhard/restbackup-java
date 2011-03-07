/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class TestBackupApiCaller {

	@Test
	public void testBackupApiCallerString() {
		String accessUrl = "https://u:p@h/";
		BackupApiCaller caller = new BackupApiCaller(accessUrl);
		assertEquals(accessUrl, caller.getAccessUrl());
	}

	@Test
	public void testList() throws ClientProtocolException, IOException {
		HttpClient client = Mockito.mock(HttpClient.class);
		Mockito.when(client.getParams()).thenReturn(new BasicHttpParams());
		Answer<Object> answer = new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				assertThat(args[0], new IsInstanceOf(HttpGet.class));
				HttpGet request = (HttpGet) args[0];
				assertEquals("/", request.getURI().toString());
				String body = "[\r\n"
						+ "{\"name\":\"/files.20100521.tar.gz\",\"size\":396265,\"date\":\"2010-05-21T11:00:03Z\",\"createtime\":1274439603,\"deletetime\":1305975603},\r\n"
						+ "{\"name\":\"/files.20100522.tar.gz\",\"size\":416849,\"date\":\"2010-05-22T11:00:04Z\",\"createtime\":1274526004,\"deletetime\":1306062004}\r\n"
						+ "]\r\n";
				return TestUtils.fakeResponse(200, body);
			}
		};
		Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(answer);
		BackupApiCaller caller = new BackupApiCaller("https://u:p@h/", client);
		Collection<FileDetails> files = caller.list();
		assertEquals(2, files.size());
		Iterator<FileDetails> iter = files.iterator();
		FileDetails file = iter.next();
		assertEquals("/files.20100521.tar.gz", file.getUri());
		assertEquals(396265, file.getSize());
		assertEquals(1274439603 * 1000L, file.getCreateTime().getTime());
		assertEquals(1305975603 * 1000L, file.getDeleteTime().getTime());
		file = iter.next();
		assertEquals("/files.20100522.tar.gz", file.getUri());
		assertEquals(416849, file.getSize());
		assertEquals(1274526004 * 1000L, file.getCreateTime().getTime());
		assertEquals(1306062004 * 1000L, file.getDeleteTime().getTime());
	}
}
