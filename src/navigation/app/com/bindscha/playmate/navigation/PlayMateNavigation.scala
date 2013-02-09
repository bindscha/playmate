/*     ___ _                          _                                                   *\
**    / _ \ | __ _ _   _  /\/\   __ _| |_ ___     PlayMate                                **
**   / /_)/ |/ _` | | | |/    \ / _` | __/ _ \    (c) 2011-2012, Laurent Bindschaedler    **
**  / ___/| | (_| | |_| / /\/\ \ (_| | ||  __/    http://www.bindschaedler.com/playmate   **
**  \/    |_|\__,_|\__, \/    \/\__,_|\__\___|                                            **
**                 |___/                                                                  **
\*                                                                                        */
package com.bindscha.playmate.navigation

import play.api._
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.mvc.BodyParsers._
import play.api.mvc.Results._
import play.core.Router

object PlayMateNavigation {
  type Out = Handler

  sealed trait PathElem
  case class Static(name: String) extends PathElem {
    override def toString = name
  }
  case object * extends PathElem
  case object ** extends PathElem

  trait Resources[T, Out] {
    def index(): Out
    def `new`(): Out
    def create(): Out
    def show(id: T): Out
    def edit(id: T): Out
    def update(id: T): Out
    def delete(id: T): Out
  }

  /**
   * Nested Resources
   */

  trait Resources2[T1, T2, Out] {
    def index(id1: T1): Out
    def `new`(id1: T1): Out
    def create(id1: T1): Out
    def show(id1: T1, id2: T2): Out
    def edit(id1: T1, id2: T2): Out
    def update(id1: T1, id2: T2): Out
    def delete(id1: T1, id2: T2): Out
  }

  trait Resources3[T1, T2, T3, Out] {
    def index(id1: T1, id2: T2): Out
    def `new`(id1: T1, id2: T2): Out
    def create(id1: T1, id2: T2): Out
    def show(id1: T1, id2: T2, id3: T3): Out
    def edit(id1: T1, id2: T2, id3: T3): Out
    def update(id1: T1, id2: T2, id3: T3): Out
    def delete(id1: T1, id2: T2, id3: T3): Out
  }

  trait Resources4[T1, T2, T3, T4, Out] {
    def index(id1: T1, id2: T2, id3: T3): Out
    def `new`(id1: T1, id2: T2, id3: T3): Out
    def create(id1: T1, id2: T2, id3: T3): Out
    def show(id1: T1, id2: T2, id3: T3, id4: T4): Out
    def edit(id1: T1, id2: T2, id3: T3, id4: T4): Out
    def update(id1: T1, id2: T2, id3: T3, id4: T4): Out
    def delete(id1: T1, id2: T2, id3: T3, id4: T4): Out
  }

  trait Resources5[T1, T2, T3, T4, T5, Out] {
    def index(id1: T1, id2: T2, id3: T3, id4: T4): Out
    def `new`(id1: T1, id2: T2, id3: T3, id4: T4): Out
    def create(id1: T1, id2: T2, id3: T3, id4: T4): Out
    def show(id1: T1, id2: T2, id3: T3, id4: T4, id5: T5): Out
    def edit(id1: T1, id2: T2, id3: T3, id4: T4, id5: T5): Out
    def update(id1: T1, id2: T2, id3: T3, id4: T4, id5: T5): Out
    def delete(id1: T1, id2: T2, id3: T3, id4: T4, id5: T5): Out
  }

  trait FlatResources[T1, T2, Out] {
    def index(id1: T1): Out
    def `new`(id1: T1): Out
    def create(id1: T1): Out
    def show(id2: T2): Out
    def edit(id2: T2): Out
    def update(id2: T2): Out
    def delete(id2: T2): Out
  }

  trait JsonResources[T1, T2, Out] {
    def index(id1: T1): Out
    def `new`(id1: T1): Out
    def create(id1: T1): Out
    def show(id2: T2): Out
    def edit(id2: T2): Out
    def update(id2: T2): Out
    def delete(id2: T2): Out
  }

  trait PlayNavigation {
    val self = this

    def routesList = _routesList.toList
    val _routesList = new collection.mutable.ListBuffer[Route[_]]
    def addRoute[R <: Route[_]](route: R) = {
      _routesList += route
      route
    }

    def redirect(url: String, status: Int = controllers.Default.SEE_OTHER) = () => Action { controllers.Default.Redirect(url, status) }

    def documentation = _documentation

    lazy val _documentation = routesList.map { route =>

      val (parts, _) = ((List[String](), route.args) /: route.routeDef.elems) {
        case ((res, x :: xs), *) => (res :+ ("[" + x + "]"), xs)
        case ((res, xs), e) => (res :+ e.toString, xs)
      }

      (route.routeDef.method.toString, parts.mkString("/", "/", "") + route.routeDef.extString, route.args.mkString("(", ", ", ")"))
    }

    def onRouteRequest(request: RequestHeader) = {
      routes.lift(request)
    }

    def onHandlerNotFound(request: RequestHeader) = {
      NotFound(play.api.Play.maybeApplication.map {
        case app if app.mode == Mode.Dev => views.html.defaultpages.devNotFound.f
        case app => views.html.defaultpages.notFound.f
      }.getOrElse(views.html.defaultpages.devNotFound.f)(request, Some(router)))
    }

