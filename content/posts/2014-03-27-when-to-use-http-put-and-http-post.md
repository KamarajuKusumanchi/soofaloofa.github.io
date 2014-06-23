---
title: "When to Use HTTP PUT and HTTP POST"
date: 2014-03-27T07:23:34Z
description: The HTTP protocol defines two methods for updating a resource -- PUT and POST. Both PUT and POST are used to modify a resource and this semantic similarity can confuse API developers. This confusion has led most developers to use POST for any action which may modify the state of a resource, ignoring PUT entirely. This article attempts to explain the semantics behind the PUT and POST methods and offers clear suggestions on when to use each method.
tags: 
  - "hypermedia"
  - "rest"
  - "api"
  - "post"
  - "put"
---

The HTTP protocol defines two methods for updating a resource -- `PUT` and
`POST`. Both `PUT` and `POST` are used to modify a resource and this semantic
similarity can confuse API developers. This confusion has led most developers to
use `POST` for any action which may modify the state of a resource, ignoring
`PUT` entirely.

This article attempts to explain the semantics behind the `PUT` and `POST`
methods and offers clear suggestions on when to use each method.

## PUT

Let's go [straight to the HTTP/1.1 RFC][1] for the [definition of PUT][2].

> The PUT method requests that the enclosed entity be stored under the supplied
> Request-URI. If the Request-URI refers to an already existing resource, the
> enclosed entity SHOULD be considered as a modified version of the one residing
> on the origin server. If the Request-URI does not point to an existing
> resource ... the origin server can create the resource with that URI.

The PUT specification requires that you already know the URL of the resource you
wish to create or update. On create, if the client chooses the identifier for a
resource a PUT request will create the new resource at the specified URL.

```
PUT /user/1234567890 HTTP/1.1
Host: http://sookocheff.com

{
	"name": "Kevin Sookocheff",
	"website": "http://kevinsookocheff.com"
}
```

The server could respond with a `201 Created` status code and the new resource's
location.

``` 
HTTP/1.1 201 Created
Location: /user/1234567890
```

In addition, if you know that a resource already exists for a URL, you can make
a PUT request to that URL to replace the state of that resource on the server.
This example updates the user's website.

```
PUT /user/1234567890 HTTP/1.1
Host: http://sookocheff.com

{
	"name": "Kevin Sookocheff",
	"website": "http://sookocheff.com"
}
```

In general the HTTP PUT method replaces the resource at the current URL with the
resource contained within the request. PUT is used to both create and update the
state of a resource on the server. 

## POST

Let's go [back to the HTTP/1.1 RFC][1] for the [definition of POST][3].

> The POST method is used to request that the origin server accept the entity
> enclosed in the request as a new subordinate of the resource identified by the
> Request-URI ...  The posted entity is subordinate to that URI in the same way
> that a file is subordinate to a directory containing it, a news article is
> subordinate to a newsgroup to which it is posted, or a record is subordinate
> to a database.

Practically speaking, POST is used to append a resource to an existing
collection. In the following example you do not know the actual URL of the
resource -- the server decides the location where it is stored under the
`user` collection. 

```
POST /user HTTP/1.1
Host: http://sookocheff.com

{
    "name": "Bryan Larson",
    "website": "http://www.bryanlarson.ca"
}
```

The server could respond with a `201 Created` status code and the resource's new
location.

```
HTTP/1.1 201 Created
Location: /user/636363
```

Subsequent updates to this user would be made through a `PUT` request to the
specific URL for the user - `/user/636363`.

The book [RESTful Web APIs][4]
classify this behaviour *POST-to-append* and is the generally recommended way to
handle a POST request within the context of a specific resource.

## Putting it Together

The [HTTP/1.1 RFC][1] offers some guidance on [distinguishing between POST and
PUT][2].

> The fundamental difference between the POST and PUT requests is reflected in
> the different meaning of the Request-URI. The URI in a POST request identifies
> the resource that will handle the enclosed entity ...  In contrast, the URI in
> a PUT request identifies the entity enclosed with the request.

By following the existing semantics of the HTTP PUT and POST methods we can
begin to take advantage of the [benefits of REST][5] to write scalable and
robust APIs. Not only is your API ready to scale and easy to maintain, it is
easy to understand and use. By consistently following these existing semantics
you can avoid inserting special cases and 'gotchas' into your API that confuse
client developers.

[1]: http://www.w3.org/Protocols/rfc2616/rfc2616.html
[2]: http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.6
[3]: http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5
[4]: http://www.amazon.ca/RESTful-Web-APIs-Leonard-Richardson/dp/1449358063
[5]: http://sookocheff.com/posts/2014-03-19-how-rest-constraints-affect-api-design/
