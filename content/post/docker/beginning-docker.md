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
# create a new virtual machine
> docker-machine create

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

# view a machines IP address
> docker-machine ip default

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
# check the docker version
> docker version

# check server information
> docker info

# create a container
> docker create redis

# start a container
> docker start d8c20e1b7e98

# create and start a container
> docker run redis

# create and start a container
> docker run 91e54dfb1179  # give an image id

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

# start a container and run a command
> docker exec -t -i d8c20e1b7e98 /bin/bash

# start a container and run a command in background mode
> docker exec -t -i -d d8c20e1b7e98 /bin/bash

# update a container
> docker pull redis:latest

# run a command in a running container
> docker exec -t -i d8c20e1b7e98 /bin/bash

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

# look at container logs (stdout and stderr)
> docker logs d8c20e1b7e98

# block for more log output
> docker logs -f d8c20e1b7e98

# view stats
> docker stats 7736a32c6e41

# view events
> docker events

# view events after a time
> docker events --since 2015-02-18T14:03:31-08:00

# view events before a time
> docker events --until 2015-02-18T14:03:31-08:00

```
