---
title: "Share multiple mt-daap libraries" 
date: 2010-08-03T18:12:34Z
tags:
  - "linux"
  - "mt-daap"
aliases:
  - /posts/2010-08-03-share-multiple-mt-daap-libraries/
---

I recently moved all of our household music to a shared network drive and was looking for a solution to stream these libraries as distinct entities; I wanted to keep my wife’s library separate mostly so I could keep all of my painstakingly catalouged music organized exactly how I like it. I was already using mt-daapd, now known as the Firefly Media Server, to stream my own library from my Linux server. Unfortunately, it doesn’t support streaming two libraries from one program instance. The solution to this is to run two copies of mt-daapd and configure them to play nice with each other. Don’t worry, it’s not quite as difficult as it sounds. The first thing you’ll need is to have a separate configuration file for the second daap instance. The easiest way to do this is to copy the files from an existing installation.

<!--more-->

Run the following commands to copy the configuration and playlist files.

    cp /etc/mt-daapd.conf /etc/mt-daapd-new_library_name.conf
    cp /etc/mt-daapd.playlist /etc/mt-daapd-new_library_name.playlist

Now edit the new configuration file and make sure you change all of the following properties:

*   port – 3690 should do.
*   db\_parms – This is the folder containing the sqlite database for your collection. I suggest you suffix the existing name with ‘-new\_library_name’ to keep things easily recognizable in the future.
*   mp3_dir – point to the new collection.
*   servername – change to a distinct name (new\_library\_name that you used above) so you can tell them apart.
*   playlist – point to the playlist file you created in the previous step.

Note that mt-daapd will create a sqlite database if one doesn’t already exist but you will need to create the containing folder if it doesn’t already exist and ensure it has the correct permissions (have a look at the permissions on the existing folder). Now you need to test the new configuration works. With your first server still running, execute the following command:

    mtdaapd -c /etc/mt-daapd-new_library_name.conf

If everything has worked then running ps -ef | grep mt-daapd should reveal two mt-daapd processes are running. You should now be able to test that both of them are working using a daapd client such as Rhythmbox. Next we need to create an init.d script to start and stop the server at system startup:

    sudo cp /etc/init.d/mt-daapd /etc/init.d/mt-daapd-new_library_name

Edit /etc/init.d/mt-daapd-new\_library\_name and add the following to the properties near the top:

    DAEMON_OPTS="-c /etc/mt-daapd-new_library_name.conf"

Change the `NAME` and `DESC` properties as follows:

    NAME=mt-daapd-new_library_name 
    DESC=mt-daapd-new_library_name

Finally we need to set the boot level of the new instance

    cd /etc/rc0.d
    ln -s /etc/init.d/mt-daapd-new_library_name K25mt-daapd-new_library_name
    cd /etc/rc1.d
    ln -s /etc/init.d/mt-daapd-new_library_name K25mt-daapd-new_library_name
    cd /etc/rc2.d
    ln -s /etc/init.d/mt-daapd-new_library_name S25mt-daapd-new_library_name
    cd /etc/rc3.d
    ln -s /etc/init.d/mt-daapd-new_library_name S25mt-daapd-new_library_name
    cd /etc/rc4.d
    ln -s /etc/init.d/mt-daapd-new_library_name S25mt-daapd-new_library_name
    cd /etc/rc5.d
    ln -s /etc/init.d/mt-daapd-new_library_name S25mt-daapd-new_library_name
    cd /etc/rc6.d
    ln -s /etc/init.d/mt-daapd-new_library_name K25mt-daapd-new_library_name

 
