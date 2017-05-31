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

package com.inland24.powersim.services.simulator.rampUpType

import akka.actor.{Actor, ActorLogging, Props}
import com.inland24.powersim.models.PowerPlantConfig.RampUpTypeConfig
import RampUpTypeSimulatorActor._
import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.execution.cancelables.SingleAssignmentCancelable
import monix.reactive.Observable

import scala.concurrent.Future


class RampUpTypeSimulatorActor private (cfg: RampUpTypeConfig)
  extends Actor with ActorLogging {

  /*
   * Initialize the Actor instance
   */
  override def preStart(): Unit = {
    super.preStart()
    log.info("in preStart() of the actor")
    self ! Init
  }

  override def postStop(): Unit = {
    super.postStop()
    log.info("in postStop() of the actor")
    cancelSubscription()
  }

  val subscription = SingleAssignmentCancelable()

  private def cancelSubscription(): Unit = {
    log.info(s"Cancelling subscription to RampUp the PowerPlant with id = ${cfg.id}")
    subscription.cancel()
  }

  // TODO: use a passed in ExecutionContext
  import monix.execution.Scheduler.Implicits.global
  private def startSubscription: Future[Unit] = Future {
    def onNext(long: Long): Future[Ack] = {
      self ! RampCheck
      Continue
    }

    val obs = Observable.intervalAtFixedRate(cfg.rampRateInSeconds)
    log.info(s"Subscribed to RampUp the PowerPlant with id = ${cfg.id}")
    subscription := obs.subscribe(onNext _)
  }

  override def receive: Receive = {
    case Init =>
      context.become(
        active(
          PowerPlantState.init(
            PowerPlantState.empty(cfg.id, cfg.minPower, cfg.rampPowerRate, cfg.rampRateInSeconds), cfg.minPower
          )
        )
      )
  }

  def active(state: PowerPlantState): Receive = {
    case StateRequest =>
      sender ! state
    case Dispatch(power) => // Dispatch to the specified power value
      startSubscription
      context.become(
        checkRamp(
          PowerPlantState.dispatch(state.copy(setPoint = power))
        )
      )
    case OutOfService =>
      context.become(
        active(state.copy(signals = PowerPlantState.unAvailableSignals))
      )
    case ReturnToService =>
      context.become(receive)
      self ! Init
  }

  /**
    * This state happens recursively when the PowerPlant ramps up
    * The recursivity happens until the PowerPlant is fully ramped
    * up. The recursivity is governed by the Monix Observable!
    */
  def checkRamp(state: PowerPlantState): Receive = {
    case StateRequest =>
      sender ! state
    case RampCheck =>
      val isDispatched = PowerPlantState.isDispatched(state)
      // We first check if we have reached the setPoint, if yes, we switch context
      if (isDispatched) {
        // we cancel the subscription first
        cancelSubscription()
        context.become(dispatched(state))
      } else {
        // time for another ramp up!
        context.become(
          checkRamp(PowerPlantState.dispatch(state))
        )
      }
    // If we need to throw this plant OutOfService, we do it
    case OutOfService =>
      // but as always, cancel the subscription first
      cancelSubscription()
      context.become(
        active(state.copy(signals = PowerPlantState.unAvailableSignals))
      )
  }

  /**
    * This is the state that is transitioned when the PowerPlant
    * is fully dispatched
    */
  def dispatched(state: PowerPlantState): Receive = {
    case StateRequest =>
      sender ! state
    // If we need to throw this plant OutOfService, we do it
    case OutOfService =>
      context.become(
        active(state.copy(signals = PowerPlantState.unAvailableSignals))
      )
    case ReturnToNormal =>
      context.become(receive)
      self ! Init
  }
}
object RampUpTypeSimulatorActor {

  sealed trait Message
  case object Init extends Message
  case object StateRequest extends Message
  case class  Dispatch(power: Double) extends Message
  case object Release extends Message
  case object RampCheck extends Message
  case object ReturnToNormal extends Message

  // These messages are meant for manually faulting and un-faulting the power plant
  case object OutOfService extends Message
  case object ReturnToService extends Message

  def props(cfg: RampUpTypeConfig): Props =
    Props(new RampUpTypeSimulatorActor(cfg))
}