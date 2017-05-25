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

import akka.actor.{Actor, Props}
import com.inland24.powersim.models.PowerPlantConfig.RampUpTypeConfig
import RampUpTypePowerPlantSimulator._

class RampUpTypePowerPlantSimulator private (cfg: RampUpTypeConfig)
  extends Actor {

  /*
   * 1. Take the config,
   * 2. Set up default values
   * 3. Prepare the case class that represents the state of this values
   * 4. send a self message with this new state of values
   */
  override def preStart(): Unit = {
    super.preStart()
    self ! Init
  }

  override def receive: Receive = {
    case Init =>
      context.become(active(PowerPlantState.init(PowerPlantState.empty(cfg.id), cfg.minPower)))
  }

  def active(state: PowerPlantState): Receive = {
    case StateRequest =>
      sender ! state
    case Dispatch(power) => // Dispatch to the specified power value
      PowerPlantState.turnOn(state, maxPower = cfg.maxPower)
    case Release => // Releasing means the Power plant is no longer in our control
      PowerPlantState.turnOff(state, minPower = cfg.minPower)
    case OutOfService =>
      state.copy(signals = PowerPlantState.unAvailableSignals)
    case ReturnToService =>
      self ! Init
  }
}
object RampUpTypePowerPlantSimulator {

  sealed trait Message
  case object Init extends Message
  case object StateRequest extends Message
  case class  Dispatch(power: Double) extends Message
  case object Release extends Message

  // These messages are meant for manually faulting and unfaulting the power plant
  case object OutOfService extends Message
  case object ReturnToService extends Message

  def props(cfg: RampUpTypeConfig): Props =
    Props(new RampUpTypePowerPlantSimulator(cfg))
}