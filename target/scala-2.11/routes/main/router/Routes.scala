
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

// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/jothi/Projects/Private/scala-projects/power-simulator/conf/routes
// @DATE:Sun May 21 19:15:31 CEST 2017

package router

import play.core.routing._
import play.core.routing.HandlerInvokerFactory._
import play.core.j._

import play.api.mvc._

import _root_.controllers.Assets.Asset

class Routes(
  override val errorHandler: play.api.http.HttpErrorHandler, 
  // @LINE:6
  MyApplicationController_1: com.inland24.powersim.controllers.MyApplicationController,
  // @LINE:15
  Assets_0: controllers.Assets,
  val prefix: String
) extends GeneratedRouter {

   @javax.inject.Inject()
   def this(errorHandler: play.api.http.HttpErrorHandler,
    // @LINE:6
    MyApplicationController_1: com.inland24.powersim.controllers.MyApplicationController,
    // @LINE:15
    Assets_0: controllers.Assets
  ) = this(errorHandler, MyApplicationController_1, Assets_0, "/")

  import ReverseRouteContext.empty

  def withPrefix(prefix: String): Routes = {
    router.RoutesPrefix.setPrefix(prefix)
    new Routes(errorHandler, MyApplicationController_1, Assets_0, prefix)
  }

  private[this] val defaultPrefix: String = {
    if (this.prefix.endsWith("/")) "" else "/"
  }

  def documentation = List(
    ("""GET""", this.prefix, """com.inland24.powersim.controllers.MyApplicationController.home"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """observable""", """com.inland24.powersim.controllers.MyApplicationController.observable"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """connectableObservable""", """com.inland24.powersim.controllers.MyApplicationController.connectableObservable"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """assets/""" + "$" + """file<.+>""", """controllers.Assets.versioned(path:String = "/public", file:Asset)"""),
    Nil
  ).foldLeft(List.empty[(String,String,String)]) { (s,e) => e.asInstanceOf[Any] match {
    case r @ (_,_,_) => s :+ r.asInstanceOf[(String,String,String)]
    case l => s ++ l.asInstanceOf[List[(String,String,String)]]
  }}


  // @LINE:6
  private[this] lazy val com_inland24_powersim_controllers_MyApplicationController_home0_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix)))
  )
  private[this] lazy val com_inland24_powersim_controllers_MyApplicationController_home0_invoker = createInvoker(
    MyApplicationController_1.home,
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.inland24.powersim.controllers.MyApplicationController",
      "home",
      Nil,
      "GET",
      """ TEST PAGE""",
      this.prefix + """"""
    )
  )

  // @LINE:9
  private[this] lazy val com_inland24_powersim_controllers_MyApplicationController_observable1_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("observable")))
  )
  private[this] lazy val com_inland24_powersim_controllers_MyApplicationController_observable1_invoker = createInvoker(
    MyApplicationController_1.observable,
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.inland24.powersim.controllers.MyApplicationController",
      "observable",
      Nil,
      "GET",
      """ MyObservable""",
      this.prefix + """observable"""
    )
  )

  // @LINE:12
  private[this] lazy val com_inland24_powersim_controllers_MyApplicationController_connectableObservable2_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("connectableObservable")))
  )
  private[this] lazy val com_inland24_powersim_controllers_MyApplicationController_connectableObservable2_invoker = createInvoker(
    MyApplicationController_1.connectableObservable,
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "com.inland24.powersim.controllers.MyApplicationController",
      "connectableObservable",
      Nil,
      "GET",
      """ MyConnectableObservable""",
      this.prefix + """connectableObservable"""
    )
  )

  // @LINE:15
  private[this] lazy val controllers_Assets_versioned3_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("assets/"), DynamicPart("file", """.+""",false)))
  )
  private[this] lazy val controllers_Assets_versioned3_invoker = createInvoker(
    Assets_0.versioned(fakeValue[String], fakeValue[Asset]),
    HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.Assets",
      "versioned",
      Seq(classOf[String], classOf[Asset]),
      "GET",
      """ Map static resources from the /public folder to the /assets URL path""",
      this.prefix + """assets/""" + "$" + """file<.+>"""
    )
  )


  def routes: PartialFunction[RequestHeader, Handler] = {
  
    // @LINE:6
    case com_inland24_powersim_controllers_MyApplicationController_home0_route(params) =>
      call { 
        com_inland24_powersim_controllers_MyApplicationController_home0_invoker.call(MyApplicationController_1.home)
      }
  
    // @LINE:9
    case com_inland24_powersim_controllers_MyApplicationController_observable1_route(params) =>
      call { 
        com_inland24_powersim_controllers_MyApplicationController_observable1_invoker.call(MyApplicationController_1.observable)
      }
  
    // @LINE:12
    case com_inland24_powersim_controllers_MyApplicationController_connectableObservable2_route(params) =>
      call { 
        com_inland24_powersim_controllers_MyApplicationController_connectableObservable2_invoker.call(MyApplicationController_1.connectableObservable)
      }
  
    // @LINE:15
    case controllers_Assets_versioned3_route(params) =>
      call(Param[String]("path", Right("/public")), params.fromPath[Asset]("file", None)) { (path, file) =>
        controllers_Assets_versioned3_invoker.call(Assets_0.versioned(path, file))
      }
  }
}
