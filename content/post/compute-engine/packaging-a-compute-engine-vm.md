---
title: "Packaging a Compute Engine Virtual Machine Image"
date: 2014-10-28T06:03:42Z
tags: 
  - "compute engine"
  - "images"
aliases:
  - "posts/2014-10-28-packaging-a-compute-engine-vm/"
---

Google Compute Engine allows you to make custom images from a running virtual
machine. The documentation provides a sufficient example but is a little bit
scattered. This article collects and presents all the steps necessary to create
your own Compute Engine images that you can use as a base for virtual machines.

<!--more-->

## 1. Create a Packaged Image

Once your have booted your instanced and ssh'd in you can create your image
using the `gcimagebundle` command.

```bash
sudo gcimagebundle -d <boot-device> -o <output-directory>
```

The boot device is `/dev/sda` so a complete example might look like this.

```bash
sudo gcimagebundle -d /dev/sda -o /tmp
```

## 2. Upload to Cloud Storage

Once you have a Compute Engine image you need to upload it to Cloud Storage by
logging into your gcloud account and copying your file to a Cloud Storage
bucket. Thankfully, Compute Engine installs the necessary gcloud and gsutil
executables in the base virtual machines.

```bash
# login to your gcloud account
gcloud auth login

# make a bucket for compute engine images
gsutil mb gs://<bucket-name>

# copy your image to the bucket
gsutil cp /tmp/<image-name>.image.tar.gz gs://<bucket-name>
```

## 3. Create a Compute Engine Image

We don't need our virtual machine any more. You can exit the machine and head
back to your console. From your console you can create a compute engine image
using `gcloud compute`.

```bash
gcloud compute images create <image-name> --source-uri gs://<bucket-name>/<image-name>.image.tar.gz
```

## 4. Use your Image to Create a Compute Engine Instance

Finally, now that we have a predefined image, you can create a compute engine
instance using your image name.

```bash
gcloud compute instances create --image <image-name>
```

Your images offer the same functionality as the Google provided versions. You
can mark them as `DEPRECATED`, `OBSOLETE` or `DELETED` to manage your
deployment.
