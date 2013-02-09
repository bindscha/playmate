package com.bindscha.playmate.boilerplate.controllers

import play.api.Logger
import play.api.templates.Html

import com.bindscha.playmate.boilerplate.views.html

/**
 * Base type for assets
 */
trait Asset

/**
 * Base type for icon assets
 */
sealed trait Icon extends Asset

case object Favicon extends Icon
case object AppleIcon57 extends Icon
case object AppleIcon72 extends Icon
case object AppleIcon114 extends Icon
case object AppleIcon144 extends Icon

/**
 * Represents an image asset
 */
case class Image(id: String) extends Asset

/**
 * Represents a stylesheet asset
 */
case class Stylesheet(id: String) extends Asset

/**
 * Represents a script asset
 */
case class Script(id: String) extends Asset
  
/**
 * Base type to map assets to HTML
 */
trait Assets extends PartialFunction[Asset, Html]

/**
 * Empty assets map
 */
trait EmptyAssets extends Assets {
  
  override def apply(asset: Asset): Html = 
    throw new java.util.NoSuchElementException(s"Asset not found: $asset")

  override def isDefinedAt(asset: Asset): Boolean = 
    false
  
}

/**
 * Empty assets map
 */
object EmptyAssets extends EmptyAssets

/**
 * Assets controller
 */
trait AssetsController {
  self: Assets =>
  
  protected def localAssetsRoot = "/assets"
  
  protected def iconDefault(icon: Icon): Html = {
    Logger.warn(s"Icon $icon is not a managed asset! AssetsController returned the default path...")
    icon match {
      case Favicon => 
        html.helpers.icon("shortcut icon", s"$localAssetsRoot/icn/favicon.ico")
      case AppleIcon57 => 
        html.helpers.icon("apple-touch-icon-precomposed", s"$localAssetsRoot/icn/apple-touch-icon-57x57.png")
      case AppleIcon72 => 
        html.helpers.icon("apple-touch-icon-precomposed", s"$localAssetsRoot/icn/apple-touch-icon-72x72.png", Some("72x72"))
      case AppleIcon114 => 
        html.helpers.icon("apple-touch-icon-precomposed", s"$localAssetsRoot/icn/apple-touch-icon-114x114.png", Some("114x114"))
      case AppleIcon144 => 
        html.helpers.icon("apple-touch-icon-precomposed", s"$localAssetsRoot/icn/apple-touch-icon-144x144.png", Some("144x144"))
    }
  }
    
  /**
   * Returns an HTML fragment for including the requested icon in a document
   */
  def icon(icon: Icon): Html = 
    self applyOrElse (icon, iconDefault)

  protected def imageDefault(image: Image): Html = {
    Logger.warn(s"Image ${image.id} is not a managed asset! AssetsController returned the default path...")
    html.helpers.image(image.id, s"$localAssetsRoot/img/${image.id}.png")
  }
    
  /**
   * Returns an HTML fragment for including the requested image in a document
   */
  def image(id: String): Html =
    self applyOrElse (Image(id), imageDefault)

  protected def stylesheetDefault(stylesheet: Stylesheet): Html = {
    Logger.warn(s"Stylesheet ${stylesheet.id} is not a managed asset! AssetsController returned the default path...")
    html.helpers.stylesheet(s"$localAssetsRoot/css/${stylesheet.id}.css")
  }
  
  /**
   * Returns an HTML fragment for including the requested stylesheet in a document
   */
  def stylesheet(id: String): Html =
    self applyOrElse (Stylesheet(id), stylesheetDefault)

  protected def scriptDefault(script: Script): Html = {
    Logger.warn(s"Script ${script.id} is not a managed asset! AssetsController returned the default path...")
    html.helpers.script(s"$localAssetsRoot/js/${script.id}.js")
  }
  
  /**
   * Returns an HTML fragment for including the requested script in a document
   */
  def script(id: String): Html =
    self applyOrElse (Script(id), scriptDefault)
  
}

/**
 * Assets map for the boilerplate
 */
trait BoilerplateAssets extends Assets {
  
  val MODERNIZR = "modernizr"
  val JQUERY = "jquery"
  val BOOTSTRAP = "bootstrap"
  val BOOTSTRAP_RESPONSIVE = "responsive"

