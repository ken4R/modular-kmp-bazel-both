plugins {
    id("android.app")
}

dependencies {
    implementation(project(":shared:umbrella"))
    implementation(project(":android:core:designsystem"))
    implementation(project(":android:feature:login"))
}
