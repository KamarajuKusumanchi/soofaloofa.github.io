---
title: "Running Multiple App Engine Modules Locally with dev_appserver.py"
date: 2014-06-17T13:09:42Z
tags: 
  - "app engine"
  - "modules"
  - "dev_appserver"
---

The recently released [App Engine Modules API](https://developers.google.com/appengine/docs/python/modules/) allows developers to compartmentalize their applications into logical units that can share state using the datastore or memcache.

The documentation for this API is fairly complete but one part is lacking — running multiple modules locally using dev_appserver.py. Thankfully, the solution is not too complicated.  Just pass the list of `.yaml` files defining your modules to dev_appserver and it will run all of your modules locally. 

```bash
dev_appserver.py src/app.yaml src/backend.yaml src/dispatch.yaml
```
