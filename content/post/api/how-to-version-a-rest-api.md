---
title: "How to Version a REST API"
date: 2014-04-01T14:16:18Z
tags: 
  - "hypermedia"
  - "rest"
  - "api"
  - "versioning"
aliases:
  - "posts/2014-04-01-how-to-version-a-rest-api/"
---

API versioning is a fact of life. Even the most well designed API changes as new features and relationships are uncovered. Unfortunately, updating an API is seldom as simple as changing the behaviour of our existing URL endpoints on her he server. If we have existing clients we need to explicitly advertise breaking changes in a seamless way. This article explains a few methods of specifying breaking changes that offer a clear upgrade path for existing API clients. 

<!--more-->

## 1) Versioned URL

URL versioning inserts a version number directly in the URL of the resource. As an example,  version one of the API could be accessed through the `v1` URL.

```bash
http://sookocheff.com/api/v1/users/12345
```

Version two of the API could be accessed through the `v2` URL. 

```bash
http://sookocheff.com/api/v2/users/12345
```

This solution has been widely adopted because it is easy to deploy and easy for client developers to understand. This method also makes each API version discoverable and browseable without using an advanced HTTP client — just alter the URL. 

The drawback to using URL versioning is that by changing the URL of a resource with each new API version we are violating the REST constraint that [each resource be accessible via a unique URL][sookocheffrest]. To mitigate this you can map the current version of the API to a non-versioned URL.

```bash
http://sookocheff.com/api/users/
```

Once this mapping is in place you can safely deprecate old URLs by redirecting to the non-versioned URL -- notifying the client to use the latest version. At all times the non-versioned URL represents the latest version of that resource.

### Pros:
- Easy to implement.
- Easy to understand.
- Direct path to deprecation.

### Cons:
- Violates REST principle of unique URLs for a resource.

## 2) Versioned Media Type

When making an HTTP request the client can request a specific MIME type (or list of MIME types) that it is willing to accept using an `Accept` header. For example, an HTML client may use the following `Accept` header to request an HTML representation of the resource.

```
GET /v1/users/12345 HTTP/1.1
Host: sookocheff.com
Accept: text/html
```

Whereas an XML client may use the following `Accept` header to request an XML representation of the resource.

```
GET /v1/users/12345 HTTP/1.1
Host: sookocheff.com
Accept: application/xml
```

We can use this functionality to allow the client to access specific versions of a resource.

```
GET  /users/12345 HTTP/1.1
Host: sookocheff.com
Accept: application/vnd.sookocheff.user+json?version=2
```

This method assumes we have defined a custom media type to represent every resource in our API and that the media type accepts a `version` parameter.

Versioning the media type does adhere to strict REST principles but causes problems in other ways. First, you need a custom media type for every resource returned by your API. This is not only reinventing the wheel -- [perfectly good][schema] [media types][iana] [already exist][mimelist] -- it also creates a media type so specific to your API that it cannot be reused elsewhere. Lastly, it is unclear whether the version parameter applies to the version of the media type or to the version of your API.

### Pros:
- Adheres to REST principles.

### Cons:
- Custom media type for every resource.
- Binds your media type to your API.
- Unclear versioning.
- Requires sophisticated API client.

## 3) Versioned HTTP Header

The [HTTP specification][httpheader] states that unknown HTTP headers MUST be forwarded on to the recipient. This means that custom HTTP headers can be set by our client and received by our API.

```bash
GET /users/12345 HTTP/1.1
Host: sookocheff.com
Accept: application/json
Users-Version: 2
```

A server receiving this request can parse the header to ascertain the version number being requested by the client and return the proper representation. 

This method requires that your API client is able to modify the HTTP headers of its requests. If the client is unable to provide a version number with the HTTP header you can assume that a request is made for the latest API version.

### Pros:
- Adheres to REST principles.

### Cons:
- Requires sophisticated API client.

## 4) Versioned Resources

The last versioning method is to set the version number in the response itself. This places the burden of versioning with the client rather than the server. A client receiving a response from a known version number can parse it and act appropriately. It would be up to the client how to handle an unknown version number.

This method is only appropriate if you as the developer have direct control over both the server and the client being deployed.

### Pros:
- Simplified server.

### Cons:
- Complex client.
- Tightly couples server to client.

## What to do?

By following [REST principles][sookocheffrest] we can guide our API versioning practices while being pragmatic about our choices so that our API can work in the real world.

My recommendation is to combine versioned URLs with custom HTTP headers using the following guidelines. With these guidelines we can safely version our API while supporting existing clients and offering them a clear upgrade path.

1. Each major version of the API recieves a versioned URL.
2. One non-versioned URL always represents the latest API version. 
3. Redirect deprecated URLs to the canonical URL after an advertised grace period.
4. Add a custom HTTP header for the version number.
	- This header specifies both major **and** minor version numbers.
	- The non-versioned URL returns the appropriate version of the resource when specified by the HTTP header.

## References

[Vinay Sahni][1] has collected a long list of best practices for pragmatic API design, including versioning. 

[stackoverflow][2] presents a collection of good answers providing best practices for API versioning.

[1]: http://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api
[2]: http://stackoverflow.com/questions/389169/best-practices-for-api-versioning

[sookocheffrest]: http://sookocheff.com/posts/2014-03-19-how-rest-constraints-affect-api-design/
[schema]: http://schema.org/
[iana]: http://www.iana.org/assignments/media-types/media-types.xhtml
[mimelist]: http://www.freeformatter.com/mime-types-list.html
[httpheader]: http://www.w3.org/Protocols/rfc2616/rfc2616-sec7.html#sec7.1
