---
title: "App Engine Pipelines API - Part 2: Connecting Pipelines" 
date: 2015-05-12T05:57:19-06:00
tags: 
  - "appengine"
  - "pipelines"
series:
  - "Pipelines API"
---

[Last time]({{< ref "2015-05-05-app-engine-pipelines-part-one-the-basics.md" >}}), 
we discussed basic pipeline instantiation and execution. This time, we will
cover sequential pipelines, answering the question "How do I connect the output
of one pipeline with the input of another pipeline"?

<!--more-->

To begin, let's review a basic pipeline that squares its input. If any of this
does not make sense refer to the [first part of this tutorial]({{< ref
"2015-05-05-app-engine-pipelines-part-one-the-basics.md" >}}).

```python
import logging
import webapp2
import pipeline


class RunPipelineHandler(webapp2.RequestHandler):
    def get(self):
        stage = SquarePipeline(10)
        stage.start()


class SquarePipeline(pipeline.Pipeline):

    def run(self, number):
        return number * number

    def finalized(self):
        logging.info('All done! Square is %s', self.outputs.default.value)
```

The first step in passing data between two pipelines is updating our pipeline to
use the generator interface. The generator interface uses the `yield` keyword as
a means of connecting pipelines together. For this contrived example, let's
create a *parent* pipeline that executes `SquarePipeline` twice in succession.

```python
class TwiceSquaredPipeline(pipeline.Pipeline):

    def run(self, number):
        first_square = yield SquarePipeline(number)
        second_square = yield SquarePipeline(first_square)
```

What now? We need a way to access the value stored in `second_square`. When
execution hits a `yield` statement a task is started to run the pipeline and a
`PipelineFuture` is returned. The `PipelineFuture` will have a value *after* the
task has finished executing but not immediately. So how do we access the value?
With a *child* pipeline that can read the result. In this example, we simply log
the value of the computation.

```python
class TwiceSquaredPipeline(pipeline.Pipeline):

    def run(self, number):
        first_square = yield SquarePipeline(number)
        second_square = yield SquarePipeline(first_square)
        yield LogResult(second_square)

class LogResult(pipeline.Pipeline):

    def run(self, number):
        logging.info('All done! Value is %s', number)
```

The rule of thumb here is that *anything you instantiate your pipeline with (and
subsequently pass to the `run` method) is accessible within your
pipeline*. These are called *immediate values* and you can treat them as regular
Python values. When this code is executed, each pipeline started by a `yield`
call is a separate App Engine Task that executes in the Task Queue. The Pipeline
runtime coordinates running these tasks and shares the results of execution
between tasks, allowing you to safely connect pipelines together.

Full source code for this example follows.

```python
import logging
import webapp2
import pipeline


class RunPipelineHandler(webapp2.RequestHandler):
    def get(self):
        stage = TwiceSquaredPipeline(10)
        stage.start()


class SquarePipeline(pipeline.Pipeline):

    def run(self, number):
        return number * number


class TwiceSquaredPipeline(pipeline.Pipeline):

    def run(self, number):
        first_square = yield SquarePipeline(number)
        second_square = yield SquarePipeline(first_square)
        yield LogResult(second_square)


class LogResult(pipeline.Pipeline):

    def run(self, number):
        logging.info('All done! Value is %s', number)


routes = [
    webapp2.Route('/pipeline-test/', handler='main.RunPipelineHandler')
]

APP = webapp2.WSGIApplication(routes)
```
