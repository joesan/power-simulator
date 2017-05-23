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


// A power plant could be in any one of the following states
sealed trait PowerPlantState
case object Ramping     extends PowerPlantState
case object Fault       extends PowerPlantState
case object Available   extends PowerPlantState
case object Dispatching extends PowerPlantState

// TODO: add more signals as needed!
case class StateMachine(
  powerPlantId: Long,
  activePower: Double,
  powerPlantState: PowerPlantState
)