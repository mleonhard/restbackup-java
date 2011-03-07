/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

/**
 * Holds the full details of a backup account, including access-URL
 * 
 * @see BackupAccount
 */
public class BackupAccountDetails extends BackupAccount {
	private final String _accessUrl;

	public BackupAccountDetails(String accessUrl, String accountId, String description,
			int retainUploadsDays) {
		super(accountId, description, retainUploadsDays);
		_accessUrl = accessUrl;
	}

	/**
	 * A URL for accessing the account through the Backup API. Contains a user
	 * name, password, and endpoint.
	 * 
	 * @return a string such as
	 *         "https://Y21R3P:Mj313x2D1OyrTSpy@us.restbackup.com/"
	 */
	public String getAccessUrl() {
		return _accessUrl;
	}

	/**
	 * Returns a string representation of the object, such as
	 * "BackupAccountDetails(accessUrl="
	 * https://Y21:Mj313x@us.restbackup.com/",accountId="/72d2e-
	 * 1dc1-daf2d1785",description="License 42a745b7",retainUploadsDays=180)"
	 */
	public String toString() {
		return String
				.format("BackupAccountDetails(accessUrl=\"%s\",accountId=\"%s\",description=\"%s\",retainUploadsDays=%d)",
						_accessUrl, getAccountId(), getDescription(), getRetainUploadsDays());
	}
}
