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

package com.inland24.powersim.observables

import com.inland24.powersim.services.database.models.PowerPlantRow
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
  * he passes to the constructor
  */
class DBServiceObservable[T] private(refreshInterval: FiniteDuration, fn: => Future[T])
  extends Observable[T] {

  override def unsafeSubscribeFn(subscriber: Subscriber[T]): Cancelable = {

    def underlying = {
      val powerPlantsFutSeq = fn.materialize.map {
        case Success(succ) => Some(succ)
        case Failure(fail) => None // TODO: log the errors!!!
      }

      Observable.fromFuture(powerPlantsFutSeq)
    }

    Observable.intervalAtFixedRate(refreshInterval)
      .flatMap(_ => underlying)
      .collect { case Some(powerPlantsSeq) => powerPlantsSeq }
      .distinctUntilChanged
      .unsafeSubscribeFn(subscriber)
  }
}
object DBServiceObservable {

  def powerPlantDBServiceObservable(refreshInterval: FiniteDuration, fn: Future[Seq[PowerPlantRow]]) =
    new DBServiceObservable[Seq[PowerPlantRow]](refreshInterval, fn)
}