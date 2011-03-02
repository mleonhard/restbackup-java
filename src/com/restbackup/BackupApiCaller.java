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
	 * Creates a new object for calling the RestBackup(tm) Management API
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
	 * Creates a new object for calling the RestBackup(tm) Management API
	 * 
	 * @param accountDetails
	 *            an object containing a valid backup account access-URL
	 */
	public BackupApiCaller(BackupAccountDetails accountDetails) {
		super(accountDetails.getAccessUrl());
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
	 * @throws RestBackupException
	 *             on error
	 * @see org.apache.http.entity.ByteArrayEntity
	 * @see org.apache.http.entity.FileEntity
	 * @see org.apache.http.entity.InputStreamEntity
	 * @see org.apache.http.entity.StringEntity
	 */
	public HttpResponse put(String uri, HttpEntity entity) throws RestBackupException {
		return doPut(uri, entity);
	}

	/**
	 * Retrieves the file at the specified uri
	 * 
	 * @param uri
	 *            the location of the file to download, such as
	 *            "/previously-uploaded-file"
	 * @return a response object
	 * @throws IllegalArgumentException
	 *             when the uri is malformed
	 * @throws RestBackupException
	 *             on error downloading the file
	 * @see org.apache.http.HttpEntity#getContent()
	 * @see org.apache.http.HttpEntity#getContentLength()
	 * @see org.apache.http.util.EntityUtils#toString(HttpEntity)
	 * @see org.apache.http.util.EntityUtils#toString(HttpEntity, String)
	 * @see org.apache.http.util.EntityUtils#toByteArray(HttpEntity)
	 */
	public HttpResponse get(String uri) throws RestBackupException, IllegalArgumentException {
		return doGet(uri, null);
	}

	/**
	 * Retrieves the specified file
	 * 
	 * @param fileDetails
	 *            object with details of the file to download
	 * @return a response object
	 * @throws IllegalArgumentException
	 *             if the file's uri is malformed
	 * @throws RestBackupException
	 *             on error downloading the file
	 * @see org.apache.http.HttpEntity#getContent()
	 * @see org.apache.http.HttpEntity#getContentLength()
	 * @see org.apache.http.util.EntityUtils#toString(HttpEntity)
	 * @see org.apache.http.util.EntityUtils#toString(HttpEntity, String)
	 * @see org.apache.http.util.EntityUtils#toByteArray(HttpEntity)
	 */
	public HttpResponse get(FileDetails fileDetails) throws RestBackupException,
			IllegalArgumentException {
		return get(fileDetails.getUri());
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
	 * @throws RestBackupException
	 *             on error
	 */
	public Collection<FileDetails> list() throws RestBackupException {
		// TODO: return an iterator and stream the list
		try {
			HttpResponse response = doGet("/", new BasicHeader("Accept", "application/json"));
			Reader reader = new InputStreamReader(response.getEntity().getContent(), UTF8_CHARSET);
			DeserializedFile[] items = new Gson().fromJson(reader, DeserializedFile[].class);
			List<FileDetails> result = new ArrayList<FileDetails>(items.length);
			for (DeserializedFile item : items) {
				result.add(new FileDetails(item.name, item.size, item.createtime, item.deletetime));
			}
			return result;
		} catch (IOException e) {
			throw new RestBackupException(e);
		}
	}

	public String toString() {
		return "BackupApiCaller(\"" + _accessUrl + "\")";
	}

//  def put_encrypted(self, passphrase, name, data):
//      """Encrypts and uploads the provided data to the backup
//      account, storing it with the specified name.  Data may be a
//      byte string or RewindableSizedInputStream object.  Returns a
//      string containing the response body.
//      
//      Uses AES for confidentiality, SHA-256 HMAC for authentication,
//      and PBKDF2 with 1000 rounds of HMAC-SHA-256 for key
//      generation.  Raises RestBackupException on error.
//      """
//      import chlorocrypt
//      if not hasattr(data, 'read'):
//          data = StringReader(data)
//      encrypted = chlorocrypt.EncryptingReader(data, passphrase)
//      extra_headers = {
//          'Content-Length':str(len(encrypted)),
//          'User-Agent' : HTTP_USER_AGENT + ' chlorocrypt/' + \
//              chlorocrypt.__version__
//          }
//      response = self.call('PUT', name, encrypted, extra_headers)
//      return response.read()
//  
//  def get_encrypted(self, passphrase, name):
//      """Retrieves the specified file and decrypts it.  Returns a
//      SizedInputStream object.
//      
//      Raises RestBackupException on network error.  Raises
//      WrongPassphraseException if the provided passphrase is
//      incorrect.  Raises DataDamagedException if file was corrupted
//      on the network.  Due to padding, the stream may yield up to 16
//      bytes less than the value of len(stream).
//      """
//      import chlorocrypt
//      extra_headers = { 'User-Agent' : \
//                            HTTP_USER_AGENT + ' chlorocrypt/' + chlorocrypt.__version__ }
//      http_response = self.call('GET', name, extra_headers=extra_headers)
//      http_reader = HttpResponseReader(http_response)
//      decrypted = chlorocrypt.DecryptingReader(http_reader, passphrase)
//      return decrypted
}
