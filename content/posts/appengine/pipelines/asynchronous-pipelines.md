---
title: "App Engine Pipelines API - Part 5: Asynchronous Pipelines" 
date: 2015-06-02T04:45:56-06:00
tags: 
  - "appengine"
  - "pipelines"
series:
  - "Pipelines API"
---

* [View all articles in the Pipeline API Series](http://sookocheff.com/series/pipelines-api/).

This article will cover fully asynchronous pipelines. The term 'asynchronous' is
misleading here — all piplines are asynchronous in the sense that yielding a
pipeline is a non-blocking operation. An asynchronous refers to a
pipeline that remains in a RUN state until outside action is taken, for example,
a button is clicked or a task is executed.

Marking a pipeline as an asynchronous pipeline is as simple as setting the
`async` class property to True.

```python
class AsyncPipeline(pipeline.Pipeline):
    async = True
```

Once this pipeline starts, it will remain in the RUN state until the pipeline is
transitioned to another state. You transition a pipeline to another state by
calling the `complete` method, using a callback. `complete()` is a
method only available to asynchronous pipelines. Calling complete will fill the
pipelines output slots and, if all slots have been filled, mark the pipeline
complete. Any barriers related to the slots being filled are notified as
described in [the previous article]({{< ref "pipeline-internals.md" >}}).

```python
class AsyncPipeline(pipeline.Pipeline):
    async = True

    def callback(self):
        self.complete()
```

## Callback URLs

The pipeline API provides convenience methods for calling the callback method.
`get_callback_url` returns a URL that, when accessed, passes any query
parameters to the callback method. For example, to generate a URL to our
pipeline with a `choice` parameter we can call get_callback_url as follows:

```python
url = get_callback_url(choice='approve')
```

This will generate a URL of the form:

```python
/_ah/pipeline/callback?choice=approve&pipeline_id=fd789852183b4310b5f1353205a967fe
```

Accessing this URL will pass the `choice` parameter to the callback function of
the pipeline with pipeline_id `fd789852183b4310b5f1353205a967fe`.

```python
class AsyncPipeline(pipeline.Pipeline):
    async = True
    public_callbacks = True

    def run(self):
        url = self.get_callback_url(choice='approve')
        logging.info('Callback URL: %s' % url)

    def callback(self, choice):
        if choice == 'approve':
            logging.info('Pipeline Complete')
            self.complete()
```

Running the pipeline above will log the Callback URL to the console. By visiting
that URL, the `callback` method will execute, completing your pipeline. You can
refer to the [EmailToContinue](https://github.com/GoogleCloudPlatform/appengine-pipelines/blob/master/python/src/pipeline/common.py) Pipeline for a more robust example.

## Callback Tasks

The second way to execute a callback method is via a callback task. The
Pipelines API provides another convenience method to generate a callback task
that will execute our pipeline. In the following example, a task is created to
trigger in the future, adding an artificial delay to our pipeline.

```python
class DelayPipeline(pipeline.Pipeline):
    async = True

    def __init__(self, seconds):
        super(DelayPipeline, self).__init__(seconds=seconds)

    def run(self, seconds=None):
        task = self.get_callback_task(
            countdown=seconds,
            name='ae-pipeline-delay-' + self.pipeline_id)
        try:
            task.add(self.queue_name)
        except (taskqueue.TombstonedTaskError, taskqueue.TaskAlreadyExistsError):
            pass

    def callback(self):
        self.complete(self.kwargs['seconds'])
```

Note that the task is queued using the pipeline_id in the task name. This helps
ensure our run method is idempotent. Full source code for an asynchronous
pipeline follows. This pipeline will delay for 10 seconds, and then log a
callback_url to the console. Visiting the callback URL will complete the
pipeline.

```python
import logging
import webapp2
import pipeline
from google.appengine.api import taskqueue


class RunPipelineHandler(webapp2.RequestHandler):
    def get(self):
        pipeline = DelayPipeline(10)
        pipeline.start()


class DelayPipeline(pipeline.Pipeline):
    async = True

    def __init__(self, seconds):
        pipeline.Pipeline.__init__(self, seconds=seconds)

    def run(self, seconds=None):
        task = self.get_callback_task(
            countdown=seconds,
            name='ae-pipeline-delay-' + self.pipeline_id)
        try:
            task.add(self.queue_name)
        except (taskqueue.TombstonedTaskError,
                taskqueue.TaskAlreadyExistsError):
            pass

    def callback(self):
        AsyncPipeline().start()


class AsyncPipeline(pipeline.Pipeline):
    async = True
    public_callbacks = True

    def run(self):
        url = self.get_callback_url(choice='approve')
        logging.info('Callback URL: %s' % url)

    def callback(self, choice):
        if choice == 'approve':
            self.complete()


routes = [webapp2.Route('/pipeline-test/', handler='main.RunPipelineHandler')]

APP = webapp2.WSGIApplication(routes)
```
