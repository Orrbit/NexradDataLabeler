import Dependencies._

ThisBuild / organization := "org.ndl"
ThisBuild / scalaVersion := "2.13.6"
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

lazy val root = (project in file(".")).
  settings(
    name := "NexradDataLabeller",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-language:postfixOps",
      "-language:higherKinds", // HKT required for Monads and other HKT types
      "-Wunused", // for scalafix
    ),
    libraryDependencies ++= Dependencies.core ++ Dependencies.scalaTest,
    // assembly / mainClass := Some("org.cscie88c.MainApp"),
    // assembly / assemblyJarName := "2022SpringScalaIntro.jar",
    assembly / mainClass := Some("org.ndl.core.MainCsvReader"),
    assembly / assemblyJarName := "NexradDataLabeller.jar",
    assembly / test := {},
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case "application.conf"            => MergeStrategy.concat
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    // see shading feature at https://github.com/sbt/sbt-assembly#shading
    assembly / assemblyShadeRules := Seq(
      ShadeRule.rename("shapeless.**" -> "shadeshapeless.@1").inAll
    )
  )

// Custom task to zip files for Project submission
lazy val zipProject = taskKey[Unit]("zip files for Project submission")

zipProject := {
  val bd = baseDirectory.value
  val targetFile = s"${bd.getAbsolutePath}/scalaProject.zip"
  val ignoredPaths =
    ".*(\\.idea|target|\\.DS_Store|\\.bloop|\\.metals|\\.vsc)/*".r.pattern
  val fileFilter = new FileFilter {
    override def accept(f: File) =
      !ignoredPaths.matcher(f.getAbsolutePath).lookingAt
  }
  println("zipping Project files ...")
  IO.delete(new File(targetFile))
  IO.zip(
    Path.selectSubpaths(new File(bd.getAbsolutePath), fileFilter),
    new File(targetFile)
  )
}