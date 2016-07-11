# Apache Spark Fundamentals

> My course notes from [Spark course](https://app.pluralsight.com/library/courses/apache-spark-fundamentals/table-of-contents) on Pluralsight.

## Hello World: Counting Words

Run spark-shell with [Docker](https://hub.docker.com/r/gettyimages/spark/)

```shell
docker run --rm -it -p 4040:4040 gettyimages/spark bash
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
