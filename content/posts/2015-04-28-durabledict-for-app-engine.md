---
title: "Durabledict for App Engine" 
date: 2015-04-29T06:19:23-06:00
tags: 
  - "durabledict"
  - "app engine"
  - "ndb"
---

## tldr;

[DatastoreDict](https://github.com/soofaloofa/datastoredict).

## What's a durabledict?

Good question. [Durabledict](https://github.com/disqus/durabledict) is a Python
implementation of a persistent dictionary. The dictionary values are cached
locally and sync with the datastore whenever a value in the datastore changes.

Disqus provides concrete implementations for Redis, Django, ZooKeeper and in
memory. This blog post details an implementation using the App Engine datastore
and memcache.

## Creating your own durabledict

By following the [guide the durabledict
README](https://github.com/disqus/durabledict) we can create our own
implementation. We need to subclass `durabledict.base.DurableDict` and implement
the following interface methods. Strictly speaking, `_pop` and `_setdefault` do
not have to be implemented but doing so makes your durabledict behave like a
base dict in all cases.

`persist(key, value)` - Persist value at key to your data store.

`depersist(key)` - Delete the value at key from your data store.

`durables()` - Return a key=val dict of all keys in your data store.

`last_updated()` - A comparable value of when the data in your data store was last updated.

`_pop(key, default=None)` - If key is in the dictionary, remove it and return its value, else return default. If default is not given and key is not in the dictionary, a KeyError is raised.

`_setdefault(key, default=None)` - If key is in the dictionary, return its value. If not, insert key with a value of default and return default. default defaults to None.

Let's implement these one-by-one.


### persist(key, value)

Persisting a value to the datastore is a relatively simple operation. If the key
already exists we update it's value. If the key does not already exist we create
it. To aid with this operation we create a `get_or_create` method that will
return an existing entity if one exists or create a new entity if one does not
exist.

```python
def persist(self, key, val):
    instance, created = get_or_create(self.model, key, val)

    if not created and instance.value != val:
        instance.value = val
        instance.put()

    self.touch_last_updated()
```

The last line of this function updates the last time this durabledict was
changed. This is used for caching. We create the `last_updated` and
`touch_last_updated` functions now.

### last_updated(key, value)

```python
def last_updated(self):
    return self.cache.get(self.cache_key)

def touch_last_updated(self):
    self.cache.incr(self.cache_key, initial_value=self.last_synced + 1)
```


### __init__

We now have the building blocks to create our initial durabledict. Within the
`__init__` method we set a manager and cache instance. The manager is
responsible for ndb datastore operations to decouple the ndb interface from the
durabledict implementation. We decouple our caching method in a similar fashion.
We also set the initial value of the cache whenever we create a new instance of
the durabledict.

```python
from google.appengine.api import memcache

from durabledict.base import DurableDict
from durabledict.encoding import NoOpEncoding


class DatastoreDict(DurableDict):

    def __init__(self,
                 model,
                 value_col='value',
                 cache=memcache,
                 cache_key='__DatastoreDict:LastUpdated__'):

        self.model = model
        self.value_col = value_col
        self.cache = cache
        self.cache_key = cache_key

        self.cache.add(self.cache_key, 1)

        super(DatastoreDict, self).__init__(encoding=NoOpEncoding)
```

### depersist(key)

Depersist implies deleting a key from the dictionary (and datastore). Here we
assume a helper method `delete` that, given an ndb model and a string
representing it's key deletes the model. Since the data has changed we also
update the last touched value to force a cache invalidation and data refresh.

```python
def depersist(self, key):
    delete(self.model, key)
    self.touch_last_updated()
```

### durables()

`durables()` returns the entire dictionary. Since we are all matching entities
from the datastore it is important to keep your dictionary relatively small --
as the dictionary grows in size, resyncing it's state with the datastore will
get more and more expensive. This function assumes a `get_all` method that will
return all instances of a model.

```python
def durables(self):
    encoded_models = get_all(self.model)
    return dict((model.key.id(), getattr(model, self.value_col)) for model in encoded_models)
```

### setdefault(key, default=None)

`_setdefault()` overrides the dictionary built-in `setdefault` which allows you
to insert a key into the dictionary, creating the key with the default value if
it does not exist and returning the existing value if it does exist.

For example, the following sequence of code creates a key for `y`, which does not
exist, and returns the existing value for `x`.

```python
>>> d = {'x': 1}
>>> d.setdefault('y', 2)
2
>>> d
{'y': 2, 'x': 1}
>>> d.setdefault('x', 3)
1
>>> d
{'y': 2, 'x': 1}
```

We can implement `_setdefault` using the `get_or_create` helper method, updating
the cache if we have changed the dictionary.

```python
def _setdefault(self, key, default=None):
    instance, created = get_or_create(self.model, key, default)

    if created:
        self.touch_last_updated()

    return getattr(instance, self.value_col)
```

### pop(key, default=None)

pop returns the value for a key and deletes the key. This is fairly straight
forward given a `get` and `delete` helper method.

```python
def _pop(self, key, default=None):
    instance = get(self.model, key)
    if instance:
        value = getattr(instance, self.value_col)
        delete(self.model, key)
        self.touch_last_updated()
        return value
    else:
        if default is not None:
            return default
        else:
            raise KeyError
```

### The Help

The previous discussion uses a few helper methods that we haven't defined yet.
Each of these methods takes an arbitrary ndb model and performs an operation on
it.

```python
def build_key(cls, key):
    return ndb.Key(DatastoreDictAncestorModel,
                   DatastoreDictAncestorModel.generate_key(cls).string_id(),
                   cls, key.lower(),
                   namespace='')


@ndb.transactional
def get_all(cls):
    return cls.query(
        ancestor=DatastoreDictAncestorModel.generate_key(cls)).fetch()


@ndb.transactional
def get(cls, key):
    return build_key(cls, key).get()


@ndb.transactional
def get_or_create(cls, key, value=None):
    key = build_key(cls, key)

    instance = key.get()
    if instance:
        return instance, False

    instance = cls(key=key, value=value)
    instance.put()

    return instance, True


@ndb.transactional
def delete(cls, key):
    key = build_key(cls, key)
    return key.delete()
```

The last item of note is the use of a parent for each DatastoreDict. This common
ancestor forces strong read consistency for the `get_all` method, allowing us to
update a dictionary and have a consistent view of the data on subsequent reads.
We use an additional model to provide the strong read consistency.

```python
class DatastoreDictAncestorModel(ndb.Model):

    @classmethod
    def generate_key(cls, child_cls):
        key_name = '__%s-%s__' % ('ancestor', child_cls.__name__)
        return ndb.Key(cls, key_name, namespace='')
```
