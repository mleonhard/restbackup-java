RestBackup(tm) Client Library
=============================

This package provides convenient classes for making calls to the
RestBackup(tm) Backup and Management APIs.  These APIs are documented
at http://dev.restbackup.com/developers

Example usage:

    // Make a caller object for the management api account
    String managementApiAccessUrl = "http://SSW1W3:1qntPfe4HsW9CeSn@us.restbackup.com/";
    ManagementApiCaller man_api = new ManagementApiCaller(managementApiAccessUrl);

    // Create a new backup account
    BackupAccountDetails newAccount = man_api.createBackupAccount("description", 9);

    // Get a backup account by ID
    BackupAccountDetails otherAccount = man_api.getBackupAccount("/576d91c7-11a1-4375-a599-a001c07584cd");

    // List backup accounts
    for (BackupAccount account : man_api.listBackupAccounts()) {
        System.out.println(account);
    }

    // Make a caller object for the new account
    String backupApiAccessUrl = newAccount.getAccessUrl();
    BackupApiCaller backup_api = new BackupApiCaller(backupApiAccessUrl);

    // Backup a file
    File file = new File("/file/to/backup");
    HttpEntity requestEntity = new org.apache.http.entity.FileEntity(file, "application/octet-stream");
    backup_api.put(name, requestEntity);

    // Restore a file
    HttpEntity entity = backup_api.get(name);
    entity.writeTo(new FileOutputStream("/restored/file"));

    // List available files
    for (FileDetails fileDetails : backup_api.list()) {
        System.out.println(fileDetails);
    }

    // Delete backup account
    man_api.deleteBackupAccount(newAccount.getAccountId());

See src/com/restbackup/Example.java for a runnable example.