    def routes = new PartialFunction[RequestHeader, Handler] {
      private var _lastHandler: () => Handler = null // XXX: this one sucks a lot

      def apply(req: RequestHeader) = _lastHandler()

      def isDefinedAt(req: RequestHeader) = {
        routesList.view.map(_.unapply(req)).collectFirst { case Some(e) => e }.map { r =>
          _lastHandler = r // XXX: performance hack
          r
        }.isDefined
      }
    }

    // Provider for Play's 404 dev page
    // This object is used ONLY for displaying routes documentation
    val router = new play.core.Router.Routes {
      def documentation = (("###", "play-navigator routes", "") +: _documentation) ++ play.api.Play.maybeApplication.flatMap(_.routes.map(r => ("###", "play standard routes (in conf/routes file)", "") +: r.documentation)).getOrElse(Nil)
      def routes = routes
      def prefix = ""
      def setPrefix(prefix: String) {}
    }

    sealed trait Method {
      def on[R](routeDef: RouteDef[R]): R = routeDef.withMethod(this)
      def matches(s: String) = this.toString == s
    }

    val root = RouteDef0(ANY, Nil)

    implicit def stringToRouteDef0(name: String) = RouteDef0(ANY, Static(name) :: Nil)
    implicit def asterixToRoutePath1(ast: *.type) = RouteDef1(ANY, ast :: Nil)
    implicit def stringToStatic(name: String) = Static(name)

    case object ANY extends Method {
      override def matches(s: String) = true
    }

    // http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
    case object OPTIONS extends Method
    case object GET extends Method
    case object HEAD extends Method
    case object POST extends Method
    case object PUT extends Method
    case object DELETE extends Method
    case object TRACE extends Method
    case object CONNECT extends Method

    // trait BasicRoutePath {
    //   def parts: List[PathElem]
    //   def method: Method
    //   def ext: Option[String]

    //   def variableIndices = parts.zipWithIndex.collect { case (e,i) if e == * => i }
    //   def length = parts.length

    //   override def toString = method.toString + "\t/" + parts.mkString("/") + extString

    //   def extString = ext.map { "." + _ } getOrElse ""
    // }

    sealed trait Route[RD] {
      def routeDef: RouteDef[RD]
      def unapply(req: RequestHeader): Option[() => Out]
      def basic(req: RequestHeader) = {
        lazy val extMatched = (for { extA <- routeDef.ext; extB <- extractExt(req.path)._2 } yield extA == extB) getOrElse true
        routeDef.method.matches(req.method) && extMatched
      }

      def splitPath(path: String) = extractExt(path)._1.split("/").dropWhile(_ == "").toList

      def extractExt(path: String) = {
        routeDef.ext.map { _ =>
          path.reverse.split("\\.", 2).map(_.reverse).toList match {
            case x :: p :: Nil => (p, Some(x))
            case p :: Nil => (p, None)
            case Nil => ("/", None)
          }
        } getOrElse (path, None)
      }

      def args: List[Manifest[_]]
    }

    case class Route0(routeDef: RouteDef0, f0: () => Out) extends Route[RouteDef0] {
      def apply(ext: Option[String] = routeDef.ext) = Call(routeDef.method.toString, PathMatcher0(routeDef.elems, ext)())
      def unapply(req: RequestHeader): Option[() => Out] =
        if (basic(req)) PathMatcher0.unapply(routeDef.elems, splitPath(req.path), f0) else None
      def args = Nil
    }

    sealed trait RouteDef[Self] {
      def withMethod(method: Method): Self
      def method: Method
      def elems: List[PathElem]
      def ext: Option[String]
      def extString = ext map { "." + _ } getOrElse ""
    }

    case class RouteDef0(method: Method, elems: List[PathElem], ext: Option[String] = None) extends RouteDef[RouteDef0] {
      def /(static: Static) = RouteDef0(method, elems :+ static)
      def /(p: PathElem) = RouteDef1(method, elems :+ p)
      def to(f0: () => Out) = addRoute(Route0(this.copy(elems = currentNamespace ::: elems), f0))
      def withMethod(method: Method) = RouteDef0(method, elems)
      def as(ext: String) = RouteDef0(method, elems, Some(ext))
      def -->[M <: PlayModule](module: PlayNavigation => M) = withNamespace(elems.collect { case s @ Static(_) => s }) {
        module(PlayNavigation.this)
      }
    }

    object PathMatcher0 {
      def apply(elems: List[PathElem], ext: Option[String])(): String = elems.mkString("/", "/", ext.map { "." + _ } getOrElse "")
      def unapply(elems: List[PathElem], parts: List[String], handler: () => Out): Option[() => Out] = (elems, parts) match {
        case (Nil, Nil) => Some(handler)
        case (Static(x) :: xs, y :: ys) if x == y => unapply(xs, ys, handler)
        case _ => None
      }
    }

    case class RouteDef1(method: Method, elems: List[PathElem], ext: Option[String] = None) extends RouteDef[RouteDef1] {
      def /(static: Static) = RouteDef1(method, elems :+ static)
      def /(p: PathElem) = RouteDef2(method, elems :+ p)
      def to[A: PathParam: Manifest](f1: (A) => Out) = addRoute(Route1(this.copy(elems = currentNamespace ::: elems), f1))
      def withMethod(method: Method) = RouteDef1(method, elems)
      def as(ext: String) = RouteDef1(method, elems, Some(ext))
    }

