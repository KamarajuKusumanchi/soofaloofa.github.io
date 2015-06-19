---
title: "Restoring an App Engine backup into a Big Query table"
date: 2014-08-04T21:18:13Z
tags: 
  - "app engine"
  - "datastore"
  - "backup"
  - "bigquery"
aliases:
  - "posts/2014-08-04-restoring-an-app-engine-backup/"
---

An unfortunate DevOps task for any team running App Engine is restoring data
from backups. One way to do this is by accessing the Google Cloud Storage URL
for a given App Engine backup and importing that backup into BigQuery. This
article will show you to get the Cloud Storage URL for an App Engine backup and
manually perform that import.

<!--more-->

## Getting the Cloud Storage URL

The first thing you need to do is access the cloud storage URL for a given App
Engine backup. First, log in to the Google Developer Console and navigate to
your backup. The filename of the backup will be a long sequence of characters
followed by the name of your model. The file extension will be `.backup_info`.
As an example, this is the filename of backup for an Account model used in one
of our projects.

```bash
agpzfnZiYy1wcm9kckILEhxfQUVfRGF0YXN0b3JlQWRtaW5fT3BlcmF0aW9uGLa6psgBDAsSFl9BRV9CYWNrdXBfSW5mb3JtYXRpb24YAQw.AccountModel.backup_info
```

Right click on your backup and copy the URL to your clipboard. The URL will be
of the form below. The name of your cloud storage bucket and the identifier for
you app have been highlighted below. Replace these with appropriate values for
your project.

```bash
https://console.developers.google.com/m/cloudstorage/b/**bucket**/o/**appid**/2014/06/19/backup-20140619-070000/AccountModel/agpzfnZiYy1wcm9kckILEhxfQUVfRGF0YXN0b3JlQWRtaW5fT3BlcmF0aW9uGLa6psgBDAsSFl9BRV9CYWNrdXBfSW5mb3JtYXRpb24YAQw.AccountModel.backup_info
```

To get the cloud storage URL in the format expected by a BigQuery import remove
everything up to the bucket name.

```bash
**bucket**/o/**appid**/2014/06/19/backup-20140619-070000/AccountModel/agpzfnZiYy1wcm9kckILEhxfQUVfRGF0YXN0b3JlQWRtaW5fT3BlcmF0aW9uGLa6psgBDAsSFl9BRV9CYWNrdXBfSW5mb3JtYXRpb24YAQw.AccountModel.backup_info
```

Now remove the `o` between the bucket name and your app identifier.

```bash
**bucket**/**appid**/2014/06/19/backup-20140619-070000/AccountModel/agpzfnZiYy1wcm9kckILEhxfQUVfRGF0YXN0b3JlQWRtaW5fT3BlcmF0aW9uGLa6psgBDAsSFl9BRV9CYWNrdXBfSW5mb3JtYXRpb24YAQw.AccountModel.backup_info
```

Finally, append `gs://` to the file to arrive at your final Google Cloud Storage
URL.

```bash
gs://**bucket**/**appid**/2014/06/19/backup-20140619-070000/AccountModel/agpzfnZiYy1wcm9kckILEhxfQUVfRGF0YXN0b3JlQWRtaW5fT3BlcmF0aW9uGLa6psgBDAsSFl9BRV9CYWNrdXBfSW5mb3JtYXRpb24YAQw.AccountModel.backup_info 
```

The next step is to import the backup into BigQuery. To do this, navigate to
your project and create a new table in your desired dataset. 

{{% img "2014-08-04-restoring-an-app-engine-backup/create-new-table.png" "Create new table." %}}

In the `Choose destination` tab pick a name for your new table. In my case I'll
name the table with the date of my backup for reference.

{{% img "2014-08-04-restoring-an-app-engine-backup/choose-destination.png" "Choose destination" %}}

Next, choose App Engine Datastore Backup as the source format and paste the
Cloud Storage URL you arrived at above in the appropriate field. 

{{% img "2014-08-04-restoring-an-app-engine-backup/select-data.png" "Select Data Source" %}}

You can choose the defaults for the next tabs and, finally, import your App
Engine backup into BigQuery and watch it being fully restored.
