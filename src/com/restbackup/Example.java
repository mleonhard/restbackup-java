package com.restbackup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

public class Example {
	public static void main(String[] args) {
		try {
			System.out.println("Starting");

			// Make a caller object for the management api account
			// String managementApiAccessUrl =
			// "http://SSW1W3:1qntPfe4HsW9CeSn@us.restbackup.com/";
			String managementApiAccessUrl = args[0];
			System.out.println("Management api access-url is \"" + managementApiAccessUrl + "\"");
			ManagementApiCaller man_api = new ManagementApiCaller(managementApiAccessUrl);

			// Create a new backup account
			String description = "NewAccount." + UUID.randomUUID().toString();
			System.out.println("Creating backup account with description \"" + description + "\":");
			BackupAccountDetails newAccount = man_api.createBackupAccount(description, 9);
			System.out.println(newAccount);

			// Get a backup account by ID
			System.out.println("Downloading details using accountId:");
			BackupAccountDetails newAccount2 = man_api.getBackupAccount(newAccount.getAccountId());
			System.out.println(newAccount2);
			assert (newAccount.equals(newAccount2));

			// List backup accounts
			System.out.println("Listing backup accounts:");
			Set<BackupAccount> accounts = new HashSet<BackupAccount>();
			for (BackupAccount account : man_api.listBackupAccounts()) {
				System.out.println(account);
				accounts.add(account);
			}
			assert (accounts.contains(newAccount));

			// Make a caller object for the new account
			String backupApiAccessUrl = newAccount.getAccessUrl();
			System.out.println("Backup api access-url is \"" + backupApiAccessUrl + "\"");
			BackupApiCaller backup_api = new BackupApiCaller(backupApiAccessUrl);

			// Backup a file
			String name = "/" + UUID.randomUUID().toString();
			System.out.println("Uploading file \"" + name + "\"");
			HttpEntity requestEntity = new StringEntity("Contents of file " + name);
			long uploadTimeMillis = System.currentTimeMillis();
			System.out.println(backup_api.put(name, requestEntity));

			// Restore a file
			System.out.println("Downloading file \"" + name + "\"");
			HttpEntity entity = backup_api.get(name);
			assert (entity.getContentLength() == requestEntity.getContentLength());
			System.out.println(">>>>>>>>>>");
			System.out.println(EntityUtils.toString(entity));
			System.out.println("<<<<<<<<<<");

			// List available files
			System.out.println("Listing available files:");
			HashMap<String, FileDetails> files = new HashMap<String, FileDetails>();
			for (FileDetails fileDetails : backup_api.list()) {
				System.out.println(fileDetails);
				files.put(fileDetails.getUri(), fileDetails);
			}
			FileDetails fileDetails = files.get(name);
			assert (fileDetails != null);
			assert (fileDetails.getUri().equals(name));
			assert (fileDetails.getSize() == requestEntity.getContentLength());
			long createTimeMillis = fileDetails.getCreateTime().getTime();
			long deleteTimeMillis = fileDetails.getDeleteTime().getTime();
			assert (Math.abs(createTimeMillis - uploadTimeMillis) < 5L * 60L * 1000L);
			final long NINE_DAYS_MILLIS = 9L * 24L * 3600L * 1000L;
			assert (deleteTimeMillis == createTimeMillis + NINE_DAYS_MILLIS);

			// Delete backup account
			System.out.println("Deleting account:");
			System.out.println(man_api.deleteBackupAccount(newAccount.getAccountId()));

			System.out.println("Done.");
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
