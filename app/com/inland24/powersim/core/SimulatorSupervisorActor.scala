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

import akka.actor.{Actor, ActorKilledException, ActorLogging, ActorRef, Kill, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.pattern.pipe
import akka.util.Timeout
import com.inland24.powersim.actors.DBServiceActor
import com.inland24.powersim.config.AppConfig
import com.inland24.powersim.core.SimulatorSupervisorActor.Init
import com.inland24.powersim.models.PowerPlantConfig
import com.inland24.powersim.models.PowerPlantConfig.OnOffTypeConfig
import com.inland24.powersim.models.PowerPlantEvent.{PowerPlantCreateEvent, PowerPlantDeleteEvent, PowerPlantUpdateEvent}
import com.inland24.powersim.models.PowerPlantType.OnOffType
import com.inland24.powersim.services.simulator.onOffType.OnOffTypeSimulatorActor
import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.execution.FutureUtils.extensions._

import scala.async.Async.{async, await}
import scala.concurrent.{Future, Promise, TimeoutException}
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
  */
class SimulatorSupervisorActor(config: AppConfig) extends Actor
  with ActorLogging {

  val simulatorActorNamePrefix = "powerPlant-simulator-actor-"

  implicit val timeout = Timeout(3.seconds)

  // Our DBServiceActor reference
  val dbActor = context.actorOf(DBServiceActor.props(config.database))

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

  private def stopActors(events: Seq[PowerPlantDeleteEvent[PowerPlantConfig]]): Future[Ack] = async {
    events.foreach {
      case event => stopActor(event)
    }
    Continue
  }

  private def stopActor(event: PowerPlantDeleteEvent[PowerPlantConfig]): Future[Ack] = async {
    await(fetchActor(event.powerPlantCfg.id).materialize) match {
      case Success(actorRef) =>
        log.info(s"Stopping Actor for PowerPlant with id = ${event.powerPlantCfg.id}")
        context.stop(actorRef)
        Continue
      case Failure(fail) =>
        log.error(s"Could not fetch Actor instance for PowerPlant =${event.powerPlantCfg.id} because of: $fail")
        Continue
    }
  }

  private def fetchActor(id: Long): Future[ActorRef] = {
    context.actorSelection(s"$simulatorActorNamePrefix$id").resolveOne(2.seconds)
  }

  def waitForStart(source: ActorRef): Receive = {
    case Continue =>
      source ! Continue
      context.become(receive)

    case someShit =>
      log.error(s"Unexpected message $someShit received while waiting for an actor to be started")
  }

  private def waitForStop(source: ActorRef, stoppedP: Promise[Continue]): Receive = {
    case Continue =>
      source ! Continue
      context.become(receive)

    case Terminated(actorRef) =>
      context.unwatch(actorRef)
      stoppedP.success(Continue)

    case someShit =>
      log.error(s"Unexpected message $someShit received while waiting for an actor to be stopped")
  }

  // ***********************************************************************************
  // Methods to Start and Stop PowerPlant Actor instances
  // ***********************************************************************************
  private def startPowerPlant(id: Long, cfg: PowerPlantConfig): Future[Ack] = cfg.powerPlantType match {
    case OnOffType =>
      context.actorOf(
        OnOffTypeSimulatorActor.props(cfg.asInstanceOf[OnOffTypeConfig]),
        s"$simulatorActorNamePrefix$id"
      )
      Continue

    case OnOffType =>
      context.actorOf(
        OnOffTypeSimulatorActor.props(cfg.asInstanceOf[OnOffTypeConfig]),
        s"$simulatorActorNamePrefix$id"
      )
      Continue
  }

  private def stopPowerPlant(id: Long, stoppedP: Promise[Continue]): Future[Ack] = async {
    await(fetchActor(id).materialize) match {
      case Success(actorRef) =>
        log.info(s"Stopping Actor for PowerPlant with id = $id")
        context.watch(actorRef)
        context.stop(actorRef)

        // If the Promise is not completed within 3 seconds or in other words, if we
        // try to force Kill the actor
        val stopActorFallback = stoppedP.future.timeout(3.seconds).recoverWith {
          case _: TimeoutException =>
            log.error(
              s"Time out waiting for PowerPlant actor $id to stop, so sending a Kill message"
            )

            actorRef ! Kill
            stoppedP.future
        }

        await(stopActorFallback)
      case Failure(fail) =>
        log.error(s"Could not fetch Actor instance for PowerPlant = $id because of: $fail")
        Continue
    }
  }
  // ***********************************************************************************

  override def receive: Receive = {
    case PowerPlantCreateEvent(id, powerPlantCfg) =>
      log.info(s"Starting PowerPlant actor with id = $id and type ${powerPlantCfg.powerPlantType}")

      // Start the PowerPlant, and pipe the message to self
      startPowerPlant(id, powerPlantCfg).pipeTo(self)
      context.become(waitForStart(sender())) // The sender is the SimulatorSupervisorActor

    case PowerPlantUpdateEvent(id, powerPlantCfg) =>
      log.info(s"Re-starting PowerPlant actor with id = $id and type ${powerPlantCfg.powerPlantType}")

      // TODO: Stop and Re-start the actor instance

    case PowerPlantDeleteEvent(id, powerPlantCfg) =>
      log.info(s"Stopping PowerPlant actor with id = $id and type ${powerPlantCfg.powerPlantType}")

      val stoppedP = Promise[Continue]()
      stopPowerPlant(id, stoppedP).pipeTo(self)
      context.become(waitForStop(sender(), stoppedP))
  }
}
object SimulatorSupervisorActor {

  case object Init

  def props(config: AppConfig): Props =
    Props(new SimulatorSupervisorActor(config))
}