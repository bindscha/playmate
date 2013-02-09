/*     ___ _                          _                                                   *\
**    / _ \ | __ _ _   _  /\/\   __ _| |_ ___     PlayMate                                **
**   / /_)/ |/ _` | | | |/    \ / _` | __/ _ \    (c) 2011-2013, Laurent Bindschaedler    **
**  / ___/| | (_| | |_| / /\/\ \ (_| | ||  __/    http://www.bindschaedler.com/playmate   **
**  \/    |_|\__,_|\__, \/    \/\__,_|\__\___|                                            **
**                 |___/                                                                  **
\*                                                                                        */
package com.bindscha.playmate.auth

import play.api.mvc._

import scala.reflect.ClassTag

/**
 * Trait to mix into [[com.bindscha.playmate.auth.UserAuth]] to provide required 
 * configuration information
 * 
 * @version 1.0
 * @author [[mailto:laurent@bindschaedler.com Laurent Bindschaedler]]
 */
trait UserAuthConfig {

  /**
   * Type used to identify a user (e.g., String, Long)
   */
  type Id
  
  implicit def idClassTag: ClassTag[Id]
  
  /**
   * Type representing a user
   */
  type User

  /**
   * Type used for authorization
   */
  type Authority

  /**
   * Function to map identifiers to users
   * 
   * @return optional user for the given identifier
   */
  def user(id: Id): Option[User]

  /**
   * [[play.api.mvc.Action]] defining the action to take upon successful login
   * 
   * Example usage:
   * {{{
   * Redirect(routes.UserArea.index)
   * }}}
   */
  def loginSuccess(implicit requestHeader : RequestHeader): Result
  
  /**
   * [[play.api.mvc.Action]] defining the action to take upon successful logout
   * 
   * Example:
   * {{{
   * Redirect(routes.Application.index)
   * }}}
   */
  def logoutSuccess(implicit requestHeader : RequestHeader): Result

  /**
   * [[play.api.mvc.Action]] defining the action to take upon failed authentication
   * 
   * Example:
   * {{{
   * Redirect(routes.Application.index).flashing(
   *   "status" -> "Invalid user/password combination"
   * )
   * }}}
   */
  def authenticationFailure(implicit requestHeader : RequestHeader): Result

  /**
   * [[play.api.mvc.Action]] defining the action to take upon failed authorization
   * 
   * Example:
   * {{{
   * Forbidden("You do not have permission to perform this action!")
   * }}}
   */
  def authorizationFailure(implicit requestHeader : RequestHeader): Result

  /**
   * Function determining whether the [[com.bindscha.playmate.auth.UserAuthConfig.this.User]] 
   * has the [[com.bindscha.playmate.auth.UserAuthConfig.this.Authority]]
   */
  def authorize(user: User, authority: Authority): Boolean
  
  /**
   * Implementation of [[com.bindscha.playmate.auth.SessionPersister]] which maps
   * sessions to users
   */
  def sessionPersister: SessionPersister[Id]

  /**
   * Timeout for a session in milliseconds
   */
  val sessionTimeout: Long = 10 * 60 * 1000
  
  /**
   * Name of the session parameter key in the cookie
   */
  val sessionIdKey = "AUTH_SESSION_ID"
  
}
