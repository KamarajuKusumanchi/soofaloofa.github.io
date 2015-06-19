---
title: "Showing a local webpage in Unity3d using the Prime[31] etcetera plugin."
date: 2012-11-15T13:14:15Z
tags:
  - "unity"
  - "prime31"
aliases:
  - "posts/etcetera-plugin-live-alerts-prompts-loading-views-push-localization-and-more/"
---

I've been working with Prime31's Etcetera plugin for iOS development with
Unity3d. There are a lot of great things in there but some of the documentation
is lacking.  Anyways, if you want to show a local web page with the
`showWebPage` command it is a two step process.

<!--more-->

Step 1:

Copy the files you wish to access into a folder called `StreamingAssets` under your `Assets` directory.

Step 2:

Call `EtceteraBinding` as follows:

    EtceteraBinding.showWebPage(Application.dataPath + "/Raw/FolderWithHTML/index.html");

where `FolderWithHTML` is the folder you placed in the `StreamingAssets` folder.
