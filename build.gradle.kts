plugins {
   id("us.ihmc.ihmc-build")
}

ihmc {
   group = "us.ihmc"
   version = "17-0.2.2"
   vcsUrl = "https://github.com/ihmcrobotics/ihmc-javafx-extensions"
   openSource = true
   
   configureDependencyResolution()
   configurePublications()
}

mainDependencies {
   var javaFXVersion = "17.0.8"
   api(ihmc.javaFXModule("base", javaFXVersion))
   api(ihmc.javaFXModule("controls", javaFXVersion))
   api(ihmc.javaFXModule("graphics", javaFXVersion))
   
   api("us.ihmc:euclid:0.22.3")
}

