---
layout: post
title: "App Engine MapReduce API - Part 1: The Basics"
author: Kevin Sookocheff
date: 2014/04/15 12:09:36
updated: 2014/04/22 06:48:36
description: <t render="markdown">This article provides an overview of the [App Engine MapReduce API](https://developers.google.com/appengine/docs/python/dataprocessing/). We will give a basic overview of what MapReduce is and how it is used to do parallel and distributed processing of large datasets.</t>
tags: 
  - App Engine
  - MapReduce
  - Python
---

## MapReduce API Series

* [Part 1: The Basics](http://sookocheff.com/posts/2014-04-15-app-engine-mapreduce-api-part-1-the-basics/)
* [Part 2: Running a MapReduce Job Using mapreduce.yaml](http://sookocheff.com/posts/2014-04-22-app-engine-mapreduce-api-part-2-running-a-mapreduce-job-using-mapreduceyaml/)

The first arcticle in this series provides an overview of the [App Engine MapReduce
API](https://developers.google.com/appengine/docs/python/dataprocessing/). We
will give a basic overview of what MapReduce is and how it is used to do
parallel and distributed processing of large datasets.

## The Map and Reduce Functions

MapReduce is based on the `map` and `reduce` functions that are commonly used in
lazily-evaluated functional programming languages. Let's look at `map` first.

### map

A `map` function is a way to apply a transformation to every element in a list.
Using Clojure as the example functional language we can use the `map` function
to increment every number in a list by `1`.

```
=> (map inc [1 2 3 4 5])
(2 3 4 5 6)
```

In this example `inc` is the increment function where `inc(x) = x+1`. More
generally, you can apply any function `fn` to all elements of a list by passing
it to the map function.

```
=> (map fn [1 2 3 4 5])
```

### reduce

Reduce applying a function `fn` of two arguments to a sequence of parameters.
Each iteration of the function call uses the value of the previous call as an
input parameter of the function. In this example we start with a base value of 0
and iteratively add to that base value to sum a list of numbers.

```
=> (reduce + 0 [1 2 3 4 5])
=> (reduce + 1 [2 3 4 5])
=> (reduce + 3 [3 4 5])
=> (reduce + 6 [4 5])
=> (reduce + 10 [5])
15
```

An interesting feature of both map and reduce is that they can be lazily
evaluated -- meaning that each operation can be performed only when it is
needed. With MapReduce, lazy evaluation allows you to work with large datasets
by processing data only when needed. 

## MapReduce Stages

The App Engine MapReduce API provides a method for operating over large datasets
via a parallel and distributed system of lazy evaluation. In contrast to the
`map` and `reduce` functions a MapReduce job may output a single value or a list
of values depending on the job requirements. 

A MapReduce job is made up of stages. Each stage completes before the next stage
begins and any intermediate data is stored in temporary storage between the
stages. MapReduce has three stages: map, shuffle and reduce.

### Map

The map stage has two components -- an *InputReader* and a *map* function. The
InputReader's job is to deliver data one record at a time to the *map* function.
The *map* function is applied to each record individually and a key-value pair
is emitted. The data emitted by the *map* function is stored in temporary
storage for processing by the next stage.

The prototypical MapReduce example counts the number of each words in a set of
documents. For example, assume the input is a document database containing a
document id and the text of that document.

```
14877 DIY Pinterest narwhal forage typewriter, quinoa Odd Future. Fap hashtag 
88390 chillwave, paleo post-ironic squid fanny pack yr PBR&B High Life. Put a bird on it
73205 gastropub leggings ennui PBR&B. Vice Pinterest 8-bit chambray. Dreamcatcher
95782 letterpress 3 wolf moon, mustache craft beer Pitchfork yr trust fund Tonx 77865 collie lassie
75093 Portland skateboard bespoke kitsch. Seitan irony mustache messenger bag,
24798 skateboard hashtag pickled tote bag try-hard meggings actually Vice quinoa
13334 plaid. Biodiesel Echo Park fashion axe direct trade, forage Neutra try-hard
```

Using the App Engine MapReduce API we can define a map function to output a
key-value pair for each occurrence of a word in the document.

```python
def map(document):
    """
	Count the occurrence of each word in a document.
    """
    for word in document:
    	yield (word.lower(), 1)
```

Our output would record each time a word was encountered within a document.

```
diy 1
pinterest 1
narwhal 1
forage 1
typewriter 1
quinoa 1
odd 1
future 1
... more records ...
pinterest 1
forage 1
quinoa 1
```

### Shuffle

The shuffle stage is done in two steps. First, the data emitted by the map stage
is sorted. Entries with the same key are grouped together. 

```
(diy, 1)
(forage, 1)
(forage, 1)
(future, 1)
(narwhal, 1)
(odd, 1)
(pinterest, 1)
(pinterest, 1)
(quinoa, 1)
(quinoa, 1)
(typewriter, 1)
```

Second, entries for each key are condensed into a single list of values. These
values are stored in temporary storage for processing by the next stage.

```
(diy, [1])
(forage, [1, 1])
(future, [1])
(narwhal, [1])
(odd, [1])
(pinterest, [1, 1])
(quinoa, [1, 1])
(typewriter, [1])
```

### Reduce

The reduce stage has two components -- a *reduce* function and an
*OutputWriter*. The reduce function is called for each unique key in the
shuffled temporary data. The *reduce* function emits a final value based on its
input. To count the number of occurrences of a word our reduce function will
look like this.

```python
def reduce(key, values):
   """
	Sum the list of values.
    """
    yield (key, sum(values))
```

Applying this reducing function to our data would give the following output.

```
(diy, 1)
(forage, 2)
(future, 1)
(narwhal, 1)
(odd, 1)
(pinterest, 2)
(quinoa, 2)
(typewriter, 1)
```

This output is passed to the *OutputWriter* which writes the data to permanent storage.

## The Benefits of MapReduce

MapReduce performs parallel and distributed operations by partitioning the data
to be processed both spatially and temporally. The spatial partitioning is done
via *sharding* while the temporal partitioning is done via *slicing*.

### Sharding: Parallel Processing

The input data is divided into multiple smaller datasets called *shards*. Each
of these shards are processed in parallel. A shard is processed by an individual
instance of the map function with its own input reader that feeds it data
reserved for this shard. Likewise for the reduce function.

The benefit of sharding is that each shard can be processed in parallel.

### Slicing: Fault Tolerance

The data in a shard is processed sequentially. Each shard is assigned a task and
that task iterates over all data in the shard using an App Engine Task Queue.
When a task is run it iterates over as much data from the shard as it can in 15
seconds (configurable). After this time period expires a new slice is created
and the process repeats until all data in the shard has been processed.

The benefit of slicing is fault tolerance. If an error occurs during the run of
a slice, that particular slice can be run again without affecting the processing
of previous or subsequent slices.

## Conclusions

MapReduce provides a convenient programming model for operating on large
datasets. In our next article we look at how to use the Python MapReduce API for
App Engine to process entities from the datastore.
