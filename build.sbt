ThisBuild / scalaVersion  := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name  := "minimal",
    libraryDependencies  ++= Seq(
      compilerPlugin(
        "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
      ),
      //"org.typelevel" %% "cats-core"             % "2.8.0",
      "org.typelevel" %% "cats-mtl"              % "1.3.0",
      "co.fs2"        %% "fs2-core"              % "3.2.13",
      "dev.optics"    %% "monocle-core"          % "3.1.0",
      "dev.optics"    %% "monocle-macro"         % "3.1.0",
      "io.estatico"   %% "newtype"               % "0.4.4",
      "eu.timepit"    %% "refined"               % "0.10.1",
      "eu.timepit"    %% "refined-cats"          % "0.10.1",
      "tf.tofu"       %% "derevo-cats"           % "0.13.0",
      "tf.tofu"       %% "derevo-cats-tagless"   % "0.13.0",
      "tf.tofu"       %% "derevo-circe-magnolia" % "0.13.0",
      "tf.tofu"       %% "tofu-core-higher-kind" % "0.10.8"
    ),
    scalacOptions ++= Seq(
      "-Ymacro-annotations", "-Wconf:cat=unused:info"
    )
  )