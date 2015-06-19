---
title: "How REST Constraints Affect API Design"
date: 2014-03-19T14:11:48Z
tags: 
  - "hypermedia"
  - "rest"
  - "api"
aliases:
  - "posts/2014-03-19-how-rest-constraints-affect-api-design/"
---

REST was developed and formalized by analyzing the existing Web and extracting
the principles that made it work. This set of principles was written down in the
[Fielding dissertation](https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm)
which lays out the set of constraints that, when enforced, will make a generic
network system into a resilient network like the Web. In Chapter 5 of the
dissertation Fielding outlines REST's interface constraints.

> REST is defined by four interface constraints: identification of resources;
> manipulation of resources using representations; self-descriptive messages;
> and, hypermedia as the engine of application state. 

<!--more-->

### Why is this important for APIs?

API design is in an infancy. Each API is designed for a single use case and
standard. This proliferation of different design ideas results in APIs that each
have their own specification and semantics. Interoperability between APIs is
nonexistent. In the early 1990s the Web was facing this exact problem and as a
result the principles of REST were formalized and adopted. The result was a
scalable, resilient and ultimately successful system. By adopting the REST
constraints in our APIs we can take advantage of this foundational work to
provide APIs that form a scalable and resilient network of their own.

We will take a look at each of the four interface constraints in turn and see
how they can be used to design an API that developers love to use. 

## 1. Identification of Resources

A resource is the key abstraction of REST. Anything that can be named is a
resource -- a video, a document, an image. We identify resources using a
unique identifier called the Uniform Resource Identifier (URI). When discussing
the Web we almost always use a more specific form of URI called a Uniform
Resource Locator (URL). A URL is what we are all accustomed to when accessing
sites through HTTP.

```
http://sookocheff.com
```

By building REST APIs over HTTP we can use URLs to identify resources accessed
by our API. To satisfy the REST constraint each URL should map to a single
resource and all access to this resource is done through that URL. As an
example, if I want to offer an API for a shipping application I might have a
resource representing an order. A URL for the individual order numbered `12345`
would have a path including the order number.

```
/orders/12345
```

This simple example shows how we can create a URL to uniquely represent each
resource represented by our API.

## 2. Manipulation of Resources Using Representations

When making a request for a resource the server responds with a representation
of the resource. This representation captures the current state of the resource
in a format that the client can understand and manipulate. Abstractly the
representation is a sequence of bytes along with metadata describing those
bytes. This metadata is known as the media type of the representation. Typical
API examples are HTML, JSON, and XML. Because the server sends a representation
of the resource it is possible for the client to request a specific
representation that fits the client's needs. For example, a client can ask for
the JSON representation of a resource or the XML representation of the resource.
The server may provide this representation if it is capable of doing so. This
concept is called *content negotiation*. You can use content negotiation in your
API to allow multiple clients to access a different representations of the
resource from the same URL. 

The way that a client asks for a specific representation is through the HTTP
`Accepts` header. The following request is asking for a plain text
representation of the order.

```
GET /orders/12345
Accept: text/plain
```

Whereas the following request is asking for a JSON representation.

```
GET /orders/12345
Accept: application/json
```

By using content negotiation in your API you can offer new resource
representations without changing the resources URL or breaking existing clients.
This keeps your client and server flexibly decoupled.

## 3. Self-Descriptive Messages

The representations served by a RESTful system contain all of the data required
by the client to understand and act on the resource. If any additional
information is needed but not contained in the response a link to that
information should be provided within the response. This means that the media
type you choose to respond with should be self documenting and should list any
related resources or actions that the client may be interested in. Specific
media types already exist for JSON APIS. These include HAL, JSON-LD,
Collection+JSON and JSONAPI. For a detailed discussion see my post [on
choosing a hypermedia
type](http://sookocheff.com/posts/2014-03-11-on-choosing-a-hypermedia-format/)

Each of these formats are aiming to solve the [same
problem](https://www.w3.org/TR/json-ld/#basic-concepts):

> JSON has no built-in support for hyperlinks, which are a fundamental building
> block on the Web. 

By linking together the resources offered by your API you can build
self-descriptive APIs.

## 4. Hypermedia As The Engine Of Application State

Together, the first three REST constraints imply the fourth and most important --
hypermedia as the engine of application state. By uniquely identifying
resources, using representations to communicate resource state and using
self-descriptive messages via media types all application state stays with the
client. 

By keeping all application state with the client a direct connection between the
client and the server is not necessary, allowing the server to scale to serve
many clients with minimal resources. This ability to scale has led to the Web's
scalable and resilient nature. 

By designing our APIs with the REST constraints we can build scalable and
resilient APIs.