    case class Route1[A: PathParam: Manifest](routeDef: RouteDef1, f1: (A) => Out) extends Route[RouteDef1] {
      def apply(a: A, ext: Option[String] = routeDef.ext) = Call(routeDef.method.toString, PathMatcher1(routeDef.elems, ext)(a))
      def unapply(req: RequestHeader): Option[() => Out] =
        if (basic(req)) PathMatcher1.unapply(routeDef.elems, splitPath(req.path), f1) else None
      def args = List(implicitly[Manifest[A]])
    }

    object PathMatcher1 {
      def apply[A](elems: List[PathElem], ext: Option[String], prefix: List[PathElem] = Nil)(a: A)(implicit ppa: PathParam[A]): String = elems match {
        case Static(x) :: rest => apply(rest, ext, prefix :+ Static(x))(a)
        case (* | **) :: rest => PathMatcher0(prefix ::: Static(ppa(a)) :: rest, ext)()
        case _ => PathMatcher0(elems, ext)()
      }
      def unapply[A](elems: List[PathElem], parts: List[String], handler: (A) => Out)(implicit ppa: PathParam[A]): Option[() => Out] = (elems, parts) match {
        case (Static(x) :: xs, y :: ys) if x == y => unapply(xs, ys, handler)
        case (* :: xs, ppa(a) :: ys) => PathMatcher0.unapply(xs, ys, () => handler(a))
        case (** :: xs, ys) => ppa.unapply(ys.mkString("/")).map { a => () => handler(a) }
        case _ => None
      }
    }

    case class RouteDef2(method: Method, elems: List[PathElem], ext: Option[String] = None) extends RouteDef[RouteDef2] {
      def /(static: Static) = RouteDef2(method, elems :+ static)
      def /(p: PathElem) = RouteDef3(method, elems :+ p)
      def to[A: PathParam: Manifest, B: PathParam: Manifest](f2: (A, B) => Out) = addRoute(Route2(this.copy(elems = currentNamespace ::: elems), f2))
      def withMethod(method: Method) = RouteDef2(method, elems)
      def as(ext: String) = RouteDef2(method, elems, Some(ext))
    }

    case class Route2[A: PathParam: Manifest, B: PathParam: Manifest](routeDef: RouteDef2, f2: (A, B) => Out) extends Route[RouteDef2] {
      def apply(a: A, b: B, ext: Option[String] = routeDef.ext) = Call(routeDef.method.toString, PathMatcher2(routeDef.elems, ext)(a, b))
      def unapply(req: RequestHeader): Option[() => Out] =
        if (basic(req)) PathMatcher2.unapply(routeDef.elems, splitPath(req.path), f2) else None
      def args = List(implicitly[Manifest[A]], implicitly[Manifest[B]])
    }

    object PathMatcher2 {
      def apply[A, B](elems: List[PathElem], ext: Option[String], prefix: List[PathElem] = Nil)(a: A, b: B)(implicit ppa: PathParam[A], ppb: PathParam[B]): String = elems match {
        case Static(x) :: rest => apply(rest, ext, prefix :+ Static(x))(a, b)
        case (* | **) :: rest => PathMatcher1(prefix ::: Static(ppa(a)) :: rest, ext)(b)
        case _ => PathMatcher0(elems, ext)()
      }
      def unapply[A, B](elems: List[PathElem], parts: List[String], handler: (A, B) => Out)(implicit ppa: PathParam[A], ppb: PathParam[B]): Option[() => Out] = (elems, parts) match {
        case (Static(x) :: xs, y :: ys) if x == y => unapply(xs, ys, handler)
        case (* :: xs, ppa(a) :: ys) => PathMatcher1.unapply(xs, ys, (b: B) => handler(a, b))
        case _ => None
      }
    }

    case class RouteDef3(method: Method, elems: List[PathElem], ext: Option[String] = None) extends RouteDef[RouteDef3] {
      def /(static: Static) = RouteDef3(method, elems :+ static)
      def /(p: PathElem) = RouteDef4(method, elems :+ p)
      def to[A: PathParam: Manifest, B: PathParam: Manifest, C: PathParam: Manifest](f3: (A, B, C) => Out) = addRoute(Route3(this.copy(elems = currentNamespace ::: elems), f3))
      def withMethod(method: Method) = RouteDef3(method, elems)
      def as(ext: String) = RouteDef3(method, elems, Some(ext))
    }

    case class Route3[A: PathParam: Manifest, B: PathParam: Manifest, C: PathParam: Manifest](routeDef: RouteDef3, f3: (A, B, C) => Out) extends Route[RouteDef3] {
      def apply(a: A, b: B, c: C, ext: Option[String] = routeDef.ext) = Call(routeDef.method.toString, PathMatcher3(routeDef.elems, ext)(a, b, c))
      def unapply(req: RequestHeader): Option[() => Out] =
        if (basic(req)) PathMatcher3.unapply(routeDef.elems, splitPath(req.path), f3) else None
      def args = List(implicitly[Manifest[A]], implicitly[Manifest[B]], implicitly[Manifest[C]])
    }

