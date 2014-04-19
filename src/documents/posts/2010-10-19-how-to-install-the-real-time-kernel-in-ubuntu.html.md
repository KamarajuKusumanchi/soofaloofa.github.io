---
layout: post
title: How to install the real-time kernel in Ubuntu
author: Kevin Sookocheff
date: 2010/10/19 15:09:18
description: To use Ubuntu as an audio production server you need the real-time kernel. This allows applications to have some guarantee of the maximum response time of any task. High priority tasks are given the CPU within a fixed minimum amount of time allowing for processor intensive tasks to actually be completed. Follow along to see how to install the real-time kernel yourself. A vanilla install of most Linux distributions does not enable the real-time kernel, but it is easy enough to install.
tags:
  - ubuntu
  - real-time
---

To use Ubuntu as an audio production server you need the real-time kernel. This allows applications to have some guarantee of the maximum response time of any task. High priority tasks are given the CPU within a fixed minimum amount of time allowing for processor intensive tasks to actually be completed. Follow along to see how to install the real-time kernel yourself. A vanilla install of most Linux distributions does not enable the real-time kernel, but it is easy enough to install. Just type

    sudo apt-get install linux-rt

to install it. Now, we have to change the boot loader (GRUB) to load the real-time kernel. Edit the file

    sudo gedit /etc/default/grub

by commenting out the line

    GRUB_HIDDEN_TIMEOUT=0

then update the GRUB configuration.

    sudo update-grub

This will allow you to select which kernel to run at boot time so you can alternate between the real-time and vanilla kernels depending on your needs.
