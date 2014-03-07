---
layout: post
title: Custom Posterous Subdomains with CPanel
author: Kevin Sookocheff
date: 2011/03/23
tags:
  - Technology
---

I recently started using Posterous and will be converting my WordPress blog to the service.  One thing I wanted was a custom subdomain (i.e., blog.kevinsookocheff.com); I couldn’t easily find clear instructions for  subdomains (just for domains) so I thought I’d share how I did it from a basic cPanel hosting environment.

1.  Login to your cPanel account.
2.  Under the Domains heading, select Simple DNS Zone Editor.
3.  Select your domain from the drop-down list.
4.  Under Add a CNAME Record fill in the following (using blog here as your subdomain). 
    1.  Name: blog
    2.  CNAME: posterous.com
5.  Click the button to Add CNAME Record.

You will have to wait a few hours for the change to propagate.  Then navigate to yoursubdomain.domain.com.  You will end up at a posterous.com 404 page.  This is because Posterous does not know to link your subdomain to your Posterous yet. Head over to posterous.com and log in.  From here click on your site settings link and enter yoursubdomain.domain.com as the custom domain for you Posterous.
