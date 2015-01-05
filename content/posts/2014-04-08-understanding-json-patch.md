---
title: "Understanding JSON Patch"
date: 2014-04-08T16:12:14Z
tags: 
  - "json"
  - "patch"
  - "api"
  - "rest"
---

The typical update cycle for an API resource is to (1) GET the representation, (2) modify it and (3) PUT back the entire representation. This can waste bandwidth and processing time for large resources. An alternative is to use the [HTTP PATCH](https://tools.ietf.org/html/rfc5789) extension method to only send the differences between two resources. HTTP PATCH applies a set of changes to the document referenced by the HTTP request.

<!--more-->

```bash
PATCH /file.txt HTTP/1.1
Host: sookocheff.com
Content-Type: application/json
If-Match: "e0036bbc6f"

[description of changes]
```

The format of the PATCH request body differs depending on the representation of the resource. For JSON documents, [JSON Patch](https://tools.ietf.org/html/rfc6902) defines this format.

A JSON Patch document is a sequential list of operations to be applied to an object. Each operation is a JSON object having exactly one `op` member.
Valid operations are `add`, `remove`, `replace`, `move`, `copy` and `test`. Any other operation is considered an error. 

```json
{ "op": "add" }
```

Each operation must also have exactly one `path` member. 
The `path` member is a [JSON Pointer](https://tools.ietf.org/html/rfc6901) that determines a location within the JSON document to modify.

```json
{ "op": "add", "path": "/player/name" }
```

The remaining elements of a JSON Patch operation depend on the particular operation being performed.

### add

The `add` operation is used in different ways depending on the target of the `path` being referenced. Generally speaking we can use `add` to append to a list, add a member to an object or update the value of an existing field. The `add` operation accepts a `value` member which is the value to update the referenced `path`. 

#### Append to a List

To append a value to a list you use an existing list as the `path` of the operation. So, given the JSON document.

```bash
{
    "orders": [{"id": 123}, {"id": 456}]
}
```

We can append an order to the list using the `add` operation.

```bash
{ "op": "add", "path": "/orders", "value": {"id": 789} }
```

After applying the patch we get the final document.

```bash
{
    "orders": [{"id": 123}, {"id": 456}, {"id": 789}]
}
```

#### Add a Member to an Object

If the `path` references a member of an object that does not exist, a new member is added to the object. We start with our JSON document listing our orders.

```json
{
    "orders": [{"id": 123}, {"id": 456}, {"id": 789"}]
}
```

Using this JSON Patch document we can add a total and a currency member to the document.

```json
[
{ "op": "add", "path": "/total", "value": 20.00 },
{ "op": "add", "path": "/currency", "value": "USD" }
]
```

After applying the patch we get the final representation.

```json
{
    "orders": [{"id": 123}, {"id": 456}, {"id": 789}],
    "total": 20.00,
    "currency": "USD"
}
```

#### Update an Existing Member of an Object

If the `path` refers to an existing object member, that member is updated with the newly supplied value.

Given the JSON document.

```json
{
    "orders": [{"id": 123}, {"id": 456}, {"id": 789}],
    "total": 20.00,
    "currency": "USD"
}
```

We can update the total by using an `add` operation.

```json
{ "op": "add", "path": "/total", "value": 30.00 },
```

Leaving the final result.

```json
{
    "orders": [{"id": 123}, {"id": 456}, {"id": 789}],
    "total": 30.00,
    "currency": "USD"
}
```

### remove

Remove is a simple operation. The target location of the `path` is removed from the object.

Starting with the following document.

```json
{
    "orders": [{"id": 123}, {"id": 456}, {"id": 789}],
    "total": 30.00,
    "currency": "USD"
}
```

We can remove the `currency` member with a `remove` operation.

```json
{ "op": "remove", "path": "/currency" }
```

```json
{
    "orders": [{"id": 123}, {"id": 456}, {"id": 789}],
    "total": 30.00
}
```

We can also remove an element from an array. All remaining elements are shifted one position to the left. To remove order `456` we can remove the array index referencing this order.

```json
{ "op": "remove", "path": "/orders/1" }
```

```json
{
    "orders": [{"id": 123}, {"id": 789}],
    "total": 30.00
}
```

### replace

Replace is used to set a new value to a member of the object. It is logically equivalent to a `remove` operation followed by an `add` operation to the same `path` or to an `add` operation to an existing member. 

Given the following JSON document.

```json
{
    "orders": [{"id": 123}, {"id": 456}, {"id": 789}],
    "total": 20.00,
    "currency": "USD"
}
```

We can apply the `replace` operation to update the order total.

```json
{ "op": "replace", "path": "/total", "value": 30.00 },
```

```json
{
    "orders": [{"id": 123}, {"id": 456}, {"id": 789}],
    "total": 30.00,
    "currency": "USD"
}
```

### move

 The `move` operation removes the value at a specified location and adds it to the target location. The removal location is given by a `from` member and the target location is given by the `path` member.

Given this starting document.

```json
{
    "orders": [{"id": 123}, {"id": 456}, {"id": 789}],
    "total": 30.00,
    "currency": "USD"
}
```

We can move an order to the root of the document by applying this JSON patch.

```
json
{ "op": "move", "from": "/orders/0", "path": "/rootOrder" }
```

```json
{
    "orders": [{"id": 456}, {"id": 789}],
    "rootOrder": {"id": 123}, 
    "total": 30.00,
    "currency": "USD"
}
```

### copy

`copy` is like `move`. It copies the value at the `from` location to the  `path` location, leaving duplicates of the data at each location.

```json
{ "op": "copy", "from": "/orders/0", "path": "/rootOrder" }
```

### test

The HTTP PATCH method is atomic and the patch should only be applied if all operations can be safely applied. The `test` operation can offer additional validation to ensure that patch preconditions or postconditions are met. If the test fails the whole patch is discarded. `test` is strictly an equality check.

```json
{ "op": "test", "value": 30.00, "path": "/total" }
```

### Conclusion

JSON Patch is an effective way to provide diffs of your API resources. Most languages already have an implementation available. There is no reason not to adopt the HTTP PATCH today.
