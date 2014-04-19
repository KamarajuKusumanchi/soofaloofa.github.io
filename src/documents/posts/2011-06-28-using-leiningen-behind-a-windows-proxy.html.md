---
layout: post
title: Using Leiningen behind a Windows proxy
author: Kevin Sookocheff
date: 2011/06/28 11:15:08
tags:
  - clojure
  - leiningen
  - windows
---

I’ve been getting familiar with the [Leiningen][1] build tool for [Clojure][2] but had trouble connecting to the Clojar and Maven repositories behind a proxy.

 [1]: https://github.com/technomancy/leiningen
 [2]: http://clojure.org/

I ended up adding my proxy to Maven’s `settings.xml` and everything worked out.

You can find `settings.xml`  at `C:\apache-maven-X.X.X\conf\settings.xml`  and can edit it according to the conventions described [here][3].

 [3]: http://maven.apache.org/guides/mini/guide-proxies.html

After editing the file run lein deps  to download your project dependencies. If still can’t connect to the repository servers you may have to copy the `settings.xml` file to your `.m2` directory at `C:\Documents and Settings\username\.m2\settings.xml`
