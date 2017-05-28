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

import akka.actor.{Actor, Props}
import com.inland24.powersim.actors.DBServiceActor
import com.inland24.powersim.models.MyMessages.{Destroy, Init, Tick}
import com.inland24.powersim.observables.{MyConnectableObservable, MyObservable}
import com.inland24.powersim.observers.MyObserver
import com.inland24.powersim.services.ZombieConnectorService
import com.typesafe.scalalogging.LazyLogging
import monix.execution.Scheduler
import monix.execution.cancelables.CompositeCancelable
import monix.reactive.observables.ConnectableObservable


class ThrowAwaySupervisor(globalChannel: GlobalOutputChannel)(implicit s: Scheduler)
  extends Actor with LazyLogging {

  private[this] val subscriptions = CompositeCancelable()

  override def preStart: Unit = {
    logger.info(s"starting Supervisor Actor [$self]")
    self ! Init
  }

  override def postStop: Unit = {
    subscriptions.cancel()
    logger.info(s"cancelling all subscriptions :: isCancelled ${subscriptions.isCanceled}")
  }

  private def initActors() = {
    // 1. Our DB Service actor
    //context.actorOf(DBServiceActor.props(appC))
  }

  private def init(): Seq[ConnectableObservable[Long]] = {
    // 1. our Observables
    val myObservable = MyObservable.apply
    val myConnectableObservable = MyConnectableObservable.apply(ZombieConnectorService.apply)

    // 2. our Subscribers (Subscribes are nothing but Observers with a Scheduler)
    val mySubscriber = MyObserver.apply(self, "hot-subscriber")
    val myConnectableSubscriber = MyObserver.apply(self, "cold-subscriber")

    // 3. marry the Observers and the Observables
    subscriptions += myObservable.unsafeSubscribeFn(mySubscriber)
    subscriptions += myConnectableObservable.unsafeSubscribeFn(myConnectableSubscriber)

    // 4. return a reference to all the connectables
    Seq(myConnectableObservable)
  }

  override def receive: Receive = {
    case Init =>
      init().foreach(elem => subscriptions += elem.connect())
    case tick: Tick =>
      // TODO: is this a good practice? exposing the internals of the GlobalChannel ???
      globalChannel.publishChannel.onNext(tick)
    case Destroy =>
      subscriptions.cancel()
  }
}
object ThrowAwaySupervisor {

  implicit val s = monix.execution.Scheduler.Implicits.global

  def props(globalChannel: GlobalOutputChannel): Props =
    Props(new ThrowAwaySupervisor(globalChannel))
}