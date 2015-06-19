---
date: 2015-06-18T20:33:37-06:00
title: Managed VMs and the Future of App Engine
tags: 
  - "app engine"
  - "managed vms"
---

I've been thinking about the transition of App Engine to Python 3 and have come
to the conclusion that it will never happen — App Engine will eventually be
deprecated in favour of Managed VMs. Let's break this apart to see why this is.

First, consider the effort required by Google to develop App Engine. The Python
runtime environment was modified to enforce the sandbox of the App
Engine environment. To provide a Python 3 environment for App Engine as we know
it, the Python 3 runtime would need to be modified with the same restrictions.
Even imagining that this would happen for Python 3.4, the effort to upgrade to
Python 3.5 would require additional effort by Google to modify the runtime.

Considering the desire to run additional languages such as Javascript (via
NodeJS) or Ruby, Google is put in an untenable position if it expects to
support modified runtimes for multiple versions of different languages. 

Now let's consider the rise of Docker and the development of Managed VMs.
Managed VMs are built on top of Docker and provide a 'dockerized' platform
hosting your language runtime. Managed VMs provide the auto-scaling,
health-check, colocation and server upgrades that are the hallmark of App Engine while
allowing the use of arbitrary runtimes within the Docker sandbox.

This diagram from the [Managed VMs documentation](https://cloud.google.com/appengine/docs/managed-vms/) 
shows the difference between the heavily modified App Engine sandbox and the
more traditional environment running within a Docker container as a Managed VM.

{{% img_url "https://cloud.google.com/appengine/docs/managed-vms/" "https://cloud.google.com/appengine/images/vmhosting.png" "Managed VM Sandbox" %}}

The great thing about Managed VMs is that they still allow access to the
traditional App Engine APIs such as Datastore, Memcached, Logging, Task Queues
and Search. This allows you to write applications in a fashion similar to your
existing App Engine projects in a Managed VM environment. You can also use the
Modules API to have a portion of your application be served by requests routed
to the Managed VM and other requests routed to your App Engine application.

Managed VMs also allow better insight into product costs. The VMs are deployed
as Compute Engine instances, giving you the ability to monitor CPU and network
usage and upgrade and downgrade your instances as you see fit.

My feeling is that as the APIs to access Google Cloud Platform are extended and
enhanced to work outside of the current App Engine sandbox, Managed VMs will
take over Google's development efforts to the point where App Engine as we know
it is replaced with Managed VMs. The next project I develop will be using
Managed VMs from the start.
