---
title: "Introducing CloudPyPI" 
date: 2015-06-16T21:19:26-06:00
tags: 
  - "python"
  - "pypi"
  - "cloudpypi"
  - "pip"
---

A common problem with Python development for large-scale teams is sharing
internal libraries. At Vendasta we've been solving this problem using a private
PyPI installation running on Google App Engine with Python eggs and wheels being
served by Google Cloud Storage. Today, we are announcing the open source version
of this tool — CloudPyPI.

CloudPyPI is a modification of
[pypiserver](https://pypi.python.org/pypi/pypiserver) for running on Google App
Engine. We've also introduced a simple user management system to allow
authenticated access to your Python packages. Together, we've found this to be a
robust tool for distributing private Python libraries internally. If this is a
problem you've been trying to solve, give CloudPyPI a try — contributions and
feature requests are always welcome.

<a class="btn btn-default" href="https://github.com/vendasta/cloudpypi" role="button">Check out CloudPyPI on Github</a>
