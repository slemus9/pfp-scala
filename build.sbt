ThisBuild / scalaVersion  := "2.13.8"

val catsMltVersion = "1.3.0"
val catsRetryVersion = "3.1.0"
val monocleVersion = "3.1.0"
val newtypeVersion = "0.4.4"
val refinedVersion = "0.10.1"
val derevoVersion = "0.13.0"
val tofuCoreHKVersion = "0.10.8"
val squantVersion = "1.6.0"
val log4catsVersion = "2.5.0"
val http4sVersion = "0.23.16"
val http4sJwtAuthVersion = "1.0.0"

lazy val root = (project in file("."))
  .settings(
    name  := "minimal",
    libraryDependencies  ++= Seq(
      compilerPlugin(
        "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
      ),
      //"org.typelevel" %% "cats-core"             % "2.8.0",
      "org.typelevel" %% "cats-mtl"              % catsMltVersion,
      "com.github.cb372" %% "cats-retry" % catsRetryVersion,
      //"co.fs2"        %% "fs2-core"              % "3.2.13",
      "dev.optics"    %% "monocle-core"          % monocleVersion,
      "dev.optics"    %% "monocle-macro"         % monocleVersion,
      "io.estatico"   %% "newtype"               % newtypeVersion,
      "eu.timepit"    %% "refined"               % refinedVersion,
      "eu.timepit"    %% "refined-cats"          % refinedVersion,
      "tf.tofu"       %% "derevo-cats"           % derevoVersion,
      "tf.tofu"       %% "derevo-cats-tagless"   % derevoVersion,
      "tf.tofu"       %% "derevo-circe-magnolia" % derevoVersion,
      "tf.tofu"       %% "tofu-core-higher-kind" % tofuCoreHKVersion,
      "org.typelevel"  %% "squants"  % squantVersion,
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "dev.profunktor" %% "http4s-jwt-auth" % http4sJwtAuthVersion
    ),
    scalacOptions ++= Seq(
      "-Ymacro-annotations", "-Wconf:cat=unused:info"
    )
  )