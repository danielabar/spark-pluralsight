# Apache Spark Fundamentals

> My course notes from [Spark course](https://app.pluralsight.com/library/courses/apache-spark-fundamentals/table-of-contents) on Pluralsight.

## Hello World: Counting Words

Run spark-shell with [Docker](https://hub.docker.com/r/gettyimages/spark/)

```shell
docker run --rm -it -p 4040:4040 -v /Users/dbaron/projects/spark-pluralsight/target/scala-2.11:/data gettyimages/spark bash
spark-shell
```

Shell automatically creates two values:

`sqlContext` -  Used with Spark sql library (discussed later in this course).

`sc` - Spark context, main starting point of a Spark application.
All Spark jobs begin by creating a Spark context, which acts as delegator for many aspects of control of the distributed application.

Tell the context _our intent_, to load a text file, and store the results in a value `textFile`:

```scala
val textFile = sc.textFile("/usr/spark-2.0.0-preview/README.md")
```

Result is immediately returned in form of `RDD`, which is the most basic abstraction of Spark, sample output:

```scala
textFile: org.apache.spark.rdd.RDD[String] = /usr/spark-2.0.0-preview/README.md MapPartitionsRDD[1] at textFile at <console>:24
```

__RDD__ - Resilient distributed dataset. It's just an _abstraction_ of the operations declared.

Spark's core operations are split into two catogories:

__Transformations__ - Lazily evaluated, only storing the intent.

__Actions__ - Results are only computed when an action is executed. For example, `first` action retrieves the first line of the text file:

```scala
textFile.first
```

This returns an actual result:

```scala
res0: String = #  Apache Spark
```

Benefits to Spark laziness, in this example, don't have to read entire text file, only the first line.

Map each line of text file, splitting it into an array of space delimited words, and flattening the resulting sequence of string arrays into a single long sequnece:

```scala
val tokenizedFileData = textFile.flatMap(line=>line.split(" "))
```

This returns another RDD, strings it represents are the words in the file.

Setup sequence for counting by changing each word into a key value pair, where word is key and value is count, seeding with a count of 1 for each uncounted word string.

```scala
val countPrep = tokenizedFileData.map(word=>(word, 1))
```

Now that data is prepared into key-value format, reduce the values down to a single count for each key, adding up all the 1's in each word bucket:

```scala
val counts = countPrep.reduceByKey((accumValue, newValue)=>accumValue + newValue)
```

Sorting list of word count pairs on the count in descending value. `_2` is Scala's way of accessing the object in the second position of a tuple, which is how key-value pair is represented.

## Spark Core: Part 1

Course will be using SBT for building Scala apps.

### Spark Application

All spark applications are managed by a single point, called the _Driver_.

__Driver__ is coordinator of work distributing to as many workers as configured. Driver management is handled through the starting point of any spark application, which is the _SparkContext_.

__SparkContext__ All spark apps are built around this central manager, which orchestrates all the separate pieces of the distributed app. Responsible for:

* Task creator - builds execution graph that will be sent to each worker.
* Scheduler - schedules work across nodes.
* Data locality - takes advantage of existing data location knowledge, sending the work to the data, to avoid unnecessary data movement across network.
* Fault tolerance - monitors tasks for failures so that it can trigger a rebuild of that portion of the dataset on another worker.

It's possible to create multiple spark context's within the same process but not recommended.

[Standalone app](src/main/scala/WordCount.scala) must build its own spark context (unlike spark-shell that automatically builds and exposes the `sc` variable).

Spark dependency is managed in [build.sbt](build.sbt). "provided" specifies that the Spark container application will provide the dependency at runtime.

To build an executable jar, run `sbt package`, noting the location of the generated jar: `/Users/dbaron/projects/spark-pluralsight/target/scala-2.11/spark-pluralsight_2.11-1.0.jar`

To submit application to be executed, pass in fully qualified name of class where main method is defined, address of master node, "*" specifies to use as many cores as possible,
and provide path to jar containing spark application that was just packaged.

This works given the volume mount specified when running the Spark Docker container.

```shell
spark-submit --class "main.WordCounter" --master "local[*]" "/data/spark-pluralsight_2.11-1.0.jar"
```

Note that any space separated arguments passed in at the end of the above command line will be passed in as arguments to the main method.
This can be used to make the application more dynamic, for example, by passing in the input and output file paths rather than hard-coding them in the program.

### What is an RDD?

Resilient distributed dataset.

Official definition: Collection of elements partitioned across the nodes of the cluster that can be operated on in parallel.

Collection, similar to list or array. Interface that makes it seem like any other collection. 

In the word count program, loading of text file, manipulating and saving data, has been working with RDD. 

Interface makes it easy to think in collections, while behind the scenes, work is distributed across cluster of machines, so that computation can be run in parallel, reducing processing time. 

Even if one point fails, rest of system can continue processing, while failure can be restarted elsewhere. RDD is designed with fault tolerance. This works because most functions in Spark are lazy.

Instead of immediately executing the functions instructions, instructions are _stored_ for later use in _DAG_ - directed acyclic graph.

Graph of instructions grows through series of calls to _Transformations_ such as map, filter, etc.

DAG is a buildup of functional lineage that will be sent to the workers, which will use instructions to compute final output for spark app.

Lineage awareness makes it possible to handle failures. Each RDD in graph knows how it was built, so it can choose the best path for recovery.

At some point, work needs to be done, this is where...

__Actions__ - set of methods that trigger computations, eg collect, count, reduce. These trigger DAG execution and result in final action against in data, such as returning to driver program or saving to persistent storage.

RDD is _immutable_. Once created, can no longer change it. This refers to the _lineage_, not the data driving it.

Each action triggers fresh execution of the graph. If underlying data changes, then so will the final results (although caching can alter this behaviour, discussed later).
 
__Spark Mechanics__

![spark mechanics](images/spark-mechanics.png "Spark Mechanics")

RDD may be operated on like any other collection, but its distributed across cluster, to be executed in parallel across cpus and/or machines.

Driver application runs just like any other application, until an action is triggered, at which point, the driver and its context distributes the tasks to each node, each of which transform their chunks.

When all nodes have completed their tasks, then next stage of DAG is triggered, repeating until entire graph is completed.

If a chunk of data is lost, DAG scheduler finds a new node and restarts the transformation from correct point, returning to synchronization with rest of the nodes.

### Loading Data