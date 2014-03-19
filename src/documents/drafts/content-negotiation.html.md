
### GET

The get a resource from your API use the HTTP `GET`. `GET` is used to retrieve a representation of a resource by specifying the host and path of the resource.

```
GET /player/1234567890
Host: api.example.com
```

The client may choose between different representations using *content negototiation*. HTTP defines an `Accept` header that the client can use to request a specific representation of the resource. For example, an HTML client may use the following `Accept` header to request an HTML representation from the server.

```
Accept: text/html
```

Whereas an XML client may use the following `Accept` header to request an XML representation from the server.

```
Accept: application/xml
```

By allowing your users to specify the type of data to be returned through your API you can introduce upgraded data types without affecting already deployed clients. Content negotiation allows the client and server to evolve independently without changing the unique resource identifier specified by the URL.