    object PathMatcher3 {
      def apply[A, B, C](elems: List[PathElem], ext: Option[String], prefix: List[PathElem] = Nil)(a: A, b: B, c: C)(implicit ppa: PathParam[A], ppb: PathParam[B], ppc: PathParam[C]): String = elems match {
        case Static(x) :: rest => apply(rest, ext, prefix :+ Static(x))(a, b, c)
        case (* | **) :: rest => PathMatcher2(prefix ::: Static(ppa(a)) :: rest, ext)(b, c)
        case _ => PathMatcher0(elems, ext)()
      }
      def unapply[A, B, C](elems: List[PathElem], parts: List[String], handler: (A, B, C) => Out)(implicit ppa: PathParam[A], ppb: PathParam[B], ppc: PathParam[C]): Option[() => Out] = (elems, parts) match {
        case (Static(x) :: xs, y :: ys) if x == y => unapply(xs, ys, handler)
        case (* :: xs, ppa(a) :: ys) => PathMatcher2.unapply(xs, ys, (b: B, c: C) => handler(a, b, c))
        case _ => None
      }
    }

    case class RouteDef4(method: Method, elems: List[PathElem], ext: Option[String] = None) extends RouteDef[RouteDef4] {
      def /(static: Static) = RouteDef4(method, elems :+ static)
      def /(p: PathElem) = RouteDef5(method, elems :+ p)
      def to[A: PathParam: Manifest, B: PathParam: Manifest, C: PathParam: Manifest, D: PathParam: Manifest](f4: (A, B, C, D) => Out) = addRoute(Route4(this.copy(elems = currentNamespace ::: elems), f4))
      def withMethod(method: Method) = RouteDef4(method, elems)
      def as(ext: String) = RouteDef4(method, elems, Some(ext))
    }

    case class Route4[A: PathParam: Manifest, B: PathParam: Manifest, C: PathParam: Manifest, D: PathParam: Manifest](routeDef: RouteDef4, f4: (A, B, C, D) => Out) extends Route[RouteDef4] {
      def apply(a: A, b: B, c: C, d: D, ext: Option[String] = routeDef.ext) = Call(routeDef.method.toString, PathMatcher4(routeDef.elems, ext)(a, b, c, d))
      def unapply(req: RequestHeader): Option[() => Out] =
        if (basic(req)) PathMatcher4.unapply(routeDef.elems, splitPath(req.path), f4) else None
      def args = List(implicitly[Manifest[A]], implicitly[Manifest[B]], implicitly[Manifest[C]], implicitly[Manifest[D]])
    }

    object PathMatcher4 {
      def apply[A, B, C, D](elems: List[PathElem], ext: Option[String], prefix: List[PathElem] = Nil)(a: A, b: B, c: C, d: D)(implicit ppa: PathParam[A], ppb: PathParam[B], ppc: PathParam[C], ppd: PathParam[D]): String = elems match {
        case Static(x) :: rest => apply(rest, ext, prefix :+ Static(x))(a, b, c, d)
        case (* | **) :: rest => PathMatcher3(prefix ::: Static(ppa(a)) :: rest, ext)(b, c, d)
        case _ => PathMatcher0(elems, ext)()
      }
      def unapply[A, B, C, D](elems: List[PathElem], parts: List[String], handler: (A, B, C, D) => Out)(implicit ppa: PathParam[A], ppb: PathParam[B], ppc: PathParam[C], ppd: PathParam[D]): Option[() => Out] = (elems, parts) match {
        case (Static(x) :: xs, y :: ys) if x == y => unapply(xs, ys, handler)
        case (* :: xs, ppa(a) :: ys) => PathMatcher3.unapply(xs, ys, (b: B, c: C, d: D) => handler(a, b, c, d))
        case _ => None
      }
    }

    case class RouteDef5(method: Method, elems: List[PathElem], ext: Option[String] = None) extends RouteDef[RouteDef5] {
      def /(static: Static) = RouteDef5(method, elems :+ static)
      def /(p: PathElem) = RouteDef6(method, elems :+ p)
      def to[A: PathParam: Manifest, B: PathParam: Manifest, C: PathParam: Manifest, D: PathParam: Manifest, E: PathParam: Manifest](f5: (A, B, C, D, E) => Out) = addRoute(Route5(this.copy(elems = currentNamespace ::: elems), f5))
      def withMethod(method: Method) = RouteDef5(method, elems)
      def as(ext: String) = RouteDef5(method, elems, Some(ext))
    }

    case class Route5[A: PathParam: Manifest, B: PathParam: Manifest, C: PathParam: Manifest, D: PathParam: Manifest, E: PathParam: Manifest](routeDef: RouteDef5, f5: (A, B, C, D, E) => Out) extends Route[RouteDef5] {
      def apply(a: A, b: B, c: C, d: D, e: E, ext: Option[String] = routeDef.ext) = Call(routeDef.method.toString, PathMatcher5(routeDef.elems, ext)(a, b, c, d, e))
      def unapply(req: RequestHeader): Option[() => Out] =
        if (basic(req)) PathMatcher5.unapply(routeDef.elems, splitPath(req.path), f5) else None
      def args = List(implicitly[Manifest[A]], implicitly[Manifest[B]], implicitly[Manifest[C]], implicitly[Manifest[D]], implicitly[Manifest[E]])
    }

