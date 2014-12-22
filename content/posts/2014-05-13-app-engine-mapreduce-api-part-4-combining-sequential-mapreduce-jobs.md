---
title: "App Engine MapReduce API - Part 4: Combining Sequential MapReduce Jobs"
date: 2014-05-13T10:40:38Z
description: "In this post we will see how to chain multiple MapReduce Pipelines together to perform sequential tasks."
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

[Last
time](http://sookocheff.com/posts/2014-04-30-app-engine-mapreduce-api-part-3-programmatic-mapreduce-using-pipelines/)
we looked at how to run a full MapReduce Pipeline to count the number of
occurrences of a character within each string. In this post we will see how to
chain multiple MapReduce Pipelines together to perform sequential tasks.

## Combining Sequential MapReduce Jobs

As a contrived example (as all examples are) let's imagine a scenario where we
want to clean up some data by deleting a business entity from the datastore.
Each business has employees stored that also need to be deleted. Our simplified
models look like this.

```python
from google.appengine.ext import ndb

class Business(ndb.model):
    """
    Model representing a business which will have employees.
    """
    name = ndb.StringProperty(required=True)
    address = ndb.StringProperty()
    
class Employee(ndb.model):
    """
    Model representing employees of a business.
    """
    name = ndb.StringProperty(required=True)
    business = ndb.StringProperty(required=True)    
```

Let's create a pipeline that will iterate over every business with a matching
`name` and delete all the employees from that business. We can take advantage of
the `filters` parameter of the `DatastoreInputReader` to find all employees
working at a business with a matching name.

```python
def delete_employee(entity):
    """ Delete an employee entity. """
    yield op.db.Delete(entity)

class DeleteBusinessPipeline(pipeline.Pipeline):
    """ Delete a business. """

    def run(self, business_name, **kwargs):
        """ run """
        employee_params = {
            "entity_kind": "app.pipelines.Employee",
            "filters": [('business', '=', business_name)],
        }
        yield mapreduce_pipeline.MapperPipeline(
            "delete_employee",
            handler_spec=app.pipelines.delete_employee,
            input_reader_spec="mapreduce.input_readers.DatastoreInputReader",
            params=employee_params,
            shards=2)
```

This simple pipeline will delete all of the employees. We can add a second
pipeline to our execution that will delete the business by simply yielding the
return value of the first pipeline to the Pipeline API.

```python
def delete_employee(entity):
    """ Delete an employee entity. """
    yield op.db.Delete(entity)

def delete_business(entity):
    """ Delete a business entity. """
    yield op.db.Delete(entity)

class DeleteBusinessPipeline(pipeline.Pipeline):
    """ Delete a business. """

    def run(self, business_name, **kwargs):
        """ run """
        employee_params = {
            "entity_kind": "app.pipelines.Employee",
            "filters": [('business', '=', business_name)],
        }
        yield mapreduce_pipeline.MapperPipeline(
            "delete_employee",
            handler_spec=app.pipelines.delete_employee,
            input_reader_spec="mapreduce.input_readers.DatastoreInputReader",
            params=employee_params,
            shards=2)

        business_params = {
            "entity_kind": "app.pipelines.Business",
            "filters": [('name', '=', business_name)],
        }
        yield mapreduce_pipeline.MapperPipeline(
            "delete_business",
            handler_spec=app.pipelines.delete_business,
            input_reader_spec="mapreduce.input_readers.DatastoreInputReader",
            params=business_params,
            shards=2)

```

The return value of the MapperPipeline call is a `PipelineFuture` object. This
future will be executed once the previous future has completed. In this case our
employee deletion pipeline will complete and the business deletion future will
execute.

And that's all it takes to run two sequential MapReduce jobs!
