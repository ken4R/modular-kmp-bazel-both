plugins {
    id("kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // `api`: SessionRepository appears in the constructor of LoginViewModel.
            api(project(":shared:core:session"))
        }
    }
}
