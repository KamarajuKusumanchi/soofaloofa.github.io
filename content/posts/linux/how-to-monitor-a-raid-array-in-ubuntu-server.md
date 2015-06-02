---
title: "How to monitor a RAID array in Ubuntu server"
date: 2010-05-28T02:15:45Z
tags: ["raid", "ubuntu", "linux"]
aliases:
  - /posts/2010-05-28-how-to-monitor-a-raid-array-in-ubuntu-server/
---

I’ve been searching for a way to reliably monitor a RAID array.  I found a few resources and decided to compile them in one post.  If you are running Ubuntu Server skip to the bottom for some good news. 

<!--more-->

## /proc/mdstat

The first resource I found is from the [Linux RAID wiki][1].

 [1]: https://raid.wiki.kernel.org/index.php/Detecting,_querying_and_testing#Monitoring_RAID_arrays:/etc/cron.d

> You can always take a look at /proc/mdstat. It won’t hurt. Let’s learn how to read the file. For
> 
> Personalities : \[raid1\]  
> read_ahead 1024 sectors  
> md0 : active raid1 sdb5\[1\] sda5\[0\]  
> 4200896 blocks \[2/2\] \[UU\]
> 
> unused devices: none
> 
> To identify the spare devices, first look for the \[#/#\] value on a line. The first number is the number of a complete raid device as defined. Lets say it is “n”. The raid role numbers \[#\] following each device indicate its role, or function, within the raid set. Any device with “n” or higher are spare disks. 0,1,..,n-1 are for the working array.
> 
> Also, if you have a failure, the failed device will be marked with (F) after the \[#\]. The spare that replaces this device will be the device with the lowest role number n or higher that is not marked (F). Once the resync operation is complete, the device’s role numbers are swapped.
> 
> The order in which the devices appear in the /proc/mdstat output means nothing.

Given this information you could write a script that polls /proc/mdstat searching for character sequence \[UU\] and then schedule this via cron. That seemed like a good solution, but I had also heard about mdadm and wanted to investigate it too.

## mdadm

Also from the [Linux RAID wiki][1].

> mdadm –detail /dev/md0
> 
> This commands will show spare and failed disks loud and clear.
> 
> You can run mdadm as a daemon by using the follow-monitor mode. If needed, that will make mdadm send email alerts to the system administrator when arrays encounter errors or fail. Also, follow mode can be used to trigger contingency commands if a disk fails, like giving a second chance to a failed disk by removing and reinserting it, so a non-fatal failure could be automatically solved.
> 
> Let’s see a basic example. Running
> 
> mdadm –monitor –mail=root@localhost –delay=1800 /dev/md2
> 
> should release a mdadm daemon to monitor /dev/md2. The delay parameter means that polling will be done in intervals of 1800 seconds. Finally, critical events and fatal errors should be e-mailed to the system manager. That’s RAID monitoring made easy.
> 
> Finally, the –program or –alert parameters specify the program to be run whenever an event is detected.

## ubuntu server

This was all great but now my dilemma was in deciding how to put this information to use.  I searched some more and came across the following from [ServerFault][2].

 [2]: http://serverfault.com/questions/49939/daemon-to-verify-linux-md-raid

> On Debian (and therefore Ubuntu) machines, cron runs:
> 
> /usr/share/mdadm/checkarray –cron –all –quiet
> 
> the first Sunday of the month by default (see /etc/cron.d).   All output is mailed to the sys admin.   This command checks all data integrity.
> 
> It basically boils down to:
> 
> echo check > /sys/block/$array/md/sync_action
> 
> but with a lot of sanity around it.   Steal it from your nearest Debian install, or from the mdadm source package.

So, it turns out that my version of Ubuntu Server was already monitoring data integrity once a month.  I then decided to search my init scripts to see what was installed by default and found /etc/init.d/mdadm.  It turns out that, by default, a daemon job to monitor the array (with the mdadm –monitor command shown above) is run at startup and will send mail to root for any strange occurrence. So, if you are running a debian-based server and wish to monitor the state of your RAID array you really don’t have to do anything at all.  But how about sending root mail to your external e-mail address.  Well, that’s the subject of [another post][3].

 [3]: http://www.kevinsookocheff.com/2010/06/01/how-to-forward-root-users-mail-to-an-external-address/
