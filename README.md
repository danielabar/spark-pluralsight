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

All spark applications are managed by a single point, called the _Driver_.

__Driver__ is coordinator of work distributing to as many workers as configured. Driver management is handled through the starting point of any spark application, which is the _SparkContext_.

__SparkContext__ All spark apps are built around this central manager, which orchestrates all the separate pieces of the distributed app. Responsible for:

* Task creator - builds execution graph that will be sent to each worker.
* Scheduler - schedules work across nodes.
* Data locality - takes advantage of existing data location knowledge, sending the work to the data, to avoid unnecessary data movement across network.
* Fault tolerance - monitors tasks for failures so that it can trigger a rebuild of that portion of the dataset on another worker.

It's possible to create multiple spark context's within the same process but not recommended.

