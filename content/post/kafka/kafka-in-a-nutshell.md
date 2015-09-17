---
title: "Kafka in a Nutshell"
date: 2015-09-15T05:27:34-06:00
draft: true
tags: 
  - "kafka"
---

Kafka is a messaging system. That's it. So why all the hype? It turns out that
messaging is a hugely important piece of infrastructure for moving data between
systems. To see why, let's look at how to move data around without a messaging
system. 

It starts with a business need to use Hadoop for storage and data processing.
The first stage in using Hadoop is getting data in. 

< data in image >

So far, not a big deal. Unfortunately, in the real world data exists on many
systems in parallel, all of which need to interact with Hadoop and with existing
systems. The situation quickly becomes more complex as we move data between
these systems. Soon, you end up with a situation where multiple data systems are
talking to one another over many channels. Each of these channels requires their
own custom protocols and communication methods and moving data between these
systems becomes a full-time job for a team of developers.

< data move image >

Let's look at this picture again, using Kafka. In this scenario, Kafka acts as a
message bus where all incoming data is first placed in the Kafka cluster and all
outgoing data is read from Kafka. Kafka centralizes communication between
producers of data and consumers of that data.

< kafka organization image >

## What is Kafka?

{{ quote "Kafka is publish-subscribe messaging rethought as a distributed commit log.", "http://kafka.apache.org/" }}

Kafka, is a distributed messaging system providing fast, highly scalable and
redundant messaging through a pub-sub model. It turns out that, in the real
world, this is an ideal fit for communication and integration between components
of large scale data systems.

As a messaging system, Kafka's distributed design gives it several advantages.
Kafka allows a large number of consumers. Kafka also does not care about
the consumers — it allows ad-hoc consumers to join and leave the cluster at any
time without affecting other consumers. Batch consumers are also not an issue
with Kafka, you can schedule a consumer to retrieve one hour worth of messages
at a time without issue. Kafka is also highly available. It is resilient to
node failures and supports automatic recovery.

## Kafka Terminology

The basic architecture of Kafka is organized around a few key terms: topics,
producers, consumers, and brokers. Messages are organized into topics. If
you wish to send a message you send it to a specific topic and if you wish
to read a message you read it from a specific topic. Kafka is a consumer
pull system. Producers push messages into Kafka and consumers subscribe to
messages and pull them from Kafka. Lastly, Kafka runs in a cluster and each
node in the cluster is called a Kafka broker.

## Anatomy of a Kafka Topic

Kafka topics are divided into a number of partitions. Partitions allow you to
parallelize a topic so that if you have a lot of data in a particular topic it
is partitioned into multiple partitions. Each partition can be placed on a
separate machine to allow for multiple consumers to read from a partition in
parallel. Consumers can also be parallelized so that multiple consumers can read
from multiple partitions in a topic allowing for very high message processing
throughput.

Each message within a partition has an identifier called its **offset**. The
offsets represent an immutable sequence of messages and Kafka maintains this
message ordering for you.  Consumers can read messages starting from a specific
offset and are allowed to read from any offset point they choose. Each specific
message is uniquely identified by a tuple consisting of the messages topic,
partition and offset within the partition.

< img anatomy of a partition >

Each partition acts as a log. A data source writes messages to the log and one
or more consumers reads from the log at the point in time they choose. In the
diagram below a data source is writing to the log and consumers A and B are
reading from the log at different offsets. 

< img from 3:10 of video >

Kafka keeps messages for a configurable period of time and it is up to the
consumers to adjust their behaviour accordingly. For instance, if Kafka is
configured to keep messages for a day and a consumer is down for a period of
longer than a day, the consumer will lose messages. However, if the consumer is
down for an hour it can begin to read messages again starting from its last
known offset. From the point of view of Kafka, it keeps no state on what the
consumers are reading.

## Partitions and Brokers

Each broker holds a number of partitions. These partitions can either be leaders
or replicas. All reads and writes go through the readers and are replicated at
the replicas. If a leader fails, a replica takes over as the new leader.
Producers write to one partition at a time and Kafka replicates the data to
additional brokers within the cluster. 

< img ... >

## Consumers and Consumer Groups

Consumers can belong to consumer groups ... each consumer can read from a
particular partition to allow you to scale throughput of message consumption
within a group.

If you have more consumers than partitions then some consumers will be idle.

< img from 5:10 of video >

## Consistency and Availability

Before beginning the discussion on consistency and availability, keep in mind
that these guarantees hold *as long as you are producing to one partition and
consuming from one partition*. All guarantees are off if you are reading from
the same partition using two consumers or writing to the same partition using
two producers.

Kafka makes the following guarantees about data consistency and availability:
(1) Messages sent to a topic partition will be appended to the commit log in
the order they are sent, (2) a single consumer instance will see messages in the
order they appear in the log, (3) a message is 'committed' when all in sync
replicas have applied it to their log, and (4) any committed message will not be
lost, as long as at least one in sync replica is alive.

