package com.dataartisans.flink_demo.examples

import com.dataartisans.flink_demo.datatypes.{GeoPoint, TaxiRide}
import com.dataartisans.flink_demo.sinks.ElasticSearchUpsertSink
import com.dataartisans.flink_demo.sources.TaxiRideSource
import com.dataartisans.flink_demo.utils.{TaxiRideStreamEnvironment, TaxiRideUtility}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala.DataStream
import com.dataartisans.flink_demo.datatypes.{GeoPoint, TaxiRide}
import com.dataartisans.flink_demo.sinks.ElasticSearchUpsertSink
import com.dataartisans.flink_demo.sources.TaxiRideSource
import com.dataartisans.flink_demo.utils.{TaxiRideStreamEnvironment, TaxiRideUtility}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.assigners.TumblingTimeWindows
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.time.Time._
import org.apache.flink.streaming.api.windowing.triggers.Trigger
import org.apache.flink.streaming.api.windowing.triggers.Trigger.{TriggerContext, TriggerResult}
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._

object TwoFixedCitiesJoinCount {
   case class passengerInfo(cellId: Int, timeStamp : Long , count : Int)

  def main(args: Array[String]) {

    // input parameters
    val data = "./data/data.gz"
    val maxServingDelay = 60
    val servingSpeedFactor = 600000f

    // window parameters
    val countWindowLength = 500 // window size in min
    val countWindowFrequency =  5 // window trigger interval in min
    val earlyCountThreshold = 50

    // Elasticsearch parameters
    val writeToElasticsearch = true // set to true to write results to Elasticsearch
    val elasticsearchHost = "127.0.0.1" // look-up hostname in Elasticsearch log output
    val elasticsearchPort = 9300
    val p1 = new GeoPoint(-73.978837999999996,40.740819999999999)
    val p2 = new GeoPoint(-73.783680000000004,40.646217)

    // set up streaming execution environment
    val env: StreamExecutionEnvironment = TaxiRideStreamEnvironment.env
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    // Define the data source
    val ridesCity1: DataStream[TaxiRide] = env.addSource(new TaxiRideSource(
      data, maxServingDelay, servingSpeedFactor))

    val cleansedRidesCity1 = ridesCity1
      // filter for trip end events
      .filter( !_.isStart )
      // filter for events in NYC
      .filter( r => TaxiRideUtility.IsEqualToCity(r.location,p1) )

    // map location coordinates to cell Id, timestamp, and passenger count
    val cellIdsCity1: DataStream[(Int, Long, Short)] = cleansedRidesCity1
      .map { r =>
        ( TaxiRideUtility.mapToGridCell(r.startlocation), r.time.getMillis, r.passengerCnt )
      }

    val passengerCntsCity1: DataStream[(Int, Long, Int)] = cellIdsCity1
      // key stream by cell Id
      .keyBy(_._1)
      // sum passengers per cell Id and update time
      .fold((0, 0L, 0), (s: (Int, Long, Int) , r: (Int, Long, Short)) =>
    { (r._1, s._2.max(r._2), s._3 + r._3) } )

    // Define the data source
    val ridesCity2: DataStream[TaxiRide] = env.addSource(new TaxiRideSource(
      data, maxServingDelay, servingSpeedFactor))

    val cleansedRidesCity2 = ridesCity2
      // filter for trip end events
      .filter( !_.isStart )
      // filter for events in NYC
      .filter( r => TaxiRideUtility.IsEqualToCity(r.location,p2) )

    // map location coordinates to cell Id, timestamp, and passenger count
    val cellIdsCity2: DataStream[(Int, Long, Short)] = cleansedRidesCity2
      .map { r =>
        ( TaxiRideUtility.mapToGridCell(r.startlocation), r.time.getMillis, r.passengerCnt )
      }

    val passengerCntsCity2: DataStream[(Int, Long, Int)] = cellIdsCity2
      // key stream by cell Id
      .keyBy(_._1)
      // sum passengers per cell Id and update time
      .fold((0, 0L, 0), (s: (Int, Long, Int), r: (Int, Long, Short)) =>
    { (r._1, s._2.max(r._2), s._3 + r._3) } )

    val joinedStream1: DataStream[(Int, Long, Int)] = passengerCntsCity1.join(passengerCntsCity2)
      .where(_._1)
      .equalTo(_._1)
      .window(TumblingTimeWindows.of(Time.milliseconds(countWindowLength)))
      .apply { (g, s) => (g._1, g._2.max(s._2), g._3 + s._3) }

    // map cell Id back to GeoPoint
    val cntByLocationCity: DataStream[(Int, Long, GeoPoint, Int)] = joinedStream1
      .map( r => (r._1, r._2, TaxiRideUtility.getGridCellCenter(r._1), r._3 ) )

    // print to console
    cntByLocationCity
      .print()

    if (writeToElasticsearch) {
      // write to Elasticsearch
      cntByLocationCity
        .addSink(new CntTimeByLocUpsert(elasticsearchHost, elasticsearchPort))
    }

    env.execute("Total passenger count per location")
    /*
    // map cell Id back to GeoPoint
    val cntByLocationCity1: DataStream[(Int, Long, GeoPoint, Int)] = passengerCntsCity1
      .map( r => (r._1, r._2, NycGeoUtils.getGridCellCenter(r._1), r._3 ) )

    // print to console
    cntByLocationCity1
      .print()

    if (writeToElasticsearch) {
      // write to Elasticsearch
      cntByLocationCity1
        .addSink(new CntTimeByLocUpsert(elasticsearchHost, elasticsearchPort))
    }*/

  }

  /*val leftData: DataSet[(String, Int, Int)] = ...
  val rightData: DataSet[(String, Int)] = ...
  val joined: DataSet[(String, Int, Int)] = leftData
    .join(rightData).where(0).equalTo(0) { (l, r) => (l._1, l._2, l._3 + r._2) ) }

  def joinStreams(
                   passengerCount1 : DataStream[(Int, Long, Int)],
                   passengerCount2 : DataStream[(Int, Long, Int)]
                 ) : DataStream[(Int,Long,Int)]  = {

    passengerCount1.join(passengerCount2)
      .where(0)
      .equalTo(0) {
         (g, s) => (g._1, g._2, g._1 + s._2)
      }
  }*/

  class CntTimeByLocUpsert(host: String, port: Int)
    extends ElasticSearchUpsertSink[(Int, Long, GeoPoint, Int)](
      host,
      port,
      "elasticsearch",
      "nyc-idx2",
      "popular-locations") {

    override def insertJson(r: (Int, Long, GeoPoint, Int)): Map[String, AnyRef] = {
      Map(
        "location" -> (r._3.lat+","+r._3.lon).asInstanceOf[AnyRef],
        "time" -> r._2.asInstanceOf[AnyRef],
        "cnt" -> r._4.asInstanceOf[AnyRef]
      )
    }

    override def updateJson(r: (Int, Long, GeoPoint, Int)): Map[String, AnyRef] = {
      Map[String, AnyRef] (
        "time" -> r._2.asInstanceOf[AnyRef],
        "cnt" -> r._4.asInstanceOf[AnyRef]
      )
    }

    override def indexKey(r: (Int, Long, GeoPoint, Int)): String = {
      // index by location
      r._1.toString
    }
  }

}
