/*
 *
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

package com.inland24.powersim.observables

import monix.execution.Cancelable
import monix.reactive.Observable
import monix.reactive.observers.Subscriber
import monix.execution.FutureUtils.extensions._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * We are streaming data from the database table and passing it to
  * whoever that subscribes to this stream. The kind of data that we
  * stream depends on the caller and is governed by the function that
  * he passes to the constructor.
  */
class DBServiceObservable[T, U] private (refreshInterval: FiniteDuration, fn: => Future[T])(mapper: T => U)
  extends Observable[U] {

  override def unsafeSubscribeFn(subscriber: Subscriber[U]): Cancelable = {

    // TODO: use passed in ExecutionContext
    import scala.concurrent.ExecutionContext.Implicits.global

    def underlying = {
      val someFuture = fn.materialize.map {
        case Success(succ) => Some(succ)
        case Failure(_) => None // TODO: log the errors!!!
      }

      Observable.fromFuture(someFuture)
    }

    Observable
      .intervalAtFixedRate(refreshInterval)
      .flatMap(_ => underlying)
      // We map it to the target type we need!
      .collect { case Some(powerPlantsSeq) => mapper(powerPlantsSeq) }
      .distinctUntilChanged
      .unsafeSubscribeFn(subscriber)
  }
}
object DBServiceObservable {

  def powerPlantDBServiceObservable[T, U](
    refreshInterval: FiniteDuration,
    fn: Future[T])(mapper: T => U): DBServiceObservable[T, U] =
    new DBServiceObservable[T, U](refreshInterval, fn)(mapper)
}