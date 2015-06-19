---
title: "Bypassing ndb hooks with the RawDatastoreInputReader"
date: 2014-07-29T20:32:42Z
tags: 
  - "app engine"
  - "mapreduce"
  - "datastore"
  - "ndb"
aliases:
  - "posts/2014-07-29-bypassing-ndb-hooks-with-the-raw-datastore-input-reader/"
---

When doing a MapReduce operation there are times when you want to edit a set of
entities without triggering the post or pre put hooks associated with those
entities. On such ocassions using the raw datastore entity allows you to process
the data without unwanted side effects. This article will show how to use the
RawDatastoreInputReader to process datastore entities.

<!--more-->

When doing a MapReduce operation there are times when you want to edit a set of entities without triggering the post or pre put hooks associated with those entities. On such ocassions using the raw datastore entity allows you to process the data without unwanted side effects.

For the sake of this discussion let's assume we want to move a `phone_number` field to a `work_number` field for all entities of a certain Kind in the datastore.

## Getting the raw datastore entity

The MapReduce library provides a `RawDatastoreInputReader` that will feed raw datastore entities to your mapping function. We can set our MapReduce operation to use the `RawDatastoreInputReader` using a `mapreduce.yaml` declaration.

```
- name: move_phone_numbers
  mapper:
    input_reader: mapreduce.input_readers.RawDatastoreInputReader
    handler: app.pipelines.move_phone_numbers_map
    params:
    - name: entity_kind
      default: MyModel
```

## Manipulating a raw datastore entity

Our `raw_datastore_map` function to use the datastore entity in its raw form. The raw form of the datastore entity provides a dictionary like interface that we can use to manipulate the entity. With this interface we can move the phone number to the correct field.

```
def move_phone_numbers_map(entity):
    phone_number = entity.get('phone_number')
    if phone_number:
        entity['work_number'] = phone_number
    del entity['phone_number']
    
    yield op.db.Put(entity)
```

Using `op.db.Put` will put the entity to the datastore using the raw datastore
API, thereby bypassing any ndb hooks that are in place.  For more information on
the raw datastore API the best resource is the source code itself, available
from the [App Engine SDK
repository](https://code.google.com/p/googleappengine/source/browse/trunk/python/google/appengine/api/datastore.py).
