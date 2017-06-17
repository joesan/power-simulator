/*
 *
 * Copyright (c) 2017 joesan @ http://github.com/joesan
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inland24.powersim.services.database

import java.sql.Timestamp
import java.util.concurrent.TimeUnit

import com.inland24.powersim.models.PowerPlantType
import com.inland24.powersim.services.database.models.{AddressRow, PowerPlantRow}
import org.joda.time.{DateTime, DateTimeZone}
import slick.jdbc.JdbcProfile

import scala.concurrent.duration._


// This schema reads / maps the tables in our database
final class DBSchema private (val driver: JdbcProfile) {

  import driver.api._

  /**
    * Mapping for using Joda Time.
    */
  implicit def dateTimeMapping = MappedColumnType.base[DateTime, java.sql.Timestamp](
    dt => new Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime, DateTimeZone.UTC)
  )

  /**
    * Mapping for using PowerPlantType conversions.
    */
  implicit def meterTypeMapping = MappedColumnType.base[PowerPlantType, String](
    powerPlantType    => PowerPlantType.toString(powerPlantType),
    powerPlantTypeStr => PowerPlantType.fromString(powerPlantTypeStr)
  )

  /**
    * Mapping between Int and FiniteDuration in Seconds
    */
  implicit def finiteDurationMapping = MappedColumnType.base[FiniteDuration, Long](
    finiteDuration => finiteDuration.toSeconds,
    longSeconds    => FiniteDuration(longSeconds, TimeUnit.SECONDS)
  )

  ///////////////// PowerPlant Table
  /**
    * The PowerPlant details are maintained in the PowerPlant table
    */
  class PowerPlantTable(tag: Tag) extends Table[PowerPlantRow](tag, "PowerPlant") {
    def id            = column[Int]("id", O.PrimaryKey)
    def orgName       = column[String]("powerPlantName")
    def orgAddressId  = column[Int]("powerPlantAddressId")
    def isActive      = column[Boolean]("isActive")
    def minPower      = column[Double]("minPower")
    def maxPower      = column[Double]("maxPower")
    def powerRampRate = column[Option[Double]]("rampRatePower")
    def rampRateSecs  = column[Option[Long]]("rampRateSecs")
    def powerPlantType= column[PowerPlantType]("powerPlantType")
    def createdAt     = column[DateTime]("created_at")
    def updatedAt     = column[DateTime]("updated_at")

    def * = {
      (id, orgName, orgAddressId, isActive, minPower, maxPower,
        powerRampRate, rampRateSecs, powerPlantType, createdAt, updatedAt) <>
        (PowerPlantRow.tupled, PowerPlantRow.unapply)
    }
  }

  object PowerPlantTable {
    /**
      * A TableQuery can be used for composing queries, inserts
      * and pretty much anything related to the sensors table.
      */
    val all = TableQuery[PowerPlantTable]

    /**
      * Query to filter and fetch all active organizations
      */
    def activePowerPlants = {
      all.filter(_.isActive === true)
    }

    /**
      * Query to de-activate a PowerPlant
      * (i.e., to set the isActive flag to false)
      */
    def deActivatePowerPlant(id: Int) = {
      all.filter(_.id === id)
      .map(elem => elem.isActive)
      .update(false)
    }

    /**
      * Query to filter and fetch organization by a given id
      */
    def powerPlantById(id: Int) = {
      all.filter(_.id === id)
    }
  }

  ///////////////// Address Table
  /**
    * The Address details are maintained in the Address table
    */
  class AddressTable(tag: Tag) extends Table[AddressRow](tag, "Address") {
    def id        = column[Int]("id", O.PrimaryKey)
    def streetNum = column[Int]("streetNum")
    def street    = column[String]("street")
    def city      = column[String]("city")
    def plz       = column[Int]("plz")
    def country   = column[String]("country")

    def * = {
      (id, streetNum, street, city, plz, country) <>
        (AddressRow.tupled, AddressRow.unapply)
    }
  }

  object AddressTable {

    val all = TableQuery[AddressTable]

    def addressById(id: Int) = {
      all.filter(_.id === id)
    }
  }
}
object DBSchema {
  def apply(driver: JdbcProfile) = {
    new DBSchema(driver)
  }
}