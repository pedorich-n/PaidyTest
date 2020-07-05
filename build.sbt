import Dependencies._

name := "forex"
version := "1.0.1"

scalaVersion := "2.12.10"
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xfuture",
  "-Xlint",
  "-Ydelambdafy:method",
  "-Xlog-reflective-calls",
  "-Yno-adapted-args",
  "-Ypartial-unification",
  "-Ywarn-dead-code",
  "-Ywarn-inaccessible",
  "-Ywarn-unused-import",
  "-Ywarn-value-discard"
)

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  compilerPlugin(Libraries.kindProjector),
  compilerPlugin(Libraries.betterMonadicFor),
  Libraries.cats,
  Libraries.catsEffect,
  Libraries.catsRetry,
  Libraries.log4Cats,
  Libraries.fs2,
  Libraries.http4sDsl,
  Libraries.http4sServer,
  Libraries.http4sCirce,
  Libraries.circeCore,
  Libraries.circeGeneric,
  Libraries.circeGenericExt,
  Libraries.circeJava8,
  Libraries.pureConfig,
  Libraries.logback,
  Libraries.enumeratum,
  Libraries.enumeratumCirce,
  Libraries.sttp,
  Libraries.sttpCirce,
  Libraries.guava,
  Libraries.scalaTest      % Test,
  Libraries.scalaCheck     % Test,
  Libraries.catsScalaCheck % Test,
  Libraries.circeLiteral   % Test
)

connectInput in run := true
fork in run := true

parallelExecution in Test := true

coverageEnabled in (Test, compile) := true
coverageEnabled in (Compile, compile) := false
coverageExcludedPackages := "<empty>;.*Main;.*Module;.*Application;"

mainClass := Some("forex.Main")
assembly := (assembly dependsOn dependencyUpdates).value
assemblyMergeStrategy in assembly := {
  case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$")      => MergeStrategy.discard
  case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
  case m if m.toLowerCase.contains("license")              => MergeStrategy.first
  case _                                                   => MergeStrategy.first
}
