---
title: "App Engine Pipelines API - Part 1: The Basics" 
date: 2015-05-05T05:57:19-06:00
tags: 
  - "appengine"
  - "pipelines"
series:
  - "Pipelines API"
---

The [Pipelines API](https://github.com/GoogleCloudPlatform/appengine-pipelines)
is a general purpose workflow engine for App Engine applications. With the
Pipelines API we can connect together complex workflows into a coherent run time
backed by the Datastore. This article provides a basic overview of the Pipelines
API and how it can be used for abritrary computational workflows.

In the most basic sense a Pipeline is an object that takes input, performs some
logic or computation on that input, and produces output. Pipelines can take two
general forms -- synchronous or asynchronous. Synchronous pipelines act as basic
functions that must complete during a single request. Asynchronous pipelines
spawn child pipelines and connect them together into a workflow by passing input
and output parameters around.

**A word of warning.**

Pipelines must be idempotent and it is up to the developer to ensure that they
are -- this is not enforced by the run-time. A pipeline may fail and be retried
and it is important that running the same pipeline with the same set of inputs
will product the same results.

## Getting Started

The first step is to grab the latest version of the Pipelines API (and its
        dependencies) using pip. The following assumes you install third party
App Engine dependencies in the lib directory relative to where pip is being run.
You can also grab the source code from
[GitHub](https://github.com/GoogleCloudPlatform/appengine-pipelines).

```python
pip install GoogleAppEnginePipeline -t lib/
```

Pipeline requests need to be handled by the Pipeline application. We set that up
by adding a handler to `app.yaml`. Since these are internal application requrest
we can secure them using the `login: admin` directive.

```yaml
handlers:
- url: /_ah/pipeline.*
  script: pipeline.handlers._APP
  login: admin
```

## Basic Synchronous Pipelines

A synchronous pipeline runs within the bounds of a single App Engine request.
Once the request has been made the pipeline starts and pipeline processing
happens automatically. We can set up this pipeline by defining a handler
responsible for starting the pipeline. For now, create a default handler that
will receive a request at the URL of your choosing.

```python
import logging
import webapp2

class RunPipelineHandler(webapp2.RequestHandler):
    def get(self):
        logging.info('Launch pipeline')
```

A request processed by this handler will kick off our Pipeline. To define a
pipeline we inherit from the Pipeline object and the method `run`. The pipeline
is launched via the `start` method. The code below instantiates a custom
pipeline and launches it. Accessing the URL for the RunPipelineHandler will
print the message 'Do something here' to the logs.

```python
import logging
import webapp2
import pipeline

class RunPipelineHandler(webapp2.RequestHandler):
    def get(self):
        logging.info('Launch pipeline')
        pipeline = MyPipeline()
        pipeline.start()


class MyPipeline(pipeline.Pipeline):
    def run(self, *args, **kwargs):
        logging.info('Do something here.')
```

We can update our pipeline to do a simple operation, like squaring a number.
You'll notice in the code that follows that the arguments passed when
initializing the pipeline are accessible as parameters to the `run` method
within the pipeline.

```python
import logging
import webapp2
import pipeline


class RunPipelineHandler(webapp2.RequestHandler):
    def get(self):
        square_stage = SquarePipeline(10)
        square_stage.start()


class SquarePipeline(pipeline.Pipeline):
    def run(self, number):
        return number * number
```

Running this pipeline will show that the pipeline executes correctly. But where
does our return value go? How can we access the output of `SquarePipeline`?

## Accessing Pipeline Output

You'll notice that in `SquarePipeline` we are returning a value directly but
we never actually access it. Pipeline output can only ever be accessed after the
pipeline has finished executing. We can check for the end of pipeline execution
using the `has_finalized` property. This property will be set to `True` when all
stages of a pipeline have finished executing. At this point in time our output
will be available as a value on the Pipeline object. Let's see what happens when
we try to check if our pipeline has finalized. To do this we need to store the
pipeline_id generated from our start method and check the `has_finalized`
property.

```python
import logging
import webapp2
import pipeline


class RunPipelineHandler(webapp2.RequestHandler):
    def get(self):
        square_stage = SquarePipeline(10)
        square_stage.start()

        pipeline_id = square_stage.pipeline_id

        stage = SquarePipeline.from_id(pipeline_id)
        if stage.has_finalized:
            logging.info('Finalized')
        else:
            logging.info('Not finalized')


class SquarePipeline(pipeline.Pipeline):
    def run(self, number):
        return number * number
```

Running the preceding code we see that our pipeline is not finalized. What
happened here? The pipeline is executed as an ayschronous task after it has been
started and may or may not complete by the time we check that it has finalized.
The pipeline itself is a future whose value has not materialized. Any output
from a pipeline is not actually available until all child pipeline tasks are
executed. So how do we get the final value of the SquarePipeline?

## Finalized

The finalized method is called by the pipeline API once a Pipeline has completed
its work (by filling all of is slots -- to be described later). By overriding
the `finalized` method we can see the result of our pipeline and do further
processing on that result if necessary. By default our output is set to
`self.outputs.default.value`. As an example, executing the following code will
log the message "All done! Square is 100".

```python
import logging
import webapp2
import pipeline


class RunPipelineHandler(webapp2.RequestHandler):
    def get(self):
        square_stage = SquarePipeline(10)
        square_stage.start()


class SquarePipeline(pipeline.Pipeline):
    def run(self, number):
        return number * number

    def finalized(self):
        logging.info('All done! Square is %s', self.outputs.default.value)
```

We will see in a later article how to connect the output of one pipeline with
another.

## Named outputs

Pipelines also allow you to explicitly name outputs, this is useful in the case
where you have more than one output to return or as a means of passing data
between one pipeline execution and the next. When using named outputs, instead
of returning a value from the `run` method we fill a pipeline slot with our
value. To use named outputs we define an `output_names` class variable listing
the names of our outputs. By calling `self.fill` on our named output we store
the return value of our pipeline for later access in the `run` method.

```python
import logging
import webapp2
import pipeline


class RunPipelineHandler(webapp2.RequestHandler):
    def get(self):
        square_stage = SquarePipeline(10)
        square_stage.start()


class SquarePipeline(pipeline.Pipeline):

    output_names = ['square']

    def run(self, number):
        self.fill(self.outputs.square, number * number)

    def finalized(self):
        logging.info('All done! Square is %s', self.outputs.square.value)
```

## Testing a pipeline

Sometimes our pipelines call out over the wire or perform expensive data
operations. The Pipeline API provides a convenient way to test pipelines. By
calling `start_test` instead of `start`. In our example we verify the
expected output of our squaring pipeline by calling `start_test`. The final
value of our pipeline is available immediately.

```python
class RunPipelineHandler(webapp2.RequestHandler):
    def get(self):
        square_stage = SquarePipeline(10)
        square_stage.start_test()
        assert stage.outputs.square.value == 100
```

If we need to mock out any behaviour from our `run` method, we can supply a
`run_test` method that is executed whenever we run our pipeline with
`start_test`. Within this method we can mock out or adjust the behaviour of the
pipeline to work under test.

## Conclusion

This article gives a basic outline of how to start and execute pipelines. Full
source code for the final example is listed below. In the next article we will
see how to pass the output of one pipeline to another and understand how parent
and child pipelines interact.

```python
import logging
import webapp2
import pipeline


class RunPipelineHandler(webapp2.RequestHandler):
    def get(self):
        square_stage = SquarePipeline(10)
        square_stage.start()


class SquarePipeline(pipeline.Pipeline):

    output_names = ['square']

    def run(self, number):
        self.fill(self.outputs.square, number * number)

    def finalized(self):
        logging.info('All done! Square is %s', self.outputs.square.value)

routes = [
    webapp2.Route('/pipeline-test/', handler='main.RunPipelineHandler')
]

APP = webapp2.WSGIApplication(routes)
```
