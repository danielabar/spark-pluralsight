name := "spark-pluralsight"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.10" % "1.4.0" % "provided",
  "org.apache.hadoop" % "hadoop-streaming" % "2.6.0"
)
