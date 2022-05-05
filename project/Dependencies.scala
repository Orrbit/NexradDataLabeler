import sbt._

object Dependencies {
  lazy val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % "3.2.11" % "test"    
  )

  val circeVersion = "0.13.0"
  val pureconfigVersion = "0.15.0"
  val sparkVersion = "3.2.1"
  val AkkaVersion = "2.6.14"
  val AkkaHttpVersion = "10.1.11"

  lazy val core = Seq(

    // support for typesafe configuration
    "com.github.pureconfig" %% "pureconfig" % pureconfigVersion,

    // spark
    //"org.apache.spark" %% "spark-sql" % sparkVersion % Provided, // for submiting spark app as a job to cluster
    "org.apache.spark" %% "spark-sql" % sparkVersion, // for simple standalone spark app

    // logging
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3",

    "com.amazonaws" % "aws-java-sdk" % "1.12.205",

    // akka streams
    "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "3.0.4",
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "3.0.4",
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-xml" % AkkaHttpVersion,


    "edu.ucar" % "netcdf4" % "4.5.5",
    
    "log4j" % "log4j" % "1.2.14"

  )
}
