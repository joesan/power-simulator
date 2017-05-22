/*
 *
 *  * Copyright (c) 2017 joesan @ http://github.com/joesan
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.inland24.powersim.services.simulator

import scala.util.Random


trait SimulatorService[T] {

  def random(from: T, within: T): T
}
object SimulatorService {

  val intRandomValue = new SimulatorService[Int] {
    val random = new Random(19)
    override def random(from: Int, within: Int): Int =
      from + random.nextInt((within - from) + 1)
  }

  val doubleRandomValue = new SimulatorService[Double] {
    val random = new Random(31)
    override def random(from: Double, within: Double): Double =
      from + (random.nextDouble() * (within - from) )
  }

  val stringRandomValue = new SimulatorService[String] {
    val rndm = new Random(2116)
    override def random(from: String, within: String): String = // TODO: make use of from and within
      rndm.nextString(from.length + within.length)
  }
}