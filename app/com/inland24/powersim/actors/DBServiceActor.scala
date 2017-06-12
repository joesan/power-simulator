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

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import com.inland24.powersim.actors.DBServiceActor.GetActivePowerPlants
import com.inland24.powersim.config.DBConfig
import com.inland24.powersim.models.PowerPlantConfig.PowerPlantsConfig
import com.inland24.powersim.observables.DBServiceObservable
import com.inland24.powersim.services.database.PowerPlantDBService
import com.inland24.powersim.models
import com.inland24.powersim.models.{PowerPlantConfig, PowerPlantDeleteEvent, PowerPlantEvent}
import monix.execution.Ack.Continue
import monix.execution.cancelables.SingleAssignmentCancelable
import monix.reactive.Observable

import scala.concurrent.duration._

// TODO: pass in the execution context
// TODO: start emitting events for updates
class DBServiceActor(dbConfig: DBConfig) extends Actor with ActorLogging {

  implicit val timeout: akka.util.Timeout = 5.seconds // TODO: revisit this timeout duration

  // TODO: import scheduler from method parameters
  implicit val scheduler = monix.execution.Scheduler.Implicits.global

  // This represents the PowerPlantDBService instance
  val powerPlantDBService = new PowerPlantDBService(dbConfig)(scheduler)

  // This will be our subscription to fetch from the database
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

    powerPlantDBSubscription := obs.subscribe { update =>
      (self ? update).map(_ => Continue)
    }
  }

  override def postStop(): Unit = {
    super.postStop()

    log.info("Cancelling DB lookup subscription")
    powerPlantDBSubscription.cancel()
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

  type PowerPlantConfigMap = Map[Long, PowerPlantConfig]

  def toEvents(oldCfg: PowerPlantsConfig, newCfg: PowerPlantsConfig): Seq[PowerPlantEvent[PowerPlantConfig]] = {
    val oldMap = oldCfg.powerPlantConfigSeq.map(elem => elem.id -> elem).toMap
    val newMap = newCfg.powerPlantConfigSeq.map(elem => elem.id -> elem).toMap

    def deletedEvents(oldMap: PowerPlantConfigMap, newMap: PowerPlantConfigMap): Seq[PowerPlantEvent[PowerPlantConfig]] = {
      oldMap.keySet.filterNot(newMap.keySet)
        .map(id => PowerPlantDeleteEvent(id, oldMap(id))) // No way that this is going to throw element not found exception
        .toSeq
    }

    def updatedEvents(oldMap: PowerPlantConfigMap, newMap: PowerPlantConfigMap): Seq[PowerPlantEvent[PowerPlantConfig]] = {
      // TODO: implement
      Seq.empty
    }

    def createdEvents(oldMap: PowerPlantConfigMap, newMap: PowerPlantConfigMap): Seq[PowerPlantEvent[PowerPlantConfig]] = {
      newMap.keySet.filterNot(oldMap.keySet)
        .map(id => PowerPlantDeleteEvent(id, newMap(id))) // No way that this is going to throw element not found exception
        .toSeq
    }

    deletedEvents(oldMap, newMap) ++ updatedEvents(oldMap, newMap) ++ createdEvents(oldMap, newMap)
  }

  sealed trait Message
  case object GetActivePowerPlants extends Message

  def props(dbConfig: DBConfig): Props =
    Props(new DBServiceActor(dbConfig))
}