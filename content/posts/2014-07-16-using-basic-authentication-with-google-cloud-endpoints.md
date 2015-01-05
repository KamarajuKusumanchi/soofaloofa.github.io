---
title: "Using Basic Authentication with Google Cloud Endpoints"
date: 2014-07-16T01:02:25Z
tags: 
  - "app engine"
  - "cloud endpoints"
  - "authentication"
series:
  - "Cloud Endpoints"
---

Cloud Endpoints provides strong integration with OAuth 2.0. If you can use this
integration -- do it. However, some legacy systems require supporting
alternative authentication mechanisms. This article will show you how to secure
an API endpoint using Basic Authentication. You can use this as a starting point
for whatever authentication method you choose.

<!--more-->

## A basic endpoint

As a starting point let's define a basic endpoint that will return a
hypothetical UserMessage defining a User resource.

```python
@endpoints.api(name='users', version='v1', description='Users Api')
class UsersApi(remote.Service):

    @endpoints.method(message_types.VoidMessage,
                      UserMessage,
                      http_method='GET',
                      path='user',
                      name='user.auth')
    def user_auth(self, request):
        ## Return a UserMessage
```

Let's build up a UserMessage based on the credentials set in the HTTP
Authorization header. We can access the HTTP headers of the request through the
[HTTPRequestState](https://developers.google.com/appengine/docs/python/tools/protorpc/remote/httprequeststateclass)
using the instance variable `request_state`.

```python
@endpoints.api(name='users', version='v1', description='Users Api')
class UsersApi(remote.Service):

    @endpoints.method(message_types.VoidMessage,
                      UserMessage,
                      http_method='GET',
                      path='user',
                      name='user.auth')
    def user_auth(self, request):
        basic_auth = self.request_state.headers.get('authorization')
        print basic_auth
        ## Return a UserMessage
```

We can test this endpoint using [httpie](https://github.com/jakubroztocil/httpie).

```
http -a username:password GET :8888/_ah/api/users/v1/user
```

Examing the logs will show that we receive the HTTP Authorization header in its
base64 encoded form.

```python
Basic dXNlcm5hbWU6cGFzc3dvcmQ=
```

The header can be decoded with the `base64` module.

```python
basic_auth = self.request_state.headers.get('authorization')
auth_type, credentials = basic_auth.split(' ')
print base64.b64decode(credentials)  # prints username:password
```

Using the username and password we can check the datastore for a User model with
the same credentials and return a `UserMessage` based on the model .

```python
@endpoints.api(name='users', version='v1', description='Users Api')
class UsersApi(remote.Service):

    @endpoints.method(message_types.VoidMessage,
                      UserMessage,
                      http_method='GET',
                      path='user',
                      name='user.auth')
    def user_auth(self, request):
        basic_auth = self.request_state.headers.get('authorization')
        auth_type, credentials = basic_auth.split(' ')
        username, password = base64.b64decode(credentials).split(':')
        user = User.get_by_username(username)
        if user and user.verify_password(password):
            return user.to_message()
        else:
            raise endpoints.UnauthorizedException
```

This should serve as a starting point for anyone wishing to use Basic
Authentication with Google Cloud Endpoints. If you've read this far, why not
subscribe to this blog through [email](http://kevinsookocheff.us3.list-manage2.com/subscribe?u=8b57d632b8677f07ca57dc9cb&id=ec7ddaa3ba) or [RSS](http://sookocheff.com/index.xml)?
