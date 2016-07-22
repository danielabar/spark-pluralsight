package main

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object WordCounter {

  // Core logic goes in main method, which takes a string array as application arguments.
  // When spark runs your code, it looks for this main signature.
  // Do not subclass scala app because that's known not to work properly.
  def main(args: Array[String]) {

    // conf object describes the spark app's configuration, can be used to set any key/value in the config
    // in this case, we're overriding the app name which is a common action, therefore it has a specialized method
    val conf = new SparkConf().setAppName("Word Counter")

    // context has convenience constructors, including empty constructor, which would set a default config and property files
    // order of precedence for config: setting in code, flags in spark submit, setting in properties file, defaults
    // if setting in code as we're doing here, must be fully setup before passing to context, because once its submitted to context,
    // it gets cloned and becomes immutable wrt context, and any further changes made to conf object will have no effect to the application.
    val sc = new SparkContext(conf)

    val textFile = sc.textFile("/usr/spark-2.0.0-preview/README.md")
    val tokenizedFileData = textFile.flatMap(line=>line.split(" "))
    val countPrep = tokenizedFileData.map(word=>(word, 1))
    val counts = countPrep.reduceByKey((accumValue, newValue)=>accumValue + newValue)
    val sortedCounts = counts.sortBy(kvPair=>kvPair._2, false)
    sortedCounts.saveAsTextFile("/usr/spark-2.0.0-preview/wordcountexample")
  }
}
