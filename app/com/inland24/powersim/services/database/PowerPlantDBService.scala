/*
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

import com.inland24.powersim.config.DBConfig
import com.inland24.powersim.services.database.models.PowerPlantRow

import scala.concurrent.{ExecutionContext, Future}
import scala.async.Async._


class PowerPlantDBService(val dbConfig: DBConfig)(
    implicit ec: ExecutionContext) { self =>

  private val schema = DBSchema(dbConfig.slickDriver)
  private val database = dbConfig.database

  /** Note: These imports should be here! Do not move it */
  import schema._
  import schema.driver.api._

  // PowerPlant related CRUD services //
  def allPowerPlants(
      fetchOnlyActive: Boolean = false): Future[Seq[PowerPlantRow]] = {
    val query =
      if (fetchOnlyActive)
        PowerPlantTable.activePowerPlants
      else
        PowerPlantTable.all

    database.run(query.result)
  }

  def powerPlantById(id: Int): Future[Option[PowerPlantRow]] = {
    database.run(PowerPlantTable.powerPlantById(id).result.headOption)
  }

  def createPowerPlant(row: PowerPlantRow): Future[Int] = async {
    val q = PowerPlantTable.all
    val sql = (q returning q.map(_.id)) += row

    await(database.run(sql))
  }

  def inActivatePowerPlant(powerPlantId: Int): Future[Int] = {
    database.run(
      PowerPlantTable.deActivatePowerPlant(powerPlantId)
    )
  }
}
