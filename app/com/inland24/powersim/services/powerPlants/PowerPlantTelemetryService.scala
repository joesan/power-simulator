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

package com.inland24.powersim.services.powerPlants

import com.inland24.powersim.config.AppConfig
import com.inland24.powersim.models.PowerPlantConfig.OnOffTypeConfig
import com.inland24.powersim.models.{PowerPlantConfig, PowerPlantType}
import com.inland24.powersim.models.PowerPlantType.{OnOffType, RampUpType, UnknownType}
import com.inland24.powersim.services.powerPlants.PowerPlantResponse.DoubleValue
import com.inland24.powersim.services.simulator.RandomValueGeneratorService
import monix.reactive.Observable
import org.joda.time.{DateTime, DateTimeZone}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration


class PowerPlantTelemetryService private (cfg: AppConfig) {

  def onOffTypeTelemetrySignals(plantConfig: PowerPlantConfig): PowerPlantResponse = {
    PowerPlantResponse.Success(
      powerPlantId = plantConfig.id,
      signals = Map(
        "activePower" -> DoubleValue(
          RandomValueGeneratorService.DoubleRandomValue.random(plantConfig.minPower, plantConfig.minPower),
          DateTime.now(DateTimeZone.UTC)
        )
      )
    )
  }

  def rampUpTypeTelemetrySignals(plantConfig: PowerPlantConfig): PowerPlantResponse = {
    // TODO: implement
    null
  }

  // tODO
  def powerPlantTelemetryFor(plantConfig: PowerPlantConfig): Future[PowerPlantResponse] = plantConfig.powerPlantType match {
    case OnOffType   => Future.successful(onOffTypeTelemetrySignals(plantConfig))
    case RampUpType  => Future.successful(rampUpTypeTelemetrySignals(plantConfig))
    case UnknownType => Future.successful(PowerPlantResponse.Error(
      powerPlantId = plantConfig.id,
      errorMessage = "Unknown PowerPlantType",
      timeStamp = DateTime.now(DateTimeZone.UTC)
    ))
  }

  def streamSignalsFor(plantConfig: PowerPlantConfig, interval: FiniteDuration):
    Observable[PowerPlantResponse] = {
    Observable.unsafeCreate { subscriber =>
      Observable.intervalAtFixedRate(interval)
        .flatMap(_ => Observable.fromFuture(powerPlantTelemetryFor(plantConfig)))
        .unsafeSubscribeFn(subscriber)
    }
  }
}
object PowerPlantTelemetryService {

  def apply(cfg: AppConfig): PowerPlantTelemetryService =
    new PowerPlantTelemetryService(cfg)
}