
// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/jothi/Projects/Private/scala-projects/power-simulator/conf/routes
// @DATE:Sat May 20 14:14:24 CEST 2017

package controllers;

import router.RoutesPrefix;

public class routes {
  
  public static final controllers.ReverseAssets Assets = new controllers.ReverseAssets(RoutesPrefix.byNamePrefix());

  public static class javascript {
    
    public static final controllers.javascript.ReverseAssets Assets = new controllers.javascript.ReverseAssets(RoutesPrefix.byNamePrefix());
  }

}
