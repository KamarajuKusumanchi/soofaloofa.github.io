---
title: "App Engine MapReduce API - Part 7: Writing a Custom Output Writer"
date: 2014-12-22T07:07:35Z
tags: 
  - "app engine"
  - "mapreduce"
  - "python"
  - "elasticsearch"
series:
  - "MapReduce API"
aliases:
  - "posts/2014-12-20-app-engine-mapreduce-api-part-7-writing-a-custom-output-writer/"
---

## MapReduce API Series

* [Part 1: The Basics](http://sookocheff.com/posts/2014-04-15-app-engine-mapreduce-api-part-1-the-basics/)
* [Part 2: Running a MapReduce Job Using mapreduce.yaml](http://sookocheff.com/posts/2014-04-22-app-engine-mapreduce-api-part-2-running-a-mapreduce-job-using-mapreduceyaml/)
* [Part 3: Programmatic MapReduce using Pipelines](http://sookocheff.com/posts/2014-04-30-app-engine-mapreduce-api-part-3-programmatic-mapreduce-using-pipelines/)
* [Part 4: Combining Sequential MapReduce Jobs](http://sookocheff.com/posts/2014-05-13-app-engine-mapreduce-api-part-4-combining-sequential-mapreduce-jobs/)
* [Part 5: Using Combiners to Reduce Data Throughput](http://sookocheff.com/posts/2014-05-20-app-engine-mapreduce-api-part-5-using-combiners-to-reduce-data-throughput/)
* [Part 6: Writing a Custom Input Reader](http://sookocheff.com/posts/2014-12-04-app-engine-mapreduce-api-part-6-writing-a-custom-input-reader/)
* [Part 7: Writing a Custom Output Writer](http://sookocheff.com/posts/2014-12-20-app-engine-mapreduce-api-part-7-writing-a-custom-output-writer/)

The MapReduce library supports a number of default output writers. You can also
write your own that implements the output writer interface. This article
examines how to write a custom output writer that pushes data from the App
Engine datastore to an elasticsearch cluster. A similar pattern can be followed
to push the output from your MapReduce job to any number of places. 

<!--more-->

An output writer must implement the abstract interface defined by the MapReduce
library. You can find the interface
[here](https://github.com/GoogleCloudPlatform/appengine-mapreduce/blob/a1844a2652d51c3bef4448c9265c7c5790c9e476/python/src/mapreduce/output_writers.py#L95).
It may be a good idea to keep a reference to that interface available while
reading this article.

The most important methods of the interface are `create` and `write`.  `create`
is used to create a new OutputWriter that will handle writing for a single
shard. Our elasiticsearch OutputWriter takes parameters specifying the
elasticsearch index to write to and the document type. We take advantage of a
helper function provided by the library (`_get_params`) to get the parameters of
a MapReduce job given the MapReduce specification.

```python
from mapreduce.output_writers import OutputWriter, _get_params

class ElasticSearchOutputWriter(OutputWriter):

    def __init__(self, default_index_name=None, default_doc_type=None):
        super(ElasticSearchOutputWriter, self).__init__()
        self.default_index_name = default_index_name
        self.default_doc_type = default_doc_type
        
    @classmethod
    def create(cls, mr_spec, shard_number, shard_attempt, _writer_state=None):
        params = _get_params(mr_spec)
        return cls(default_index_name=params.get('default_index_name',
                   default_doc_type=params.get('default_doc_type'))
```

Now that we can create an instance of our OutputWriter we can implement the
`write` method to write data to elasticsearch. We use a MutationPool for this
(the MutationPool itself will be discussed shortly). The MutationPool is
attached to the current execution context of this MapReduce job. Every MapReduce
job has it's own persistent context that can store information required for the
current execution of the job. This allows multiple OutputWriter shards to write
into the MutationPool and have the MutationPool write data out to its final
destination. 

In this piece of code we check if we have a MutationPool associated with our
context and create a new MutationPool if we don't.  Once we've retrieved or
created the MutationPool we add the output operation to the pool.

```python
from mapreduce import context

def write(self, data):
   ctx = context.get()
   es_pool = ctx.get_pool('elasticsearch_pool')
   if not es_pool:
       es_pool = _ElasticSearchPool(ctx=ctx,
                                    default_index_name=default_index_name,
                                    default_doc_type=default_doc_type)
       ctx.register_pool('elasticsearch_pool', es_pool)

   es_pool.append(data)
```

These two methods provide the basis of our OutputWriter, implementing the
`to_json`, `from_json` and `finalize` methods is left up to the reader.
`finalize` does not need any functionality but you may want to log a message
upon completion.

Now on to the MutationPool. The MutationPool acts as a buffered writer of data
changes. It acts as an abstraction that collects any sequence of operations that
are to be performed together. After `x` number of operations have been collected
we operate on them all at once.  Mutation pools are strictly a performance
improvement but they can quickly become essential when processing large amounts
of data. For example, rather than writing to the datastore after each map
operation with `ndb.put` we can collect a sequence of writes and put them all at
once with `ndb.put_multi`. 

For an `elasticsearch` OutputWriter our mutation pool will collect and buffer
indexing tasks and perform them all during a single [streaming
bulk](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/bulk.html)
operation. Within our OutputWriter we collect our sequence of operations in a
private list variable `_actions`.

```python
class _ElasticSearchPool(context.Pool):
    def __init__(self, ctx=None, default_index_name=None, default_doc_type=None):
        self._actions = []
        self._size = 0
        self._ctx = ctx
        self.default_index_name = default_index_name
        self.default_doc_type = default_doc_type
```

We then implement the `append` method to add an action to the current
MutationPool. In this example we simply add the action to our list. If our list
is greater than `200` elements we flush our MutationPool.

```python
def append(self, action):
    self._actions.append(action)
    self._size += 1
    if self._size > 200:
        self.flush()
 ```
 
Finally, to flush the MutationPool we write all the data collected so far to
elasticsearch and clear our list of actions.
 
```python
def flush(self):
   es_client = elasticsearch(hosts=["127.0.0.1"])  # instantiate elasticsearch client
   if self._actions:
       results = helpers.streaming_bulk(es_client,
                                                                   self._actions,
                                                                   chunk_size=200)
    self._actions = []
    self._size = 0
```
                                                  
Now, as long as the map function of our MapReduce job outputs operations in a
format recognizeable by elasticsearch the OutputWriter will collect those
operations into a MutationPool and periodically flush the results to our
elasticsearch cluster.

You can use this code as the basis for writing OutputWriters for almost any
custom destination.
