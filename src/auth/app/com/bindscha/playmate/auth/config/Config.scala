/*     ___ _                          _                                                   *\
**    / _ \ | __ _ _   _  /\/\   __ _| |_ ___     PlayMate                                **
**   / /_)/ |/ _` | | | |/    \ / _` | __/ _ \    (c) 2011-2013, Laurent Bindschaedler    **
**  / ___/| | (_| | |_| / /\/\ \ (_| | ||  __/    http://www.bindschaedler.com/playmate   **
**  \/    |_|\__,_|\__, \/    \/\__,_|\__\___|                                            **
**                 |___/                                                                  **
\*                                                                                        */
package com.bindscha.playmate.auth.config

object Config {

  import play.api.Play.current
  import play.api.Play.configuration

  import scala.concurrent.duration._

  val SESSION_ID_LENGTH =
    configuration.getInt("user.session_id.length") getOrElse 64

}