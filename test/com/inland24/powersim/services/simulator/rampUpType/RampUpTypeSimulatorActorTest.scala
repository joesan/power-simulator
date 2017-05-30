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

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.inland24.powersim.services.simulator.rampUpType.RampUpTypeSimulatorActor._
import com.inland24.powersim.models.PowerPlantConfig.RampUpTypeConfig
import com.inland24.powersim.models.PowerPlantType
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._


class RampUpTypeSimulatorActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val rampUpTypeCfg = RampUpTypeConfig(
    id = 1,
    name = "someConfig",
    minPower = 10.0,
    maxPower = 100.0,
    rampPowerRate = 100.0,
    rampRateInSeconds = 2.seconds,
    powerPlantType = PowerPlantType.OnOffType
  )

  private val rampUpTypeSimActor = system.actorOf(RampUpTypeSimulatorActor.props(rampUpTypeCfg))

  "RampUpTypeSimulatorActor" must {

    "start with minPower when in initialized to Active state" in {
      val initPowerPlantState = PowerPlantState.init(PowerPlantState.empty(
        id = rampUpTypeCfg.id,
        minPower = rampUpTypeCfg.minPower,
        rampRate = rampUpTypeCfg.rampPowerRate,
        rampRateInSeconds = rampUpTypeCfg.rampRateInSeconds
      ), minPower = rampUpTypeCfg.minPower)

      rampUpTypeSimActor ! StateRequest
      expectMsgPF() {
        case state: PowerPlantState =>
          assert(state.signals === initPowerPlantState.signals, "signals did not match")
          assert(state.powerPlantId === initPowerPlantState.powerPlantId, "powerPlantId did not match")
          assert(state.rampRate === initPowerPlantState.rampRate, "rampRate did not match")
          assert(state.setPoint === initPowerPlantState.setPoint, "setPoint did not match")
        case x: Any => // If I get any other message, I fail
          fail(s"Expected a PowerPlantState as message response from the Actor, but the response was $x")
      }
    }
  }
}