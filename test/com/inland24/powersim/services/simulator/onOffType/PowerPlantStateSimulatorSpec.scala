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

package com.inland24.powersim.services.simulator.onOffType

import com.inland24.powersim.models.PowerPlantConfig.OnOffTypeConfig
import com.inland24.powersim.models.PowerPlantType
import org.scalatest.FlatSpec


class PowerPlantStateSimulatorSpec extends FlatSpec {

  behavior of PowerPlantState.getClass.getCanonicalName

  val onOffTpeCfg = OnOffTypeConfig(
    id = 1,
    name = "someConfig",
    minPower = 10.0,
    maxPower = 100.0,
    powerPlantType = PowerPlantType.OnOffType
  )

  "PowerPlantState#init" should "initialize to a default state (available = true && onOff = false)" in {
    val init = PowerPlantState.init(onOffTpeCfg.minPower)
    init.foreach {
      case (key, value) if key == PowerPlantState.activePowerSignalKey => assert(value === onOffTpeCfg.minPower.toString)
      case (key, value) if key == PowerPlantState.isAvailableSignalKey => assert(value.toBoolean)
      case (key, value) if key == PowerPlantState.isOnOffSignalKey     => assert(value.toBoolean)
    }
  }

  "PowerPlantState#turnOn" should "turnOn when in Off state and in available state" in {

  }
}