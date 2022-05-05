package org.ndl.core

import java.nio.file.Paths
import akka.stream.scaladsl.FileIO
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.alpakka.csv.scaladsl.CsvToMap
import akka.stream.scaladsl.{Source, Flow}
import scala.concurrent.Future
import akka.stream.IOResult
import org.ndl.models.InputStormEvent
import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.alpakka.s3.ListBucketResultContents
import akka.stream.scaladsl.Sink
import java.text.SimpleDateFormat
import java.util.Date
import scala.util.Try

object Stages {
  def csvFileSource(filename: String): Source[Map[String,String],Future[IOResult]] = {
    val file = Paths.get(filename)
    val fileSource = FileIO.fromPath(file)
      
    fileSource
      .via(CsvParsing.lineScanner())
      .via(CsvToMap.toMapAsStrings())
  }

  def flowMapToStormEvent(): Flow[Map[String,String],InputStormEvent,NotUsed] = {
      Flow[Map[String,String]].map(InputStormEvent.apply(_))
        .via(Flow[Option[InputStormEvent]].collect{ case Some(value: InputStormEvent) => value})
  }

  def flowStormEventToBucketContents(bucket: String): Flow[InputStormEvent,Seq[(ListBucketResultContents, InputStormEvent)],NotUsed] = {
    Flow[InputStormEvent]
    .map(se => (se, InputStormEvent.getAssociatedNexradPrefix(se)))
    .flatMapConcat(tuple => S3.listBucket(bucket, Some(tuple._2)).map((_, tuple._1)).grouped(10000))
  }

  def flowGetClosestFileToStormEvent(): Flow[Seq[(ListBucketResultContents, InputStormEvent)],(ListBucketResultContents, InputStormEvent),NotUsed] = {
    Flow[Seq[(ListBucketResultContents, InputStormEvent)]]
        .map(seq => {
            val closestTuple = seq.map(tuple => {
                Try{
                    val format = new SimpleDateFormat("hhmmss")
                    val time:String = tuple._1.key.split("_")(1)
                    val dateBucket = format.parse(time)
                    val timeStringStorm = f"${tuple._2.beginHour}%02d${tuple._2.beginMinute}%02d00"
                    val dateStorm = format.parse(timeStringStorm)
                    val diff = Math.abs(dateStorm.getTime() - dateBucket.getTime())
                    (tuple._1, tuple._2, diff)
                }.toOption
            }).collect{ case Some(value) => value}
            .reduce((acc, element) => {
                if (acc._3 < element._3) acc else element
            })
            (closestTuple._1, closestTuple._2)
        })
  }
}
