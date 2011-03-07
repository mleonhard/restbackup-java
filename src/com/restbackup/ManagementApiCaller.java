/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Interface class for the RestBackup(tm) Management API. Instantiate like this:
 * 
 * <pre>
 * String manApiAccessUrl = &quot;https://HF7X7S:7I1xxQEW@us.restbackup.com/&quot;;
 * ManagementApiCaller man_api = new ManagementApiCaller(manApiAccessUrl);
 * </pre>
 * 
 * @author Michael Leonhard
 */
public class ManagementApiCaller extends HttpCaller {
	public static final int NEW_ACCOUNT_DELAY_SECONDS = 5;

	/**
	 * Creates a new object for calling the RestBackup(tm) Management API
	 * 
	 * @param accessUrl
	 *            a string with the form "https://USER:PASS@host/"
	 * @throws IllegalArgumentException
	 *             if the access url is malformed
	 */
	public ManagementApiCaller(String accessUrl) throws IllegalArgumentException {
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
	public ManagementApiCaller(String accessUrl, HttpClient httpClient)
			throws IllegalArgumentException {
		super(accessUrl, httpClient);
	}

	/**
	 * Holds account details deserialized from JSON responses
	 */
	protected static class DeserializedAccount {
		public String account = null;
		public int retaindays = -1;
		public String description = null;
		@SerializedName("access-url")
		public String accessUrl = null;

		public DeserializedAccount() {
		};
	}

	/**
	 * Creates a new backup account. Sleeps for the specified number of seconds
	 * to give the new account time to become active. This prevents immediate
	 * subsequent requests to the new backup account from failing with 401 Not
	 * Authorized.
	 * 
	 * @param description
	 *            a string description of the account, such as
	 *            "Account for Customer 102917"
	 * @param retainDays
	 *            the number of days to keep uploaded files
	 * @param delaySeconds
	 *            number of seconds to sleep after creating the backup account.
	 *            Does not sleep if this value is less than 1.
	 * @return an object with the account details. You use this object to
	 *         construct a BackupApiCaller object tied to the backup account.
	 * @throws UnauthorizedException
	 *             if the access-URL is not accepted
	 * @throws RestBackupException
	 *             on all other errors
	 */
	public BackupAccountDetails createBackupAccount(String description, int retainDays,
			int delaySeconds) throws UnauthorizedException, RestBackupException {
		String[] postParams = { "description", description, "retaindays",
				String.valueOf(retainDays) };
		HttpResponse response = doPost("/", postParams);
		expectStatusCode(response, 201); // Ok
		if (response.getEntity() == null) {
			throw new RestBackupException("Response contains no body", response);
		}
		try {
			HttpEntity entity = response.getEntity();
			InputStream entityStream = entity.getContent();
			Reader reader = new InputStreamReader(entityStream, UTF8_CHARSET);
			Gson gson = new Gson();
			DeserializedAccount account = gson.fromJson(reader, DeserializedAccount.class);
			if (delaySeconds > 0) {
				Thread.sleep(1000 * delaySeconds);
			}
			return new BackupAccountDetails(account.accessUrl, account.account,
					account.description, account.retaindays);
		} catch (IOException e) {
			throw new RestBackupException(e);
		} catch (InterruptedException e) {
			throw new RestBackupException(e);
		}
	}

	/**
	 * Creates a new backup account. Sleeps for several seconds to give the new
	 * account time to become active. This prevents immediate subsequent
	 * requests to the new backup account from failing with 401 Not Authorized.
	 * 
	 * @param description
	 *            a string description of the account, such as
	 *            "Account for Customer 102917"
	 * @param retainDays
	 *            the number of days to keep uploaded files
	 * @return an object with the account details. You use this object to
	 *         construct a BackupApiCaller object tied to the backup account.
	 * @throws UnauthorizedException
	 *             if the access-URL is not accepted
	 * @throws RestBackupException
	 *             on all other errors
	 */
	public BackupAccountDetails createBackupAccount(String description, int retainDays)
			throws UnauthorizedException, RestBackupException {
		return createBackupAccount(description, retainDays, NEW_ACCOUNT_DELAY_SECONDS);
	}

	/**
	 * Looks up the backup account with the specified account id
	 * 
	 * @param accountId
	 *            the accountId of the account to look up, such as
	 *            "/f1093e71-60d0-41ae-9179-a82bd232a45a"
	 * @return an object with the account details. You use this object to
	 *         construct a BackupApiCaller object tied to the backup account.
	 * @throws ResourceNotFoundException
	 *             if the account was not found
	 * @throws UnauthorizedException
	 *             if the access-URL is not accepted
	 * @throws RestBackupException
	 *             on all other errors
	 */
	public BackupAccountDetails getBackupAccount(String accountId)
			throws ResourceNotFoundException, UnauthorizedException, RestBackupException {
		HttpResponse response = doGet(accountId, null);
		if (response.getStatusLine().getStatusCode() == 404) { // Not Found
			throw new ResourceNotFoundException(response);
		}
		expectStatusCode(response, 200); // Ok
		if (response.getEntity() == null) {
			throw new RestBackupException("Response contains no body", response);
		}
		try {
			Reader reader = new InputStreamReader(response.getEntity().getContent(), UTF8_CHARSET);
			DeserializedAccount account = new Gson().fromJson(reader, DeserializedAccount.class);
			return new BackupAccountDetails(account.accessUrl, account.account,
					account.description, account.retaindays);
		} catch (IOException e) {
			throw new RestBackupException(e);
		}
	}

	/**
	 * Deletes the backup account with the specified account id
	 * 
	 * @param accountId
	 *            the accountId of the account to delete, such as
	 *            "/f1093e71-60d0-41ae-9179-a82bd232a45a"
	 * @return the response body
	 * @throws ResourceNotFoundException
	 *             if the account was not found
	 * @throws UnauthorizedException
	 *             if the access-URL is not accepted
	 * @throws RestBackupException
	 *             on all other errors
	 */
	public String deleteBackupAccount(String accountId) throws ResourceNotFoundException,
			UnauthorizedException, RestBackupException {
		HttpResponse response = doDelete(accountId);
		if (response.getStatusLine().getStatusCode() == 404) { // Not Found
			throw new ResourceNotFoundException(response);
		}
		expectStatusCode(response, 200); // Ok
		return HttpCaller.readEntity(response);
	}

	/**
	 * Downloads the list of backup accounts
	 * 
	 * @return a list of objects representing the backup accounts. Use
	 *         getBackupAccount() to get full details of an account, including
	 *         access-URLs.
	 * @throws UnauthorizedException
	 *             if the access-URL is not accepted
	 * @throws RestBackupException
	 *             on all other errors
	 * @see #getBackupAccount(String accountId)
	 */
	public Collection<BackupAccount> listBackupAccounts() throws UnauthorizedException,
			RestBackupException {
		HttpResponse response = doGet("/", null);
		expectStatusCode(response, 200); // Ok
		try {
			Reader reader = new InputStreamReader(response.getEntity().getContent(), UTF8_CHARSET);
			DeserializedAccount[] items = new Gson().fromJson(reader, DeserializedAccount[].class);
			List<BackupAccount> result = new ArrayList<BackupAccount>(items.length);
			for (DeserializedAccount item : items) {
				result.add(new BackupAccount(item.account, item.description, item.retaindays));
			}
			return result;
		} catch (IOException e) {
			throw new RestBackupException(e);
		}
	}

	/**
	 * Returns a string representation of the object, such as
	 * "ManagementApiCaller(\"https://HF7X7S:7I1xxQEW@us.restbackup.com/\")"
	 */
	public String toString() {
		return "ManagementApiCaller(\"" + _accessUrl + "\")";
	}
}
