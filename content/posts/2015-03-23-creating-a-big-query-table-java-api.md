---
title: "Creating a BigQuery Table using the Java Client Library"
date: 2015-03-23T15:32:37-06:00
tags: 
  - "bigquery"
  - "java"
  - "table"
---

I haven't been able to find great documentation on creating a BigQuery
TableSchema using the Java Client Library. This blog post hopes to rectify that
:).

You can use the [BigQuery sample
code](https://github.com/GoogleCloudPlatform/bigquery-samples-java) for an idea
of how to create a client connection to BigQuery. Assuming you have the
connection set up you can start by creating a new `TableSchema`. The
`TableSchema` provides a method for setting the list of fields that make up the
columns of your BigQuery Table. Those columns are defined as an Array of
`TableFieldSchema` objects.

```java
ArrayList<TableFieldSchema> fieldSchema = new ArrayList<TableFieldSchema>();
```

For simple types you can populate your columns with the correct type and mode
according to the [BigQuery API
documentation](https://cloud.google.com/bigquery/docs/reference/v2/tables#resource).
For example, to create a STRING field that is NULLABLE you can use the
following.

```java
fieldSchema.add(new TableFieldSchema().setName("username").setType("STRING").setMode("NULLABLE"));
```

And for repeated fields you can use the REPEATED mode.

```java
fieldSchema.add(new TableFieldSchema().setName("email").setType("STRING").setMode("REPEATED"));
```

To create nested records you specify the parent as a RECORD mode and then call
`setFields` for each column of nested data you want to insert. The columns of a
nested type are the same format as for the parent -- a list of TableFieldSchema
objects.

```java
fieldSchema.add(
  new TableFieldSchema().setName("location").setType("RECORD").setFields(
    new ArrayList<TableFieldSchema>() {
      {
        add(new TableFieldSchema().setName("city").setType("STRING"));
        add(new TableFieldSchema().setName("address").setType("STRING"));
        add(new TableFieldSchema().setName("zipcode").setType("STRING"));
      }
    }
  )
);
```

The last step is to set the entire schema as the fields of our table schema.

```java
TableSchema schema = new TableSchema();
schema.setFields(fieldSchema);
```

Then we set a `TableReference` that holds the current project id, dataset id and
table id. We use this `TableReference` to create our `Table` using the `TableSchema`.

```java
TableReference ref = new TableReference();
ref.setProjectId(PROJECT_ID);
ref.setDatasetId("pubsub");
ref.setTableId("review_test");

Table content = new Table();
content.setTableReference(ref);
content.setSchema(schema);

client.tables().insert(ref.getProjectId(), ref.getDatasetId(), content).execute();
```

Putting this all together gives you a working sample of creating a BigQuery Table using the Java Client Library.

```java
public static void main(String[] args) throws IOException, InterruptedException {
  Bigquery client = createAuthorizedClient(); // As per the BQ sample code
  
  ArrayList<TableFieldSchema> fieldSchema = new ArrayList<TableFieldSchema>();
  
  fieldSchema.add(new TableFieldSchema().setName("username").setType("STRING").setMode("NULLABLE"));
  fieldSchema.add(new TableFieldSchema().setName("email").setType("STRING").setMode("REPEATED"));
  fieldSchema.add(
    new TableFieldSchema().setName("location").setType("RECORD").setFields(
      new ArrayList<TableFieldSchema>() {
        {
          add(new TableFieldSchema().setName("city").setType("STRING"));
          add(new TableFieldSchema().setName("address").setType("STRING"));
          add(new TableFieldSchema().setName("zipcode").setType("STRING"));
        }
  }));
  
  TableSchema schema = new TableSchema();
  schema.setFields(fieldSchema);
  
  TableReference ref = new TableReference();
  ref.setProjectId("<YOUR_PROJECT_ID>");
  ref.setDatasetId("<YOUR_DATASET_ID>");
  ref.setTableId("<YOUR_TABLE_ID>");
  
  Table content = new Table();
  content.setTableReference(ref);
  content.setSchema(schema);
  
  client.tables().insert(ref.getProjectId(), ref.getDatasetId(), content).execute();
}
```
