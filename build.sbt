name := """akka-persistence-POC"""

version := "2.4.4"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence-query-experimental" % "2.4.10",
  "com.typesafe.akka" %% "akka-actor" % "2.4.4",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.4",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.4",
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.4.4",
  "com.typesafe.akka" %% "akka-distributed-data-experimental" % "2.4.4",
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.21"
)

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))

// It needs for pack task (see plugins proyect)
packAutoSettings

fork in run := true

cancelable in Global := true
