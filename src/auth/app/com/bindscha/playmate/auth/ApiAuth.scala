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
 * Trait to mix into [[play.api.mvc.Controller]] to use API authentication and 
 * authorization in the controller's [[play.api.mvc.Action]]
 * 
 * @version 1.0
 * @author [[mailto:laurent@bindschaedler.com Laurent Bindschaedler]]
 */
trait ApiAuth {
  self: Controller with ApiAuthConfig =>
    
  /**
   * An `AuthorizedApiAction` is performed if the API key has the authority
   * 
   * @param authority the [[com.bindscha.playmate.auth.ApiAuthConfig.this.KeyAuthority]] protecting this action
   * @param f the function to apply if authentication and authorization are successful
   * @return the action
   */
  def AuthorizedApiAction(authority: KeyAuthority)(f: ApiKey => Request[AnyContent] => Result): Action[AnyContent] =
    AuthorizedApiAction(BodyParsers.parse.anyContent, authority)(f)

  /**
   * An `AuthorizedApiAction` is performed if the API key has the authority
   * 
   * @param p the [[play.api.mvc.BodyParser]] for the request
   * @param authority the [[com.bindscha.playmate.auth.ApiAuthConfig.this.KeyAuthority]] protecting this action
   * @param f the function to apply if authentication and authorization are successful
   * @return the action
   */
  def AuthorizedApiAction[A](p: BodyParser[A], authority: KeyAuthority)(f: ApiKey => Request[A] => Result): Action[A] =
    Action(p)(req => authorized(authority)(req).right.map(u => f(u)(req)).merge)
  
  // Returns the ApiKey is it is authorized under the provided Authority, 
  // otherwise returns `authenticationFailure` or `authorizationFailure`
  private def authorized(authority: KeyAuthority)(implicit request: RequestHeader): Either[Result, ApiKey] = for {
    apiKey <- request.headers.toSimpleMap.find(_._1.toLowerCase == "x-api-key").map(_._2).flatMap(apiKey).toRight(keyAuthenticationFailure(request)).right
    _ <- Either.cond(authorizeKey(apiKey, authority), (), keyAuthorizationFailure(request)).right
  } yield apiKey
  
}
