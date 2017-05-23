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

package com.inland24.powersim.models


sealed trait PowerPlantSignal {
  def powerPlantId: Long
}

// Represents the signals / commands that we send to the power plant
trait InputSignal extends PowerPlantSignal
case class Dispatch(powerPlantId: Long, dispatchValue: Double) extends InputSignal
case class TurnOnOff(powerPlantId: Long, turnOnOff: Boolean) extends InputSignal

// Represents the set of output signals sent by the power plant
trait OutputSignal extends PowerPlantSignal
case class PowerPlantTelemetry(
  powerPlantId: Long,
  signals: Map[String, String]
) extends OutputSignal