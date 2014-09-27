---
title: "Composing Asynchronous Functions With Tasklets"
date: 2014-09-27T15:25:29Z
description: "Asynchronous functions can provide a boon to application performance by allowing time consuming functions to operate in parallel and without blocking the main execution thread. This article explains how to use the Tasklet API to compose and execute asynchronous functions in Google App Engine."
tags: 
  - "ndb"
  - "app engine"
  - "tasklets"
---

Asynchronous functions can provide a boon to application performance by allowing time consuming functions to operate in parallel and without blocking the main execution thread. This article explains how to use the Tasklet API to compose and execute asynchronous functions in Google App Engine.

## ndb.Future

A [Future](https://developers.google.com/appengine/docs/python/ndb/futureclass) is a class representing an asynchronous I/O operation. `ndb` provides asynchronous versions of datastore operations that will return a future instead of immediately returning data.

```python
future = User.get_by_id_async(uid)
```

When a Future is first created it has no data while the I/O operation is running. By calling `get_result()` on the Future the application will stop execution of the current thread until the data is available from the I/O operation.

```python
future = User.get_by_id_async(uid)
user = future.get_result()  # Return the data, blocking execution until the data is ready.
```

The above code is equivalent to calling the non-asynchronous `ndb.get` function.

```python
user = User.get_by_id(uid)
```

Using futures in this way allows you to run multiple I/O operations in parallel.

```python
# Run two asynchronous operations in parallel
user_future = User.get_by_id_async(uid)
accounts_future = Account.query(Account.user_id==uid).fetch_async()
```

## ndb.tasklet

Tasklets allow you to create your own asynchronous functions that return a Future. The application can call `get_result()` on that Future to return the data. 

```python
tasklet_future = my_tasklet()  # A tasklet
result = tasklet_future.get_result()
```

You can use Tasklets to create fine grained asynchronous functions, in some cases simplifying how a method is programmed. 

When AppEngine encounters a tasklet function the Tasklet framework inserts the tasklet into an event loop. The event loop will cycle through all tasklets and execute them until a `yield` statement is reached within the tasklet. The `yield` statement is where you put the asynchronous work so that the framework can execute your `yield` statement (asynchronously) and then move on to another tasklet to resume execution until its `yield` statement is reached. In this way all of the `yield` statements are done asynchronously. For even more performance, NDB implements a batch job framework that will bundle up multiple requests in a single batch RPC to the server.

As a simple example, we can use a tasklet to define an asynchronous query and return the result.

```python
@ndb.tasklet
def query_tasklet():
    result = yield Model.query().fetch_async()
    raise ndb.Return(result)
```

The line `result = yield Model.query().fetch_async()` will alert the tasklet framework that this is an asynchronous line of code and that the framework can wait here and execute other code while the asynchronous line completes. To force the asynchronous code to complete you call `get_result()` on the return value of the tasklet function.

```python
future = query_tasklet()
future.get_result()
```

So how do we use this in our code? There are three distinct cases.

## Case 1: Processing an asynchronous result

Suppose that you have an asynchronous function that returns a Future and you want to do some processing on the result before returning from your function. In that case you may have code like this.

```python
def process_a_query():
	future = Model.query().fetch_async()
	return process_result(future.get_result())
```

To turn this into an asynchronous tasklet function you can add the tasklet decorator and yield your asynchronous fetch.

```python
@ndb.tasklet
def process_a_query():
	result = yield Model.query().fetch_async()
	raise ndb.Return(process_result(result))
```

Now your function `process_a_query` can be called asynchronously.

```python
future = process_a_query()
# ...
future.get_result()
```


## Case 2: Composing two asynchronous functions

In this case, suppose you have two asynchronous functions that depend on each other and you want to combine them with the tasklet framework.

```python
def multiple_query():
	future_a = ModelA.query().fetch_async()
	a = future_a.get_result()
	future_b = ModelB.query(ModelB.id==a).fetch_async()
	return future_b
```

The above code becomes simpler with tasklets.

```python
@ndb.tasklet
def multiple_query():
    a = yield ModelA.query().fetch_async()
    b = yield ModelB.query(ModelB.id==a).fetch_async()
    raise ndb.Return(b)
```

## Case 3: Parallel Computation

The last case to discuss is parallel computation. In this scenario you have two independent asynchronous functions that you want to run in parallel.

```python
@ndb.tasklet
def parallel_query():
	  a, b = yield ModelA.query().fetch_async(), yield ModelB.query().fetch_async()
	  raise ndb.Return((a,b))
```

## Summary

In all of these cases we show how to combine and compose asynchronous functions using the tasklet framework. This allows you to define your own asynchronous functions that are can be used just like the ndb asynchronous functions.