    object PathMatcher5 {
      def apply[A, B, C, D, E](elems: List[PathElem], ext: Option[String], prefix: List[PathElem] = Nil)(a: A, b: B, c: C, d: D, e: E)(implicit ppa: PathParam[A], ppb: PathParam[B], ppc: PathParam[C], ppd: PathParam[D], ppe: PathParam[E]): String = elems match {
        case Static(x) :: rest => apply(rest, ext, prefix :+ Static(x))(a, b, c, d, e)
        case (* | **) :: rest => PathMatcher4(prefix ::: Static(ppa(a)) :: rest, ext)(b, c, d, e)
        case _ => PathMatcher0(elems, ext)()
      }
      def unapply[A, B, C, D, E](elems: List[PathElem], parts: List[String], handler: (A, B, C, D, E) => Out)(implicit ppa: PathParam[A], ppb: PathParam[B], ppc: PathParam[C], ppd: PathParam[D], ppe: PathParam[E]): Option[() => Out] = (elems, parts) match {
        case (Static(x) :: xs, y :: ys) if x == y => unapply(xs, ys, handler)
        case (* :: xs, ppa(a) :: ys) => PathMatcher4.unapply(xs, ys, (b: B, c: C, d: D, e: E) => handler(a, b, c, d, e))
        case _ => None
      }
    }

    case class RouteDef6(method: Method, elems: List[PathElem], ext: Option[String] = None) extends RouteDef[RouteDef6] {
      def /(static: Static) = RouteDef6(method, elems :+ static)
      // def /(p: PathElem) = RouteDef7(method, elems :+ p)
      def to[A: PathParam: Manifest, B: PathParam: Manifest, C: PathParam: Manifest, D: PathParam: Manifest, E: PathParam: Manifest, F: PathParam: Manifest](f6: (A, B, C, D, E, F) => Out) = addRoute(Route6(this.copy(elems = currentNamespace ::: elems), f6))
      def withMethod(method: Method) = RouteDef6(method, elems)
      def as(ext: String) = RouteDef6(method, elems, Some(ext))
    }

    case class Route6[A: PathParam: Manifest, B: PathParam: Manifest, C: PathParam: Manifest, D: PathParam: Manifest, E: PathParam: Manifest, F: PathParam: Manifest](routeDef: RouteDef6, f6: (A, B, C, D, E, F) => Out) extends Route[RouteDef6] {
      def apply(a: A, b: B, c: C, d: D, e: E, f: F, ext: Option[String] = routeDef.ext) = Call(routeDef.method.toString, PathMatcher6(routeDef.elems, ext)(a, b, c, d, e, f))
      def unapply(req: RequestHeader): Option[() => Out] =
        if (basic(req)) PathMatcher6.unapply(routeDef.elems, splitPath(req.path), f6) else None
      def args = List(implicitly[Manifest[A]], implicitly[Manifest[B]], implicitly[Manifest[C]], implicitly[Manifest[D]], implicitly[Manifest[E]], implicitly[Manifest[F]])
    }

    object PathMatcher6 {
      def apply[A, B, C, D, E, F](elems: List[PathElem], ext: Option[String], prefix: List[PathElem] = Nil)(a: A, b: B, c: C, d: D, e: E, f: F)(implicit ppa: PathParam[A], ppb: PathParam[B], ppc: PathParam[C], ppd: PathParam[D], ppe: PathParam[E], ppf: PathParam[F]): String = elems match {
        case Static(x) :: rest => apply(rest, ext, prefix :+ Static(x))(a, b, c, d, e, f)
        case (* | **) :: rest => PathMatcher5(prefix ::: Static(ppa(a)) :: rest, ext)(b, c, d, e, f)
        case _ => PathMatcher0(elems, ext)()
      }
      def unapply[A, B, C, D, E, F](elems: List[PathElem], parts: List[String], handler: (A, B, C, D, E, F) => Out)(implicit ppa: PathParam[A], ppb: PathParam[B], ppc: PathParam[C], ppd: PathParam[D], ppe: PathParam[E], ppf: PathParam[F]): Option[() => Out] = (elems, parts) match {
        case (Static(x) :: xs, y :: ys) if x == y => unapply(xs, ys, handler)
        case (* :: xs, ppa(a) :: ys) => PathMatcher5.unapply(xs, ys, (b: B, c: C, d: D, e: E, f: F) => handler(a, b, c, d, e, f))
        case _ => None
      }
    }

    trait PathParam[T] {
      def apply(t: T): String
      def unapply(s: String): Option[T]
    }

    def silent[T](f: => T) = try { Some(f) } catch { case _: Throwable => None }
    implicit val IntPathParam: PathParam[Int] = new PathParam[Int] {
      def apply(i: Int) = i.toString
      def unapply(s: String) = silent(s.toInt)
    }

    implicit val LongPathParam: PathParam[Long] = new PathParam[Long] {
      def apply(l: Long) = l.toString
      def unapply(s: String) = silent(s.toLong)
    }

    implicit val DoublePathParam: PathParam[Double] = new PathParam[Double] {
      def apply(d: Double) = d.toString
      def unapply(s: String) = silent(s.toDouble)
    }

    implicit val FloatPathParam: PathParam[Float] = new PathParam[Float] {
      def apply(f: Float) = f.toString
      def unapply(s: String) = silent(s.toFloat)
    }

    implicit val StringPathParam: PathParam[String] = new PathParam[String] {
      def apply(s: String) = s
      def unapply(s: String) = Some(s)
    }

    implicit val BooleanPathParam: PathParam[Boolean] = new PathParam[Boolean] {
      def apply(b: Boolean) = b.toString
      def unapply(s: String) = s.toLowerCase match {
        case "1" | "true" | "yes" => Some(true)
        case "0" | "false" | "no" => Some(false)
        case _ => None
      }
    }

