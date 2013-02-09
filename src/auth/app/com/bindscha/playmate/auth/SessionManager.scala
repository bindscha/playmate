/*     ___ _                          _                                                   *\
**    / _ \ | __ _ _   _  /\/\   __ _| |_ ___     PlayMate                                **
**   / /_)/ |/ _` | | | |/    \ / _` | __/ _ \    (c) 2011-2013, Laurent Bindschaedler    **
**  / ___/| | (_| | |_| / /\/\ \ (_| | ||  __/    http://www.bindschaedler.com/playmate   **
**  \/    |_|\__,_|\__, \/    \/\__,_|\__\___|                                            **
**                 |___/                                                                  **
\*                                                                                        */
package com.bindscha.playmate.auth

import play.api.cache.Cache
import play.api.mvc._
import play.api.Play._

import scala.util.Try

import org.joda.time.DateTime

/**
 * Trait to mix to access and manage user sessions
 * 
 * @version 1.0
 * @author [[mailto:laurent@bindschaedler.com Laurent Bindschaedler]]
 */
trait SessionManager {
  self: UserAuthConfig => 

  private val CACHE_SESSION_ID_PREFIX = "SESSION_ID:"
  private val CACHE_SESSION_TIMEOUT = (sessionTimeout / 1000).toInt
    
  /**
   * Look up the user for the given session identifier
   * 
   * @param sessionId session identifier
   * @return optional user for the given identifier
   */
  def sessionUser(sessionId: String) : Option[User] = 
    Cache.getAs[Id](CACHE_SESSION_ID_PREFIX + sessionId) orElse {
      sessionPersister.sessionUserId(sessionId)
    } flatMap { userId => 
      Cache.set(CACHE_SESSION_ID_PREFIX + sessionId, userId, CACHE_SESSION_TIMEOUT)
      user(userId)
    }
  
  /**
   * Create a new session for the given user
   * 
   * @param userId the user identifier
   * @param persistenceDuration life span in milliseconds for the persistence of the session
   * @return newly created [[com.bindscha.playmate.auth.Session]]
   */
  def sessionNew(userId: Id, persistenceDuration: Long) : Try[Session] = {
    val dt = (if(persistenceDuration < sessionTimeout) sessionTimeout else persistenceDuration).toInt
    sessionPersister withTransaction {
      sessionPersister.userSessionDel(userId)
      sessionPersister.userSessionNew(userId, Session(None, DateTime.now.plusMillis(dt)))
    }
  }
  
  /**
   * Remove the session for the given identifier
   * 
   * @param sessionId session identifier to remove
   * @return optional user for the given identifier
   */
  def sessionDel(sessionId: String) : Option[User] = 
    Cache.getAs[Id](CACHE_SESSION_ID_PREFIX + sessionId) orElse {
      sessionPersister.sessionUserId(sessionId)
    } flatMap { userId => 
      // Could fail if session had already timed out but still in the cache
      sessionPersister.userSessionDel(userId)
      Cache.remove(CACHE_SESSION_ID_PREFIX + sessionId)
      user(userId)
    }
  
}