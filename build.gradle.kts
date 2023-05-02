plugins {
   id("us.ihmc.ihmc-build")
   id("us.ihmc.ihmc-ci") version "7.7"
   id("us.ihmc.ihmc-cd") version "1.21"
}

ihmc {
   group = "us.ihmc"
   version = "17-0.2.1"
   vcsUrl = "https://github.com/ihmcrobotics/ihmc-javafx-extensions"
   openSource = true
   
   configureDependencyResolution()
   configurePublications()
}

mainDependencies {
   var javaFXVersion = "17.0.2"
   api(ihmc.javaFXModule("base", javaFXVersion))
   api(ihmc.javaFXModule("controls", javaFXVersion))
   api(ihmc.javaFXModule("graphics", javaFXVersion))
   
   api("us.ihmc:euclid:0.20.0")
}

