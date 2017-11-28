name := """idols"""
organization := "edu.utexas.tacc"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

// https://mvnrepository.com/artifact/net.sourceforge.htmlcleaner/htmlcleaner
libraryDependencies += "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.21"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "edu.utexas.tacc.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "edu.utexas.tacc.binders._"