The first and second guarantee ensure that message ordering is preserved for
each partition. This is important because in many systems ordering of messages
is important for business reasons. The third and fourth guarantee ensure that
committed messages can be retrieved. In Kafka, the partition that is elected the
leader is responsible for syncing any messages received to replicas. Once the
replicas have acknowledged the message that replica is considered in sync. To
understand this further, lets take a closer look at what happens during a write.

### What Happens During Write?

When communicating with a Kafka cluster, all messages are sent to the
partition's leader. The leader is responsible for writing the message to its own
in sync replica and, once that message has been committed, is responsible for
propagating the message to additional replicas on different nodes. Each replica
acknowledges that they have received the message and can now be called in sync.

< img leader-writes-to-replicas >

When every broker in the cluster is up and running consumers and producers can
merrily read and write from the leading partition of a topic without issue.
Unfortunately, there are two types of failures that may happen: failing
replicas and failing leaders.

### Handling Failure

What happens when a replica fails? Writes will no longer reach the failed
replica and it will no longer receive messages, falling further and further out
of sync.

< img first-failed-replica >

No what happens when a second replica fails? The second replica will also no
longer receive messages and become out of sync.

< img second-failed-replica >

This leaves only the leader in sync. In Kafka terminology we still have one in
sync replica even though that replica happens to be part the leader for this
partition.

Now, what happens if the leader dies? We are left with three dead replicas.

<img third-failed-replica>

Replica one does not miss data and is actually still in sync — it cannot receive
any new data but it is in sync with everything that was possible to receive.
Replica two is missing some data, and replica three (the first to go down) is
missing even more data.

Given this state, there are two possible solutions. The first, and simplest,
scenario is to wait until the leader is back up before continuing. Once
the leader is back up it will begin receiving and writing messages and as
the replicas are brought back online they will be made in sync with the leader.

The second scenario is to elect the first node to come back up as the new
leader. This node will be out of sync with the leader and all data written after
this node went down will be lost. As additional nodes come back up, the will see
that they have committed messages that do not exist on the new leader and drop
those messages. The outcome is that we have missed messages. These missed
messages may be an acceptable trade off over the minimized downtime of electing
any available leader.

Taking a step back, we can view a scenario where the leader goes down while in
sync replicas still exist.

<img leader-fails >

In this case, the Kafka controller will detect the node loss and elect a new
leader from the pool of in sync replicas. This may take a few seconds and result
in LeaderNotAvailable errors from the client. However, no data loss will occur
as long as clients handle this possibility.

It is also possible to do a controlled shutdown where the leader will transfer
leadership to an in sync replica with no downtime or data loss.  

Finally, you can configure Kafka to have a minimum number of in sync replicas.
This is a soft guarantee that saves us from having the last replica crash
completely and miss incoming messages.

## Consistency as a Kafka Client

Kafka clients come in two flavours: producer and consumer. Each of these can be
configured to different levels of consistency. 

For a producer we have three
choices. On each message we can (1) wait for all in sync replicas to acknowledge
the message, (2) wait for only the leader to acknowledge the message, or (3) do
not wait at all. Each of these methods have their merits and drawbacks and it is
up to the implementer to decide on the appropriate strategy for their system.

On the consumer side, we can only ever read committed messages (i.e., those that have
been written to all in sync replicas). Given that, we have three methods of
providing consistency as a consumer: (1) receive each message at most once, (2)
receive each message at least once, or (3) receive each message exactly
once. Each of these scenarios deserves a discussion of its own.

For at most once message delivery, the consumer reads data from a partition,
commits the offset that it has read, and then processes the message. If the
consumer crashes between committing the offset and processing the message it
will restart from the next offset without ever having processed the message.
This would lead to potentially undesirable message loss.

<img at most once>

A better alternative is at least once message delivery. For at least once
delivery, the consumer reads data from a partition, processes the message, and
then commits the offset of the message it has processed. In this case, the
consumer could crash between processing the message and committing the offset
and when the consumer restarts it will process the message again. This leads to
duplicate messages in downstream systems but no data loss.

<img at least once>

Exactly once delivery is guaranteed by having the consumer process a message and
commit the output of the message along with the offset to a transactional system. 
If the consumer crashes it can re-read the last transaction committed and resume
processing from there. This leads to no data loss and no data duplication. In
practice however, exactly once delivery implies significantly decreasing the
throughput of the system as each message processed is committed as a
transaction.

<img exactly once>

In practice most Kafka consumer applications choose at least once delivery
because it offers the best trade-off between throughput and correctness. It
would be up to downstream systems to handle duplicate messages in their own way.

## Conclusion

Kafka is quickly becoming the backbone of many organizations data. And with good
reason. By using Kafka as a message bus connecting systems together we achieve a
high level of decoupling between data producers and data consumers making our
architecture more flexible and adaptable to change. Enjoy learning Kafka and
putting this tool to more use!
