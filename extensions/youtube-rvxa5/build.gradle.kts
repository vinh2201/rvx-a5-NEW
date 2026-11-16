dependencies {
    compileOnly(project(":extensions:youtube-rvxa5:stub"))
    compileOnly(libs.annotation)
    compileOnly(libs.appcompat)
}

android {
    defaultConfig {
        // YouTube 16.40.36 supports Android 5.0+.
        minSdk = 21
    }
}
