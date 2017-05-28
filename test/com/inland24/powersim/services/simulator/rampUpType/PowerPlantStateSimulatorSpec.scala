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

import com.inland24.powersim.models.PowerPlantConfig.RampUpTypeConfig
import com.inland24.powersim.models.PowerPlantType
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.FlatSpec

import scala.concurrent.duration._


class PowerPlantStateSimulatorSpec extends FlatSpec {

  val cfg = RampUpTypeConfig(
    id = 1,
    name = "RampUpType",
    minPower = 400,
    maxPower = 800,
    rampPowerRate = 100,
    rampRateInSeconds = 4.seconds,
    powerPlantType = PowerPlantType.RampUpType
  )

  behavior of PowerPlantState.getClass.getCanonicalName

  "PowerPlantState#empty" should "start with a default state" in {
    val emptyState = PowerPlantState.empty(cfg.id, cfg.minPower, cfg.rampPowerRate, cfg.rampRateInSeconds)

    assert(emptyState.rampRate == cfg.rampPowerRate)
    assert(emptyState.powerPlantId == cfg.id)
    assert(emptyState.lastRampTime.getMillis < DateTime.now(DateTimeZone.UTC).getMillis)
    assert(emptyState.signals.size === 0)
  }

  "PowerPlantState#init" should "initialize the default signals " +
    "(available = true, activePower = minPower, isDispatched = false)" in {
    val initState = PowerPlantState.init(
      PowerPlantState.empty(cfg.id, cfg.minPower, cfg.rampPowerRate, cfg.rampRateInSeconds), cfg.minPower
    )

    assert(initState.signals.size === 3) // expecting 3 elements in the signals Map
    initState.signals.foreach {
      case (key, value) if key == PowerPlantState.isDispatchedKey      => assert(!value.toBoolean)
      case (key, value) if key == PowerPlantState.isAvailableSignalKey => assert(value.toBoolean)
      case (key, value) if key == PowerPlantState.activePowerSignalKey => assert(value.toDouble === cfg.minPower)
    }

    assert(initState.setPoint === cfg.minPower)
    assert(initState.rampRate === cfg.rampPowerRate)
  }

  "PowerPlantState#dispatch" should "start dispatching the power plant according to its ramp rate" in {
    val initState = PowerPlantState.init(
      PowerPlantState.empty(cfg.id, cfg.minPower, cfg.rampPowerRate, cfg.rampRateInSeconds), cfg.minPower
    )

    /*
     * Let's dispatch this Plant to its maxPower which is 800
     * The plant is currently operating at its minPower which is 400
     * and it has a rampRate of 100 in 4 seconds, so for it to go
     * from 400 to 800, it needs in total 16 seconds
     * Let us now test if this happens!
     * The first dispatch command should take its activePower to 500
     */
    val dispatchState1 = PowerPlantState.dispatch(initState.copy(setPoint = cfg.maxPower))
    assert(dispatchState1.signals(PowerPlantState.activePowerSignalKey).toDouble === 500)

    /*
     * On our second dispatch, we should go from 500 to 600, but we got to wait 4 seconds
     * Blocking may be a bad idea, so we simulate time (i.e., add 4 seconds to the isRampUp check)
     */
    val dispatchState2 = PowerPlantState.dispatch(dispatchState1.copy(lastRampTime = dispatchState1.lastRampTime.plusSeconds(4)))
    assert(dispatchState2.signals(PowerPlantState.activePowerSignalKey).toDouble === 600)

    // Let's try another dispatch immediately, this should have no effect and we should still stay at 600
    val dispatchState2_copy = PowerPlantState.dispatch(dispatchState2)
    assert(dispatchState2 === dispatchState2_copy)

    // Another 4 seconds elapse, we move to 700
    val dispatchState3 = PowerPlantState.dispatch(dispatchState2.copy(lastRampTime = dispatchState2.lastRampTime.plusSeconds(4)))
    assert(dispatchState3.signals(PowerPlantState.activePowerSignalKey).toDouble === 700)

    // Another 4 seconds elapse, we move to 800, our setPoint
    val dispatchState4 = PowerPlantState.dispatch(dispatchState3.copy(lastRampTime = dispatchState3.lastRampTime.plusSeconds(4)))
    assert(dispatchState4.signals(PowerPlantState.activePowerSignalKey).toDouble === 800)
  }
}