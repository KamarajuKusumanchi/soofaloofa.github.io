---
title: "Unit Testing Cloud Endpoints"
date: 2014-07-10T14:32:15Z
tags: 
  - "app engine"
  - "cloud endpoints"
  - "unit test"
  - "testing"
series:
  - "Cloud Endpoints"
aliases:
  - "posts/2014-07-10-unit-testing-cloud-endpoints/"
---

Writing unit tests for App Engine Cloud Endpoints is a fairly straight forward
process. Unfortunately it is not well documented and a few gotchas exist. This
article provides a template you can use to unit test Cloud Endpoints including
full source code for a working example.

<!--more-->

## The Model

Let's use a simple User model as the resource being exposed by our API. This
model has two properties -- a username and an email address. The class also
provides `to_message` function that converts the model to a ProtoRPC Message for
transmission by the Cloud Endpoints API.

```python
class User(ndb.Model):
    """
    A basic user model.
    """
    username = ndb.StringProperty(required=True)
    email = ndb.StringProperty(required=True)

    def to_message(self):
        """
        Convert the model to a ProtoRPC messsage.
        """
        return UserMessage(id=self.key.id(),
                           username=self.username,
                           email=self.email)


class UserMessage(messages.Message):
    """
    A message representing a User model.
    """
    id = messages.IntegerField(1)
    username = messages.StringField(2)
    email = messages.StringField(3)
```

## The API

To keep things simple the API for this resource provides a single `GET` endpoint
that returns a `UserMessage` based on a `User` in the datastore. We parameterize
our endpoint with an `ID_RESOURCE` that takes an `IntegerField` holding the id
of the User resource.

```
ID_RESOURCE = endpoints.ResourceContainer(message_types.VoidMessage,
                                          id=messages.IntegerField(1, 
                                                                   variant=messages.Variant.INT32, 
                                                                   required=True))
```

The API itself has one method, `users_get`, that returns a user given an id or
`404` if no user with the specified id exists.

```
@endpoints.api(name='users', version='v1', description='Users Api')
class UsersApi(remote.Service):

    @endpoints.method(ID_RESOURCE,
                      UserMessage,
                      http_method='GET',
                      path='users/{id}',
                      name='users.get')
    def users_get(self, request):
        entity = User.get_by_id(request.id)
        if not entity:
            message = 'No user with the id "%s" exists.' % request.id
            raise endpoints.NotFoundException(message)

        return entity.to_message()
```

## The Tests

The setup for our tests is similar to many App Engine test cases. We set our
environment and initialize any test stubs we may need.

```python
class GaeTestCase(unittest.TestCase):
    """
    API unit tests.
    """

    def setUp(self):
        super(GaeTestCase, self).setUp()
        tb = testbed.Testbed()
        tb.setup_env(current_version_id='testbed.version')  # Required for the endpoints API
        tb.activate()
        tb.init_all_stubs()
        self.api = UsersApi()  # Set our API under test
        self.testbed = tb

    def tearDown(self):
        self.testbed.deactivate()
        super(GaeTestCase, self).tearDown()
```

The actual tests call the endpoints method directly. Endpoint methods that
are set to receive a `ResourceContainer` expect a `CombinedContainer` as the
parameter to the function. The `ResourceContainer` class has a property called
`combined_message_class` that returns a `CombinedContainer` class that can be
instantiated and passed to our endpoint. We instantiate our container with the
identifier we expect for our User resource.

```python
def test_get_returns_entity(self):
    user = User(username='soofaloofa', email='soofaloofa@example.com')
    user.put()

    container = ID_RESOURCE.combined_message_class(id=user.key.id())
    response = self.api.users_get(container)
    self.assertEquals(response.username, 'soofaloofa')
    self.assertEquals(response.email, 'soofaloofa@example.com')
    self.assertEquals(response.id, user.key.id())
```

We can also add a test for the `404` condition by calling `assertRaises` on our
endpoint with an identifier that does not correspond to a User resource.

```python
def test_get_returns_404_if_no_entity(self):
    container = ID_RESOURCE.combined_message_class(id=1)
    self.assertRaises(endpoints.NotFoundException, self.api.users_get, container)
```

Full source code follows.

```python
import unittest
import endpoints

from protorpc import remote
from protorpc import messages
from protorpc import message_types
from google.appengine.ext import testbed
from google.appengine.ext import ndb


class User(ndb.Model):
    """
    A basic user model.
    """
    username = ndb.StringProperty(required=True)
    email = ndb.StringProperty(required=True)

    def to_message(self):
        """
        Convert the model to a ProtoRPC messsage.
        """
        return UserMessage(id=self.key.id(),
                           username=self.username,
                           email=self.email)


class UserMessage(messages.Message):
    """
    A message representing a User model.
    """
    id = messages.IntegerField(1)
    username = messages.StringField(2)
    email = messages.StringField(3)

ID_RESOURCE = endpoints.ResourceContainer(message_types.VoidMessage,
                                          id=messages.IntegerField(1, variant=messages.Variant.INT32, required=True))


@endpoints.api(name='users', version='v1', description='Users Api')
class UsersApi(remote.Service):

    @endpoints.method(ID_RESOURCE,
                      UserMessage,
                      http_method='GET',
                      path='users/{id}',
                      name='users.get')
    def users_get(self, request):
        entity = User.get_by_id(request.id)
        if not entity:
            message = 'No user with the id "%s" exists.' % request.id
            print message
            raise endpoints.NotFoundException(message)

        return entity.to_message()


class GaeTestCase(unittest.TestCase):
    """
    API unit tests.
    """

    def setUp(self):
        super(GaeTestCase, self).setUp()
        tb = testbed.Testbed()
        tb.setup_env(current_version_id='testbed.version')
        tb.activate()
        tb.init_all_stubs()
        self.api = UsersApi()
        self.testbed = tb

    def tearDown(self):
        self.testbed.deactivate()
        super(GaeTestCase, self).tearDown()

    def test_get_returns_entity(self):
        user = User(username='soofaloofa', email='soofaloofa@example.com')
        user.put()

        container = ID_RESOURCE.combined_message_class(id=user.key.id())
        response = self.api.users_get(container)
        self.assertEquals(response.username, 'soofaloofa')
        self.assertEquals(response.email, 'soofaloofa@example.com')
        self.assertEquals(response.id, user.key.id())

    def test_get_returns_404_if_no_entity(self):
        container = ID_RESOURCE.combined_message_class(id=1)
        self.assertRaises(endpoints.NotFoundException, self.api.users_get, container)
```
