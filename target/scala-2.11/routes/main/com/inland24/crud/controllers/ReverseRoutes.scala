
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/jothi/Projects/Private/scala-projects/power-simulator/conf/routes
// @DATE:Sat May 20 14:14:24 CEST 2017

import play.api.mvc.{ QueryStringBindable, PathBindable, Call, JavascriptLiteral }
import play.core.routing.{ HandlerDef, ReverseRouteContext, queryString, dynamicString }


import _root_.controllers.Assets.Asset

// @LINE:6
package com.inland24.crud.controllers {

  // @LINE:6
  class ReverseMyApplicationController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:9
    def observable(): Call = {
      import ReverseRouteContext.empty
      Call("GET", _prefix + { _defaultPrefix } + "observable")
    }
  
    // @LINE:12
    def connectableObservable(): Call = {
      import ReverseRouteContext.empty
      Call("GET", _prefix + { _defaultPrefix } + "connectableObservable")
    }
  
    // @LINE:6
    def home(): Call = {
      import ReverseRouteContext.empty
      Call("GET", _prefix)
    }
  
  }


}
