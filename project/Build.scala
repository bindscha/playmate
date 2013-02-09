import sbt._
import Keys._
import play.Project._

object PlayMateBuild extends Build {

  lazy val AuthProject = play.Project(
    "PlayMate-Auth",
    path = file("src/auth")).settings(
      (BuildSettings.GLOBAL_SETTINGS ++
        Seq(
          libraryDependencies ++= Dependencies.SCATTER,
          resolvers ++= (Resolvers.TYPESAFE ++ Resolvers.BINDSCHA))))

  lazy val BoilerplateProject = play.Project(
    "PlayMate-Boilerplate",
    path = file("src/boilerplate")).settings(
      (BuildSettings.GLOBAL_SETTINGS ++
        Seq(
          libraryDependencies ++= Dependencies.SCATTER,
          resolvers ++= (Resolvers.TYPESAFE ++ Resolvers.BINDSCHA),

          // Less & Javascript assets
          lessEntryPoints <<= baseDirectory { base =>
            (base / "app" / "assets" / "boilerplate" / "css" / "bootstrap.less") +++
              (base / "app" / "assets" / "boilerplate" / "css" / "responsive.less")
          },
          javascriptEntryPoints <<= baseDirectory { base =>
            (base / "app" / "assets" / "boilerplate" / "js" * "application.js")
          })))

  lazy val NavigationProject = play.Project(
    "PlayMate-Navigation",
    path = file("src/navigation")).settings(
      (BuildSettings.GLOBAL_SETTINGS ++
        Seq(
          libraryDependencies ++= Dependencies.SCATTER,
          resolvers ++= (Resolvers.TYPESAFE ++ Resolvers.BINDSCHA))))

  lazy val RootProject = play.Project(
    BuildSettings.PROJECT_NAME).settings(
      (BuildSettings.GLOBAL_SETTINGS ++
        com.typesafe.sbt.SbtScalariform.scalariformSettings ++
        Seq(
          libraryDependencies ++= Dependencies.SCATTER,
          resolvers ++= (Resolvers.TYPESAFE ++ Resolvers.BINDSCHA),
          
          // Do not publish root project
          publish := {},

          // Override doc task to generate one documentation for all subprojects 
          doc <<= Tasks.docTask(file("documentation/api")),
          aggregate in doc := false))).aggregate(
        AuthProject,
        BoilerplateProject,
        NavigationProject)

  object BuildSettings {
    val ORGANIZATION = "com.bindscha"
    val ORGANIZATION_NAME = "Laurent Bindschaedler"
    val ORGANIZATION_HOMEPAGE = "http://www.bindschaedler.com"

    val PROJECT_NAME = "PlayMate"
    val PROJECT_VERSION = "0.1.0"

    val INCEPTION_YEAR = 2011

    val PUBLISH_DOC = Option(System.getProperty("publish.doc")).isDefined

    val SCALA_VERSION = "2.10.0"
    val BINARY_SCALA_VERSION = CrossVersion.binaryScalaVersion(SCALA_VERSION)

    val GLOBAL_SETTINGS: Seq[sbt.Project.Setting[_]] = Seq(
      organization := ORGANIZATION,
      organizationName := ORGANIZATION_NAME,
      organizationHomepage := Some(url(ORGANIZATION_HOMEPAGE)),

      version := PROJECT_VERSION,

      scalaVersion := SCALA_VERSION,
      scalaBinaryVersion := BINARY_SCALA_VERSION,

      publishArtifact in packageDoc := PUBLISH_DOC,

      scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-encoding", "utf8"),
      javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-g", "-encoding", "utf8"))
  }

  object Resolvers {
    val TYPESAFE_RELEASES = "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/"
    val TYPESAFE_SNAPSHOTS = "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
    val TYPESAFE = Seq(TYPESAFE_RELEASES) ++ (if (BuildSettings.PROJECT_VERSION.endsWith("SNAPSHOT")) Seq(TYPESAFE_SNAPSHOTS) else Nil)

    val BINDSCHA_RELEASES = "Bindscha Releases Repo" at "http://repo.bindschaedler.com/releases/"
    val BINDSCHA_SNAPSHOTS = "Bindscha Snapshots Repo" at "http://repo.bindschaedler.com/snapshots/"
    val BINDSCHA = Seq(BINDSCHA_RELEASES) ++ (if (BuildSettings.PROJECT_VERSION.endsWith("SNAPSHOT")) Seq(BINDSCHA_SNAPSHOTS) else Nil)
  }

  object Dependencies {

    val SCATTER = Seq(
      "com.bindscha" %% "scatter-random" % "0.1.0",
      "com.bindscha" %% "scatter-regex" % "0.1.0")

  }

  object Tasks {

    // ----- Generate documentation
    def docTask(docRoot: java.io.File, maximumErrors: Int = 10) = (dependencyClasspath in Test, compilers, streams) map { (classpath, compilers, streams) =>
      // Clear the previous version of the doc
      IO.delete(docRoot)

      // Grab all jars and source files
      val jarFiles = (file("src") ** "*.jar").get
      val sourceFiles = (file("src") ** ("*.scala" || "*.java")).get

      // Run scaladoc
      new Scaladoc(maximumErrors, compilers.scalac)(
        BuildSettings.PROJECT_NAME + " " + BuildSettings.PROJECT_VERSION + " - " + "Scala API",
        sourceFiles,
        classpath.map(_.data) ++ jarFiles,
        docRoot,
        Seq(
          "-external-urls:" + (Map(
            "scala" -> "http://www.scala-lang.org/api/current/") map (p => p._1 + "=" + p._2) mkString (";")),
          "-skip-packages", Seq(
            "controllers") mkString (":"),
          "-doc-footer", "Copyright (c) " +
            BuildSettings.INCEPTION_YEAR + "-" + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) +
            " " + BuildSettings.ORGANIZATION_NAME + ". All rights reserved.",
          "-diagrams"),
        streams.log)

      // Return documentation root
      docRoot
    }

  }

  // ----- Augment sbt.Project with a settings method that takes a Seq

  class ImprovedProject(val sbtProject: Project) {
    def settings(ss: Seq[sbt.Project.Setting[_]]): Project =
      sbtProject.settings(ss: _*)
  }

  implicit def project2improvedproject(sbtProject: Project): ImprovedProject = new ImprovedProject(sbtProject)
  implicit def improvedproject2project(improvedProject: ImprovedProject): Project = improvedProject.sbtProject

}
