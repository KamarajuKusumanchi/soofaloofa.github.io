---
title: "Stripe checkout opening in a new tab"
date: 2014-03-25T07:23:45Z
tags: 
  - "stripe"
  - "debugging"
---

At [VendAsta](http://www.vendasta.com) we've been integrating with the [Stripe](http://stripe.com/checkout) payment system using Stripe Checkout. The experience has been completely painless and surprisingly simple. Then came a hiccup. While demoing the new functionality we found that one particular computer in the office would open the checkout modal dialog in a new browser window. Just one laptop. It was running the same version of Chrome that we were developing on. It was running the same OS as a working test machine. But the dialog would consistently open in a new browser window on just this one laptop. 

<!--more-->

After a Friday afternoon debugging session our Product Owner decided to jump onto IRC and chat directly with the Stripe developers. They were quickly able to diagnose our problem: touch. The troublesome laptop was a Windows 8 machine with a touch display — and Stripe Checkout will open in a new browser window when used on a tablet.

This episode was a great reminder of how the first step in debugging is often to [find the difference between the working and non-working system](http://www.hanselman.com/blog/BackToBasicsAssertYourAssumptionsAndDiffYourSourceCode.aspx). Hopefully this post will help someone else with the same problem. 

P.S. Kudos to Stripe for their prompt help via IRC.
