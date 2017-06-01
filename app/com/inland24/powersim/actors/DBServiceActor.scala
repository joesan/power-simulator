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

package com.inland24.powersim.actors

import akka.actor.{Actor, Props}
import akka.pattern.ask
import com.inland24.powersim.actors.DBServiceActor.GetActivePowerPlants
import com.inland24.powersim.config.DBConfig
import com.inland24.powersim.models.PowerPlantConfig
import com.inland24.powersim.models.PowerPlantConfig.PowerPlantsConfig
import com.inland24.powersim.observables.DBServiceObservable
import com.inland24.powersim.services.database.PowerPlantDBService
import com.inland24.powersim.models
import monix.execution.Ack.Continue
import monix.execution.cancelables.SingleAssignmentCancelable
import monix.reactive.Observable

import scala.concurrent.duration._

// TODO: pass in the execution context
class DBServiceActor(dbConfig: DBConfig) extends Actor {

  implicit val timeout: akka.util.Timeout = 5.seconds // TODO: revisit this timeout duration

  // This represents the PowerPlantDBService instance
  val powerPlantDBService = new PowerPlantDBService(dbConfig)(scala.concurrent.ExecutionContext.Implicits.global)

  // This will be our subscription that we can use for cancelling!
  val powerPlantDBSubscription = SingleAssignmentCancelable()

  override def preStart(): Unit = {
    super.preStart()

    // Initialize the DBServiceObservable - for fetching the PowerPlant's and pipe it to self
    val obs: Observable[PowerPlantsConfig] =
      DBServiceObservable.powerPlantDBServiceObservable(
        powerPlantDBService.dbConfig.refreshInterval,
        powerPlantDBService.allPowerPlants(fetchOnlyActive = true)
      ).map(
        powerPlantRowSeq => models.toPowerPlantsConfig(powerPlantRowSeq)
      )

    // TODO: import scheduler from method parameters
    implicit val scheduler = monix.execution.Scheduler.Implicits.global //scala.concurrent.ExecutionContext.Implicits.global

    powerPlantDBSubscription := obs.subscribe { update =>
      (self ? update).map(_ => Continue)
    }
  }

  override def receive: Receive = {
    case powerPlantsConfig: PowerPlantsConfig =>
      context.become(active(powerPlantsConfig))
  }

  def active(powerPlants: PowerPlantsConfig): Receive = {
    case allPowerPlantsConfig: PowerPlantsConfig =>
      context.become(active(allPowerPlantsConfig))
    case GetActivePowerPlants =>
      sender() ! powerPlants
  }
}
object DBServiceActor {

  sealed trait Message
  case object GetActivePowerPlants extends Message

  def props(dbConfig: DBConfig): Props =
    Props(new DBServiceActor(dbConfig))
}