---
title: "App Engine Pipelines API - Part 3: Fan In, Fan Out, Sequencing" 
date: 2015-05-19T05:57:19-06:00
tags: 
  - "appengine"
  - "pipelines"
series:
  - "Pipelines API"
aliases:
  - /posts/2015-05-19-app-engine-pipelines-part-three-fan-in-fan-out/
---

* [View all articles in the Pipeline API Series](http://sookocheff.com/series/pipelines-api/).

[Last time]({{< ref "connecting-pipelines.md" >}}),
we studied how to connect two pipelines together. In this post, we expand on
this topic, exploring how to fan-out to do multiple tasks in parallel, fan-in
to combine multiple tasks into one, and how to do sequential work.

<!--more-->

## Fan-Out

Fan-Out refers to spreading a task to multiple destinations in parallel. Using
the Pipelines API, fan-out can be achieved elegantly by yielding a new pipeline
for every task you wish to execute. Each of these pipelines is exeucted
immediately via a Task in the App Engine Task Queue. Fan-out parallelizes
implicitly when additional App Engine instances are started to handle the
increased number of requests arriving in the Task Queue. You can moderate the
amount of fan-out by changing the processing rate on the task queue that
executes your pipelines.

```python
class SquarePipeline(pipeline.Pipeline):

    def run(self, number):
        logging.info('Squaring: %s', number)
        return number * number


class FanOutPipeline(pipeline.Pipeline):

    def run(self, count):
        for i in xrange(0, count):
            yield SquarePipeline(i)
        # All children run immediately
```

## Fan-In

Fan-In implies waiting for a collection of related tasks to complete before
continuing processing. The example can be extended by summing the list of
squared values — when we call `yield Sum(*results)` the pipeline run-time will
wait until all results are ready before executing Sum. Internally, a *barrier*
record is created that blocks execution of Sum and tracks the dependencies
required to lift the barrier. Once all dependencies have been satisfied the
barrier is lifted and Sum can execute.

```python
class SquarePipeline(pipeline.Pipeline):

    def run(self, number):
        logging.info('Squaring: %s' % number)
        return number * number


class Sum(pipeline.Pipeline):

    def run(self, *args):
        value = sum(list(args))
        logging.info('Sum: %s', value)
        return value


class FanInPipeline(pipeline.Pipeline):

    def run(self, count):
        results = []
        for i in xrange(0, count):
            result = yield SquarePipeline(i)
            results.append(result)

        # Waits until all SquarePipeline results are complete
        yield Sum(*results)
```

## Sequencing

A common workflow is running pipelines in a predefined sequence. The Pipelines
API provides context managers that will force execution ordering using the
`with` keyword. This is useful for Pipelines with no output that you wish to
execute in a specific order — we cannot wait for the output and so no barrier
must be satisfied, but we still want to enforce an execution order. In the
following example, we extend the FanOutFanInPipeline to update an HTML
dashboard with our results and, once that is complete, send out an e-mail to the
development team. This example is taken from the excellent [Pipelines API
introductory video](https://www.youtube.com/watch?v=Rsfy_TYA2ZY).

```python
class FanOutFanInPipeline(pipeline.Pipeline):

    def run(self, count):
        results = []
        for i in xrange(0, count):
            result = yield SquarePipeline(i)
            results.append(result)

        result = yield Sum(*results)
        with pipeline.InOrder():
            yield UpdateDashboard()
            yield EmailTeam()
```

## Conclusion

This article describes how to coordinate pipeline tasks using fan-in, fan-out
and sequencing. The next article we will discuss Pipeline API internals.

Full source code of both Fan-In and Fan-Out follows.

```python
import logging
import webapp2
import pipeline


class RunPipelineHandler(webapp2.RequestHandler):
    def get(self):
        stage = FanOutFanInPipeline(10)
        stage.start()


class SquarePipeline(pipeline.Pipeline):

    def run(self, number):
        logging.info('Squaring: %s' % number)
        return number * number


class Sum(pipeline.Pipeline):

    def run(self, *args):
        value = sum(list(args))
        logging.info('Sum: %s', value)
        return value


class FanOutFanInPipeline(pipeline.Pipeline):

    def run(self, count):
        results = []
        for i in xrange(0, count):
            result = yield SquarePipeline(i)
            results.append(result)

        yield Sum(*results)


routes = [
    webapp2.Route('/pipeline-test/', handler='main.RunPipelineHandler')
]

APP = webapp2.WSGIApplication(routes)
```
