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
import com.inland24.powersim.services.simulator.rampUpType.PowerPlantState._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._


class RampUpTypeSimulatorActorTest extends TestKit(ActorSystem("RampUpTypeSimulatorActorTest"))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  private val rampUpTypeCfg = RampUpTypeConfig(
    id = 1,
    name = "someConfig",
    minPower = 400.0,
    maxPower = 800.0,
    rampPowerRate = 100.0,
    rampRateInSeconds = 2.seconds,
    powerPlantType = PowerPlantType.OnOffType
  )

  private val initPowerPlantState = PowerPlantState.init(PowerPlantState.empty(
    id = rampUpTypeCfg.id,
    minPower = rampUpTypeCfg.minPower,
    rampRate = rampUpTypeCfg.rampPowerRate,
    rampRateInSeconds = rampUpTypeCfg.rampRateInSeconds
  ), minPower = rampUpTypeCfg.minPower)

  "RampUpTypeSimulatorActor" must {

    val rampUpTypeSimActor = system.actorOf(RampUpTypeSimulatorActor.props(rampUpTypeCfg))

    // PowerPlant # Init / Active tests
    "start with minPower when initialized to Active state" in {
      // We do this shit just so that the Actor has some time to Init
      within(2.seconds) {
        expectNoMsg()
      }
      rampUpTypeSimActor ! StateRequest
      expectMsgPF(3.seconds) {
        case state: PowerPlantState =>
          assert(state.signals === initPowerPlantState.signals, "signals did not match")
          assert(state.powerPlantId === initPowerPlantState.powerPlantId, "powerPlantId did not match")
          assert(state.rampRate === initPowerPlantState.rampRate, "rampRate did not match")
          assert(state.setPoint === initPowerPlantState.setPoint, "setPoint did not match")
        case x: Any => // If I get any other message, I fail
          fail(s"Expected a PowerPlantState as message response from the Actor, but the response was $x")
      }
    }

    // PowerPlant # RampUp tests
    "start to RampUp when a Dispatch command is sent" in {
      within(10.seconds) {
        rampUpTypeSimActor ! Dispatch(rampUpTypeCfg.maxPower)
        expectNoMsg
      }
      rampUpTypeSimActor ! StateRequest
      expectMsgPF() {
        case state: PowerPlantState =>
          // check the signals
          assert(
            state.signals(activePowerSignalKey).toDouble === 800.0,
            "expecting activePower to be 800.0, but was not the case"
          )
          assert(
            state.signals(isDispatchedSignalKey).toBoolean,
            "expected isDispatched signal to be true, but was false instead"
          )
          assert(
            state.signals(isAvailableSignalKey).toBoolean,
            "expected isAvailable signal to be true, but was false instead"
          )
          assert(
            state.rampRate === initPowerPlantState.rampRate,
            "rampRate did not match"
          )
          assert(
            state.setPoint === 800.0,
            "setPoint did not match"
          )
        case x: Any => // If I get any other message, I fail
          fail(s"Expected a PowerPlantState as message response from the Actor, but the response was $x")
      }
    }

    "ignore multiple Dispatch commands and should respond only to the first dispatch command" in {
      within(10.seconds) {
        // expected activePower should be this one here
        rampUpTypeSimActor ! Dispatch(rampUpTypeCfg.maxPower)

        // this dispatch command should be ignored!!
        rampUpTypeSimActor ! Dispatch(10000.0)
        expectNoMsg
      }
      rampUpTypeSimActor ! StateRequest
      expectMsgPF() {
        case state: PowerPlantState =>
          // check the signals
          assert(
            state.signals(activePowerSignalKey).toDouble === 800.0,
            "expecting activePower to be 800.0, but was not the case"
          )
          assert(
            state.signals(isDispatchedSignalKey).toBoolean,
            "expected isDispatched signal to be true, but was false instead"
          )
          assert(
            state.signals(isAvailableSignalKey).toBoolean,
            "expected isAvailable signal to be true, but was false instead"
          )
          assert(
            state.rampRate === initPowerPlantState.rampRate,
            "rampRate did not match"
          )
          assert(
            state.setPoint === 800.0,
            "setPoint did not match"
          )
        case x: Any => // If I get any other message, I fail
          fail(s"Expected a PowerPlantState as message response from the Actor, but the response was $x")
      }
    }

    // PowerPlant # OutOfService tests
    "send the PowerPlant into OutOfService when OutOfService message is sent during Active" in {
      within(5.seconds) {
        rampUpTypeSimActor ! OutOfService
        expectNoMsg()
      }

      rampUpTypeSimActor ! StateRequest
      expectMsgPF(5.seconds) {
        case state: PowerPlantState =>
          assert(state.signals === PowerPlantState.unAvailableSignals)
        case x: Any =>
          fail(s"Expected a PowerPlantState as message response from the Actor, but the response was $x")
      }
    }

    "throw the PowerPlant into OutOfService when OutOfService message is sent during RampUp" in {
      // 1. Send a Dispatch message
      within(2.seconds) {
        rampUpTypeSimActor ! Dispatch(rampUpTypeCfg.maxPower)
        expectNoMsg()
      }

      // 2. Send a OutOfService message
      rampUpTypeSimActor ! OutOfService

      // 3. Send a StateRequest message
      rampUpTypeSimActor ! StateRequest
      expectMsgPF() {
        case state: PowerPlantState =>
          assert(state.signals === PowerPlantState.unAvailableSignals)
        case x: Any =>
          fail(s"Expected a PowerPlantState as message response from the Actor, but the response was $x")
      }
    }

    // PowerPlant # ReturnToService tests
    "return the PowerPlant from OutOfService to Active when sending ReturnToService message" in {
      // 1. First make the PowerPlant OutOfService
      within(3.seconds) {
        rampUpTypeSimActor ! OutOfService
        expectNoMsg()
      }

      // 2. Send a ReturnToService message
      within(1.seconds) {
        rampUpTypeSimActor ! ReturnToService
      }

      // 3. Send a StateRequest message and check the signals
      rampUpTypeSimActor ! StateRequest
      expectMsgPF() {
        case state: PowerPlantState =>
          assert(state.signals === initPowerPlantState.signals, "signals did not match")
          assert(state.powerPlantId === initPowerPlantState.powerPlantId, "powerPlantId did not match")
          assert(state.rampRate === initPowerPlantState.rampRate, "rampRate did not match")
          assert(state.setPoint === initPowerPlantState.setPoint, "setPoint did not match")
        case x: Any =>
          fail(s"Expected a PowerPlantState as message response from the Actor, but the response was $x")
      }
    }

    // PowerPlant # ReturnToNormal tests
    "return the PowerPlant to Normal when ReturnToNormal message is sent in dispatched state" in {
      // 1. Send a Dispatch message
      within(12.seconds) {
        rampUpTypeSimActor ! Dispatch(rampUpTypeCfg.maxPower)
        expectNoMsg()
      }

      // 2. Send a ReturnToNormal message
      within(2.seconds) {
        rampUpTypeSimActor ! ReturnToNormal
        expectNoMsg()
      }

      // 3. Send a StateRequest message
      rampUpTypeSimActor ! StateRequest
      expectMsgPF(3.seconds) {
        case state: PowerPlantState =>
          assert(state.signals === initPowerPlantState.signals, "signals did not match")
          assert(state.powerPlantId === initPowerPlantState.powerPlantId, "powerPlantId did not match")
          assert(state.rampRate === initPowerPlantState.rampRate, "rampRate did not match")
          assert(state.setPoint === initPowerPlantState.setPoint, "setPoint did not match")
        case x: Any =>
          fail(s"Expected a PowerPlantState as message response from the Actor, but the response was $x")
      }
    }
  }
}