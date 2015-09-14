---
title: "Beginning Docker"
date: 2015-09-14T20:26:21-06:00
tags: 
  - "docker"
---

I'm writing this article as a means of tracking commonly used docker commands in
a place where I won't forget them. If you find it useful or have additional
suggestions let me know in the comments.

<!--more-->

## Docker Machine

```bash
# create a new virtual machine using virtualbox, name it default
> docker-machine create --driver virtualbox default

# list available VMs
> docker-machine ls 

# list available VMs
> docker-machine ls 

# list environment variables needed connect to default VM
> docker-machine env default

# source environment variables into current shell session
> eval $(docker-machine env default)

# SSH into the VM
> docker-machine ssh default

# check a machine's status
> docker-machine status default

# start a machine
> docker-machine start default

# stop a machine
> docker-machine stop default

# remove a machine
> docker-machine rm default
```

## Docker

```bash
# create a container
> docker create redis

# start a container
> docker start d8c20e1b7e98

# create and start a container
> docker run d8c20e1b7e98

# pause a container (it won't be scheduled to execute tasks)
> docker pause d8c20e1b7e98

# unpause a container (resume scheduling)
> docker unpause d8c20e1b7e98

# set the port mappings between container and host
> docker run -p 6379:6379 redis

# auto-restart a container
> docker run --restart=always redis

# auto-restart up to three times on failure
> docker run --restart=on-failure:3 redis

# list all running containers
> docker ps

# list *all* containers
> docker ps -a

# run a command (ex: bash)
> docker run redis bash

# run an interactive container (t=>connect TTY, i=>interactive session)
> docker run -ti redis bash

# inspect a container
> docker inspect d8c20e1b7e98

# delete a container
> docker rm d8c20e1b7e98

# delete *all* containers
> docker rm $(docker ps -a -q)

# list all images
> docker images

# delete an image
> docker rmi redis:latest

# delete all images
> docker rmi $(docker images -q)
```
