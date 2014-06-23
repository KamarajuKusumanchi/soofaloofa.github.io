---
title: "Copying from Vim to the OS X Mountain Lion clipboard"
date: 2013-01-22T08:16:42Z
tags:
  - "vim"
  - "osx"
---

The latest version of OS X (Mountain Lion) broke compatibility with the vim and the OS clipboard. In most cases you can configure vim to use the operating system clipboard by setting `clipboard=unnamed` in your `.vimrc`. Unfortunately this setting does not work in OS X because the default version of vim was not compiled with clipboard support. You can check if your version of vim is compiled with clipboard support by typing `vim –version`  and looking for `clipboard`. A prepended `+` means support is enabled. A prepended `–` means support is disabled.

So, how do we get clipboard support? By using Macports to install a new version of vim using the `huge` flag of `port install`. I also compiled with support for python, ruby, and cscope.

``` bash
sudo port install vim huge python ruby cscope
```
