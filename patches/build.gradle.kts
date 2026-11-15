group = "app.revanced"

// In GitHub Actions GITHUB_REPOSITORY is "<owner>/<repo>", so the links below fill themselves in.
// For a local build, set the environment variable or replace the fallback text.
val repositorySlug = System.getenv("GITHUB_REPOSITORY") ?: "OWNER/revanced-patches-android5"

patches {
    about {
        name = "ReVanced Extended for Android 5 (YouTube 16.40.36)"
        description = "RVX-style patches for YouTube 16.40.36 (Android 5+), ported to ReVanced patcher v22"
        source = "https://github.com/$repositorySlug"
        author = "d4n3436 / kitadai31 / inotia00 / ReVanced (ported to patcher v22)"
        contact = "https://github.com/$repositorySlug/issues"
        website = "https://github.com/$repositorySlug"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    // Required due to smali, or build fails. Can be removed once smali is bumped.
    implementation(libs.guava)

    // Android API stubs defined here.
    compileOnly(project(":patches:stub"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexplicit-backing-fields",
            "-Xcontext-parameters"
        )
    }
}
