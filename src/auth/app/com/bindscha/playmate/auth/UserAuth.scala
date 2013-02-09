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
 * Trait to mix into [[play.api.mvc.Controller]] to use authentication and 
 * authorization of users in the controller's [[play.api.mvc.Action]]
 * 
 * @version 1.0
 * @author [[mailto:laurent@bindschaedler.com Laurent Bindschaedler]]
 */
trait UserAuth extends SessionManager{
  self: Controller with UserAuthConfig =>
  
  /**
   * An `AuthorizedAction` is performed if the user has the authority
   * 
   * @param authority the [[com.bindscha.playmate.auth.UserAuthConfig.this.Authority]] protecting this action
   * @param f the function to apply if authentication and authorization are successful
   * @return the action
   */
  def AuthorizedAction(authority: Authority)(f: User => Request[AnyContent] => Result): Action[AnyContent] =
    AuthorizedAction(BodyParsers.parse.anyContent, authority)(f)

  /**
   * An `AuthorizedAction` is performed if the user has the authority
   * 
   * @param p the [[play.api.mvc.BodyParser]] for the request
   * @param authority the [[com.bindscha.playmate.auth.UserAuthConfig.this.Authority]] protecting this action
   * @param f the function to apply if authentication and authorization are successful
   * @return the action
   */
  def AuthorizedAction[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => Result): Action[A] =
    Action(p)(req => authorized(authority)(req).right.map(u => f(u)(req)).merge)

  /**
   * A `UserAwareAuthorizedAction` performs action `f` if the user has the authority and 
   * action `g` otherwise
   * 
   * @param authority the [[com.bindscha.playmate.auth.UserAuthConfig.this.Authority]] protecting this action
   * @param f the function to apply if authentication and authorization are successful
   * @param g the function to apply if authentication or authorization fail
   * @return the action
   */
  def UserAwareAuthorizedAction(authority: Authority)(f: User => Request[AnyContent] => Result)(g: Request[AnyContent] => Result): Action[AnyContent] =
    UserAwareAuthorizedAction(BodyParsers.parse.anyContent, authority)(f)(g)

  /**
   * A `UserAwareAuthorizedAction` performs action `f` if the user has the authority and 
   * action `g` otherwise
   * 
   * @param p the [[play.api.mvc.BodyParser]] for the request
   * @param authority the [[com.bindscha.playmate.auth.UserAuthConfig.this.Authority]] protecting this action
   * @param f the function to apply if authentication and authorization are successful
   * @param g the function to apply if authentication or authorization fail
   * @return the action
   */
  def UserAwareAuthorizedAction[A](p: BodyParser[A], authority: Authority)(f: User => Request[A] => Result)(g: Request[A] => Result): Action[A] =
    OptionalUserAuthorizedAction(p, authority) { u => ((u map f) getOrElse g) }
  
  /**
   * An `OptionalUserAuthorizedAction` is performed regardless of whether the user has the authority.
   * However, if the user is authenticated and authorized, they will be passed in to the 
   * inner action.
   * 
   * @param authority the [[com.bindscha.playmate.auth.UserAuthConfig.this.Authority]] protecting this action
   * @param f the function to apply after authentication and authorization lookup (receives `Option[User]` as first parameter)
   * @return the action
   */
  def OptionalUserAuthorizedAction(authority: Authority)(f: Option[User] => Request[AnyContent] => Result): Action[AnyContent] =
    OptionalUserAuthorizedAction(BodyParsers.parse.anyContent, authority)(f)

  /**
   * An `OptionalUserAuthorizedAction` is performed regardless of whether the user has the authority.
   * However, if the user is authenticated and authorized, they will be passed in to the 
   * inner action.
   * 
   * @param p the [[play.api.mvc.BodyParser]] for the request
   * @param authority the [[com.bindscha.playmate.auth.UserAuthConfig.this.Authority]] protecting this action
   * @param f the function to apply after authentication and authorization lookup (receives `Option[User]` as first parameter)
   * @return the action
   */
  def OptionalUserAuthorizedAction[A](p: BodyParser[A], authority: Authority)(f: Option[User] => Request[A] => Result): Action[A] =
    Action(p)(request => f(restoreUser(request) filter (_ => authorized(authority)(request).isRight))(request))

  /**
   * An `OptionalUserAction` is performed regardless of whether the user has the authority.
   * However, if there is an active user session, the user will be passed in to the 
   * inner action.
   * 
   * @param authority the [[com.bindscha.playmate.auth.UserAuthConfig.this.Authority]] protecting this action
   * @param f the function to apply after user lookup (receives `Option[User]` as first parameter)
   * @return the action
   */
  def OptionalUserAction(f: Option[User] => Request[AnyContent] => Result): Action[AnyContent] =
    OptionalUserAction(BodyParsers.parse.anyContent)(f)

  /**
   * An `OptionalUserAction` is performed regardless of whether the user has the authority.
   * However, if there is an active user session, the user will be passed in to the 
   * inner action.
   * 
   * @param p the [[play.api.mvc.BodyParser]] for the request
   * @param authority the [[com.bindscha.playmate.auth.UserAuthConfig.this.Authority]] protecting this action
   * @param f the function to apply after user lookup (receives `Option[User]` as first parameter)
   * @return the action
   */
  def OptionalUserAction[A](p: BodyParser[A])(f: Option[User] => Request[A] => Result): Action[A] =
    Action(p) { request => f(restoreUser(request))(request) }
    
  /**
   * A `UserAwareAction` performs action `f` if the user has the authority and 
   * action `g` otherwise
   * 
   * @param f the function to apply if there is a user is scope
   * @param g the function to apply if there is no user
   * @return the action
   */
  def UserAwareAction(f: User => Request[AnyContent] => Result)(g: Request[AnyContent] => Result): Action[AnyContent] =
    UserAwareAction(BodyParsers.parse.anyContent)(f)(g)

  /**
   * A `UserAwareAction` performs action `f` if there is an active user session and 
   * action `g` otherwise
   * 
   * @param p the [[play.api.mvc.BodyParser]] for the request
   * @param f the function to apply if there is a user is scope
   * @param g the function to apply if there is no user
   * @return the action
   */
  def UserAwareAction[A](p: BodyParser[A])(f: User => Request[A] => Result)(g: Request[A] => Result): Action[A] =
    OptionalUserAction(p) { u => ((u map f) getOrElse g) }
    
  // Returns the User if they are authorized under the provided Authority, 
  // otherwise returns `authenticationFailure` or `authorizationFailure`
  private def authorized(authority: Authority)(implicit request: RequestHeader): Either[Result, User] = for {
    user <- restoreUser(request).toRight(authenticationFailure(request)).right
    _ <- Either.cond(authorize(user, authority), (), authorizationFailure(request)).right
  } yield user
  
  // Pull up the User from the session cookie (if available)
  private def restoreUser(implicit request: RequestHeader): Option[User] = for {
    sessionId <- request.session.get(sessionIdKey)
    user <- sessionUser(sessionId)
  } yield user

}
