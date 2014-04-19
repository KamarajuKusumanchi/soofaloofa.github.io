---
layout: post
title: Date.now() in IE6
author: Kevin Sookocheff
date: 2011/06/09 11:10:09
tags:
  - javascript
  - ie
---

I was fiddling with a JavaScript timer application and noticed a bug in IE6. I’m sure this is documented elsewhere but I couldn’t find anything with some quick searches; hence this post.

`Date.now()` is not supported by IE6. Use `new Date().getTime()` instead.