    trait ResourcesRouting[T] {
      val index: Route0
      val `new`: Route0
      val create: Route0
      val show: Route1[T]
      val edit: Route1[T]
      val update: Route1[T]
      val delete: Route1[T]
    }

    /**
     * Nested Resource Routing
     */

    trait ResourcesRouting2[T1, T2] {
      val index: Route1[T1]
      val `new`: Route1[T1]
      val create: Route1[T1]
      val show: Route2[T1, T2]
      val edit: Route2[T1, T2]
      val update: Route2[T1, T2]
      val delete: Route2[T1, T2]
    }

    trait ResourcesRouting3[T1, T2, T3] {
      val index: Route2[T1, T2]
      val `new`: Route2[T1, T2]
      val create: Route2[T1, T2]
      val show: Route3[T1, T2, T3]
      val edit: Route3[T1, T2, T3]
      val update: Route3[T1, T2, T3]
      val delete: Route3[T1, T2, T3]
    }

    trait ResourcesRouting4[T1, T2, T3, T4] {
      val index: Route3[T1, T2, T3]
      val `new`: Route3[T1, T2, T3]
      val create: Route3[T1, T2, T3]
      val show: Route4[T1, T2, T3, T4]
      val edit: Route4[T1, T2, T3, T4]
      val update: Route4[T1, T2, T3, T4]
      val delete: Route4[T1, T2, T3, T4]
    }

    trait ResourcesRouting5[T1, T2, T3, T4, T5] {
      val index: Route4[T1, T2, T3, T4]
      val `new`: Route4[T1, T2, T3, T4]
      val create: Route4[T1, T2, T3, T4]
      val show: Route5[T1, T2, T3, T4, T5]
      val edit: Route5[T1, T2, T3, T4, T5]
      val update: Route5[T1, T2, T3, T4, T5]
      val delete: Route5[T1, T2, T3, T4, T5]
    }

    trait FlatResourcesRouting[T1, T2] {
      val index: Route1[T1]
      val `new`: Route1[T1]
      val create: Route1[T1]
      val show: Route1[T2]
      val edit: Route1[T2]
      val update: Route1[T2]
      val delete: Route1[T2]
    }

    trait JsonResourcesRouting[T1, T2] {
      val index: Route1[T1]
      val `new`: Route1[T1]
      val create: Route1[T1]
      val show: Route1[T2]
      val edit: Route1[T2]
      val update: Route1[T2]
      val delete: Route1[T2]
    }

    // resources
    def resources[T: PathParam: Manifest](name: String, controller: Resources[T, Out]) = new ResourcesRouting[T] {
      val index = GET on name to controller.index
      val `new` = GET on name / "new" to controller.`new`
      val create = POST on name to controller.create
      val show = GET on name / * to controller.show
      val edit = GET on name / * / "edit" to controller.edit
      val update = PUT on name / * to controller.update
      val delete = DELETE on name / * to controller.delete
    }

    /**
     * Nested Resource Definitions
     */

    def resources2[T1: PathParam: Manifest, T2: PathParam: Manifest](name1: String, name2: String, controller: Resources2[T1, T2, Out]) = new ResourcesRouting2[T1, T2] {
      val index = GET on name1 / * / name2 to controller.index
      val `new` = GET on name1 / * / name2 / "new" to controller.`new`
      val create = POST on name1 / * / name2 to controller.create
      val show = GET on name1 / * / name2 / * to controller.show
      val edit = GET on name1 / * / name2 / * / "edit" to controller.edit
      val update = PUT on name1 / * / name2 / * to controller.update
      val delete = DELETE on name1 / * / name2 / * to controller.delete
    }

    def resources3[T1: PathParam: Manifest, T2: PathParam: Manifest, T3: PathParam: Manifest](name1: String, name2: String, name3: String, controller: Resources3[T1, T2, T3, Out]) = new ResourcesRouting3[T1, T2, T3] {
      val index = GET on name1 / * / name2 / * / name3 to controller.index
      val `new` = GET on name1 / * / name2 / * / name3 / "new" to controller.`new`
      val create = POST on name1 / * / name2 / * / name3 to controller.create
      val show = GET on name1 / * / name2 / * / name3 / * to controller.show
      val edit = GET on name1 / * / name2 / * / name3 / * / "edit" to controller.edit
      val update = PUT on name1 / * / name2 / * / name3 / * to controller.update
      val delete = DELETE on name1 / * / name2 / * / name3 / * to controller.delete
    }

    def resources4[T1: PathParam: Manifest, T2: PathParam: Manifest, T3: PathParam: Manifest, T4: PathParam: Manifest](name1: String, name2: String, name3: String, name4: String, controller: Resources4[T1, T2, T3, T4, Out]) = new ResourcesRouting4[T1, T2, T3, T4] {
      val index = GET on name1 / * / name2 / * / name3 / * / name4 to controller.index
      val `new` = GET on name1 / * / name2 / * / name3 / * / name4 / "new" to controller.`new`
      val create = POST on name1 / * / name2 / * / name3 / * / name4 to controller.create
      val show = GET on name1 / * / name2 / * / name3 / * / name4 / * to controller.show
      val edit = GET on name1 / * / name2 / * / name3 / * / name4 / * / "edit" to controller.edit
      val update = PUT on name1 / * / name2 / * / name3 / * / name4 / * to controller.update
      val delete = DELETE on name1 / * / name2 / * / name3 / * / name4 / * to controller.delete
    }

