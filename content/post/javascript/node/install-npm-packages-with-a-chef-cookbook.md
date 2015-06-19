---
title: "Install node.js packages via npm with a Chef Cookbook"
date: 2013-06-06T08:45:51Z
tags: 
  - "npm"
  - "node"
  - "chef"
aliases:
  - "posts/2013-06-04-install-npm-packages-with-a-chef-cookbook/"
---

I wanted to set up a Vagrant instance with node.js and some specific packages pre-installed. I found a Chef cookbook to install node and after a bit of work have a cookbook that will install arbitrary node packages through npm.  I based this heavily on [balbeko][2]'s chef-npm cookbook with modifications to accept a data bag list of npm packages. 

<!--more-->

Any packages in this list will be installed by Chef.

 [1]: http://www.opscode.com/chef/
 [2]: https://github.com/balbeko/chef-npm

Behold! My first [Chef cookbook][3].

 [3]: https://github.com/soofaloofa/chef-npm-package-install
