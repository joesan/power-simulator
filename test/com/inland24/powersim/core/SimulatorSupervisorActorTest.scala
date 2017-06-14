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

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.inland24.powersim.actors.DBServiceActor
import com.inland24.powersim.config.AppConfig
import com.inland24.powersim.models.PowerPlantConfig
import com.inland24.powersim.models.PowerPlantConfig.PowerPlantsConfig
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import monix.execution.FutureUtils.extensions._

import scala.util.{Failure, Success}
import scala.concurrent.duration._

// TODO complete the tests
class SimulatorSupervisorActorTest extends TestKit(ActorSystem("SimulatorSupervisorActorTest"))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  private def loadTestAppConfig = {
    System.setProperty("env", "test")
    AppConfig.load()
  }

  "SimulatorSupervisorActor" must {

    import monix.execution.Scheduler.Implicits.global

    // We test against the test environment
    val appCfg = loadTestAppConfig
    val dbActorRef = system.actorOf(DBServiceActor.props(appCfg.database))
    val simSupervisorActor = system.actorOf(SimulatorSupervisorActor.props(dbActorRef))

    // TODO: revist this test!
    pending
    "initialize simulator actors for all active PowerPlant's" in {

      implicit val timeout = Timeout(5.seconds)

      (dbActorRef ? DBServiceActor.GetActivePowerPlants).mapTo[PowerPlantsConfig]
      expectMsgPF(5.seconds) {
        case activePowerPlants: PowerPlantsConfig =>
          val powerPlantCfgSeq = activePowerPlants.powerPlantConfigSeq
          // each element in the sequence should have an actor instance up and running
          powerPlantCfgSeq.foreach {
            powerPlantCfg: PowerPlantConfig =>
              val actorRef = system.actorSelection(self.path / s"asset-simulator-${powerPlantCfg.id}")
                .resolveOne(2.seconds)
              actorRef.materialize.map {
                case Success(succ) => assert(succ.path.name === s"asset-simulator-${powerPlantCfg.id}")
                case Failure(_) => fail(s"TODO....")
              }
          }
        case _ =>
          fail(s"TODO...")
      }
    }
  }
}