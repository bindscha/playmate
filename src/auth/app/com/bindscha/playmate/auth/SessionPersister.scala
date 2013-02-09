/*     ___ _                          _                                                   *\
**    / _ \ | __ _ _   _  /\/\   __ _| |_ ___     PlayMate                                **
**   / /_)/ |/ _` | | | |/    \ / _` | __/ _ \    (c) 2011-2013, Laurent Bindschaedler    **
**  / ___/| | (_| | |_| / /\/\ \ (_| | ||  __/    http://www.bindschaedler.com/playmate   **
**  \/    |_|\__,_|\__, \/    \/\__,_|\__\___|                                            **
**                 |___/                                                                  **
\*                                                                                        */
package com.bindscha.playmate.auth

import play.api.mvc._

import scala.util.Try

import org.joda.time.DateTime

import com.bindscha.playmate.auth.config.Config

/**
 * Represents a user session
 * 
 * @param id session identifier
 * @param timeout session expiry timestamp
 */
case class Session(
  id: Option[String], 
  timeout: DateTime
) {
  assert(id != null && (!id.isDefined || id.get.length == Config.SESSION_ID_LENGTH), "Invalid session identifier")
  assert(timeout != null && timeout.isAfter(DateTime.now.minusSeconds(10)), "Invalid session timeout")
}

/**
 * Trait to persist user sessions
 * 
 * @version 1.0
 * @author [[mailto:laurent@bindschaedler.com Laurent Bindschaedler]]
 */
trait SessionPersister[Id] {

  /**
   * Look up the session for the given user identifier
   * 
   * @param userId user identifier
   * @return optional session for the given identifier
   */
  def userSession(userId: Id): Option[Session]
  
  /**
   * Look up the user for the given session identifier
   * 
   * @param sessionId session identifier
   * @return optional user for the given identifier
   */
  def sessionUserId(sessionId: String): Option[Id]
  
  /**
   * Create and persist a new user session
   * 
   * @param userId the user identifier
   * @param session the session to persist
   */
  def userSessionNew(userId: Id, session: Session): Try[Session]
  
  /**
   * Modify a user session
   * 
   * @param userId the user identifier
   * @param session the session to modify
   */
  def userSessionIs(userId: Id, session: Session): Try[Session]
  
  /**
   * Removes a user session
   * 
   * @param userId the user identifier
   * @return session the session to persist
   */
  def userSessionDel(userId: Id): Option[Session]
  
  /**
   * Run `f` inside a transaction
   */
  def withTransaction[A](f: => A) : A
  
}