    def resources5[T1: PathParam: Manifest, T2: PathParam: Manifest, T3: PathParam: Manifest, T4: PathParam: Manifest, T5: PathParam: Manifest](name1: String, name2: String, name3: String, name4: String, name5: String, controller: Resources5[T1, T2, T3, T4, T5, Out]) = new ResourcesRouting5[T1, T2, T3, T4, T5] {
      val index = GET on name1 / * / name2 / * / name3 / * / name4 / * / name5 to controller.index
      val `new` = GET on name1 / * / name2 / * / name3 / * / name4 / * / name5 / "new" to controller.`new`
      val create = POST on name1 / * / name2 / * / name3 / * / name4 / * / name5 to controller.create
      val show = GET on name1 / * / name2 / * / name3 / * / name4 / * / name5 / * to controller.show
      val edit = GET on name1 / * / name2 / * / name3 / * / name4 / * / name5 / * / "edit" to controller.edit
      val update = PUT on name1 / * / name2 / * / name3 / * / name4 / * / name5 / * to controller.update
      val delete = DELETE on name1 / * / name2 / * / name3 / * / name4 / * / name5 / * to controller.delete
    }

    def flatResources[T1: PathParam: Manifest, T2: PathParam: Manifest](name1: String, name2: String, controller: FlatResources[T1, T2, Out]) = new FlatResourcesRouting[T1, T2] {
      val index = GET on name1 / * / name2 to controller.index
      val `new` = GET on name1 / * / name2 / "new" to controller.`new`
      val create = POST on name1 / * / name2 to controller.create
      val show = GET on name2 / * to controller.show
      val edit = GET on name2 / * / "edit" to controller.edit
      val update = PUT on name2 / * to controller.update
      val delete = DELETE on name2 / * to controller.delete
    }

    def jsonResources[T1: PathParam: Manifest, T2: PathParam: Manifest](name1: String, name2: String, controller: JsonResources[T1, T2, Out]) = new JsonResourcesRouting[T1, T2] {
      val index = GET on name1 / * / name2 to controller.index
      val `new` = GET on name1 / * / name2 / "new" to controller.`new`
      val create = POST on name1 / * / name2 to controller.create
      val show = GET on name2 / * to controller.show
      val edit = GET on name2 / * / "edit" to controller.edit
      val update = PUT on name2 / * to controller.update
      val delete = DELETE on name2 / * to controller.delete
    }

    // namespace
    protected val namespaceStack = new collection.mutable.Stack[Static]
    def currentNamespace = namespaceStack.toList.reverse
    def namespace(path: Static)(f: => Unit) = {
      namespaceStack push path
      f
      namespaceStack.pop
    }

    def withNamespace[T](path: List[Static])(f: => T) = {
      path.foreach { p => namespaceStack push p }
      val r = f
      path.foreach { p => namespaceStack.pop }
      r
    }

    class Namespace(path: Static) extends DelayedInit {
      def delayedInit(body: => Unit) = namespace(path)(body)
    }
  }

  class PlayModule(parent: PlayNavigation) extends PlayNavigation with DelayedInit {
    def delayedInit(body: => Unit) = withNamespace(parent.currentNamespace)(body)
  }

  trait PlayResourcesController[T] extends Resources[T, Handler] with Controller

  /**
   * Resource controllers
   */

  def OPERATION_NOT_IMPLEMENTED = Action { MethodNotAllowed }

  // TODO: parametrise create, etc. so we can use an action parser
  // i.e. def create[A] would allow ApiAction(parse.json, ...)

  trait ResourcesController[T1] extends Resources[T1, Handler] with Controller {
    def index() = OPERATION_NOT_IMPLEMENTED
    def `new`() = OPERATION_NOT_IMPLEMENTED
    def create() = OPERATION_NOT_IMPLEMENTED
    def show(id1: T1) = OPERATION_NOT_IMPLEMENTED
    def edit(id1: T1) = OPERATION_NOT_IMPLEMENTED
    def update(id1: T1) = OPERATION_NOT_IMPLEMENTED
    def delete(id1: T1) = OPERATION_NOT_IMPLEMENTED
  }

  trait ResourcesController2[T1, T2] extends Resources2[T1, T2, Handler] with Controller {
    def index(id1: T1) = OPERATION_NOT_IMPLEMENTED
    def `new`(id1: T1) = OPERATION_NOT_IMPLEMENTED
    def create(id1: T1) = OPERATION_NOT_IMPLEMENTED
    def show(id1: T1, id2: T2) = OPERATION_NOT_IMPLEMENTED
    def edit(id1: T1, id2: T2) = OPERATION_NOT_IMPLEMENTED
    def update(id1: T1, id2: T2) = OPERATION_NOT_IMPLEMENTED
    def delete(id1: T1, id2: T2) = OPERATION_NOT_IMPLEMENTED
  }

  trait ResourcesController3[T1, T2, T3] extends Resources3[T1, T2, T3, Handler] with Controller {
    def index(id1: T1, id2: T2) = OPERATION_NOT_IMPLEMENTED
    def `new`(id1: T1, id2: T2) = OPERATION_NOT_IMPLEMENTED
    def create(id1: T1, id2: T2) = OPERATION_NOT_IMPLEMENTED
    def show(id1: T1, id2: T2, id3: T3) = OPERATION_NOT_IMPLEMENTED
    def edit(id1: T1, id2: T2, id3: T3) = OPERATION_NOT_IMPLEMENTED
    def update(id1: T1, id2: T2, id3: T3) = OPERATION_NOT_IMPLEMENTED
    def delete(id1: T1, id2: T2, id3: T3) = OPERATION_NOT_IMPLEMENTED
  }

