---
title: "Keeping App Engine Search Documents and Datastore Entities In Sync"
date: 2015-02-23T08:20:37-06:00
tags: 
  - "app engine"
  - "search api"
  - "datastore"
---

At Vendasta the App Engine Datastore serves as the single point of truth for
most operational data and the majority of interactions are against this single
point of truth. However, a piece of required functionality in many of our
products is to provide a searchable view of the data in the App Engine
Datastore. Search is difficult using the Datastore and so we have moved to using
the [Search API](https://cloud.google.com/appengine/docs/python/search/) as a
managed solution for searching datastore entities. In this use case, every edit
to an entity in the Datastore is reflected as a change to a Search Document.
This article details an architecture for keeping Datastore entities and Search
Documents in sync throughout failure and race conditions.

## Updating the Search Document Using a _post_put_hook

To ensure that every put of an entity to the Datastore results in an update to
the associated search document, we update the search document in the
_post_put_hook of the entity. The _post_put_hook is executed every time
the entity is put so each time the entity has changed we will put a new and
updated search document. 

```python
from google.appengine.api import search
from google.appengine.ext import ndb

class UserModel(ndb.model):

    username = ndb.StringProperty(required=True)
    email = ndb.StringProperty(required=True)

    def _post_put_hook(self, future):
        document = search.Document(
            doc_id = self.username,
            fields=[
               search.TextField(name='username', value=self.username),
               search.TextField(name='email', value=self.email),
               ])
        try:
            index = search.Index(name="UserIndex")
            index.put(document)
        except search.Error:
            logging.exception('Put failed')
```

Updating the search document during every put as part of the post put hook is a
light weight way to keep the search document up-to-date with changes to the
entity. However, this design does not account for the potential error conditions
where putting the search document or the Datastore entity fails. We will need
some additional functionality to handle these cases.

## Handling Search Document Put Failures

The first obstacle to overcome is handling failures when putting the search
document. One method for handling failures is retrying. We can add retrying to
our workflow by separating updating the search document into its own task and
deferring that task using the deferred library. This accomplishes two things.
First, moving the search document functionality into its own function makes our
code more modular. Second, the App Engine task queue mechanism allows us to
specify our retry semantics, handling backoff and failure conditions gracefully.
In this example, we allow infinite retries of failed tasks, allowing DevOps to find
search documents that may have become out of sync with their Datastore entities
and correct any problems that may arise. We assume in the example below that the
username acts as the ndb Key.

```python
from google.appengine.api import search
from google.appengine.ext import ndb
from google.appengine.ext import deferred

class UserModel(ndb.model):

    username = ndb.StringProperty(required=True)
    email = ndb.StringProperty(required=True)

    @classmethod
    def put_search_document(cls, username):
        model = ndb.Key(cls, username).get()
        if model:
            document = search.Document(
                doc_id = username,
                fields=[
                   search.TextField(name='username', value=self.username),
                   search.TextField(name='email', value=self.email),
                   ])
            index = search.Index(name="UserIndex")
            index.put(document)

    def _post_put_hook(self, future):
        deferred.defer(UserModel.put_search_document, self.username)
```

## Handling Datastore Put Failures

The second obstacle to overcome is safely handling Datastore put failures. In
this architecture, each change to a Datastore entity is required to run within a
transaction. We update the _post_put_hook to queue a transactional task -- which
forces the task to only be queued if the current transaction has successfully
completed. This guarantees that failed Datastore puts will not result in search
documents being updated and becoming out of sync with the Datastore.

We specify that the task should be run as a transaction by passing the result of
the `in_transaction` function to the `_transactional` parameter of `defer`.
`in_transaction` returns `True` if the currently executing code is running in a
transaction and `False` otherwise.

```python
from google.appengine.api import search
from google.appengine.ext import ndb
from google.appengine.ext import deferred

class UserModel(ndb.model):

    username = ndb.StringProperty(required=True)
    email = ndb.StringProperty(required=True)

    @classmethod
    def put_search_document(cls, username):
        model = ndb.Key(cls, username).get()
        if model:
            document = search.Document(
                doc_id = username,
                fields=[
                   search.TextField(name='username', value=self.username),
                   search.TextField(name='email', value=self.email),
                   ])
            index = search.Index(name="UserIndex")
            index.put(document)

    def _post_put_hook(self, future):
        deferred.defer(UserModel.put_search_document,
                       self.username,
                       _transactional=ndb.in_transaction())
```

Now we have sastisfied the case where either the search document or the
Datastore put has failed. If the search document put has failed we retry, if the
Datastore put has failed we do not put the search document. We still have one
remaining problem: Dirty Reads.

## Handling Dirty Reads

The last obstacle to overcome is dealing with race conditions that could lead to
reading stale data and writing that data to the search document. Consider the
case where two subsequent puts to the Datastore occur back-to-back within a
short time frame. Each of these puts will write new data to the Datastore and
queue a task to put the updated search document to the Datastore. The dirty
read problem arises when the second task to update the search document reads old
data from the Datastore that may not have been fully replicated throughout the
Datastore.

{{% img "2015-02-23-syncing-search-documents-with-datastore-entities/SyncingSearchDocuments.png"  "Syncing Search Documents" %}}

We can overcome this problem by versioning our tasks to coincide with the
version of our Datastore entity. We add a version number to the entity and
update the version number during a _pre_put_hook.

```python
from google.appengine.ext import ndb

class UserModel(ndb.model):

    username = ndb.StringProperty(required=True)
    email = ndb.StringProperty(required=True)
    version = ndb.IntegerProperty(default=0)

    def _pre_put_hook(self):
        self.version = self.version + 1
```

Now during the _post_put_hook we queue a task corresponding to the version number
of the Datastore entity we are putting. This ties the task to the point in time
when the Datastore entity was put.

```python
import logging
from google.appengine.api import search
from google.appengine.ext import ndb
from google.appengine.ext import deferred

class UserModel(ndb.model):

    username = ndb.StringProperty(required=True)
    email = ndb.StringProperty(required=True)
    version = ndb.IntegerProperty(default=0)

    @classmethod
    def put_search_document(cls, username, version):
        model = ndb.Key(cls, username).get()
        if model:
            if version < model.version:
                logging.warning('Attempting to write stale data. Ignore')
                return

            if version > model.version:
                msg = 'Attempting to write future data. Retry to await consistency.'
                logging.warning(msg)
                raise Exception(msg)

            # Versions match. Update the search document
            document = search.Document(
                doc_id = username,
                fields=[
                   search.TextField(name='username', value=model.username),
                   search.TextField(name='email', value=model.email),
                   search.TextField(name='version', value=model.version),
                   ])
            index = search.Index(name="UserIndex")
            index.put(document)

    def _pre_put_hook(self):
        self.version = self.version + 1

    def _post_put_hook(self, future):
        deferred.defer(UserModel.put_search_document,
                       self.username,
                       self.version,
                       _transactional=ndb.in_transaction())
```

If the version number of the task being executed is less than the version number
written to the Datastore, we are attempting to write stale data and do not need
to process this request. If the version number is greater than the task being
executed, we are attempting to write data to the search document that has not
been fully replicated throughout the Datastore. In this case, we raise an
exception to retry putting the search document. In subsequent retries the data
will have propagated and our put will succeed. Note that if another task is
executed while the current task is retrying, the version number of our retrying
task will become stale and when the task is next executed we do not write the
now stale data to the search document.

This still handles the case when a search document put fails -- whenever our
version number becomes out of sync due to the failed put, we do not write the
data to the search document. Furthermore, if our Datastore put fails then our
task to put the search document will not be queued *as long as the Datastore put
is run within a transaction*. The version number will not be incremented in this
case because the value set during the _pre_put_hook will not be persisted during
a failed transaction. 

## Conclusion

Putting this all together, we've developed a solution for keeping search
documents in sync with Datastore entities that is robust to failure and race
conditions. This same technique can be used for syncing the state of any number
of datasets that are dependent on the Datastore being the single point of truth
in your system.
