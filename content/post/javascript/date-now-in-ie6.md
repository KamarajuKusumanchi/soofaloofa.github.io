---
title: "Date.now() in IE6" 
date: 2011-06-09T11:10:09Z
tags:
  - "javascript"
  - "ie"
aliases:
  - "posts/2011-06-09-date-now-in-ie6/"
---

I was fiddling with a JavaScript timer application and noticed a bug in IE6. I’m sure this is documented elsewhere but I couldn’t find anything with some quick searches; hence this post.

<!--more-->

`Date.now()` is not supported by IE6. Use `new Date().getTime()` instead.
