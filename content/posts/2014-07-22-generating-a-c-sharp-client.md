---
title: "Generating a C# client for an App Engine Cloud Endpoints API"
date: 2014-07-22T06:29:56Z
tags: 
  - "app engine"
  - "cloud endpoints"
  - "authentication"
series:
  - "Cloud Endpoints"
---

The Cloud Endpoints API comes packaged with endpointscfg.py to generate client
libraries in JavaScript, Objective-C (for iOS) and Java (for Android). You can
also generate a few additional client libraries using the [Google APIs client
generator](https://code.google.com/p/google-apis-client-generator/). This
article will show you how to use the generator to create a C# client library.

<!--more-->

The client generator is a Python application you can install with `pip`.

```bash
pip install google-apis-client-generator
```

The client generator works by taking an API discovery document, parsing it into
an object model, and then using a language template to transform the object
model to running code. 

To run the generator you will need the discovery document for your API. You can
find this document from the root API discovery URL. First, download the root API
discovery document using [httpie](https://github.com/jakubroztocil/httpie).

```bash
http --download https://example.appspot.com/_ah/api/discovery/v1/apis
```

```json
"items": [
    {
        "description": "Example Api",
        "discoveryLink": "./apis/example/v1/rest",
        "discoveryRestUrl": "https://example.appspot.com/_ah/api/discovery/v1/apis/example/v1/rest",
        "icons": {
            "x16": "http://www.google.com/images/icons/product/search-16.gif",
            "x32": "http://www.google.com/images/icons/product/search-32.gif"
        },
        "id": "example:v1",
        "kind": "discovery#directoryItem",
        "name": "example",
        "preferred": true,
        "version": "v1"
    }
]
```

The root discovery document will have an `items` member listing the available
APIs and a `discoveryLink` for each API. The `discoveryLink` provides the schema
for the API. We can download this schema and use it as input to the client
generator.

```bash
http --download https://example.appspot.com/_ah/api/discovery/v1/apis/example/v1/rest

generate_library --input=rest.json --language=csharp --output_dir=tmp/csharp
```

Your C# client library is now ready to use. As of this writing you can generate
client libraries in C++, C#, Dart, GWT, Java, PHP and Python.
