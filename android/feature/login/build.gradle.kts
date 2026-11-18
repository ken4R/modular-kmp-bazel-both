plugins {
    id("android.feature")
}

dependencies {
    // `api`: LoginRoute and LoginScreen take the shared ViewModel and its state as parameters.
    api(project(":shared:feature:login"))
    implementation(project(":android:core:designsystem"))
}