  trait ResourcesController4[T1, T2, T3, T4] extends Resources4[T1, T2, T3, T4, Handler] with Controller {
    def index(id1: T1, id2: T2, id3: T3) = OPERATION_NOT_IMPLEMENTED
    def `new`(id1: T1, id2: T2, id3: T3) = OPERATION_NOT_IMPLEMENTED
    def create(id1: T1, id2: T2, id3: T3) = OPERATION_NOT_IMPLEMENTED
    def show(id1: T1, id2: T2, id3: T3, id4: T4) = OPERATION_NOT_IMPLEMENTED
    def edit(id1: T1, id2: T2, id3: T3, id4: T4) = OPERATION_NOT_IMPLEMENTED
    def update(id1: T1, id2: T2, id3: T3, id4: T4) = OPERATION_NOT_IMPLEMENTED
    def delete(id1: T1, id2: T2, id3: T3, id4: T4) = OPERATION_NOT_IMPLEMENTED
  }

  trait ResourcesController5[T1, T2, T3, T4, T5] extends Resources5[T1, T2, T3, T4, T5, Handler] with Controller {
    def index(id1: T1, id2: T2, id3: T3, id4: T4) = OPERATION_NOT_IMPLEMENTED
    def `new`(id1: T1, id2: T2, id3: T3, id4: T4) = OPERATION_NOT_IMPLEMENTED
    def create(id1: T1, id2: T2, id3: T3, id4: T4) = OPERATION_NOT_IMPLEMENTED
    def show(id1: T1, id2: T2, id3: T3, id4: T4, id5: T5) = OPERATION_NOT_IMPLEMENTED
    def edit(id1: T1, id2: T2, id3: T3, id4: T4, id5: T5) = OPERATION_NOT_IMPLEMENTED
    def update(id1: T1, id2: T2, id3: T3, id4: T4, id5: T5) = OPERATION_NOT_IMPLEMENTED
    def delete(id1: T1, id2: T2, id3: T3, id4: T4, id5: T5) = OPERATION_NOT_IMPLEMENTED
  }

  trait FlatResourcesController[T1, T2] extends FlatResources[T1, T2, Handler] with Controller {
    def index(id1: T1) = OPERATION_NOT_IMPLEMENTED
    def `new`(id1: T1) = OPERATION_NOT_IMPLEMENTED
    def create(id1: T1) = OPERATION_NOT_IMPLEMENTED
    def show(id2: T2) = OPERATION_NOT_IMPLEMENTED
    def edit(id2: T2) = OPERATION_NOT_IMPLEMENTED
    def update(id2: T2) = OPERATION_NOT_IMPLEMENTED
    def delete(id2: T2) = OPERATION_NOT_IMPLEMENTED
  }

  def JSON_OPERATION_NOT_IMPLEMENTED = Action[JsValue](parse.json) { request => MethodNotAllowed }

  /*trait JsonResourcesController[T1] extends Resources[T1, Action[JsValue]] with Controller {
    def index() = JSON_OPERATION_NOT_IMPLEMENTED
    def `new`() = JSON_OPERATION_NOT_IMPLEMENTED
    def create() = JSON_OPERATION_NOT_IMPLEMENTED
    def show(id1: T1) = JSON_OPERATION_NOT_IMPLEMENTED
    def edit(id1: T1) = JSON_OPERATION_NOT_IMPLEMENTED
    def update(id1: T1) = JSON_OPERATION_NOT_IMPLEMENTED
    def delete(id1: T1) = JSON_OPERATION_NOT_IMPLEMENTED
  }
  
  trait JsonResourcesController2[T1, T2] extends Resources2[T1, T2, Action[JsValue]] with Controller {
    def index(id1: T1) = JSON_OPERATION_NOT_IMPLEMENTED
    def `new`(id1: T1) = JSON_OPERATION_NOT_IMPLEMENTED
    def create(id1: T1) = JSON_OPERATION_NOT_IMPLEMENTED
    def show(id1: T1, id2: T2) = JSON_OPERATION_NOT_IMPLEMENTED
    def edit(id1: T1, id2: T2) = JSON_OPERATION_NOT_IMPLEMENTED
    def update(id1: T1, id2: T2) = JSON_OPERATION_NOT_IMPLEMENTED
    def delete(id1: T1, id2: T2) = JSON_OPERATION_NOT_IMPLEMENTED
  }*/

  trait JsonResourcesController[T1, T2] extends JsonResources[T1, T2, Out] with Controller {
    def index(id1: T1) = OPERATION_NOT_IMPLEMENTED
    def `new`(id1: T1) = OPERATION_NOT_IMPLEMENTED
    def create(id1: T1) = OPERATION_NOT_IMPLEMENTED
    def show(id2: T2) = OPERATION_NOT_IMPLEMENTED
    def edit(id2: T2) = OPERATION_NOT_IMPLEMENTED
    def update(id2: T2) = OPERATION_NOT_IMPLEMENTED
    def delete(id2: T2) = OPERATION_NOT_IMPLEMENTED
  }

}
