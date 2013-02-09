/*     ___ _                          _                                                   *\
**    / _ \ | __ _ _   _  /\/\   __ _| |_ ___     PlayMate                                **
**   / /_)/ |/ _` | | | |/    \ / _` | __/ _ \    (c) 2011-2013, Laurent Bindschaedler    **
**  / ___/| | (_| | |_| / /\/\ \ (_| | ||  __/    http://www.bindschaedler.com/playmate   **
**  \/    |_|\__,_|\__, \/    \/\__,_|\__\___|                                            **
**                 |___/                                                                  **
\*                                                                                        */
package com.bindscha.playmate.auth

import play.api.cache.Cache
import play.api.Play._
import play.api.mvc._

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.util.Try

import org.joda.time.DateTime

import com.bindscha.playmate.auth.config.Config
import com.bindscha.scatter.Random

/**
 * `CacheSessionPersister` persists sessions to the in-memory cache
 * 
 * @version 1.0
 * @author [[mailto:laurent@bindschaedler.com Laurent Bindschaedler]]
 */
class CacheSessionPersister[Id : ClassTag] extends SessionPersister[Id] {

  // This persister is non transactional
  
  private val SESSION_ID_PREFIX = "CACHE_PERSIST_SESSION_ID:"
  private val USER_ID_PREFIX = "CACHE_PERSIST_USER_ID:"
  
  def userSession(userId: Id): Option[Session] = 
    Cache.getAs[String](USER_ID_PREFIX + userId) map { s => Session(Some(s), DateTime.now) }
  
  def sessionUserId(sessionId: String): Option[Id] = 
    Cache.getAs[Id](SESSION_ID_PREFIX + sessionId)
  
  def userSessionNew(userId: Id, session: Session): Try[Session] = 
    createOrReplaceUserSession(userId, session)
  
  def userSessionIs(userId: Id, session: Session): Try[Session] = Try {
    (for {
      _ <- userSessionDel(userId)
      session <- createOrReplaceUserSession(userId, session).toOption if session.id.isDefined
    } yield session) getOrElse (throw new RuntimeException("Failed to update user session"))
  }
    
  def userSessionDel(userId: Id): Option[Session] = 
    userSession(userId) map { sessionId => 
      Cache.remove(SESSION_ID_PREFIX + sessionId.id)
      Cache.remove(USER_ID_PREFIX + userId)
      sessionId
    }
  
  def withTransaction[A](f: => A) : A = f
    
  private def createOrReplaceUserSession(userId: Id, session: Session): Try[Session] = Try {
    val sessionId = session.id getOrElse generateSessionId
    
    if(session.timeout.plusSeconds(10).isBeforeNow) {
      throw new RuntimeException("Session timeout must be a date in the future")
    } else {
      val timeout : Int = (session.timeout.minus(DateTime.now.getMillis).getMillis / 1000).toInt
      Cache.set(SESSION_ID_PREFIX + sessionId, userId, timeout)
      Cache.set(USER_ID_PREFIX + userId, sessionId, timeout)
      session.copy(id = Some(sessionId))
    }
  }
  
  @tailrec
  private def generateSessionId: String = {
    val table = "abcdefghijklmnopqrstuvwxyz1234567890-_.!~*'()"
    val token = Random.randomString(table)(Config.SESSION_ID_LENGTH)
    if(sessionUserId(token).isDefined) generateSessionId else token
  }
  
}
