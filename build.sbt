name := "org.ngnono.ytsearch"
 
version := "1.0"
 
scalaVersion := "2.10.1"

libraryDependencies ++= Seq(
                            "com.github.seratch" %% "scalikesolr" % "[4.3,)",
                            "org.rogach" %% "scallop" %"0.9.4"
                           )