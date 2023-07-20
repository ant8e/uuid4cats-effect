import com.typesafe.tools.mima.core._

ThisBuild / tlBaseVersion := "0.3" // your current series x.y

ThisBuild / organization := "tech.ant8e"
ThisBuild / organizationName := "Antoine Comte"
ThisBuild / startYear := Some(2023)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("ant8e", "Antoine Comte")
)

// publish to s01.oss.sonatype.org (set to true to publish to oss.sonatype.org instead)
ThisBuild / tlSonatypeUseLegacyHost := false

// publish website from this branch
//ThisBuild / tlSitePublishBranch := Some("main")

val Scala213 = "2.13.11"
ThisBuild / crossScalaVersions := Seq(Scala213, "3.3.0")
ThisBuild / scalaVersion := Scala213 // the default Scala

lazy val root = tlCrossRootProject.aggregate(core)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "uuid4cats-effect",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % "2.9.0",
      "org.typelevel" %%% "cats-effect" % "3.5.1",
      "org.scalameta" %%% "munit" % "0.7.29" % Test,
      "org.typelevel" %%% "munit-cats-effect-3" % "1.0.7" % Test
    ),
    mimaBinaryIssueFilters ++= Seq(
      ProblemFilters.exclude[ReversedMissingMethodProblem](
        "tech.ant8e.uuid4cats.TimestampedUUIDGeneratorBuilder.tech$ant8e$uuid4cats$TimestampedUUIDGeneratorBuilder$$GeneratorState"
      )
    )
  )

lazy val docs = project.in(file("site")).enablePlugins(TypelevelSitePlugin)
