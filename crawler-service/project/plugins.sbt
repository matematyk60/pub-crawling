addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.4.1")

addSbtPlugin("com.thesamet"  % "sbt-protoc"   % "0.99.34")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.21")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.8"
