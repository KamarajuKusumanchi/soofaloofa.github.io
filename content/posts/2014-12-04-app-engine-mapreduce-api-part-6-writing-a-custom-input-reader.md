---
title: "App Engine MapReduce API - Part 6: Writing a Custom Input Reader"
date: 2014-12-04T22:54:12Z
tags: 
  - "app engine"
  - "mapreduce"
  - "python"
series:
  - "MapReduce API"
---

## MapReduce API Series

* [Part 1: The Basics](http://sookocheff.com/posts/2014-04-15-app-engine-mapreduce-api-part-1-the-basics/)
* [Part 2: Running a MapReduce Job Using mapreduce.yaml](http://sookocheff.com/posts/2014-04-22-app-engine-mapreduce-api-part-2-running-a-mapreduce-job-using-mapreduceyaml/)
* [Part 3: Programmatic MapReduce using Pipelines](http://sookocheff.com/posts/2014-04-30-app-engine-mapreduce-api-part-3-programmatic-mapreduce-using-pipelines/)
* [Part 4: Combining Sequential MapReduce Jobs](http://sookocheff.com/posts/2014-05-13-app-engine-mapreduce-api-part-4-combining-sequential-mapreduce-jobs/)
* [Part 5: Using Combiners to Reduce Data Throughput](http://sookocheff.com/posts/2014-05-20-app-engine-mapreduce-api-part-5-using-combiners-to-reduce-data-throughput/)
* [Part 6: Writing a Custom Input Reader](http://sookocheff.com/posts/2014-12-04-app-engine-mapreduce-api-part-6-writing-a-custom-input-reader/)
* [Part 7: Writing a Custom Output Writer](http://sookocheff.com/posts/2014-12-20-app-engine-mapreduce-api-part-7-writing-a-custom-output-writer/)

One of the great things about the MapReduce library is the abilitiy to write a
cutom InputReader to process data from any data source. In this post we will
explore how to write an InputReader the leases tasks from an AppEngine pull
queue by implementing the `InputReader` interface.

<!--more-->

The interface we need to implement is available at
[`mapreduce.input_readers.InputReader`](https://github.com/GoogleCloudPlatform/appengine-mapreduce/blob/master/python/src/mapreduce/input_readers.py#L119).
Take a minute to examine the abstract methods that need to be implmemented.
Relevant portions of the source are copied below.

```python
class InputReader(json_util.JsonMixin):
  """Abstract base class for input readers.
  InputReaders have the following properties:
   * They are created by using the split_input method to generate a set of
     InputReaders from a MapperSpec.
   * They generate inputs to the mapper via the iterator interface.
   * After creation, they can be serialized and resumed using the JsonMixin
     interface.
  """

  def next(self):
    """Returns the next input from this input reader as a key, value pair.
    Returns:
      The next input from this input reader.
    """
    raise NotImplementedError("next() not implemented in %s" % self.__class__)

  @classmethod
  def from_json(cls, input_shard_state):
    """Creates an instance of the InputReader for the given input shard state.
    Args:
      input_shard_state: The InputReader state as a dict-like object.
    Returns:
      An instance of the InputReader configured using the values of json.
    """
    raise NotImplementedError("from_json() not implemented in %s" % cls)

  def to_json(self):
    """Returns an input shard state for the remaining inputs.
    Returns:
      A json-izable version of the remaining InputReader.
    """
    raise NotImplementedError("to_json() not implemented in %s" %
                              self.__class__)

  @classmethod
  def split_input(cls, mapper_spec):
    """Returns a list of input readers.
    This method creates a list of input readers, each for one shard.
    It attempts to split inputs among readers evenly.
    Args:
      mapper_spec: model.MapperSpec specifies the inputs and additional
        parameters to define the behavior of input readers.
    Returns:
      A list of InputReaders. None or [] when no input data can be found.
    """
    raise NotImplementedError("split_input() not implemented in %s" % cls)

  @classmethod
  def validate(cls, mapper_spec):
    """Validates mapper spec and all mapper parameters.
    Input reader parameters are expected to be passed as "input_reader"
    subdictionary in mapper_spec.params.
    Pre 1.6.4 API mixes input reader parameters with all other parameters. Thus
    to be compatible, input reader check mapper_spec.params as well and
    issue a warning if "input_reader" subdicationary is not present.
    Args:
      mapper_spec: The MapperSpec for this InputReader.
    Raises:
      BadReaderParamsError: required parameters are missing or invalid.
    """
    if mapper_spec.input_reader_class() != cls:
      raise BadReaderParamsError("Input reader class mismatch")
```

Let's fill out this interface with our InputReader that leases tasks from an
AppEngine pull queue. To start, we implement the `split_input` method that
instantiates a list of InputReaders, splitting the work among each reader. One
of the standard parameters for a MapReduce job is the number of shards you want
to use. For leasing tasks we will create one InputReader for shard
parameter.

```python
@classmethod
def split_input(cls, mapper_spec):
    """
    Returns a list of input readers
    """
    shard_count = mapper_spec.shard_count

    return [cls()] * shard_count
```

`split_input` is called to start our InputReader and returns a list of readers.
Each of these reader instances must implement a the `next` method which returns
a single value from our Reader. This method is part of the generator interface
and will be called during MapReduce operation. We can use `next` to attempt to lease
a single task from our queue, returning the task as a key-value tuple.

```python
def next(self):
    """
    Returns the queue, and a task leased from it as a tuple
    Returns:
      The next input from this input reader.
    """
    ctx = context.get()
    input_reader_params = ctx.mapreduce_spec.mapper.params.get('input_reader', {})
    queue_name = input_reader_params.get(self.QUEUE_PARAM)
    tag = input_reader_params.get(self.TAG_PARAM)
    lease_seconds = input_reader_params.get(self.LEASE_SECONDS_PARAM, 60)

    # Attempt to lease a task
    queue = taskqueue.Queue(queue_name)
    if tag:
        tasks = queue.lease_tasks_by_tag(lease_seconds, 1, tag=tag)
    else:
        tasks = queue.lease_tasks(lease_seconds, 1)

    if tasks:
        operation.counters.Increment(self.TASKS_LEASED_COUNTER)(ctx)
        return (queue, tasks[0])
    raise StopIteration()
```

We begin this function by reading in our parameters, using the context helper to
find the current parameters for this InputReder. We then attempt to lease a
task. If tasks are available to lease we return the task, otherwise we raise
`StopIteration` to halt the generator.

This basic implementation is all that's needed to write an InputReader -- split
our source into multiple shards and return a single `next` value from within
each shard. The MapReduce library will use this skeleton to call your `map`
function for each `next` value that is returned by the input reader.

To finish this up, we add some boilerplate required for serialization of reader
state and parameter validation. 

If your InputReader needs to hold any state between execution of the `next`
method you must serialize that state using the `to_json` and `from_json`
methods. `to_json` returns the current state of the reader in JSON format.
`from_json` creates an instance of an InputReader given a JSON format. Typically
we use this to save the constructor values used to create our InputReader. We'll
also need to formally define our constructor here.

The constructor takes only a few parameters. A queue name, a tag to lease tasks
with and the number of seconds to hold the lease.

```python
def __init__(self, queue_name='default', tag=None, lease_seconds=60):
    super(TaskInputReader, self).__init__()
    self.queue_name = queue_name
    self.tag = tag
    self.lease_seconds = lease_seconds
```

Now we can define how to serialize and deserialize the state of our reader.

```python
@classmethod
def from_json(cls, input_shard_state):
    """Creates an instance of the InputReader for the given input shard state.
    Args:
      input_shard_state: The InputReader state as a dict-like object.
    Returns:
      An instance of the InputReader configured using the values of json.
    """
    return cls(input_shard_state.get('queue_name'),
               input_shard_state.get('tag'),
               input_shard_state.get('lease_seconds')))

def to_json(self):
    """Returns an input shard state for the remaining inputs.
    Returns:
      A json-izable version of the remaining InputReader.
    """
    return {
        'queue_name': self.queue_name,
        'tag': self.tag,
        'lease_seconds': self.lease_seconds,
    }
```

The last method to implement is `validate`. This method parses the parameters
used to start your InputReader to make sure they are valid. In our example we
validate that the `queue_name` we are attempting to lease tasks from is valid
and that the number of seconds we wish to lease is an integer.

```python
@classmethod
def validate(cls, mapper_spec):
    """
    Validates mapper spec and all mapper parameters.
    Input reader parameters are expected to be passed as "input_reader"
    subdictionary in mapper_spec.params.
    Args:
      mapper_spec: The MapperSpec for this InputReader.
    Raises:
      BadReaderParamsError: required parameters are missing or invalid.
    """
    if mapper_spec.input_reader_class() != cls:
        raise BadReaderParamsError("Input reader class mismatch")

    # Check that a valid queue is specified
    input_reader_params = mapper_spec.params.get('input_reader', {})
    queue_name = input_reader_params.get('queue_name')
    lease_seconds = input_reader_params.get('lease_seconds', 60)
    if not queue_name:
        raise BadReaderParamsError('queue_name is required')
    if not isinstance(lease_seconds, int):
        raise BadReaderParamsError('lease_seconds must be an integer')
    try:
        queue = taskqueue.Queue(name=queue_name)
        queue.fetch_statistics()
    except Exception as e:
        raise BadReaderParamsError('queue_name is invalid', e.message)
```

Putting this all together we get our final InputReader. We can use this as a
basis to make more complex readers for additional data sources.

```python
"""
TaskInputReader
"""
from google.appengine.api import taskqueue

from mapreduce.input_readers import InputReader
from mapreduce.errors import BadReaderParamsError
from mapreduce import context
from mapreduce import operation


class TaskInputReader(InputReader):
    """
    Input reader for Pull-queue tasks
    """

    QUEUE_PARAM = 'queue'
    TAG_PARAM = 'tag'
    LEASE_SECONDS_PARAM = 'lease-seconds'

    TASKS_LEASED_COUNTER = 'tasks leased'

    def next(self):
        """
        Returns the queue, and a task leased from it as a tuple

        Returns:
          The next input from this input reader.
        """
        ctx = context.get()
        input_reader_params = ctx.mapreduce_spec.mapper.params.get('input_reader', {})
        queue_name = input_reader_params.get(self.QUEUE_PARAM)
        tag = input_reader_params.get(self.TAG_PARAM)
        lease_seconds = input_reader_params.get(self.LEASE_SECONDS_PARAM, 60)

        # Attempt to lease a task
        queue = taskqueue.Queue(queue_name)
        if tag:
            tasks = queue.lease_tasks_by_tag(lease_seconds, 1, tag=tag)
        else:
            tasks = queue.lease_tasks(lease_seconds, 1)

        if tasks:
            operation.counters.Increment(self.TASKS_LEASED_COUNTER)(ctx)
            return (queue, tasks[0])
        raise StopIteration()

    @classmethod
    def from_json(cls, input_shard_state):
        """Creates an instance of the InputReader for the given input shard state.

        Args:
          input_shard_state: The InputReader state as a dict-like object.

        Returns:
          An instance of the InputReader configured using the values of json.
        """
        return cls(input_shard_state.get(cls.QUEUE_NAME),
               input_shard_state.get(cls.TAG),
               input_shard_state.get(cls.LEASE_SECONDS)))

    def to_json(self):
        """Returns an input shard state for the remaining inputs.

        Returns:
          A json-izable version of the remaining InputReader.
        """
        return {
            'queue_name': self.queue_name,
            'tag': self.tag,
            'lease_seconds': self.lease_seconds,
        }

    @classmethod
    def split_input(cls, mapper_spec):
        """
        Returns a list of input readers
        """
        shard_count = mapper_spec.shard_count

        return [cls()] * shard_count

    @classmethod
    def validate(cls, mapper_spec):
        """
        Validates mapper spec and all mapper parameters.

        Input reader parameters are expected to be passed as "input_reader"
        subdictionary in mapper_spec.params.

        Args:
          mapper_spec: The MapperSpec for this InputReader.

        Raises:
          BadReaderParamsError: required parameters are missing or invalid.
        """
        if mapper_spec.input_reader_class() != cls:
            raise BadReaderParamsError("Input reader class mismatch")

        # Check that a valid queue is specified
        input_reader_params = mapper_spec.params.get('input_reader', {})
        queue_name = input_reader_params.get(cls.QUEUE_NAME)
        lease_seconds = input_reader_params.get(cls.LEASE_SECONDS, 60)
        if not queue_name:
            raise BadReaderParamsError('%s is required' % cls.QUEUE_NAME)
        if not isinstance(lease_seconds, int):
            raise BadReaderParamsError('%s must be an integer' % cls.LEASE_SECONDS)
        try:
            queue = taskqueue.Queue(name=queue_name)
            queue.fetch_statistics()
        except Exception as e:
            raise BadReaderParamsError('%s is invalid' % cls.QUEUE_NAME, e.message)
```
