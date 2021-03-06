/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

/**
 * Holds basic information about a backup account. Does not contain the
 * access-URL.
 * 
 * @see BackupAccountDetails
 */
public class BackupAccount {
	private final String _accountId;
	private final String _description;
	private final int _retainUploadsDays;

	public BackupAccount(String accountId, String description, int retainUploadsDays) {
		_accountId = accountId;
		_description = description;
		_retainUploadsDays = retainUploadsDays;
	}

	/**
	 * Retrieves the ID of this backup account. Use this ID when making calls to
	 * the Management API.
	 * 
	 * @return a string like "/171633a5-233f-4510-9098-bac142260013"
	 */
	public String getAccountId() {
		return _accountId;
	}

	/**
	 * @return the description attached to this backup account if available
	 */
	public String getDescription() {
		return _description;
	}

	/**
	 * Files uploaded to this account are kept for this number of days and then
	 * automatically deleted
	 * 
	 * @return a positive non-zero number
	 */
	public int getRetainUploadsDays() {
		return _retainUploadsDays;
	}

	/**
	 * Returns a string representation of the object, such as
	 * "BackupAccountDetails(accountId="/72d2e-
	 * 1dc1-daf2d1785",description="License 42a745b7",retainUploadsDays=180)"
	 */
	public String toString() {
		return String.format(
				"BackupAccountDetails(accountId=\"%s\",description=\"%s\",retainUploadsDays=%d)",
				_accountId, _description, _retainUploadsDays);
	}

	public boolean equals(Object obj) {
		return obj != null && obj.toString().equals(toString());
	}

	public int hashCode() {
		return toString().hashCode();
	}
}
