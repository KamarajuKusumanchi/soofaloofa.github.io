---
title: "Create a Google Cloud Dataflow Project with Gradle"
date: 2015-02-11T06:20:37-06:00
tags: 
  - "dataflow"
  - "gradle"
---

I've been experimenting with the [Google Cloud
Dataflow](https://cloud.google.com/dataflow/) [Java
SDK](https://github.com/GoogleCloudPlatform/DataflowJavaSDK) for running managed
data processing pipelines. One of the first tasks is getting a build environment
up and running. For this I chose Gradle.

We start by declaring this a java application and listing the configuration
variables that declare the source compatibility level (which for now must be
1.7) and the main class to be executed by the `run` task to be defined later.

```
apply plugin: 'java'
apply plugin: 'application'

sourceCompatibility = '1.7'

mainClassName = 'com.sookocheff.dataflow.Main'
```

We then declare the mavenCentral repository where the dependencies are located
and the basic dependencies for a Cloud Dataflow application.

```
repositories {
    mavenCentral()
}

dependencies {
    compile 'com.google.guava:guava:18.0'
    compile 'com.google.cloud.dataflow:google-cloud-dataflow-java-sdk-all:0.3.150109'
    
    testCompile 'junit:junit:4.11'
}
```

Last, we create our run task that will launch the Cloud Dataflow application.
The Cloud Dataflow runtime expects the folder `resources/main` to exist in your
build. If you are not actually shipping any resources with your application you
will need to tell Gradle to create the correct directory. We also pass any
parameters to our main class using the -P flag.  These two steps are
encapsulated below.

```
task resources {
    def resourcesDir = new File('build/resources/main')
    resourcesDir.mkdirs()
}

run {
    if (project.hasProperty('args')) {
        args project.args.split('\\s')
    }
}

run.mustRunAfter 'resources'
```

You should now be able to launch your Cloud Dataflow application using the
`gradle run` task, passing your project identifiers as parameters. For example,	

```
gradle run -Pargs="--project=<your-project> --runner=BlockingDataflowPipelineRunner --stagingLocation=gs://<staging-location>"
```
