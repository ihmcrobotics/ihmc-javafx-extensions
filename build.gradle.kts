plugins {
   id("us.ihmc.ihmc-build")
   id("us.ihmc.ihmc-ci") version "7.4"
   id("us.ihmc.ihmc-cd") version "1.17"
}

ihmc {
   group = "us.ihmc"
   version = "15-0.0.2"
   vcsUrl = "https://github.com/ihmcrobotics/ihmc-javafx-extensions"
   openSource = true
   
   configureDependencyResolution()
   configurePublications()
}

mainDependencies {
   var javaFXVersion = "15.0.1"
   api(ihmc.javaFXModule("base", javaFXVersion))
   api(ihmc.javaFXModule("controls", javaFXVersion))
   api(ihmc.javaFXModule("graphics", javaFXVersion))
}

