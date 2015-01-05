---
title: "Creating a new file or directory in Vim using NERDTree"
date: 2013-04-19T12:18:19Z
tags:
  - "vim"
  - "nerdtree"
---

I’m not sure why this was so difficult to discover. For the longest time I was not sure how to create a new file using NERDTree. I finally sat down and figure out how it works.

<!--more-->

First, bring up NERDTree and navigate to the directory where you want to create the new file. Press `m` to bring up the NERDTree Filesystem Menu. This menu allows you to create, rename, and delete files and directories. Type `a` to add a child node and then simply enter the filename. You’re done! To create a directory follow the same steps but append a `/` to the filename.
