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

package com.inland24.powersim

import java.util.concurrent.TimeUnit

import com.inland24.powersim.models.MyMessages.{Destroy, Init, Tick}
import com.inland24.powersim.models.PowerPlantConfig.{OnOffTypeConfig, PowerPlantsConfig, RampUpTypeConfig}
import com.inland24.powersim.models.PowerPlantType.{OnOffType, RampUpType}
import com.inland24.powersim.services.database.models.PowerPlantRow
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._

import scala.language.implicitConversions
import scala.concurrent.duration.FiniteDuration


package object models {

  // implicit conversion from database row types to model types
  implicit def toPowerPlantsConfig(seqPowerPlantRow: Seq[PowerPlantRow]): PowerPlantsConfig = {
    PowerPlantsConfig(
      DateTime.now(DateTimeZone.UTC),
      seqPowerPlantRow.map(powerPlantRow => {
        powerPlantRow.powerPlantTyp match {
          case _: OnOffTypeConfig =>
            OnOffTypeConfig(
              id = powerPlantRow.id,
              name = powerPlantRow.orgName,
              minPower = powerPlantRow.minPower,
              maxPower = powerPlantRow.maxPower,
              powerPlantType = OnOffType
            )
          case _: RampUpTypeConfig
            if powerPlantRow.rampRatePower.isDefined && powerPlantRow.rampRateSecs.isDefined =>
            RampUpTypeConfig(
              id = powerPlantRow.id,
              name = powerPlantRow.orgName,
              minPower = powerPlantRow.minPower,
              maxPower = powerPlantRow.maxPower,
              rampPowerRate = powerPlantRow.rampRatePower.get,
              rampRateInSeconds = FiniteDuration(powerPlantRow.rampRateSecs.get, TimeUnit.SECONDS),
              powerPlantType = RampUpType
            )
        }
      })
    )
  }

  implicit object MyMessagesWrites extends Writes[MyMessages] {

    def writes(myMessages: MyMessages): JsValue = myMessages match {
      case Init =>
        Json.toJson("INIT")
      case Destroy =>
        Json.toJson("DESTROY")
      case tick: Tick =>
        Json.obj(
          "source " -> tick.source,
          "long" -> tick.long
        )
    }
  }
}