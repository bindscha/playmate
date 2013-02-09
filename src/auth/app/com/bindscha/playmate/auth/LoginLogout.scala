/*     ___ _                          _                                                   *\
**    / _ \ | __ _ _   _  /\/\   __ _| |_ ___     PlayMate                                **
**   / /_)/ |/ _` | | | |/    \ / _` | __/ _ \    (c) 2011-2013, Laurent Bindschaedler    **
**  / ___/| | (_| | |_| / /\/\ \ (_| | ||  __/    http://www.bindschaedler.com/playmate   **
**  \/    |_|\__,_|\__, \/    \/\__,_|\__\___|                                            **
**                 |___/                                                                  **
\*                                                                                        */
package com.bindscha.playmate.auth

import play.api.mvc._

import scala.annotation.tailrec

import org.joda.time.DateTime

import com.bindscha.scatter.Random

/**
 * Trait to mix into [[play.api.mvc.Controller]] to use login and logout
 * of users in the controller's [[play.api.mvc.Action]]
 * 
 * @version 1.0
 * @author [[mailto:laurent@bindschaedler.com Laurent Bindschaedler]]
 */
trait LoginLogout extends SessionManager{
  self: Controller with UserAuthConfig =>
  
  /**
   * An `AuthenticateAction` is performed to log the user in. The action creates an authenticated
   * session for the user and redirects the user to whichever access restricted location
   * is specified.
   * 
   * @param userId the identifier for the user
   * @param sessionPersistenceDuration life span in milliseconds for the persistence of the session
   * @param redirect action to take to redirect the user to the appropriate location upon completed login
   * @return the action
   */
  def AuthenticateAction(userId: Id, sessionPersistenceDuration: Long = 0, redirect: RequestHeader => Result = (requestHeader => loginSuccess(requestHeader))): Action[AnyContent] = 
    AuthenticateAction(BodyParsers.parse.anyContent)(userId, sessionPersistenceDuration, redirect)
    
  /**
   * An `AuthenticateAction` is performed to log the user in. The action creates an authenticated
   * session for the user and redirects the user to whichever access restricted location
   * is specified.
   * 
   * @param p the [[play.api.mvc.BodyParser]] for the request
   * @param userId the identifier for the user
   * @param sessionPersistenceDuration life span in milliseconds for the persistence of the session
   * @param redirect action to take to redirect the user to the appropriate location upon completed login
   * @return the action
   */
  def AuthenticateAction[A](p: BodyParser[A])(userId: Id, sessionPersistenceDuration: Long = 0, redirect: RequestHeader => Result = (requestHeader => loginSuccess(requestHeader))) : Action[A] = Action(p){ implicit request => 
    (for {
      session <- sessionNew(userId, if(sessionPersistenceDuration < 0) sessionPersistenceDuration else 0).toOption
      sessionId <- session.id
    } yield {
    redirect(request).withSession((sessionIdKey -> sessionId))
    }) getOrElse InternalServerError
  }
  
  /**
   * A `DeauthenticateAction` is performed to log the user out. The action removes the user 
   * session and performs a clean logoff. Upon completion, the user is redirected to 
   * the default logout location.
   * 
   * @return the action
   */
  def DeauthenticateAction(flashing: (String, String)*): Action[AnyContent] = 
    DeauthenticateAction(BodyParsers.parse.anyContent)(flashing : _*)
  
  /**
   * A `DeauthenticateAction` is performed to log the user out. The action removes the user 
   * session and performs a clean logoff. Upon completion, the user is redirected to 
   * the default logout location.
   * 
   * @param p the [[play.api.mvc.BodyParser]] for the request
   * @return the action
   */
  def DeauthenticateAction[A](p: BodyParser[A])(flashing: (String, String)*) : Action[A] = Action(p){ implicit request => 
    request.session.get(sessionIdKey) foreach sessionDel
    logoutSuccess(request).withNewSession.flashing(flashing : _*)
  }
  
  /**
   * A `NoUserAction` is performed for actions that require the absence of a pre-existing
   * session. If such a session exists, it will be terminated before performing the action.
   * This is useful to prevent double logins or logouts, etc.
   * 
   * @param f the function to apply
   * @return the action
   */
  def NoUserAction(f: Request[AnyContent] => Result): Action[AnyContent] = 
    NoUserAction(BodyParsers.parse.anyContent)(f)
    
  /**
   * A `NoUserAction` is performed for actions that require the absence of a pre-existing
   * session. If such a session exists, it will be terminated before performing the action.
   * This is useful to prevent double logins or logouts, etc.
   * 
   * @param p the [[play.api.mvc.BodyParser]] for the request
   * @param f the function to apply
   * @return the action
   */
  def NoUserAction[A](p: BodyParser[A])(f: Request[A] => Result): Action[A] = Action(p){ implicit request => 
    request.session.get(sessionIdKey) foreach sessionDel
    f(request).withNewSession
  }
  
}
