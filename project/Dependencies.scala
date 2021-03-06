import sbt._

object Dependencies {

  object Versions {
    val cats                = "2.1.0"
    val catsEffect          = "2.1.3"
    val catsRetry           = "1.1.1"
    val log4Cats            = "1.1.1"
    val fs2                 = "2.4.2"
    val http4s              = "0.21.6"
    val circe               = "0.13.0"
    val pureConfig          = "0.13.0"

    val kindProjector       = "0.11.0"
    val betterMonadicFor    = "0.3.1"
    val logback             = "1.2.3"
    val scalaCheck          = "1.14.3"
    val scalaTest           = "3.1.0"
    val catsEffectTesting   = "0.4.0"
    val catsScalaCheck      = "0.2.0"

    val enumeratum          = "1.6.1"
    val sttp                = "2.2.1"
    val guava               = "29.0-jre"
  }

  object Libraries {
    def circe(artifact: String): ModuleID = "io.circe"    %% artifact % Versions.circe
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    lazy val cats                = "org.typelevel"         %% "cats-core"                  % Versions.cats
    lazy val catsEffect          = "org.typelevel"         %% "cats-effect"                % Versions.catsEffect
    lazy val catsRetry           = "com.github.cb372"      %% "cats-retry"                 % Versions.catsRetry
    lazy val log4Cats            = "io.chrisdavenport"     %% "log4cats-slf4j"             % Versions.log4Cats
    lazy val fs2                 = "co.fs2"                %% "fs2-core"                   % Versions.fs2

    lazy val http4sDsl           = http4s("http4s-dsl")
    lazy val http4sServer        = http4s("http4s-blaze-server")
    lazy val http4sCirce         = http4s("http4s-circe")
    lazy val circeCore           = circe("circe-core")
    lazy val circeGeneric        = circe("circe-generic")
    lazy val circeGenericExt     = circe("circe-generic-extras")
    lazy val circeLiteral        = circe("circe-literal")
    lazy val circeJava8          = "io.circe" %% "circe-java8" % "0.11.1"
    lazy val pureConfig          = "com.github.pureconfig" %% "pureconfig"                 % Versions.pureConfig

    lazy val enumeratum          = "com.beachape"                 %% "enumeratum"          % Versions.enumeratum
    lazy val enumeratumCirce     = "com.beachape"                 %% "enumeratum-circe"    % Versions.enumeratum
    lazy val sttp                = "com.softwaremill.sttp.client" %% "core"                % Versions.sttp
    lazy val sttpCirce           = "com.softwaremill.sttp.client" %% "circe"               % Versions.sttp
    // For ThreadFactoryBuilder only
    lazy val guava               =  "com.google.guava"            % "guava"                % Versions.guava

    // Compiler plugins
    lazy val kindProjector       = "org.typelevel"         %% "kind-projector"             % Versions.kindProjector cross CrossVersion.full
    lazy val betterMonadicFor    = "com.olegpy"            %% "better-monadic-for"         % Versions.betterMonadicFor

    // Runtime
    lazy val logback             = "ch.qos.logback"        %  "logback-classic"            % Versions.logback

    // Test
    lazy val scalaTest           = "org.scalatest"         %% "scalatest"                  % Versions.scalaTest
    lazy val scalaCheck          = "org.scalacheck"        %% "scalacheck"                 % Versions.scalaCheck
    lazy val catsScalaCheck      = "io.chrisdavenport"     %% "cats-scalacheck"            % Versions.catsScalaCheck
  }

}
