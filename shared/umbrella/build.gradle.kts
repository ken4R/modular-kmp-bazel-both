import modular.buildlogic.exportToSwift

plugins {
    id("kmp.umbrella")
}

kotlin {
    // What is listed here is the Swift API of the Shared framework: one line per feature.
    // Core modules are not exported: Swift reaches them through ViewModels only.
    exportToSwift(
        project(":shared:feature:login"),
        libs.androidx.lifecycle.viewmodel,
    )

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:session"))
        }
    }
}
