---
title: "A Guided Tour of Google Compute Engine"
date: 2014-11-05T04:45:05Z
draft: True
description: "This article will provide a broad overview of the Compute Engine
ecosystem. Compute Engine is defined by Resources and each of these Resources
are explained throughout this article."
tags: 
  - "compute engine"
---

## Overview

To start, take a few minutes to watch this video overview of
[Compute Engine core concepts](https://www.youtube.com/watch?v=43gvHZyPRVk). As
this video shows, Compute Engine is defined by Resources, and each of these
Resources is available through the Compute Engine API. You can access this API
through the Google API Explorer or by installing the
[https://cloud.google.com/sdk/](gcloud sdk).

## Key Resources

There are few basic resources required to get up and running with any virtual
machine running on Compute Engine. These key resources are Images, Instances and
Disks.

{{% img 2014-11-05-a-guided-tour-of-compute-engine/ImagesInstancesDisks.png "Images, Instances and Disks" %}}

### Images

An Image contains a boot loader, an operating system and a root file system that
is necessary for starting up an instance.  For all intents and purposes, the
Image is the OS that you are running.

### Disks

The disk acts as a filesystem. They operate as physical disks attached to your
virtual machine but are more analogous to a network attached file system. As an
example, we can separate a disk from a virtual machine and reattach it to
another virtual machine.

### Instances

An instance encapsulates the concept of a virtual machine. The virtual machine
runs the OS and root filesystem defined by the image you choose at create time. 

## Instance Groups

Given the Key Resources we should have an understanding of a single virtual
machine as viewed through the lens of the Compute Engine ecosystem. 

A single virtual machine is great, but modern web services usually require
multiple machines working in concert to provide a scalable and fault tolerant
system. Compute Engine offers the abstraction of `Instance Groups` to represent
a group of virtual machines.

Instance groups allow you to collectively manage a set of virtual machines and
attach services to that group of machines. A service is a simple label attached
to the group that advertises this group of virtual machines as providing the
same backend web service.

{{% img 2014-11-05-a-guided-tour-of-compute-engine/InstanceGroup.png "Instance Groups" %}}

## Instance Group Manager

Instance Groups operate over a set of virtual machines. To add a virtual machine
to an Instance Group you first spin up your virtual machine and then call the
Instance Group API to tell it Compute Engine that this instance is now part of
that group. 

The Instance Group Manager automates how virtual machines are added to the
Instance Group. There are a few caveats. First, instances in a managed Instance
Group must be homogenous.  That means they must all be created using the same
base image, disk and machine type. To facilitate this homogeneity the Instance
Group Manager operates over an Instance abstraction called an Instance Template.

### Instance Templates

Instance templates are configuration files that define the settings of an
Instance (and by extension of an Image and Disk). Instance templates can define
some or all of the configuration options. For example, the OS, the RAM, or the
hard drive size. Instance templates separate the configuration of your virtual
machine from the creation and running of the virtual machine. 

When creating a new managed Instance Group using the Instance Group Manager you
specify the Instance Template to use for the Group. You also tell the Instance
Group Manager the size of your instance group the Manager will create those
instances and assign them to your managed group. You can then resize your group
with one command and those instances will be brought to life and join your
instance group. If any instances are destroyed or deleted the manager will bring
them back to life for you.

{{% img 2014-11-05-a-guided-tour-of-compute-engine/InstanceGroupManager.png "Instance Group Manager." %}}

## Regions & Zones

Compute Engine allows you to specify the region and zone where your virtual
machine exists. This gives you control over where your data is stored and used.

Regions are broad categories such as central US or eastern Asia. Zones are
subdivisions within a region such as zone `a` or zone `b`. The way to reference
a zone is as `<region>-<zone>`. For example `<us-central1-a>` has the region
us-central1 and the zone `a`.

To build a fault tolerant system ideally you want to have your instances spread
out over multiple zones. To distribute your system globally you can spread your
instances across multiple regions. In the following example, we have two
separate Managed Instance Groups in two different zones.

{{% img 2014-11-05-a-guided-tour-of-compute-engine/RegionsAndZones.png "Instance Group Manager" %}}

## Load Balancing

If we've built a system along these lines, at this point we have a group of
virtual machines running the same hardware that can be easily resized with the
Instance Group Manager. The question now is how do we address these machines?
This is where a load balancer comes into play. There are a lot of moving parts
in the load balancer. Thankfully they all perform very particular tasks and
once you set it up you don't have to worry about it anymore.

### The Backend Service

To use HTTP load balancing we need to declare our instance groups as backend
services. This is a simple label attached to the group. In effect we are telling
Compute Engine which groups are related to one another.

{{% img 2014-11-05-a-guided-tour-of-compute-engine/BackendServices.png "Backend Services" %}}

### URL Maps

URL maps define what URL requests to send to what backend service. For example ,
we can route requests to the url `/static` to a different backend service
than requests to the url `/`. In this example we have a URL Map that routes
requests with the pattern `/static` and `/images` to the `static` Backend
Service. Any other requests are routed to the `web` service.

{{% img 2014-11-05-a-guided-tour-of-compute-engine/UrlMap.png "Url Map" %}}

### Target HTTP Proxy

The Target HTTP Proxy is a simple proxy that receives requests and routes them
to the URL Maps. 

{{% img 2014-11-05-a-guided-tour-of-compute-engine/TargetHttpProxy.png "Target HTTP Proxy" %}}

### Global Forwarding Rule

The global forwarding rule provides an external IP address that we can use to
address our instances. This IP address routes to the target HTTP proxy which
ultimately directs our traffic to the proper Backend Service through the URL Map.

{{% img 2014-11-05-a-guided-tour-of-compute-engine/GlobalForwardingRule.png "Global Forwarding Rule" %}}

## Conclusion

That completes our guided tour of Compute Engine. Given this foundation you
should have a solid base with which to navigate the documentation and explore
the API.
