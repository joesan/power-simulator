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

package com.inland24.powersim.core

import akka.actor.{Actor, ActorKilledException, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.pattern.ask
import akka.util.Timeout
import com.inland24.powersim.actors.DBServiceActor
import com.inland24.powersim.core.SimulatorSupervisorActor.Init
import com.inland24.powersim.models.PowerPlantConfig
import com.inland24.powersim.models.PowerPlantConfig.{OnOffTypeConfig, PowerPlantsConfig, RampUpTypeConfig}
import com.inland24.powersim.models.PowerPlantType.{OnOffType, RampUpType}
import com.inland24.powersim.services.simulator.onOffType.OnOffTypeSimulatorActor
import com.inland24.powersim.services.simulator.rampUpType.RampUpTypeSimulatorActor
import monix.execution.FutureUtils.extensions._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration._

// TODO: What happens if any of the child actor dies???
/**
  * This actor is responsible for starting, stopping all
  * the simulators. We have one simulator per PowerPlant
  * where the type of the simulator is determined by the
  * type of the PowerPlant. This actor additionally also
  * listens for update events from the DBServiceActor
  * which sends events depending on if a new PowerPlant
  * is added, updated or deleted in the database.
  * @param dbActor
  */
class SimulatorSupervisorActor(dbActor: ActorRef) extends Actor
  with ActorLogging {

  implicit val timeout = Timeout(3.seconds)

  override def preStart() = {
    super.preStart()
    self ! Init
  }

  private def selectActor(name: String) : Future[ActorRef] = {
    context.actorSelection(name).resolveOne(2.seconds)
  }

  import monix.execution.Scheduler.Implicits.global

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 5.seconds) {
      case _: ActorKilledException =>
        SupervisorStrategy.Stop

      case e: Exception =>
        log.error("power-simulator", e)
        SupervisorStrategy.Resume
    }

  private def startSimulatorActors(powerPlantCfgSeq: Seq[PowerPlantConfig]) = {
    powerPlantCfgSeq.foreach {
      case powerPlantCfg if powerPlantCfg.powerPlantType == OnOffType =>
        selectActor(s"asset-simulator-${powerPlantCfg.id}").materialize.map {
          case Success(actorRef) => s"asset simulator actor with id ${powerPlantCfg.id} is already running $actorRef"
          case Failure(_) =>
            context.actorOf(
              OnOffTypeSimulatorActor.props(powerPlantCfg.asInstanceOf[OnOffTypeConfig]),
              s"asset-simulator-${powerPlantCfg.id}"
            )
        }
      case powerPlantCfg if powerPlantCfg.powerPlantType == RampUpType =>
        selectActor(s"asset-simulator-${powerPlantCfg.id}").materialize.map {
          case Success(resolved) => s"asset simulator actor with id ${powerPlantCfg.id} is already running"
          case Failure(_) =>
            context.actorOf(
              RampUpTypeSimulatorActor.props(powerPlantCfg.asInstanceOf[RampUpTypeConfig]),
              s"asset-simulator-${powerPlantCfg.id}"
            )
        }
    }
  }

  override def receive: Receive = {
    case Init =>
      (dbActor ? DBServiceActor.GetActivePowerPlants).mapTo[PowerPlantsConfig].materialize.map {
        case Success(powerPlantsCfg) =>
          startSimulatorActors(powerPlantsCfg.powerPlantConfigSeq)
        case Failure(fail) =>
          // TODO: What do we do when we fail....
      }
  }
}
object SimulatorSupervisorActor {

  case object Init

  def props(dbActorRef: ActorRef): Props =
    Props(new SimulatorSupervisorActor(dbActorRef))
}