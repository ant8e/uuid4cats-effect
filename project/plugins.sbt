val zioSbtVersion = "0.4.0-alpha.32"

addSbtPlugin("dev.zio" % "zio-sbt-ecosystem" % zioSbtVersion)
addSbtPlugin("dev.zio" % "zio-sbt-ci"        % zioSbtVersion)
addSbtPlugin("dev.zio" % "zio-sbt-website"   % zioSbtVersion)

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix"  % "0.14.4")
addSbtPlugin("org.scalameta" % "sbt-scalafmt"  % "2.5.6")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.0")
addSbtPlugin("org.typelevel" % "sbt-tpolecat"  % "0.5.2")

resolvers ++= Resolver.sonatypeOssRepos("public")
