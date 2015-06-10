---
title: "App Engine Pipelines API - Part 6: The Pipeline UI" 
date: 2015-06-09T20:43:56-06:00
tags: 
  - "appengine"
  - "pipelines"
series:
  - "Pipelines API"
---

* [View all articles in the Pipeline API Series](http://sookocheff.com/series/pipelines-api/).

This article will serve as a reminder of the Pipeline UI as much for the writer
as for the reader. The Pipeline UI requires the MapMeduce library to be
installed. If you are not familiar with MapReduce please refer to the [MapReduce
API Series of articles](http://sookocheff.com/series/mapreduce-api/).

Once MapReduce is installed you will need to add a few indices to `index.yaml`
to properly query for pipeline records for display in the UI.

```python
indexes:
- kind: _AE_Pipeline_Record
  properties: 
    - name: is_root_pipeline 
    - name: start_time 
      direction: desc

- kind: _AE_Pipeline_Record
  properties: 
    - name: class_path 
    - name: is_root_pipeline 
    - name: start_time
      direction: desc
```

## List Root Pipelines

```bash
/mapreduce/pipeline/list
```

This URL will list all root pipelines in the system, ordered by their starting
time. You can filter pipelines by their class path and check on an individual
pipelines status using this UI.

{{% img "appengine/pipelines/pipeline-ui/root-pipelines.png" "List Root Pipelines" %}}

## Pipeline Status

```bash
/mapreduce/pipeline/status?root=7ba9b9b2b2e24787b3b4c11079178cb6
```

If you know a pipeline's root identifier you can jump directly to the status
page. This page presents you with a UI displaying the status of a pipeline and
any of the pipeline's children.

{{% img "appengine/pipelines/pipeline-ui/pipeline-status.png" "Pipeline Status" %}}

## List MapReduce Pipelines

```bash
/mapreduce/status
```

If your pipeline is a MapReduce job, it will have an entry on the MapReduce
status page. You can navigate to individual jobs to check their sharding and
processing status or cleanup a job by removing datastore entries for the
pipeline.

{{% img "appengine/pipelines/pipeline-ui/mapreduce-list.png" "MapReduce List" %}}

## MapReduce Status

```bash
/mapreduce/detail?mapreduce_id=1574647046653BE202D4D
```

If you know a mapreduce job's identifier you can jump directly to the status
page. This page will show any counters you have defined and show how the
processing of each shard of data.

{{% img "appengine/pipelines/pipeline-ui/mapreduce-status.png" "MapReduce Status" %}}
