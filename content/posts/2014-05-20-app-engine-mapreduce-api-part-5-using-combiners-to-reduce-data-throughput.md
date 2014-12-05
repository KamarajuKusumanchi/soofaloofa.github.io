---
title: "App Engine MapReduce API - Part 5: Using Combiners to Reduce Data Throughput"
date: 2014-05-20T08:54:12Z
description: "In this post we will look at how to reduce the amount of data transfer during a MapReduce job using a combiner."
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

So far we've looked at using MapReduce pipelines to perform calculations over
large data sets and combined multiple pipelines in succession. In this article
we will look at how to reduce the amount of data transfer by using a combiner.

##  What is a combiner?

A combiner is a function that takes the output of a series of map calls as input and outputs a value of the same format to be processed by the reducer. The combiner is run just before the output of the mapper is written to disk. In fact, the combiner may not be run at all if the data can reside completely in memory and so your algorithm must be able to complete with our without the combiner. By reducing the amount of data that needs to be written to disk you can increase performance of the reduce stage. 

## Example

Let's look at an example that uses a combiner to reduce data throughput. To drive this discussion we will use an example that counts the number of occurrences of a character in a string. We originally looked at this example [here](http://sookocheff.com/posts/2014-04-30-app-engine-mapreduce-api-part-3-programmatic-mapreduce-using-pipelines/). In this version we will only include the character or characters that occur the most. The operation will work like this: the mapper function will count the occurrence of each character in a string. The combiner will take these (key, value) pairs and output only the character or characters that appear the most. Finally, the reducer will sum those values to find our result. This contrived problem will provide a working example of a combiner.

Let's start with the MapReduce job from our previous example.

```
"""
app.pipelines
"""
import collections

from mapreduce.lib import pipeline
from mapreduce import mapreduce_pipeline

###
### MapReduce Pipeline
###
def character_count_map(random_string):
    """ yield the number of occurrences of each character in random_string. """
    counter = collections.Counter(random_string)
    for character in counter.elements():
        yield (character, counter[character])

def character_count_reduce(key, values):
    """ sum the number of characters found for the key. """
    yield (key, sum([int(i) for i in values]))

class CountCharactersPipeline(pipeline.Pipeline):
    """ Count the number of occurrences of a character in a set of strings. """

    def run(self, *args, **kwargs):
        """ run """
        mapper_params = {
            "count": 100,
            "string_length": 20,
        }
        reducer_params = {
            "mime_type": "text/plain"
        }
        output = yield mapreduce_pipeline.MapreducePipeline(
            "character_count",
            mapper_spec="app.pipelines.character_count_map",
            mapper_params=mapper_params,
            reducer_spec="app.pipelines.character_count_reduce",
            reducer_params=reducer_params,
            input_reader_spec="mapreduce.input_readers.RandomStringInputReader",
            output_writer_spec="mapreduce.output_writers.BlobstoreOutputWriter",
            shards=16)
```

Given this base we add a combiner step to the `MapreducePipeline` by passing the `combiner_spec` argument to the initialization.

```
       output = yield mapreduce_pipeline.MapreducePipeline(
            "character_count",
            mapper_spec="app.pipelines.character_count_map",
            mapper_params=mapper_params,
            reducer_spec="app.pipelines.character_count_reduce",
            reducer_params=reducer_params,
            combiner_spec="app.pipelines.character_count_combine",
            input_reader_spec="mapreduce.input_readers.RandomStringInputReader",
            output_writer_spec="mapreduce.output_writers.BlobstoreOutputWriter",
            shards=16)
```

Our combine function accepts a few parameters the key, a list of values for that key and a list of previously combined results. The combiner function yields combined values that might be processed by another combiner call and that will eventually end up in the reducer function.

Let's write our simple combiner function. We yield only a value instead of a `(key, value)` tuple because the key is assumed to stay the same.

```
def character_count_combine(key, values, previously_combined_values):
    """ emit the maximum value in values and previously_combined_values """
    yield max(values + previously_combined_values)
```

Our combiner function is not guaranteed to run so we need to update our reduce function to take the maximum of the list of values as well.

```
def character_count_reduce(key, values):
    """ sum the number of characters found for the key. """
    yield (key, max(values))
```

This gives us our final pipeline using map, reduce and combine.

```
###
### MapReduce Pipeline
###
def character_count_map(random_string):
    """ yield the number of occurrences of each character in random_string. """
    counter = collections.Counter(random_string)
    for character in counter.elements():
        yield (character, counter[character])

def character_count_reduce(key, values):
    """ sum the number of characters found for the key. """
    yield (key, max(values))

def character_count_combine(key, values, previously_combined_values):
    """ emit the maximum value in values and previously_combined_values """
    yield max(values + previously_combined_values)

class CountCharactersPipeline(pipeline.Pipeline):
    """ Count the number of occurrences of a character in a set of strings. """

    def run(self, *args, **kwargs):
        """ run """
        mapper_params = {
            "count": 100,
            "string_length": 20,
        }
        reducer_params = {
            "mime_type": "text/plain"
        }
        output = yield mapreduce_pipeline.MapreducePipeline(
            "character_count",
            mapper_spec="app.pipelines.character_count_map",
            mapper_params=mapper_params,
            reducer_spec="app.pipelines.character_count_reduce",
            reducer_params=reducer_params,
            combiner_spec="app.pipelines.character_count_combine",
            input_reader_spec="mapreduce.input_readers.RandomStringInputReader",
            output_writer_spec="mapreduce.output_writers.BlobstoreOutputWriter",
            shards=16)
```


