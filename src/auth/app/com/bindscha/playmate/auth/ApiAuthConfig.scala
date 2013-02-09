/*     ___ _                          _                                                   *\
**    / _ \ | __ _ _   _  /\/\   __ _| |_ ___     PlayMate                                **
**   / /_)/ |/ _` | | | |/    \ / _` | __/ _ \    (c) 2011-2013, Laurent Bindschaedler    **
**  / ___/| | (_| | |_| / /\/\ \ (_| | ||  __/    http://www.bindschaedler.com/playmate   **
**  \/    |_|\__,_|\__, \/    \/\__,_|\__\___|                                            **
**                 |___/                                                                  **
\*                                                                                        */
package com.bindscha.playmate.auth

import play.api.mvc._

/**
 * Trait to mix into [[com.bindscha.playmate.auth.ApiAuth]] to provide required 
 * configuration information
 * 
 * @version 1.0
 * @author [[mailto:laurent@bindschaedler.com Laurent Bindschaedler]]
 */
trait ApiAuthConfig {
    
  /**
   * Type representing an API key
   */
  type ApiKey
  
  /**
   * Type used for authorization
   */
  type KeyAuthority
  
  /**
   * Function to map Strings to API Keys
   * 
   * @return API key for the given String
   */
  def apiKey(str: String): Option[ApiKey]
  
  /**
   * [[play.api.mvc.Action]] defining the action to take upon failed authentication
   * 
   * Example:
   * {{{
   * Unauthorized("You do not have permission to access this resource!")
   * }}}
   */
  def keyAuthenticationFailure(implicit requestHeader : RequestHeader): Result
  
  /**
   * [[play.api.mvc.Action]] defining the action to take upon failed authorization
   * 
   * Example:
   * {{{
   * Forbidden("You do not have permission to access this resource!")
   * }}}
   */
  def keyAuthorizationFailure(implicit requestHeader : RequestHeader): Result
  
  /**
   * Function determining whether the [[com.bindscha.playmate.auth.ApiAuthConfig.this.ApiKey]] 
   * has the [[com.bindscha.playmate.auth.ApiAuthConfig.this.Authority]]
   */
  def authorizeKey(apiKey: ApiKey, authority: KeyAuthority): Boolean
  
}
