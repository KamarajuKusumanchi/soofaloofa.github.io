---
title: "Converting an ndb model to a BigQuery schema"
date: 2014-08-14T17:58:03Z
tags: 
  - "app engine"
  - "ndb"
  - "bigquery"
aliases:
  - "posts/2014-08-14-converting-an-ndb-model-to-a-bigquery-schema/"
---

I have been working on the problem of recording changes to an ndb model. One way to accomplish this is to stream data changes to a BigQuery table corresponding to the ndb model. It would be great to do this in a generic way which gives us the problem of generating a BigQuery table given an ndb model. This article will describe one solution to this problem.

<!--more-->

## Accessing the properties of an ndb class

The first step in the process is to find all the properties of the class via the
ndb `_properties` accessor. By iterating over this field we can find all of the
properties on the class and their ndb types.

```python
def tablify(ndb_model):
    """
    Convert ndb_model into a BigQuery table schema.
    """
    for name, ndb_type in ndb_model.__class__._properties.iteritmes():
       print name, ndb_type
```

## Converting properties to BigQuery schema types

Now that we have the set of properties on the class we can map from the type of
each property to a BigQuery type. Here is a helper function that provides a
simple mapping.

```python
def ndb_type_to_bigquery_type(_type):
    """
    Convert a python type to a bigquery type.
    """
    if isinstance(_type, ndb.IntegerProperty):
        return "INTEGER"
    elif isinstance(_type, ndb.FloatProperty):
        return "FLOAT"
    elif isinstance(_type, ndb.BooleanProperty):
        return "BOOLEAN"
    elif type(_type) in [ndb.StringProperty, ndb.TextProperty, ndb.ComputedProperty]:
        return "STRING"
    elif type(_type) in [ndb.DateTimeProperty, ndb.DateProperty, ndb.TimeProperty]:
        return "TIMESTAMP"
```

The last task is to format everything as a [BigQuery table
resource](https://developers.google.com/bigquery/docs/reference/v2/tables). This
involves adding some boiler-plate around each field in our BigQuery schema and
fleshing out the structure of the JSON.

```python

from google.appengine.ext import ndb

SUPPORTED_TYPES = [ndb.IntegerProperty,
                   ndb.FloatProperty,
                   ndb.BooleanProperty,
                   ndb.StringProperty,
                   ndb.TextProperty,
                   ndb.DateTimeProperty,
                   ndb.DateProperty,
                   ndb.TimeProperty,
                   ndb.ComputedProperty]


def ndb_type_to_bigquery_type(_type):
    """
    Convert a python type to a bigquery type.
    """
    if isinstance(_type, ndb.IntegerProperty):
        return "INTEGER"
    elif isinstance(_type, ndb.FloatProperty):
        return "FLOAT"
    elif isinstance(_type, ndb.BooleanProperty):
        return "BOOLEAN"
    elif type(_type) in [ndb.StringProperty, ndb.TextProperty, ndb.ComputedProperty]:
        return "STRING"
    elif type(_type) in [ndb.DateTimeProperty, ndb.DateProperty, ndb.TimeProperty]:
        return "TIMESTAMP"


def ndb_property_to_bigquery_field(name, ndb_type):
    """
    Convert from ndb property to a BigQuery schema table field.
    """
    if type(ndb_type) not in SUPPORTED_TYPES:
        raise ValueError('Unsupported object property')

    field = {
        "description": name,
        "name": name,
        "type": ndb_type_to_bigquery_type(ndb_type)
    }

    if ndb_type._repeated:
        field['mode'] = 'REPEATED'

    return field


def tablify_schema(obj):
    """
    Convert ndb_model into a BigQuery table schema.
    """
    table_schema = {'fields': []}
    
    for name, ndb_type in obj.__class__._properties.iteritems():
        table_schema['fields'].append(ndb_property_to_bigquery_field(name, ndb_type))

    return table_schema


def tablify(obj, project_id, dataset_id, table_id):
    """
    Return a BigQuery table resource representing an ndb object.
    """
    return {
        "kind": "bigquery#table",
        "id": table_id,
        "tableReference": {
            "projectId": project_id,
            "datasetId": dataset_id,
            "tableId": table_id
        },
        "description": "Table Resource",
        "schema": tablify_schema(obj)
    }
```

## Creating the new table on BigQuery.

Now that we have a BigQuery schema we can create the table in BigQuery using the BigQuery api client.

```python
from oauth2client.appengine import AppAssertionCredentials
from apiclient.discovery import build

credentials = AppAssertionCredentials(scope='https://www.googleapis.com/auth/bigquery')
http = credentials.authorize(httplib2.Http())
big_query_service = build('bigquery', 'v2', http=http)
        
table_resource = tablify(ndb_model, project_id, dataset_id, table_id)
                response = big_query_service.tables().insert(projectId=project_id,
                                                             datasetId=dataset_id,
                                                             body=table_resource).execute()
```

And that's it. This article outlined a quick method of generating a BigQuery
table scheme from an ndb model. If you found this useful let me know in the
comments.
