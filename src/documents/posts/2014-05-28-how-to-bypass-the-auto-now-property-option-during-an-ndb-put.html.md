---
layout: post
standalone: true
title: "How to bypass the auto_now property option during an ndb put"
author: Kevin Sookocheff
date: 2014/05/28 05:55:48
tags: 
  - app engine
  - ndb
  - auto_now
---

In App Engine the `auto_now` option sets a property to the current date/time
whenever the entity is created or updated. This is a great feature for tracking
the time when an entity was last updated. However, sometimes you may want to put
an entity without updating an `auto_now` timestamp. This article will show you
how.

First, let's start with a very basic ndb model with an `updated` property having
the `auto_now` option set to `True`.

```python
from google.appengine.ext import ndb

class Article(ndb.model):
    title = ndb.model.StringProperty()
    updated = ndb.model.DateTimeProperty(auto_now=True)
```

Now, let's put the entity to the datastore *without updating the timestamp* and
*completely bypassing the `auto_now` option*.

```python
article = Article(title='Python versus Ruby')
article._properties['updated']._auto_now = False
article.put()
```

It's pretty simple, but with caveats. Putting the entity using the code above
will store the updated entity in the instance cache (and memcache). If we get
the entity it will be retrieved from the instance cache with the `auto_now`
property still set to `False`. This can have unwanted side-effects because
subsequent updates to the entity will not trigger the `auto_now` functionality.

```python
article = Article(title='Python versus Ruby')
article._properties['updated']._auto_now = False
key = article.put() # Put the entity with the auto_now option set to False

article = key.get() # Get the entity from instance cache
article.title = 'Python versus Go'
article.put() # Put the entity with the auto_now option *still* set to False
```

You can set the `auto_now` option to `True` again to re-enable the functionality.

```python
article = Article(title='Python versus Ruby')
article._properties['updated']._auto_now = False
key = article.put()

article._properties['updated']._auto_now = True
article = key.get() # Get the entity from instance cache
article.title = 'Python versus Go'
article.put() # Puts the entity with the auto_now option set to True
```

For more information on ndb caching [refer to the
documentation](https://developers.google.com/appengine/docs/python/ndb/cache).
