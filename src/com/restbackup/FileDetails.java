/**
 * Copyright (C) 2011 Rest Backup LLC.
 * 
 * Use of this software is subject to the RestBackup.com Terms of Use at
 * http://www.restbackup.com/terms
 */
package com.restbackup;

import java.util.Date;

/**
 * Holds details about a file stored in a backup account
 */
public class FileDetails {
	private final String _uri;
	private final long _size;
	private final Date _createTime;
	private final Date _deleteTime;

	public FileDetails(String uri, long size, long createTimeSec, long deleteTimeSec) {
		_uri = uri;
		_size = size;
		_createTime = new Date(createTimeSec * 1000);
		_deleteTime = new Date(deleteTimeSec * 1000);
	}

	/**
	 * @return a string with the uri of the file, such as
	 *         "/path-to-uploaded-file"
	 */
	public String getUri() {
		return _uri;
	}

	/**
	 * @return the size of the file in bytes
	 */
	public long getSize() {
		return _size;
	}

	/**
	 * @return the creation time of the file
	 */
	public Date getCreateTime() {
		return _createTime;
	}

	/**
	 * @return the time when the file will be automatically deleted
	 */
	public Date getDeleteTime() {
		return _deleteTime;
	}

	/**
	 * @return a string representation of the object, such as
	 *         "FileDetails(uri="/previously-uploaded-file",
	 *         size=1947648,createTime=1299076727,deleteTime=1299681527)"
	 */
	public String toString() {
		return String.format("FileDetails(uri=\"%s\",size=%d,createTime=%d,deleteTime=%d)", _uri,
				_size, _createTime.getTime() / 1000L, _deleteTime.getTime() / 1000L);
	}

	public boolean equals(Object obj) {
		return obj != null && obj.toString().equals(toString());
	}

	public int hashCode() {
		return toString().hashCode();
	}
}
