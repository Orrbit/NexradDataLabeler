package org.ndl.models

import scala.util.Try

final case class InputStormEvent(
    beginYear: Int,
    beginMonth: Int,
    beginDay: Int,
    beginHour: Int,
    beginMinute: Int,
    eventId: Long,
    eventType: String,
    latitude: Float,
    longitude: Float,
    closestStation: String
)

object InputStormEvent{
    def apply(mapRow: Map[String, String]): Option[InputStormEvent] = {
        Try{
            InputStormEvent(
                mapRow.getOrElse("beginYear", throw new Exception).toInt,
                mapRow.getOrElse("beginMonth", throw new Exception).toInt,
                mapRow.getOrElse("beginDay", throw new Exception).toInt,
                mapRow.getOrElse("beginHour", throw new Exception).toInt,
                mapRow.getOrElse("beginMinute", throw new Exception).toInt,
                mapRow.getOrElse("eventId", throw new Exception).toLong,
                mapRow.getOrElse("eventType", throw new Exception),
                mapRow.getOrElse("latitude", throw new Exception).toFloat,
                mapRow.getOrElse("longitude", throw new Exception).toFloat,
                mapRow.getOrElse("stationId", throw new Exception)
            )
        }.toOption
    }

    def getAssociatedNexradPrefix(s: InputStormEvent): String = {
        return f"${s.beginYear}/${s.beginMonth}%02d/${s.beginDay}%02d/${s.closestStation}/"
    }
}