  val MODERNIZR_VERSION = "2.6.2"
  val JQUERY_VERSION = "1.9.1"
  val BOOTSTRAP_VERSION = "2.3.0"
  
  val BOILERPLATE_ASSETS_ROOT = "/assets/boilerplate"
    
  private val ASSETS_MAP : Map[Asset, Html] = Map(
    Favicon -> 
      html.helpers.icon("shortcut icon", s"$BOILERPLATE_ASSETS_ROOT/icn/favicon.ico"),
    AppleIcon57 -> 
      html.helpers.icon("apple-touch-icon-precomposed", s"$BOILERPLATE_ASSETS_ROOT/icn/apple-touch-icon-57x57.png"),
    AppleIcon72 -> 
      html.helpers.icon("apple-touch-icon-precomposed", s"$BOILERPLATE_ASSETS_ROOT/icn/apple-touch-icon-72x72.png", Some("72x72")),
    AppleIcon114 -> 
        html.helpers.icon("apple-touch-icon-precomposed", s"$BOILERPLATE_ASSETS_ROOT/icn/apple-touch-icon-114x114.png", Some("114x114")),
    AppleIcon144 -> 
        html.helpers.icon("apple-touch-icon-precomposed", s"$BOILERPLATE_ASSETS_ROOT/icn/apple-touch-icon-144x144.png", Some("144x144")),
    Stylesheet(BOOTSTRAP) -> 
      html.helpers.stylesheet(
        s"$BOILERPLATE_ASSETS_ROOT/css/bootstrap.css",
        Some(s"$BOILERPLATE_ASSETS_ROOT/css/bootstrap.min.css"),
        None),
    Stylesheet(BOOTSTRAP_RESPONSIVE) -> 
      html.helpers.stylesheet(
        s"$BOILERPLATE_ASSETS_ROOT/css/responsive.css",
        Some(s"$BOILERPLATE_ASSETS_ROOT/css/responsive.min.css"),
        None),
    Script(MODERNIZR) -> 
      html.helpers.script(
        s"$BOILERPLATE_ASSETS_ROOT/js/$MODERNIZR-$MODERNIZR_VERSION.js",
        Some(s"$BOILERPLATE_ASSETS_ROOT/js/$MODERNIZR-$MODERNIZR_VERSION.min.js"),
        Some(s"//cdnjs.cloudflare.com/ajax/libs/modernizr/$MODERNIZR_VERSION/$MODERNIZR.min.js"),
        Some("window.Modernizr")),
    Script(JQUERY) -> 
      html.helpers.script(
        s"$BOILERPLATE_ASSETS_ROOT/js/$JQUERY-$JQUERY_VERSION.js",
        Some(s"$BOILERPLATE_ASSETS_ROOT/js/$JQUERY-$JQUERY_VERSION.min.js"),
        Some(s"//ajax.googleapis.com/ajax/libs/jquery/$JQUERY_VERSION/$JQUERY.min.js"),
        Some("window.jQuery")),
    Script(BOOTSTRAP) -> 
      html.helpers.script(
        s"$BOILERPLATE_ASSETS_ROOT/js/$BOOTSTRAP-$BOOTSTRAP_VERSION.js",
        Some(s"$BOILERPLATE_ASSETS_ROOT/js/$BOOTSTRAP-$BOOTSTRAP_VERSION.min.js"),
        Some(s"//cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/$BOOTSTRAP_VERSION/$BOOTSTRAP.min.js"),
        Some("(typeof $.fn.alert.Constructor == 'function')"))
  )
  
  abstract override def apply(asset: Asset): Html = 
    ASSETS_MAP.applyOrElse(asset, super.apply _)

  abstract override def isDefinedAt(asset: Asset): Boolean = 
    ASSETS_MAP.contains(asset) || super.isDefinedAt(asset)
  
}

/**
 * Assets map for the boilerplate
 */
object BoilerplateAssets 
  extends EmptyAssets 
  with BoilerplateAssets

/**
 * Assets controller for the boilerplate
 */
trait BoilerplateAssetsController 
  extends AssetsController 
  with EmptyAssets 
  with BoilerplateAssets

/**
 * Assets controller for the boilerplate
 */
object BoilerplateAssetsController extends BoilerplateAssetsController
