enablePlugins(
  ZioSbtEcosystemPlugin,
  ZioSbtCiPlugin,
)

inThisBuild(
  List(
    name                     := "zio-uuid",
    organization             := "com.guizmaii",
    homepage                 := Some(url("https://github.com/guizmaii-opensource/zio-uuid")),
    zioVersion               := "2.1.15",
    scala213                 := "2.13.16",
    scala3                   := "3.3.5",
    crossScalaVersions -= scala212.value,
    ciEnabledBranches        := Seq("main"),
    ciPostReleaseJobs        := Seq.empty,
    Test / parallelExecution := false,
    Test / fork              := true,
    run / fork               := true,
    ciJvmOptions ++= Seq("-Xms6G", "-Xmx6G", "-Xss4M", "-XX:+UseG1GC"),
    scalafixDependencies ++= List(
      "com.github.vovapolu"                      %% "scaluzzi" % "0.1.23",
      "io.github.ghostbuster91.scalafix-unified" %% "unified"  % "0.0.9",
    ),
    licenses                 := Seq(License.Apache2),
    developers               := List(
      Developer(
        "ant8e",
        "Antoine Comte",
        "",
        url("https://github.com/ant8e"),
      ),
      Developer(
        "guizmaii",
        "Jules Ivanic",
        "",
        url("https://github.com/guizmaii"),
      ),
    ),
  )
)

addCommandAlias("updateReadme", "reload;docs/generateReadme")

lazy val root =
  project
    .in(file("."))
    .settings(
      name               := "zio-uuid",
      publish / skip     := true,
      crossScalaVersions := Nil, // https://www.scala-sbt.org/1.x/docs/Cross-Build.html#Cross+building+a+project+statefully
    )
    .aggregate(
      `zio-uuid`
    )

lazy val `zio-uuid` =
  project
    .in(file("zio-uuid"))
    .settings(stdSettings(Some("zio-uuid")))
    .settings(addOptionsOn("2.13")("-Xsource:3"))
    .settings(
      libraryDependencies ++= Seq(
        "dev.zio"           %%% "zio"         % zioVersion.value,
        "dev.zio"            %% "zio-prelude" % "1.0.0-RC39",
        "dev.zio"           %%% "zio-json"    % "0.7.36"         % Optional,
        "dev.zio"           %%% "zio-test"    % zioVersion.value % Test,
        "org.scalameta"     %%% "munit"       % "1.1.0"          % Test,
        "com.github.poslegm" %% "munit-zio"   % "0.3.0"          % Test,
      )
    )

lazy val docs =
  project
    .in(file("zio-uuid-docs"))
    .settings(
      moduleName                                 := "zio-uuid-docs",
      scalacOptions -= "-Yno-imports",
      scalacOptions -= "-Xfatal-warnings",
      projectName                                := "zio-uuid",
      mainModuleName                             := (`zio-uuid` / moduleName).value,
      projectStage                               := ProjectStage.ProductionReady,
      ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(`zio-uuid`),
      readmeCredits                              :=
        "This library is a fork of the [uuid4cats-effect](https://github.com/ant8e/uuid4cats-effect) library made by Antoine Comte (https://github.com/ant8e)",
      readmeLicense += s"\n\nCopyright 2023-${java.time.Year.now()} Jules Ivanic and the zio-uuid contributors.",
    )
    .enablePlugins(WebsitePlugin)
    .dependsOn(`zio-uuid`)
