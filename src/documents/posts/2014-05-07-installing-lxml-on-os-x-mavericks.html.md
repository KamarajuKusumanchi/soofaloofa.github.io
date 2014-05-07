---
layout: post
standalone: true
title: "Installing lxml on OS X Mavericks"
author: Kevin Sookocheff
date: 2014/06/07 10:26:45
tags: 
  - os x
  - lxml
  - pip
---

I recently tried installing lxml for use within an App Engine project on OS X
Mavericks only to be hit with an error message from `clang`.

```
clang: error: unknown argument: '-mno-fused-madd' [-Wunused-command-line-argument-hard-error-in-future]

clang: note: this will be a hard error (cannot be downgraded to a warning) in the future

error: command 'cc' failed with exit status 1
```

The `clang` compiler distributed with version 5.1 of Xcode tightened up some
restrictions and turned compiler warnings into hard errors. To disable this you need to
add a specific flag to ingore these warnings when installing affected packages.

```
ARCHFLAGS=-Wno-error=unused-command-line-argument-hard-error-in-future pip install lxml
```
