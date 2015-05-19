---
title: "App Engine Pipelines API - Part 4: Pipeline Internals" 
date: 2015-05-26T05:57:19-06:00
tags: 
  - "appengine"
  - "pipelines"
series:
  - "Pipelines API"
draft: "true"
---

[We've learned how to execute and chain together pipelines]({{< ref
"2015-05-12-app-engine-pipelines-part-two-connecting-pipelines.md" >}}),
now let's take a look at how pipelines execute under the hood. If necessary,
you can refer to the [source code of the pipelines
project](https://github.com/GoogleCloudPlatform/appengine-pipelines) to
clarify any details.

## The Pipeline Data Model

Let's start with the pipeline data model. Note that each Kind defined by the
pipelines API is prefixed by `_AE_Pipeline`, making it easy to view individual
pipeline details by viewing the data stored in the entity.

### PipelineRecord

Every pipeline is represented by a *PipelineRecord* entity in the datastore. This
PipelineRecord records the pipelines root (if this pipeline is a child), any
child pipelines spawned by this pipeline, the current status of the pipeline,
and a few additional details of book keeping. At any point in time a Pipeline
may be in one of four states: WAITING, RUN, DONE, ABORTED. WAITING implies that
this pipeline has a barrier that must be satisfied before the pipeline can be
RUN. RUN means that the pipeline has been started. DONE means that the pipeline
is complete. ABORTED means that the pipeline has been manually aborted.

### SlotRecord

The output of a pipeline is represented as a *Slot* stored in the datastore as a
*SlotRecord*. When a pipeline completes, it stores its output in the SlotRecord
to be made available to further pipelines.

## Pipeline execution

## Barriers

## Slots

## ?
