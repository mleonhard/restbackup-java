/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

import java.io.IOException;

public class RestBackupException extends IOException {
	private static final long serialVersionUID = 1L;

	public RestBackupException(String s) {
		super(s);
	}

	public RestBackupException(Throwable e) {
		super(e);
	}
}
