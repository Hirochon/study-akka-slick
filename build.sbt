name := "study-akka-slick"

version := "0.1"

scalaVersion := "2.12.13"


// Akka HTTP は、Akka 自体から独立したモジュールとして、独自のリリースサイクルで提供されています。
// Akka HTTP は、Akka 2.5、Akka 2.6、および Akka HTTP 10.2.x のライフタイム中にリリースされたそれ以降の 2.x バージョンと互換性があります。
// ただし、このモジュールは akka-actor や akka-stream には依存していないため、
// ユーザーは実行する Akka のバージョンを選択し、選択したバージョンの akka-stream に手動で依存関係を追加する必要があります。
val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.4"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.3.0",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.0",
  "mysql" % "mysql-connector-java" % "6.0.6",
  // これ
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  // と、これ
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
)