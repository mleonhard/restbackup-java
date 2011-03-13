/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicHeader;
import com.google.gson.Gson;

/**
 * Interface class for the RestBackup(tm) Management API. Instantiate like this:
 * 
 * <pre>
 * String backupApiAccessUrl = &quot;https://Y21R3P:Mj313x2D1OyrTSpy@us.restbackup.com/&quot;;
 * BackupApiCaller backup_api = new BackupApiCaller(backupApiAccessUrl);
 * </pre>
 * 
 * @author Michael Leonhard
 */
public class BackupApiCaller extends HttpCaller {
	/**
	 * Creates a new object for calling the RestBackup(tm) Management API
	 * 
	 * @param accessUrl
	 *            a string with the form "https://USER:PASS@host/"
	 * @throws IllegalArgumentException
	 *             if the access url is malformed
	 */
	public BackupApiCaller(String accessUrl) {
		super(accessUrl);
	}

	/**
	 * Creates a new object for calling the RestBackup(tm) Management API.
	 * 
	 * @param accessUrl
	 *            a string with the form "https://USER:PASS@host/"
	 * @param httpClient
	 *            the client to use for requests from this caller
	 * @throws IllegalArgumentException
	 *             if the access url is malformed
	 */
	public BackupApiCaller(String accessUrl, HttpClient httpClient) {
		super(accessUrl, httpClient);
	}

	/**
	 * Uploads the provided data to the backup account, storing it at the
	 * specified uri
	 * 
	 * @param uri
	 *            the location of the new file, such as "/newly-uploaded-file"
	 * @param entity
	 *            the data to upload
	 * @return a response object
	 * @throws ResourceExistsException
	 *             if a file already exists at that uri
	 * @throws UnauthorizedException
	 *             if the access-URL is not accepted
	 * @throws RestBackupException
	 *             on all other errors
	 * @see org.apache.http.entity.ByteArrayEntity
	 * @see org.apache.http.entity.FileEntity
	 * @see org.apache.http.entity.InputStreamEntity
	 * @see org.apache.http.entity.StringEntity
	 */
	public String put(String uri, HttpEntity entity) throws ResourceExistsException,
			UnauthorizedException, RestBackupException {
		HttpResponse response = doPut(uri, entity);
		try { // Method Not Allowed
			if (response.getStatusLine().getStatusCode() == 405) {
				throw new ResourceExistsException(response);
			}
			expectStatusCode(response, 201); // Created
			return HttpCaller.readEntity(response);
		} finally {
			closeResponseEntityInputStream(response);
		}
	}

	/**
	 * Retrieves the file at the specified uri. Be sure to call
	 * entity.getContent().close() to release the http connection.
	 * 
	 * @param uri
	 *            the location of the file to download, such as
	 *            "/previously-uploaded-file"
	 * @return an entity object with the file data
	 * @throws IllegalArgumentException
	 *             when the uri is malformed
	 * @throws ResourceNotFoundException
	 *             if there is no resource at the specified uri
	 * @throws UnauthorizedException
	 *             if the access-URL is not accepted
	 * @throws RestBackupException
	 *             on all other errors
	 * @see org.apache.http.HttpEntity#getContent()
	 * @see org.apache.http.HttpEntity#getContentLength()
	 * @see org.apache.http.util.EntityUtils#toString(HttpEntity)
	 * @see org.apache.http.util.EntityUtils#toString(HttpEntity, String)
	 * @see org.apache.http.util.EntityUtils#toByteArray(HttpEntity)
	 */
	public HttpEntity get(String uri) throws IllegalArgumentException, ResourceNotFoundException,
			UnauthorizedException, RestBackupException {
		HttpResponse response = doGet(uri, null);
		if (response.getStatusLine().getStatusCode() == 404) { // Not Found
			closeResponseEntityInputStream(response);
			throw new ResourceNotFoundException(response);
		}
		expectStatusCode(response, 200); // Ok
		if (response.getEntity() == null) {
			closeResponseEntityInputStream(response);
			throw new RestBackupException("Response contains no body", response);
		}
		return response.getEntity();
	}

	/**
	 * Holds file details deserialized from JSON responses
	 */
	public static class DeserializedFile {
		public String name = null;
		public long size = -1;
		public long createtime = -1;
		public long deletetime = -1;

		public DeserializedFile() {
		};
	}

	/**
	 * Downloads the list of files currently stored in the backup account
	 * 
	 * @return a collection of objects with details about the files
	 * @throws UnauthorizedException
	 *             if the access-URL is not accepted
	 * @throws RestBackupException
	 *             on all other errors
	 */
	public Collection<FileDetails> list() throws UnauthorizedException, RestBackupException {
		// TODO: return an iterator and stream the list
		HttpResponse response = doGet("/", new BasicHeader("Accept", "application/json"));
		expectStatusCode(response, 200); // Ok
		if (response.getEntity() == null) {
			closeResponseEntityInputStream(response);
			throw new RestBackupException("Response contains no body", response);
		}
		try {
			Reader reader = new InputStreamReader(response.getEntity().getContent(), UTF8_CHARSET);
			DeserializedFile[] items = new Gson().fromJson(reader, DeserializedFile[].class);
			List<FileDetails> result = new ArrayList<FileDetails>(items.length);
			for (DeserializedFile item : items) {
				result.add(new FileDetails(item.name, item.size, item.createtime, item.deletetime));
			}
			return result;
		} catch (IOException e) {
			throw new RestBackupException(e);
		} finally {
			closeResponseEntityInputStream(response);
		}
	}

	/**
	 * Returns a string representation of the object, such as
	 * "BackupApiCaller(accessUrl=" https://Y21:Mj313x@us.restbackup.com/")"
	 */
	public String toString() {
		return "BackupApiCaller(\"" + _accessUrl + "\")";
	}
}
