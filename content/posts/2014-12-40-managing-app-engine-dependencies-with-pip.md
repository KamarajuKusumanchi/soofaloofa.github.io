---
title: "Managing App Engine Dependencies Using pip"
date: 2014-12-30T20:35:48Z
tags: 
  - "app engine"
  - "pip"
  - "python"
---

One unfortunate difficulty when working with App Engine is managing your local
dependencies. You don't have access to your Python environment so all libraries
you wish to use must be *vendored* with your installation. That is, you need to
copy all of your library code into a local folder to ship along with your app.

<!--more-->

This usually doesn't cause any problems but difficulties start to crop up when
you manage multiple dependencies that rely on each other. For example, the
official [elasticsearch
client](https://github.com/elasticsearch/elasticsearch-py) requires
[urllib3](https://github.com/shazow/urllib3) between version `1.8` and `2.0`.

Traditionally, [pip](https://pip.pypa.io/en/latest/) is used to install these
dependencies on your behalf. The command `pip install elasticsearch` will
automatically fetch the urllib3 dependency for you and install it to your local
Python environment. By adding the `-t` flag you can provide a destination folder
to install your libraries. As an example, we can install the elasticsearch
and urllib3 libraries to the folder `src/lib` with the following command.

```python
pip install elasticsearch -t src/lib
```

This works great for App Engine which requires the source of your libraries to
be shipped with your application. Unfortunately, it starts to break down when
you need to upgrade your dependencies. Installing with the `-t` flag does not
overwrite the contents of the existing folder so running the same command again
results in an error.

A solution to this can be found with some basic shell scripting. The first portion of our script installs the requested package and it's
dependencies to a temporary directory and removes any extra files that we don't
need.

```python
pip install elasticsearch -t $TEMP_DIRECTORY
rm -r $TEMP_DIRECTORY/*.egg-info >/dev/null 2>&1
rm -r $TEMP_DIRECTORY/*.dist-info >/dev/null 2>&1
```

The next step is to remove the specific libraries being installed from our App
Engine library directory and copy the contents of our temporary directory in their place.

```python
TARGET=src/lib
for i in $(ls $TEMP_DIRECTORY); do
  rm -r $TARGET/$i >/dev/null 2>&1  # remove existing module
  cp -R $TEMP_DIRECTORY/$i $TARGET  # copy the replacement in
done
```

This code can be used as a starting point to write a more user friendly and
robust script. Although this does not truly solve the problem of dependency
management with App Engine it does provide a way to seamlessly vendor Python
libraries and all of their dependencies with your application.
