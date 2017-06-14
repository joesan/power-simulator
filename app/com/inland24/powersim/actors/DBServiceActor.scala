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
import com.inland24.powersim.models.PowerPlantEvent.{PowerPlantCreateEvent, PowerPlantDeleteEvent, PowerPlantUpdateEvent}
import com.inland24.powersim.models.{PowerPlantConfig, PowerPlantEvent}
import monix.execution.Ack.Continue
import monix.execution.cancelables.SingleAssignmentCancelable
import monix.reactive.Observable

import scala.concurrent.duration._

// TODO: pass in the execution context
// TODO: start emitting events for updates
class DBServiceActor(dbConfig: DBConfig) extends Actor with ActorLogging {

  // TODO: revisit this timeout duration, should come from parameters
  implicit val timeout: akka.util.Timeout = 5.seconds

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
  type PowerPlantEvents = Seq[PowerPlantEvent[PowerPlantConfig]]

  /**
    * Transform a given sequence of old and new state of PowerPlantConfig
    * to a sequence of events. These events will determine how the actors
    * representing the PowerPlant might be stopped, started or re-started
    * depending on whether the PowerPlant is deleted, created or updated.
    */
  def toEvents(oldCfg: PowerPlantsConfig, newCfg: PowerPlantsConfig): PowerPlantEvents = {
    val oldMap = oldCfg.powerPlantConfigSeq.map(elem => elem.id -> elem).toMap
    val newMap = newCfg.powerPlantConfigSeq.map(elem => elem.id -> elem).toMap

    def deletedEvents(oldMap: PowerPlantConfigMap, newMap: PowerPlantConfigMap): PowerPlantEvents = {
      oldMap.keySet.filterNot(newMap.keySet)
        .map(id => PowerPlantDeleteEvent(id, oldMap(id))) // No way this is going to throw element not found exception
        .toSeq
    }

    def updatedEvents(oldMap: PowerPlantConfigMap, newMap: PowerPlantConfigMap): PowerPlantEvents = {
      oldMap.keySet.intersect(newMap.keySet)
        .collect {
          case id if !oldMap(id).equals(newMap(id)) => PowerPlantUpdateEvent(id, newMap(id))
        }
        .toSeq
    }

    def createdEvents(oldMap: PowerPlantConfigMap, newMap: PowerPlantConfigMap): PowerPlantEvents = {
      newMap.keySet.filterNot(oldMap.keySet)
        .map(id => PowerPlantCreateEvent(id, newMap(id))) // No way this is going to throw element not found exception
        .toSeq
    }

    deletedEvents(oldMap, newMap) ++ updatedEvents(oldMap, newMap) ++ createdEvents(oldMap, newMap)
  }

  sealed trait Message
  case object GetActivePowerPlants extends Message

  def props(dbConfig: DBConfig): Props =
    Props(new DBServiceActor(dbConfig))
}