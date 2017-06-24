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
import com.inland24.powersim.models.PowerPlantConfig.{OnOffTypeConfig, RampUpTypeConfig}
import com.inland24.powersim.models.PowerPlantEvent.{PowerPlantCreateEvent, PowerPlantDeleteEvent, PowerPlantUpdateEvent}
import com.inland24.powersim.models.PowerPlantType.{OnOffType, RampUpType}
import com.inland24.powersim.services.simulator.onOffType.OnOffTypeSimulatorActor
import com.inland24.powersim.services.simulator.rampUpType.RampUpTypeSimulatorActor
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

  private def fetchActor(id: Long): Future[ActorRef] = {
    context.actorSelection(s"$simulatorActorNamePrefix$id").resolveOne(2.seconds)
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

    case RampUpType =>
      context.actorOf(
        RampUpTypeSimulatorActor.props(cfg.asInstanceOf[RampUpTypeConfig]),
        s"$simulatorActorNamePrefix$id"
      )
      Continue

    case _ => Continue
      Continue
  }

  private def fetchActorRef(id: Long): Future[Option[ActorRef]] = async {
    await(fetchActor(id).materialize) match {
      case Success(actorRef) =>
        log.info(s"Fetched Actor for PowerPlant with id = $id")
        Some(actorRef)
      case Failure(fail) =>
        log.warning(s"Unable to fetch Actor for PowerPlant with id = $id because of ${fail.getCause}")
        None
    }
  }

  private def timeoutPowerPlantActor(id: Long, actorRef: ActorRef, stoppedP: Promise[Continue]) = {
    // If the Promise is not completed within 3 seconds or in other words, if we
    // try to force Kill the actor. This will trigger an ActorKilledException which
    // will subsequently result in a Terminated(actorRef) message being sent to this
    // SimulatorSupervisorActor instance
    stoppedP.future.timeout(3.seconds).recoverWith {
      case _: TimeoutException =>
        log.error(s"Time out waiting for PowerPlant actor $id to stop, so sending a Kill message")
        actorRef ! Kill
        stoppedP.future
    }
  }

  def waitForStart(source: ActorRef): Receive = {
    case Continue =>
      source ! Continue
      context.become(receive)

    case someShit =>
      log.error(s"Unexpected message $someShit received while waiting for an actor to be started")
  }

  sealed trait ActorState
  case class Start(actorRef: ActorRef, promise: Promise[Continue]) extends ActorState
  case class Restart(actorRef: ActorRef, promise: Promise[Continue])extends ActorState
  case class Stop(actorRef: ActorRef, promise: Promise[Continue])extends ActorState
  case class Unknown(promise: Promise[Continue]) extends ActorState

  /**
    * TODO: We do the following:
    *
    * Create Event
    * ------------
    * 1. We check if the Actor for the given PowerPlant exists
    * 2. If it exists, we forcefully kill it and spin up a new Actor instance
    *
    * Update Event
    * ------------
    * 1. Check for existence of the Actor for the given PowerPlant
    * 2. If exists, stop it - asynchronously wait for the stop
    * 3. Start a new instance of this Actor
    *
    * Delete Event
    * ------------
    * 1. PowerPlantDeleteEvent is called
    * 2. We do a context.stop
    * 3. We set a Promise
    */
  override def receive(actorUpdates: Map[ActorRef, ActorState]): Receive = {

    /*
     * When we get a Terminated message, we remove this ActorRef from
     * the Map that we pass around!
     */
    case Terminated(actorRef) =>
      context.unwatch(actorRef)
      val newUpdate = if (actorUpdates.contains(actorRef)) {
        actorUpdates - actorRef
      } else {
        actorUpdates
      }
      context.become(receive(newUpdate))

    case PowerPlantCreateEvent(id, powerPlantCfg) =>
      log.info(s"Starting PowerPlant actor with id = $id and type ${powerPlantCfg.powerPlantType}")

      // Start the PowerPlant, and pipe the message to self
      startPowerPlant(id, powerPlantCfg).pipeTo(self)
      context.become(waitForStart(sender())) // The sender is the SimulatorSupervisorActor

    // TODO: Stop and Re-start the Actor instance and write some tests later!
    case PowerPlantUpdateEvent(id, powerPlantCfg) =>
      log.info(s"Re-starting PowerPlant actor with id = $id and type ${powerPlantCfg.powerPlantType}")

    case PowerPlantDeleteEvent(id, powerPlantCfg) =>
      log.info(s"Stopping PowerPlant actor with id = $id and type ${powerPlantCfg.powerPlantType}")

      val stoppedP = Promise[Continue]()
      fetchActorRef(id)
        .map {
          case Some(actorRef) =>
            // 1. We first try to stop using context.stop
            context.stop(actorRef)
            context.watch(actorRef)
            // Let's now as a fallback, Timeout the future and force kill the Actor if needed
            timeoutPowerPlantActor(id, actorRef, stoppedP)

          case _ => // TODO: Log and shit out!
        }
  }
}
object SimulatorSupervisorActor {

  case object Init

  def props(config: AppConfig): Props =
    Props(new SimulatorSupervisorActor(config))
}