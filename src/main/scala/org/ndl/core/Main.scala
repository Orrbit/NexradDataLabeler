package org.ndl.core

import akka.stream.alpakka.s3._
import com.typesafe.scalalogging.{ LazyLogging }
import org.ndl.infrastructure.AkkaStreamUtils
import akka.stream.alpakka.s3.scaladsl.{S3}
import akka.stream.alpakka.s3.S3Settings
import akka.stream.scaladsl.{Source, Sink, Flow, Compression}
import akka.util.ByteString
import akka.{Done, NotUsed}
import scala.util.Success
import scala.util.Failure
import java.io.PrintWriter
import java.io.StringWriter
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.NetcdfFile;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.util.zip.GZIPInputStream
import java.io.ByteArrayInputStream

import akka.stream.scaladsl.Compression
import akka.stream.scaladsl.StreamConverters
import java.io.ByteArrayOutputStream

import scala.collection.JavaConverters._
import ucar.ma2.Section
import ucar.nc2.dataset.NetcdfDataset
import akka.stream.scaladsl.FileIO
import java.nio.file.Paths
import org.ndl.models.InputStormEvent
import org.ndl.infrastructure.ConfigUtils
import pureconfig.generic.auto._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

final case class NexradDataConf(
  stormLocationFile: String,
  nexradBucket: String
)

object Main extends LazyLogging{

  def main(args: Array[String]): Unit = {
    import AkkaStreamUtils.defaultActorSystem._

    implicit val conf = ConfigUtils.loadAppConfig[NexradDataConf](
        "org.ndl.core.nexrad-data-conf"
      )

    logger.info("Starting data label pipeline...")


    val firstLine = Stages.csvFileSource(conf.stormLocationFile).limit(150)
      .via(Stages.flowMapToStormEvent())
      .via(Stages.flowStormEventToBucketContents(conf.nexradBucket))
      .via(Stages.flowGetClosestFileToStormEvent())
      .runWith(Sink.foreach( tuple => {
        logger.info(s"\n${tuple._2.toString()} \n\t Above event has closest bucket ${tuple._1.key} \n\n")
      }))

    Await.ready(firstLine, Duration.Inf)

    return //return statement here to disable the flow into viewing netcdf data

    val bucketKey = "2014/01/01/KBUF/KBUF20140101_000436_V06.gz"

    val s3File: Source[Option[(Source[ByteString, NotUsed], ObjectMetadata)], NotUsed] =
      S3.download(conf.nexradBucket, bucketKey)
      
    val fileStream = s3File.runWith(Sink.head)

    fileStream.onComplete({
      case Success(fileOption) => fileOption match {
        case Some(value) => {
          logger.info(s"downloaded an object with metadata ${value._2.metadata.map(_.toString())}")
          val outputStream = new ByteArrayOutputStream()
          val futureIo = value._1.via(Compression.gunzip()).runWith(StreamConverters.fromOutputStream(() => outputStream))
          logger.info("Got back all unzipped bytes")
          futureIo.onComplete({
            case Success(value) => {
              logger.info(s"finished with a file size of ${value.count}")
              val f = NetcdfFile.openInMemory(bucketKey, outputStream.toByteArray())
              logger.info(s"Turned into netcdf file: ${f.getFileTypeDescription()}")
              
              val dataset = new NetcdfDataset(f)
              val radialSweep: RadialDatasetSweep = TypedDatasetFactory.open(FeatureType.RADIAL, dataset, null, null).asInstanceOf[RadialDatasetSweep]
              debugRadialDatasetInformation(radialSweep)
              

            }
            case Failure(exception) => logger.info(exception.getMessage())
          })
          
        }        
        case None => logger.info(s"S3 file was not found")
      }
      case Failure(exception) => {
        val sw = new StringWriter
        exception.printStackTrace(new PrintWriter(sw))
        logger.info(s"Could not get s3 file: ${sw.toString}")
      }
    })

    Thread.sleep(10000)

    logger.info("Done with pipeline...")

  }


  def downloadNetcdf() = {
    val bucket = "noaa-nexrad-level2"
    val bucketKey = "2014/01/01/KBUF/KBUF20140101_000436_V06.gz"

    val s3 = new AmazonS3Client(new AnonymousAWSCredentials(), new ClientConfiguration());
    val usEast1 = Region.getRegion(Regions.US_EAST_1);
    s3.setRegion(usEast1);

    val obj = s3.getObject(bucket, bucketKey);
    val objectInputStream = obj.getObjectContent();

    val gunzip = new GZIPInputStream(new ByteArrayInputStream(objectInputStream.readAllBytes()));

    val ncfile = NetcdfFile.openInMemory(bucketKey, gunzip.readAllBytes());
  }

  def debugRadialDatasetInformation(ds: RadialDatasetSweep){
    logger.info("Formatted in radial dataset sweep")
    logger.info(s"station id ${ds.getRadarID()} ${ds.getRadarName()}")

    logger.info(s"station located at ${ds.getCommonOrigin.getLatitude()} ${ds.getCommonOrigin.getLongitude()}")

    //RadialVelocity is the other dimension that we are after
    val ref = ds.getDataVariable("Reflectivity").asInstanceOf[RadialDatasetSweep.RadialVariable]

    logger.info(s"has this many sweeps ${ref.getNumSweeps()}")
    
    val sweep0 = ref.getSweep(0)
    logger.info(s"# rads ${sweep0.getRadialNumber()}")
    logger.info(s"# gates ${sweep0.getGateNumber()}")
    logger.info(s"first distance ${sweep0.getRangeToFirstGate()}")
    logger.info(s"gate size ${sweep0.getGateSize()}")
    logger.info(s"sweep at radial 1:  ${sweep0.readData(0)}")
  }
}