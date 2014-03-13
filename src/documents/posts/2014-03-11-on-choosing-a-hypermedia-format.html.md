---
layout: post
title: "On choosing a hypermedia type for your API - HAL, JSON-LD, Collection+JSON, SIREN, Oh My!"
author: Kevin Sookocheff
date: 2014/03/11
updated: 2014/03/13
tags: 
  - Hypermedia
  - REST
  - API
---

In recent years REST has been at the forefront of modern API 
design. This has led to APIs with manageable URLs that respect the HTTP verbs
(GET, POST, PUT [and the
 rest](http://www.w3.org/Protocols/rfc2616/rfc2616.html)), producing an
intuitive model for client developers. Unfortunately, there are two problems
that REST doesn't solve alone.

The first problem is standardized responses. Most every enterprise has defined
their own custom API format, usually a JSON response that maps neatly to their
own data model. A Facebook API client cannot communicate with a Twitter API and
vice versa. This leads to a proliferation of API clients that do almost -- but
not quite -- the same thing. Duplication of effort abounds.

The second problem is linking. As the [W3C puts it](https://www.w3.org/TR/json-ld/#basic-concepts):

> JSON has no built-in support for hyperlinks, which are a fundamental building
> block on the Web. 

The drawback of this is that two API endpoints are only linked together by API
documentation. As a user you are forced to scour through walls of API
documentation to understand the relationships between API endpoints and grasp
exactly what actions you can and cannot take against a given resource.

To solve these problems we can look at how we structure our API responses. By
using *hypermedia* in our responses we can offer links between API endpoints and
documentation, potential actions, and related endpoints. This allows for
*discoverable* APIs where it is clear from the API response the set of next
actions that a client may want to take. Furthermore, by *standardizing* on a
hypermedia type clients developed for one API can understand the format of
another API and communicate with minimal duplicated effort.

In this post I will evaluate a few mature hypermedia types for APIs,
offering a side-by-side comparison of their strengths and weaknesses. If you
are impatient for the final result you can [jump straight to the code](https://gist.github.com/soofaloofa/9350847). 

### The Model

To drive this discussion let's use a hypothetical API for managing a `Player`
resource derived from the [`GKPlayer`
class](https://developer.apple.com/library/ios/documentation/GameKit/Reference/GKPlayer_Ref/Reference/Reference.html#//apple_ref/occ/cl/GKPlayer)
used by Apple's GameCenter API. The `Player` resource can be expressed with this
simple diagram.

![PlayerResource](/img/2014-02-06-on-choosing-a-hypermedia-format/player-model.png "Player Resource")

Representing this as a typical JSON response would yield something like the
following.

```bash
GET https://api.example.com/player/1234567890
```

```json
{
    "playerId": "1234567890",
    "alias": "soofaloofa",
    "displayName": "Kevin Sookocheff",
    "profilePhotoUrl": "https://api.example.com/player/1234567890/avatar.png"
}
```

And the list of this player's friends could be retrieved with a separate API call.

```bash
GET https://api.example.com/player/1234567890/friends
```

```json
[
{
    "playerId": "1895638109",
    "alias": "sdong",
    "displayName": "Sheldon Dong",
    "profilePhotoUrl": "https://api.example.com/player/1895638109/avatar.png"
},
{
    "playerId": "8371023509",
    "alias": "mliu",
    "displayName": "Martin Liu",
    "profilePhotoUrl": "https://api.example.com/player/8371023509/avatar.png"
}
]
```

Let's take a look at how this API can be represented using hypermedia types.

### JSON-LD

We'll start by looking at JSON for Linked Documents (JSON-LD). JSON-LD is a well
supported media type endorsed by the [World Wide Web
Consortium](https://www.w3.org).

The selling point of JSON-LD is that you can adopt the standard without
introducing breaking changes to your API. The syntax is designed to not disturb
already deployed systems and to provide a smooth migration path from JSON to
JSON with added semantics. 

JSON-LD introduces keywords that augment an existing response with additional
information. The most important augmentation is the *context*. A context in
JSON-LD defines a set of terms that are scoped and valid within the
representation being discussed. A context is assigned to a JSON response using
the `@context` keyword.

```json
{
  "@context": {}
}
```  

Within the context properties are assigned to a URL that provides documentation
about the meaning of that property.

```json
{
    "@context": {
        "displayName": "https://schema.org/name"
    },
    "displayName": "Kevin Sookocheff"
}
```

It's a good idea to use standard naming for our APIs so we can go ahead and
rename `displayName` to `name`.

```json
{
    "@context": {
        "name": "https://schema.org/name"
    },
    "name": "Kevin Sookocheff"
}
```

At this point we have an unambiguous definition of what the property `name`
means within the API response by visiting `https://schema.org/name` to read the
semantics of this property. We can go further and add context to the rest of the
properties. To be consistent with existent naming we will change
`profilePhotoUrl` to `image` and `alias` to `alternateName`.

```bash
GET https://api.example.com/player/1234567890
```

```json
{
    "@context": {
        "name": "https://schema.org/name",
        "alternateName": "https://schema.org/alternateName",
        "image": {
            "@id": "https://schema.org/image",
            "@type": "@id"
        }
    },
    "@id": "https://api.example.com/player/1234567890",
    "playerId": "1234567890",
    "name": "Kevin Sookocheff",
    "alternateName": "soofaloofa",
    "image": "https://api.example.com/player/1234567890/avatar.png"
}
```

In this example we've added the `@id` annotation. `@id` signifies *identifiers*.
Identifiers allow unique external references to any resource, providing similar
semantcis to URLs. In JSON-LD terminology every distinct resource is a node in
the JSON-LD graph. These distinct nodes should have identifiers that can be used
to retrieve a representation of that node. 

The last element from our model that is missing from our JSON-LD response is the
list of friends. With JSON-LD unordered lists can be specified using simple
array notation.  In this example we will represent friends by the identifiers
that point to their resources. An HTTP GET request to those URLs would return
the full representation of each friend.

```bash
GET https://api.example.com/player/1234567890
```

```json
{
    "@context": {
        "name": "https://schema.org/name",
        "alternateName": "https://schema.org/alternateName",
        "image": {
            "@id": "https://schema.org/image",
            "@type": "@id"
        },
        "friends": {
            "@container": "@set"
         }
    },
    "@id": "https://api.example.com/player/1234567890",
    "playerId": "1234567890",
    "name": "Kevin Sookocheff",
    "alternateName": "soofaloofa",
    "image": "https://api.example.com/player/1234567890/avatar.png",
    "friends": [ 
        {
            "@id": "https://api.example.com/player/1895638109"
        },
        {
            "@id": "https://api.example.com/player/8371023509"
        }
    ]
}
```

This gives us the representation of our `Player` resource in JSON-LD. This
example doesn't cover all of JSON-LD but should give you a flavour of how the
format can be used. 

Thanks to [Markus Lanthaler](https://twitter.com/markuslanthaler) for offering
suggestions on how to simplify this even more. In this example we define a
`@vocab` for our context that encompasses the terms that we use within our
response. Our list of friends is provided as a simple link to a separate
endpoint.

```bash
GET https://api.example.com/player/1234567890
```

```
{
    "@context": {
        "@vocab": "https://schema.org/",
        "image": { "@type": "@id" },
        "friends": { "@type": "@id" }
    },
    "@id": "https://api.example.com/player/1234567890",
    "playerId": "1234567890",
    "name": "Kevin Sookocheff",
    "alternateName": "soofaloofa",
    "image": "https://api.example.com/player/1234567890/avatar.png",
    "friends": "https://api.example.com/player/1234567890/friends"
}
```

If you want to dive fully into JSON-LD you can always read
the [specification](https://www.w3.org/TR/json-ld/).

JSON-LD lacks support for specifying the actions you can take on a resource. To
address this short-coming [HYDRA](http://www.markus-lanthaler.com/hydra/)
provides a vocabulary allowing client-server communication using the JSON-LD
message format.

To specify the actions available on a resource we would use the `operation`
property.

```bash
GET https://api.example.com/player/1234567890/friends
```
 
```json
{
    "@context": [
        "http://www.w3.org/ns/hydra/core",
        {
            "@vocab": "https://schema.org/",
            "image": { "@type": "@id" },
            "friends": { "@type": "@id" }
        }
    ],
    "@id": "https://api.example.com/player/1234567890/friends",
    "operation": {
        "@type": "BefriendAction",
        "method": "POST",
        "expects": {
            "@id": "http://schema.org/Person",
            "supportedProperty": [
                { "property": "name", "range": "Text" },
                { "property": "alternateName", "range": "Text" },
                { "property": "image", "range": "URL" }
            ]
        }
    }
}
```

The `operation` property defines a `method` term that specifies the HTTP method
that the endpoint allows. HYDRA also provides a template of the expected
properties and their data types. In our example a POST request to
`https://api.example.com/player/1234567890/friends` (the resource's URL) will
add a new friend to our user's friend list.

HYDRA also provides a `member` property that allows us to embed additional
resources within our current representations. In the following example we embed
our friends directly within the resource as a list. 

```
GET https://api.example.com/player/1234567890/friends
```

```
{
    "@context": [
        "http://www.w3.org/ns/hydra/core",
        {
            "@vocab": "https://schema.org/",
            "image": { "@type": "@id" },
            "friends": { "@type": "@id" }
        }
    ],
    "@id": "https://api.example.com/player/1234567890/friends",
    "operation": {
        "@type": "BefriendAction",
        "method": "POST",
        "expects": {
            "@id": "http://schema.org/Person",
            "supportedProperty": [
                { "property": "name", "range": "Text" },
                { "property": "alternateName", "range": "Text" },
                { "property": "image", "range": "URL" }
            ]
        }
    },
    "member": [
            {
                "@id": "https://api.example.com/player/1895638109",
                "name": "Sheldon Dong",
                "alternateName": "sdong",
                "image": "https://api.example.com/player/1895638109/avatar.png",
                "friends": "https://api.example.com/player/1895638109/friends"
            },
            {
                "@id": "https://api.example.com/player/8371023509",
                "name": "Martin Liu",
                "alternateName": "mliu",
                "image": "https://api.example.com/player/8371023509/avatar.png",
                "friends": "https://api.example.com/player/8371023509/friends"
            }
        ],
    "nextPage": "https://api.example.com/player/1234567890/friends?page=2"
}
```

We've also added a `nextPage` property which is a property defined by HYDRA for
paged collections. For more details on HYDRA's reserved properties you can read
the full
[documentation](http://www.markus-lanthaler.com/hydra/spec/latest/core/#properties).

### HAL

HAL is a lightweight media type that uses the idea of *Resources* and *Links* to
model your JSON responses. *Resources* can contain *State* defined by key-value
pairs of data, *Links* leading to additional resources and *Embedded Resources*
which are children of the current resource embedded in the representation for
convenience.

HAL is simple to use and easy to understand. These virtues have lead HAL to
become one of the leading hypermedia types in modern APIs.

#### State

State is the traditional JSON key-value pairs defining the current state of the
resource.

```bash
GET https://api.example.com/player/1234567890
```

```json
{
    "playerId": "1234567890",
    "name": "Kevin Sookocheff",
    "alternateName": "soofaloofa",
    "image": "https://api.example.com/player/1234567890/avatar.png"
}
```

#### Links

Links in HAL are identified as a JSON object named `_links`. Keys within
`_links` are the name of the link and should describe the relationship between
the current resource and the link. At a minimum the `_links` property should
contain a `self` entry pointing to the current resource.

```bash
GET https://api.example.com/player/1234567890
```

```json
{
    "_links": {
        "self": { "href": "https://api.example.com/player/1234567890" }
    },
    "playerId": "1234567890",
    "name": "Kevin Sookocheff",
    "alternateName": "soofaloofa",
    "image": "https://api.example.com/player/1234567890/avatar.png"
}
```

We can easily add a link to the `Friends` resource which can be used to retrieve
the full list.

```bash
GET https://api.example.com/player/1234567890
```

```json
{

    "_links": {
        "self": { "href": "https://api.example.com/player/1234567890" },
        "friends": { "href": "https://api.example.com/player/1234567890/friends" }
    },
    "playerId": "1234567890",
    "name": "Kevin Sookocheff",
    "alternateName": "soofaloofa",
    "image": "https://api.example.com/player/1234567890/avatar.png"
}
```

#### Embedded Resources

Making a GET request to the `Friends` link would return a full list of
`Player` resources. Each `Player` returned is embedded in the representation
as an *Embedded Resource*. *Embedded Resources* augment the current resource
state with additional, related resources. These resources are provided as a
convenience to the client application and can be easily used to represent a
list of items.

```bash
GET https://api.example.com/player/1234567890/friends
```

```json
{
    "_links": {
        "self": { "href": "https://api.example.com/player/1234567890/friends" },
        "next": { "href": "https://api.example.com/player/1234567890/friends?page=2" }
    },
    "size": "2",
    "_embedded": { 
        "player": [
            { 
                "_links": { 
                    "self": { "href": "https://api.example.com/player/1895638109" },
                    "friends": { "href": "https://api.example.com/player/1895638109/friends" }
                },
                "playerId": "1895638109",
                "name": "Sheldon Dong",
                "alternateName": "sdong",
                "image": "https://api.example.com/player/1895638109/avatar.png"
            },
            { 
                "_links": { 
                    "self": { "href": "https://api.example.com/player/8371023509" },
                    "friends": { "href": "https://api.example.com/player/8371023509/friends" }
                },
                "playerId": "8371023509",
                "name": "Martin Liu",
                "alternateName": "mliu",
                "image": "https://api.example.com/player/8371023509/avatar.png"
            }
        ]
    }
}
```

In this response we've added a `next` link to represent a paged collection and
provide a reference to get the next set of friends in the list. The embedded
resources are a list contained within the `player` property. 

#### Curies

An important point about HAL is that each link relation points to a URL with
documentation about that relation. This makes the API discoverable by always
providing documentation about the links available from the current resource. In
the next example a URL for `friends` points to documentation about that resource.

```bash
GET https://api.example.com/player/1234567890
```

```json
{
    "_links": {
        "self": { "href": "https://api.example.com/player/1234567890" },
        "https://api.example.com/docs/rels/friends": { "href": "https://api.example.com/player/1234567890/friends" }
    },
    "playerId": "1234567890",
    "name": "Kevin Sookocheff",
    "alternateName": "soofaloofa",
    "image": "https://api.example.com/player/1234567890/avatar.png"
}
```

Since URLs are long and unwieldy, HAL provides `curies`. `Curies` are a reserved
link relation acting as a base URL that is expanded upon by each term. In this
example we will define a `curie` `ex` that references the `URI` 
`https://api.example.com/docs/rels/{rel}`. `Curies` are expanded by postfixing
the curie name with a `:` followed by the name of the resource.

```bash
GET https://api.example.com/player/1234567890
```

```json
{
    "_links": {
        "self": { "href": "https://api.example.com/player/1234567890" },
        "curies": [{ "name": "ex", "href": "https://api.example.com/docs/rels/{rel}", "templated": true }],
        "ex:friends": { "href": "https://api.example.com/player/1234567890/friends" }
    },
    "playerId": "1234567890",
    "name": "Kevin Sookocheff",
    "alternateName": "soofaloofa",
    "image": "https://api.example.com/player/1234567890/avatar.png"
}
```

HAL's lightweight syntax and model make it a popular choice for API developers
and users. For more information on HAL you can refer to the draft standard that
has been submitted to the [Network Working
Group](https://tools.ietf.org/html/draft-kelly-json-hal-06). 

### Collection+JSON

The Collection+JSON standard is a media type that standardizes the reading,
writing and querying of items in a collection. Although geared to handling
collections, by representing a single item as a collection of one element,
Collection+JSON can elegantly handle most API responses.

At a minimum a Collection+JSON response must contain a `collection` object with
a `version` and a URI pointing to itself.

```bash
GET https://api.example.com/player/1234567890
```

```json
{
    "collection": {
        "version": "1.0",
        "href": "https://api.example.com/player/1234567890"
    }
}
```

#### Returning Data

Typically, the response would include a list of items in the collection. For a
single resource, this collection would be a list of a single element. The
properties of each element are given by explicit name/value pairs within a
`data` attribute as in the following example.

```bash
GET https://api.example.com/player/1234567890
```

```json
{
    "collection": {
        "version": "1.0",
        "href": "https://api.example.com/player",
        "items": [
            {
                "href": "https://api.example.com/player/1234567890",
                "data": [
                      { "name": "playerId", "value": "1234567890", "prompt": "Identifier" },
                      { "name": "name", "value": "Kevin Sookocheff", "prompt": "Full Name" },
                      { "name": "alternateName", "value": "soofaloofa", "prompt": "Alias" }
                ]
            }
        ]
    }
}
```

#### Links

Links can be a property of the collection or of individual items in the
collection. Links may may also include a `name` and a `prompt` which can be
useful when creating HTML forms to reference the collection or item.

In this example we will add links for the `Players` avatar and friends.

```bash
GET https://api.example.com/player/1234567890
```

```json
{
    "collection": {
        "version": "1.0",
        "href": "https://api.example.com/player",
        "items": [
            {
                "href": "https://api.example.com/player/1234567890",
                "data": [
                      {"name": "playerId", "value": "1234567890", "prompt": "Identifier"},
                      {"name": "name", "value": "Kevin Sookocheff", "prompt": "Full Name"},
                      {"name": "alternateName", "value": "soofaloofa", "prompt": "Alias"}
                ],
                "links": [
                    {"rel": "image", "href": "https://api.example.com/player/1234567890/avatar.png", "prompt": "Avatar", "render": "image" },
                    {"rel": "friends", "href": "https://api.example.com/player/1234567890/friends", "prompt": "Friends" }
                ]
            }
        ]
    }
}
```

#### Templates

As the name would imply, Collection+JSON is uniquely suited to handling
collections. Templates are one aspect of this. A template is an object that
represents an item in the collection. The client can then fill in this template
and POST it to the collection to add an element, or PUT it to update an existing
item.

In this example we define a template for adding to the user's list of friends.

```bash
GET https://api.example.com/player/1234567890/friends
```

```json
{
    "collection":
    {
        "version": "1.0",
        "href": "https://api.example.com/player/1234567890/friends",
        "links": [
            {"rel": "next", "href": "https://api.example.com/player/1234567890/friends?page=2"}
        ],
        "items": [
            {
                "href": "https://api.example.com/player/1895638109",
                "data": [
                      {"name": "playerId", "value": "1895638109", "prompt": "Identifier"},
                      {"name": "name", "value": "Sheldon Dong", "prompt": "Full Name"},
                      {"name": "alternateName", "value": "sdong", "prompt": "Alias"}
                ],
                "links": [
                    {"rel": "image", "href": "https://api.example.com/player/1895638109/avatar.png", "prompt": "Avatar", "render": "image" },
                    {"rel": "friends", "href": "https://api.example.com/player/1895638109/friends", "prompt": "Friends" }
                ]
            },
            {
                "href": "https://api.example.com/player/8371023509",
                "data": [
                      {"name": "playerId", "value": "8371023509", "prompt": "Identifier"},
                      {"name": "name", "value": "Martin Liu", "prompt": "Full Name"},
                      {"name": "alternateName", "value": "mliu", "prompt": "Alias"}
                ],
                "links": [
                    {"rel": "image", "href": "https://api.example.com/player/8371023509/avatar.png", "prompt": "Avatar", "render": "image" },
                    {"rel": "friends", "href": "https://api.example.com/player/8371023509/friends", "prompt": "Friends" }
                ]
            }
        ],
        "template": {
            "data": [
                {"name": "playerId", "value": "", "prompt": "Identifier"},
                {"name": "name", "value": "", "prompt": "Full Name"},
                {"name": "alternateName", "value": "", "prompt": "Alias"},
                {"name": "image", "value": "", "prompt": "Avatar"}
            ]
        }
        
    }
}
```

To add a friend to this collection you would POST the data specified by the
template to the `href` link defined by the collection
(`https://api.example.com/player/1234567890/friends`).

#### Queries

The final piece of Collecion+JSON is the `queries` property. Queries, as the
name implies, define the queries that are supported by this collection. Here the
`data` object specifies the query parameters supported by the server.

```bash
GET https://api.example.com/player/1234567890/friends
```

```json
{
    "collection":
    {
        "version": "1.0",
        "href": "https://api.example.com/player/1234567890/friends",
        "links": [
            {"rel": "next", "href": "https://api.example.com/player/1234567890/friends?page=2"}
        ],
        "items": [
            {
                "href": "https://api.example.com/player/1895638109",
                "data": [
                      {"name": "playerId", "value": "1895638109", "prompt": "Identifier"},
                      {"name": "name", "value": "Sheldon Dong", "prompt": "Full Name"},
                      {"name": "alternateName", "value": "sdong", "prompt": "Alias"}
                ],
                "links": [
                    {"rel": "image", "href": "https://api.example.com/player/1895638109/avatar.png", "prompt": "Avatar", "render": "image" },
                    {"rel": "friends", "href": "https://api.example.com/player/1895638109/friends", "prompt": "Friends" }
                ]
            },
            {
                "href": "https://api.example.com/player/8371023509",
                "data": [
                      {"name": "playerId", "value": "8371023509", "prompt": "Identifier"},
                      {"name": "name", "value": "Martin Liu", "prompt": "Full Name"},
                      {"name": "alternateName", "value": "mliu", "prompt": "Alias"}
                ],
                "links": [
                    {"rel": "image", "href": "https://api.example.com/player/8371023509/avatar.png", "prompt": "Avatar", "render": "image" },
                    {"rel": "friends", "href": "https://api.example.com/player/8371023509/friends", "prompt": "Friends" }
                ]
            }
        ],
        "queries": [
            {
                "rel": "search", "href": "https://api.example.com/player/1234567890/friends/search", "prompt": "Search",
                "data": [
                    {"name": "search", "value": ""}
                ]
            }
        ],
        "template": {
            "data": [
                {"name": "playerId", "value": "", "prompt": "Identifier" },
                {"name": "name", "value": "", "prompt": "Full Name"},
                {"name": "alternateName", "value": "", "prompt": "Alias"},
                {"name": "image", "value": "", "prompt": "Avatar"}
            ]
        }
    }
}
```

By defining the template and queries within the response Collection+JSON makes
navigation by a new API user relatively simple without needing to understand the
full meaning of the API. It also provides a level of interoperability between
APIs using the Collection+JSON media type. Collection+JSON was designed by [Mike
Amundsen](https://amundsen.com). You can find detailed examples, the full spec
and sample code [on his website](https://amundsen.com/media-types/collection/).

### SIREN

The last media type we'll look at is
[SIREN](https://github.com/kevinswiber/siren). SIREN aims to represent generic
entities along with actions for modifying those entities and links for client
navigation.

#### Entities

Each SIREN entity may have an optional class that describes the nature of the
entity. This class defines the type of resource being returned by the API.
Think of this as a data model for your API. By defining our response as
returning a `player` class the API user can immediately gain insight about the
data being returned.

```bash
GET https://api.example.com/player/1234567890
```

```json
{
    "class": "player"
}
```

#### Properties

The state of the entity is reflected as key-value pairs in a `properties` object.

```
{
    "class": "player",
    "properties": {
        "playerId": "1234567890",
        "name": "Kevin Sookocheff",
        "alternateName": "soofaloofa",
        "image": "https://api.example.com/player/1234567890/avatar.png"
    }
}
```

#### Links

Links are used in the same sense we've already seen in other media types --
navigating to related resources. With SIREN links have a relation and a URL.

```bash
GET https://api.example.com/player/1234567890
```

```json
{
    "class": "player",
    "links": [
        { "rel": [ "self" ], "href": "https://api.example.com/player/1234567890" },
        { "rel": [ "friends" ], "href": "https://api.example.com/player/1234567890/friends" }
    ],
    "properties": {
        "playerId": "1234567890",
        "name": "Kevin Sookocheff",
        "alternateName": "soofaloofa",
        "image": "https://api.example.com/player/1234567890/avatar.png"
    }
}
```

#### Actions

One of the biggest pieces missing from common Hypermedia types is the ability to
dictate what requests can be made to alter the application state. SIREN
facilitates this by defining `actions` that a client can take on the given
resource.

SIREN actions show the available HTTP request method and includes the URL for
the request along with fields or variables that the URL accepts. As an example,
our resource for listing a players friends can offer an action to add a
friend to the list, or search for a friend.

```bash
GET https://api.example.com/player/1234567890/friends
```

```json
{
    "class": "player",
    "links": [
        {"rel": [ "self" ], "href": "https://api.example.com/player/1234567890/friends"},
        {"rel": [ "next" ], "href": "https://api.example.com/player/1234567890/friends?page=2"}
    ],
    "actions": [{
        "class": "add-friend",
        "href": "https://api.example.com/player/1234567890/friends",
        "method": "POST",
        "fields": [
            {"name": "name", "type": "string"},
            {"name": "alternateName", "type": "string"},
            {"name": "image", "type": "href"}
        ]
    }],
    "properties": {
        "size": "2"
    },
    "entities": [
        { 
            "links": [ 
                {"rel": [ "self" ], "href": "https://api.example.com/player/1895638109"},
                {"rel": [ "friends" ], "href": "https://api.example.com/player/1895638109/friends"}
            ],
            "properties": {
                "playerId": "1895638109",
                "name": "Sheldon Dong",
                "alternateName": "sdong",
                "image": "https://api.example.com/player/1895638109/avatar.png"
            }
        },
        { 
            "links": [
                {"rel": [ "self" ], "href": "https://api.example.com/player/8371023509"},
                {"rel": [ "friends" ], "href": "https://api.example.com/player/8371023509/friends" }
            ],
            "properties": {
                "playerId": "8371023509",
                "name": "Martin Liu",
                "alternateName": "mliu",
                "image": "https://api.example.com/player/8371023509/avatar.png"
            }
        }
    ]
}
```

#### Entities

The previous example also introduces `entities` to the response.  Any related
entities that you wish to embed in the current representation are entered as a
list of `entities`. Entities are nested. Each entity in this list can have a
class, properties and additional entities.

### Conclusions

I've create a [Gist](https://gist.github.com/soofaloofa/9350847) comparing each
of the media types discussed in this post. 

After going through this exercise I've come to a few conclusions. 

#### JSON-LD

JSON-LD is great for augmenting existing APIs without introducing
breaking changes. This augmentation mostly serves as a way to self document your
API. If you are looking to add operations to a JSON-LD response look to HYDRA.
HYDRA adds a vocabulary for communicating using the JSON-LD specification. This
is an interesting choice as it decouples the API serialization format from the
communication format.

#### HAL

The light weight syntax and semantics of HAL is appealing in a lot of contexts.
HAL is a minimal representation that offers most of the benefits of using a
hypermedia type without adding too much complexity to the implementation. One
area where HAL falters is, like JSON-LD, the lack of support for specifying
actions.

#### Collection+JSON

Don't be fooled by the name. Collection+JSON can be used to represent single
items as well and it does this quite well. Of course it shines when representing
data collections. Particularly appealing is the ability to list queries that
your collection supports and templates that clients can use to alter your
collection. For publishing user editable data Collection+JSON shines.

#### SIREN

SIREN attempts to represent generic classes of items and overcome the main
drawback of HAL -- support for actions. It does this admirably well and also
introduces the concept of classes to your model bringing a sense of type
information to your API responses.

#### And the winner is?

Unfortunately, there is no clear winner. It depends on the contraints in place
on your API. However, I will offer some suggestions.

If you are augmenting existing API responses choose JSON-LD.
If you are keeping it simple choose HAL.
If you are looking for a full featured media type choose Collection+JSON.

Did I cover all the bases? Completely miss the mark? Let me know in the
comments!
