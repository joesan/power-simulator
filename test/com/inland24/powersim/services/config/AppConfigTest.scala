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

package com.inland24.powersim.services.config

import com.inland24.powersim.config.{AppConfig, DBConfig}
import org.scalatest.FlatSpec

import scala.concurrent.duration._


class AppConfigTest extends FlatSpec {

  val dbConfigTest = DBConfig(
    url = "jdbc:h2:mem:power-simulator;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
    user = None,
    password = None,
    driver = "org.h2.Driver",
    refreshInterval = 5.seconds
  )

  "AppConfig#load" should "load the default configuration when nothing is specified in the environment" in {
    System.clearProperty("config.file")
    System.clearProperty("ENV")
    System.clearProperty("env")
    val appConfig = AppConfig.load()
    assert(appConfig.environment === "default")
  }

  "AppConfig#load" should "load the test configuration when specified in the environment" in {
    System.setProperty("ENV", "test")
    val appConfig = AppConfig.load()
    assert(appConfig.environment === "test")
    assert(appConfig.database === dbConfigTest)
  }
}