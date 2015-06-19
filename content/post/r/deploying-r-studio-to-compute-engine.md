---
title: "Deploying R Studio on Compute Engine" 
date: 2015-03-23T15:32:37-06:00
tags: 
  - "compute engine"
  - "r"
  - "r studio"
aliases:
  - /posts/r/deploying-r-studio-to-compute-engine/
  - /posts/2015-03-30-deploying-r-studio-to-compute-engine/
---

Sometimes you have a data analysis problem that is just too big for your desktop
or laptop. The limiting factor here is generally RAM. Thankfully, services like
Google Compute Engine allow you to lease servers with up to 208GB of RAM, large
enough for a wide variety of intensive tasks. An ancillary benefit of using a
service like Compute Engine is that it allows you to easily load your data from
a Cloud Storage Bucket, meaning you don't need to keep a copy of the large
dataset locally at all times. 

R Studio has a remote mode allowing you to install it on a server with access
through a remote interface. This tutorial details how to start a Compute Engine
instance, install R Studio on it and access R Studio from the remote interface.

The rest of this tutorial assumes that you have a Google Cloud Platform account
with billing enabled and have installed the [Google Cloud
SDK](https://cloud.google.com/sdk/).

## Deploying a Compute Engine Instance

The first step is to deploy your Compute Engine instance. The `gcloud compute`
command allows you to create instances. The only required parameter to create an
instance is the instance name. We will call our instance `r-studio` but you can
choose any name you like. R Studio Server is typically built on Ubuntu so it is
safest to use the Ubuntu distribution for your server.

```bash
gcloud compute instances create r-studio
```

You will be prompted to choose a
[Zone](https://cloud.google.com/compute/docs/zones). Just choose a zone close to
you. You can also specify the zone when creating the instance using the `--zone`
parameter. For example.

```bash
gcloud compute instances create r-studio --zone us-central1-a
```

You will also have to open the Compute Engine firewall to allow port 8787 for R
Studio.

```bash
gcutil addfirewall allow-r-studio --allowed=tcp:8787
```

## Installing R Studio

Once we have our Compute Engine instance set up, we log in to the machine using ssh.

```bash
gcloud compute ssh r-studio --zone us-central1-a
```

Now that we are logged in to the Compute Engine instance, it's time to install
R by first updating the Debian apt-get repository and then installing R.

```bash
sudo apt-get update
sudo apt-get install r-base r-base-dev
```

R Studio currently requries OpenSSL version 0.9.8. We need to install this
separately and then install install R Studio

```bash
wget http://ftp.us.debian.org/debian/pool/main/o/openssl/libssl0.9.8_0.9.8o-4squeeze14_amd64.deb
sudo dpkg -i libssl0.9.8_0.9.8o-4squeeze14_amd64.deb
sudo apt-get install gdebi-core
wget http://download2.rstudio.org/rstudio-server-0.98.1103-amd64.deb
sudo gdebi rstudio-server-0.98.1103-amd64.deb
```

You should be up and running with R Studio on your compute engine instance. To
verify, navigate to the IP address of your Compute Engine instance on port 8787
(the default R Studio port).

```bash
http://<ipaddress>:8787
```

R Studio only permits access to users of the system, we can add a user with
standard Linux tools like adduser. For example, to create a new user named
rstudio and specify the password you could execute the following commands.

```bash
sudo adduser rstudio
```

You will be prompted to enter a password for the user and confirm the users name
and phone number.

Afterwards, logging in with the user you created will present a web UI of the
familiar R Studio. You can now perform analysis on those larger data sets using
the R Studio that just weren't possible on a laptop.
