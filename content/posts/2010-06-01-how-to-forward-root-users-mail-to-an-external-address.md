---
title: "How to forward root users mail to an external address" 
date: 2010-06-01T14:12:32Z
description: "I recently set up a RAID array wanted to keep notified of any errors that may occur. By default Ubuntu Server sends mail to the root user whenever an error occurs but logging in as root to check mail every so often didn’t seem very convenient. There had to be a better way. The best information that I found came from the Ubuntu community forums. Here I’ll expand on that discussion, hopefully helping other people who are trying to do the same thing. The traditional way to forward mail is to create a .forward file in your home directory, entering the addresses that you would like to forward mail to."
tags: ["ubuntu", "linux"]
---

I recently set up a RAID array wanted to keep notified of any errors that may occur. By default Ubuntu Server sends mail to the root user whenever an error occurs but logging in as root to check mail every so often didn’t seem very convenient. There had to be a better way. The best information that I found came from the Ubuntu community forums. Here I’ll expand on that discussion, hopefully helping other people who are trying to do the same thing. The traditional way to forward mail is to create a .forward file in your home directory, entering the addresses that you would like to forward mail to.

For example,

    sudo su cd ~ vim .forward

And add the line

    jane.doe@gmail.com

before saving and closing the file. Unfortunately, things won’t be that easy for two reasons.

1.  Postfix needs to be set up to handle and recognize external addresses.
2.  As a security measure, the root user cannot send mail to external addresses, so we need to forward root mail to another user before sending it on to an external address.

So, if you are following along, delete the .forward file from the root directory so that we are starting fresh. First, install postfix if you haven’t already done so. You can choose no configuration since we will manually configure it.

    sudo apt-get install postfix

Postfix configuration is specified in /etc/postfix/main.cf. Edit this file by replacing it with the following code. The important parts are highlighted by a large comment block.

    # Debian specific: Specifying a file name will cause the first 
    # line of that file to be used as the name. The Debian default 
    # is /etc/mailname. 
    #myorigin = /etc/mailname 
    
    smtpd_banner = $myhostname ESMTP $mail_name (Ubuntu)
    biff = no  
    
    # appending .domain is the MUA's job. 
    append_dot_mydomain = no  
    
    # Uncomment the next line to generate "delayed mail" warnings 
    #delay_warning_time = 4h 
    
    # TLS parameters 
    smtpd_tls_cert_file=/etc/ssl/certs/ssl-cert-snakeoil.pem
    smtpd_tls_key_file=/etc/ssl/private/ssl-cert-snakeoil.key
    smtpd_use_tls=yes
    smtpd_tls_session_cache_database = btree:${queue_directory}/smtpd_scache
    smtp_tls_session_cache_database = btree:${queue_directory}/smtp_scache  
    
    # See /usr/share/doc/postfix/TLS_README.gz in the postfix-doc package for 
    # information on enabling SSL in the smtp client. 
    
    myhostname = localhost
    alias_maps = hash:/etc/aliases
    alias_database = hash:/etc/aliases
    mydestination = localhost, localhost.localdomain
    mynetworks = 127.0.0.0/8
    mailbox_size_limit = 0
    recipient_delimiter =  
    inet_interfaces = loopback-only
    inet_protocols = all  
    
    ############################# 
    # enable forwarding to external address
    ############################# 
    
    ##### client TLS parameters ##### 
    smtp_tls_loglevel=1
    smtp_tls_security_level=encrypt
    smtp_sasl_auth_enable=yes
    smtp_sasl_password_maps=hash:/etc/postfix/sasl/passwd
    smtp_sasl_security_options = noanonymous  
    
    ##### map jane@localhost to jane.doe@gmail.com ##### 
    smtp_generic_maps=hash:/etc/postfix/generic
    relayhost=[smtp.gmail.com]:587

By default, Postfix tries to deliver mail directly to the Internet. The relayhost tells Postfix to use gmails servers to provide mail delivery service instead. This is a good thing for two reasons.

1.  Google’s mail servers (or your ISPs, or whoever is your mail provider) are already set up for secure smtp. Running a mail server is non-trivial so in most cases you want someone else to do this for you.
2.  Most services will reject mail that does not originate from a registered domain name in order to cut down on spam. I use my server locally and don’t have a domain name; by using GMail as my relayhost all mail is sent from Google’s servers which will not be rejected for delivery.

Note that to use DNS the relayhost needs square brackets around it, otherwise it will look for an MX record.  I’ve seen a few tutorials that don’t properly explain this and it took a bit of fiddling to figure out what was wrong. Other important parts of the main.cf file are

    smtp_sasl_password_maps=hash:/etc/postfix/sasl/passwd

and

    smtp_generic_maps=hash:/etc/postfix/generic

The /etc/postfix/sasl/passwd file contains your Gmail password. You can edit the file and add the lines

    [smtp.gmail.com]:587 jane.doe@gmail.com:doeadeer

Postfix reads a hashmap database generated from the passwd file.  To create passwd.db and set ownership and permissions appropriately run the following commands:

    cd /etc/postfix/sasl
    postmap passwd
    chown root.root passwd passwd.db
    chmod 600 passwd passwd.db

The file /etc/postfix/generic tells Postfix how to map local e-mail addresses to Internet addresses when mail is sent.  Postfix rewrites “From:” headers to make e-mail appear to come from  instead of jane@localhost. You can edit this file and add the line

    jane@localhost jane.doe@gmail.com

Postfix is expecting to read a hashmap just like the passwd.db file above.  You can generate /etc/postfix/generic.db by using the postmap command:

    cd /etc/postfix postmap generic

Start or reload Postfix

    /etc/init.d/postfix restart

Now, let’s test out what we have done. To check that basic delivery works, run the following command as a normal user (replacing “jane” with your username):

    sendmail -bv jane

Then, while logged in as jane check to see that you have mail.

    mail

To check that Postfix can successfully connect to Gmail, run

    sendmail -bv jane.doe@gmail.com

and check your Gmail account to see that you received your test message. If both of these commands worked we know that you can properly send mail both locally and externally. This doesn’t quite solve our problem yet.  We still need to forward mail from root to a local user and from the local user to your external address.

First we’ll forward mail from root on to a local user

    sudo su cd ~ vim .forward

Add the line

    jane.localhost

And then to forward from the local user to gmail create a .forward file in the local users home directory and add the line

    jane.doe@gmail.com

Lastly, send mail to root.

    sendmail -bv root

and then open Gmail to ensure everything worked!
