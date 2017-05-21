
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/jothi/Projects/Private/scala-projects/power-simulator/conf/routes
// @DATE:Sat May 20 14:14:24 CEST 2017

import play.api.routing.JavaScriptReverseRoute
import play.api.mvc.{ QueryStringBindable, PathBindable, Call, JavascriptLiteral }
import play.core.routing.{ HandlerDef, ReverseRouteContext, queryString, dynamicString }


import _root_.controllers.Assets.Asset

// @LINE:6
package com.inland24.crud.controllers.javascript {
  import ReverseRouteContext.empty

  // @LINE:6
  class ReverseMyApplicationController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:9
    def observable: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.inland24.crud.controllers.MyApplicationController.observable",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "observable"})
        }
      """
    )
  
    // @LINE:12
    def connectableObservable: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.inland24.crud.controllers.MyApplicationController.connectableObservable",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "connectableObservable"})
        }
      """
    )
  
    // @LINE:6
    def home: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "com.inland24.crud.controllers.MyApplicationController.home",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + """"})
        }
      """
    )
  
  }


}
