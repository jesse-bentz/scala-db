organization  := "jesse-bentz"

version       := "0.1"

scalaVersion  := "2.10.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "2.3.7" % "test",
  "com.typesafe" % "config" % "1.2.1",
  "org.scalaz" %% "scalaz-core" % "7.0.6",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.7",
  //"ch.qos.logback"   % "logback-classic" % "1.1.2",
  "org.clapper" %% "grizzled-scala" % "1.1.6",
  "com.typesafe" % "config" % "1.2.1",
  "org.mongodb" %% "casbah" % "2.7.3",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.googlecode.flyway" % "flyway-core" % "2.3.1",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41"
)
