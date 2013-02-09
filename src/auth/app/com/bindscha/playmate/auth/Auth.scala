/*     ___ _                          _                                                   *\
**    / _ \ | __ _ _   _  /\/\   __ _| |_ ___     PlayMate                                **
**   / /_)/ |/ _` | | | |/    \ / _` | __/ _ \    (c) 2011-2013, Laurent Bindschaedler    **
**  / ___/| | (_| | |_| / /\/\ \ (_| | ||  __/    http://www.bindschaedler.com/playmate   **
**  \/    |_|\__,_|\__, \/    \/\__,_|\__\___|                                            **
**                 |___/                                                                  **
\*                                                                                        */
package com.bindscha.playmate.auth

import play.api.mvc._

import scala.util.Either.MergeableEither

/**
 * Trait to mix into [[play.api.mvc.Controller]] to use user and API 
 * authentication and authorization in the controller's [[play.api.mvc.Action]]
 * 
 * @version 1.0
 * @author [[mailto:laurent@bindschaedler.com Laurent Bindschaedler]]
 */
trait Auth extends UserAuth with ApiAuth {
  self: Controller with AuthConfig => 
  
  /**
   * An `UserAwareAuthorizedApiAction` performs an action if there is a user session with proper authentication 
   * and authorization or if there is an API key supplied with proper authentication and 
   * authorization.
   * 
   * @param userAuthority the [[com.bindscha.playmate.auth.UserAuthConfig.this.Authority]] protecting this action
   * @param keyAuthority the [[com.bindscha.playmate.auth.ApiAuthConfig.this.KeyAuthority]] protecting this action
   * @param f the function to apply if authentication and authorization are successful
   * @return the action
   */
  def UserAwareAuthorizedApiAction(userAuthority: Authority, keyAuthority: KeyAuthority)(f: Request[AnyContent] => Result): Action[AnyContent] =
    UserAwareAuthorizedApiAction(BodyParsers.parse.anyContent, userAuthority, keyAuthority)(f)

  /**
   * An `UserAwareAuthorizedApiAction` performs an action if there is a user session with proper authentication 
   * and authorization or if there is an API key supplied with proper authentication and 
   * authorization.
   * 
   * @param p the [[play.api.mvc.BodyParser]] for the request
   * @param userAuthority the [[com.bindscha.playmate.auth.UserAuthConfig.this.Authority]] protecting this action
   * @param keyAuthority the [[com.bindscha.playmate.auth.ApiAuthConfig.this.KeyAuthority]] protecting this action
   * @param f the function to apply if authentication and authorization are successful
   * @return the action
   */
  def UserAwareAuthorizedApiAction[A](p: BodyParser[A], userAuthority: Authority, keyAuthority: KeyAuthority)(f: Request[A] => Result): Action[A] =
    UserAwareAuthorizedAction(p, userAuthority) { user => request => 
      f(request)
    } { request => 
      AuthorizedApiAction(p, keyAuthority) { key => request => 
        f(request)
      }(request)
    }
  
}
