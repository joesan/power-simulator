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
import com.inland24.powersim.observables.DBServiceObservable
import com.inland24.powersim.services.database.PowerPlantDBService
import com.inland24.powersim.services.database.models.PowerPlantRow
import monix.execution.Ack.Continue
import monix.execution.cancelables.SingleAssignmentCancelable
import monix.reactive.Observable
import monix.execution.FutureUtils.extensions._

// TODO: pass in zhe execution context
import scala.concurrent.ExecutionContext.Implicits.global

class DBServiceActor(dbConfig: DBConfig) extends Actor {

  // This represents the PowerPlantDBService instance
  val powerPlantDBService = new PowerPlantDBService(dbConfig)

  // This will be our subscription that we can use for cancelling!
  val powerPlantDBSubscription = SingleAssignmentCancelable()

  override def preStart(): Unit = {
    super.preStart()

    // Initialize the DBServiceObservable - for fetching the PowerPlant's and pipe it to self
    val obs: Observable[Seq[PowerPlantRow]] =
      DBServiceObservable.powerPlantDBServiceObservable(
        powerPlantDBService.dbConfig.refreshInterval,
        powerPlantDBService.allPowerPlants(fetchOnlyActive = true)
      )

    powerPlantDBSubscription := obs.subscribe { update =>
      (self ? update).map(_ => Continue)
    }
  }

  override def receive: Receive = {
    case powerPlantRowSeq: Seq[PowerPlantRow] =>
      context.become(active(powerPlantRowSeq))
  }

  def active(powerPlants: Seq[PowerPlantRow]): Receive = {
    case powerPlantRowSeq: Seq[PowerPlantRow] =>
      context.become(active(powerPlantRowSeq))
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