---
title: "Creating RESTful APIs with App Engine Cloud Endpoints"
date: 2014-07-02T06:14:23Z
tags: 
  - "app engine"
  - "rest"
  - "api"
  - "cloud endpoints"
---

[App Engine Cloud
Endpoints](https://developers.google.com/appengine/docs/python/endpoints/) can
is a great way to quickly and easily create JSON API endpoints. What's not clear
is how to structure your `Message` code to support a RESTful
create-read-update-delete (CRUD) API. This article will show the basic CRUD
operations for one Resource. The results can easily be adapted to support a full
REST API.

To support this discussion let's use a concrete resource for our API -- a `User`
resource. We can give our `User` model a few simple attributes.

{{% img 2014-07-02-creating-restful-apis-with-app-engine-cloud-endpoints/user-model.png %}}

A CRUD API for this resource would support a URL structure and HTTP verbs
for each operation.

```bash
# Create a new user
HTTP POST /users/

# Read a user by id
HTTP GET /users/{id}

# Update a user by id
HTTP PUT /users/{id}

# Delete a user by id
HTTP DELETE /users/{id}
```

Given our model we can define a basic Cloud Endpoints message representing a `User`.

```python
class UserMessage(messages.Message):
    id = messages.StringField(1)
    email = messages.StringField(2)
    username = messages.StringField(3)
```

Now we can write the **C** (create) portion of our CRUD API using HTTP POST
and a `ResourceContainer` to hold the message we wish to submit to the API.

```python
POST_RESOURCE = endpoints.ResourceContainer(UserMessage)

...

@endpoints.method(POST_RESOURCE,
                  UserMessage,
                  path='/users',
                  http_method='POST',
                  name='users.create')
def create(self, request):
    user = User(username=request.username, email=request.email)
    user.put()
    return user.to_message()
```

Similarly we can define our **R** (read) portion of the API using an HTTP GET
method. To parameterize our cloud endpoint we need to add the parameter to our
`ResourceContainer`. I'll call it `id` here. The actual message type is
`VoidMessage` because we are not passing any information in our request to the
API endpoint other than the `id` parameter.

Our response simply gets the entity from the datastore and returns it as a
message.

```python
ID_RESOURCE = endpoints.ResourceContainer(message_types.VoidMessage,
                                          id=messages.StringField(1,
                                                                  variant=messages.Variant.STRING,
                                                                  required=True))

...

@endpoints.method(ID_RESOURCE,
                  UserMessage,
                  http_method='GET',
                  path='users/{id}',
                  name='users.read')
def read(self, request):
    entity = User.get_by_id(request.id)
    if not entity:
        message = 'No User with the id "%s" exists.' % request.id
        raise endpoints.NotFoundException(message)

    return entity.to_message()
```

The **U** (update) operation uses a similar parameterized `ResourceContainer` to
access a User given an id. We augment this request with the `UserMessage` which
defines the content of the body of the message. The endpoint takes the content
of the message and updates the entity with that content.

```python
PUT_RESOURCE = endpoints.ResourceContainer(UserMessage,
                                           id=messages.StringField(1,
                                                                   variant=messages.Variant.STRING,
                                                                   required=True))

...

@endpoints.method(PUT_RESOURCE,
                  UserMessage,
                  http_method='PUT',
                  path='users/{id}',
                  name='users.update')
def update(self, request):
    entity = User.update_from_message(request.id, request)
    if not entity:
        message = 'No User with the id "%s" exists.' % request.id
        raise endpoints.NotFoundException(message)

    return entity.to_message()
```

Lastly, the **D** (delete) endpoint takes an identifier which we have previously
defined as `ID_RESOURCE`. The endpoint deletes the entity referred to by that
identifier and returns a `VoidMessage` which is converted to an `HTTP 204 No
Content` response by the cloud endpoints API.

```python
@endpoints.method(ID_RESOURCE,
                  message_types.VoidMessage,
                  http_method='DELETE',
                  path='users/{id}',
                  name='users.delete')
def delete(self, request):
    entity = User.get_by_id(request.id)
    if not entity:
        message = 'No User with the id "%s" exists.' % request.id
        raise endpoints.NotFoundException(message)

    entity.key.delete()
    return message_types.VoidMessage()
```

This basic pattern can be used with any resource that your API wishes to
support and gives a basic pattern with which to build out your full API.

If you have any questions please send me an email or let me know in the
comments!
