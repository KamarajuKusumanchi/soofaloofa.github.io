# MapReduce Part 4: Combining Sequential MapReduce Jobs

## MapReduce API Series
* 
-
-
-

[Last time](http://sookocheff.com/posts/2014-04-30-app-engine-mapreduce-api-part-3-programmatic-mapreduce-using-pipelines/) we looked at how to run a full MapReduce Pipeline to count the number of occurrences of a character within each string. In this post we will see how to chain multiple MapReduce Pipelines together to perform sequential tasks.

As a contrived example (as all examples are) let's imagine a scenario where we want to clean up some data by deleting a business entity from the datastore. Each business has employees stored as a different entity that also need to be deleted. Our simplified models look like this.

```python
from google.appengine.ext import ndb

class Business(ndb.model):
    """
    Model representing a business which will have employees
    """
    name = ndb.StringProperty(required=True)
    address = ndb.StringProperty()
    
class Employee(ndb.model):
    """
    Model representing a business which will have employees
    """
    name = ndb.StringProperty(required=True)
    business = ndb.StringProperty(required=True)    
```

Let's create a pipeline that will iterate over every business with a matching `name` and delete all the employees from that business. We can take advantage of the `filters` parameter of the `DatastoreInputReader` to find all employees working at a business with a matching name.

```python
def delete_employee(entity):
    """ Delete an employee entity. """
    yield op.db.Delete(entity)

class DeleteBusinessPipeline(pipeline.Pipeline):
    """ Count the number of occurrences of a character in a set of strings. """

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

This simple pipeline will delete all of the employees. We can add a second pipeline to our execution that will delete the business by simply yielding the pipeline back to the Pipeline API.

```python
def delete_employee(entity):
    """ Delete an employee entity. """
    yield op.db.Delete(entity)

def delete_business(entity):
    """ Sum the number of characters found for the key. """
    yield op.db.Delete(entity)

class DeleteBusinessPipeline(pipeline.Pipeline):
    """ Count the number of occurrences of a character in a set of strings. """

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

The return value of the MapperPipeline call is a `PipelineFuture` object. This future will be executed once the previous future has completed. In this case our employee deletion pipeline will complete and the business deletion future will execute.

And that's all it takes to run two sequential MapReduce jobs!
