"""`shared_aar`: the Android library of a shared Kotlin module, as Gradle publishes it.

Gradle builds the shared code and `tools/shared/sync --android` publishes one AAR per module in the
local Maven repository android/shared/maven, which is never committed. Bazel imports the AARs from
there, the way it imports Shared.xcframework for iOS.

    shared/feature/login  ->  android/shared/maven/com/example/modular/shared/feature/login-android/<version>/
"""

load("@rules_android//android:rules.bzl", "aar_import")

# Base package of the project, as BASE_PACKAGE in build-logic.
_BASE_PACKAGE = "com.example.modular"

# The local Maven repository, relative to this package.
_REPOSITORY = "maven"

# aar_import checks that every class an AAR uses comes from one of its direct deps. The root artifact
# of a Kotlin Multiplatform library holds no classes: its classes are in the artifact of a platform,
# `-android` for most AndroidX libraries, `-jvm` for the kotlinx libraries, which Android uses as is.
# The deps below name those artifacts.

# What every shared module uses, as the Kotlin plugin and `kmp.core` add it: the Kotlin library,
# the nullability annotations of the compiled code, and coroutines.
_COMMON_DEPS = [
    "@maven//:org_jetbrains_annotations",
    "@maven//:org_jetbrains_kotlin_kotlin_stdlib",
    "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
]

# What `kmp.feature` adds: the ViewModel of AndroidX.
_FEATURE_DEPS = [
    "@maven//:androidx_lifecycle_lifecycle_viewmodel_android",
]

def shared_aar(name, module, deps = [], visibility = None):
    """Imports the AAR of one shared module.

    Visibility follows the module rules: a shared feature is visible to its own Android feature and to
    the application, the umbrella to the application only, a shared core module to every Android module.

    The dependencies that the convention plugins add are added here too: Kotlin and coroutines for
    every module, the AndroidX ViewModel for a feature.

    Args:
        name: name of the target.
        module: path of the Gradle module under shared/, such as "feature/login".
        deps: what the build.gradle.kts of the module adds to its convention plugin: other shared
            modules and Maven libraries. An AAR does not declare its dependencies to Bazel. A Kotlin
            Multiplatform library is named by the artifact of its Android classes, such as
            `@maven//:androidx_lifecycle_lifecycle_viewmodel_android`.
        visibility: overrides the visibility described above.
    """
    parts = module.split("/")
    artifact = parts[-1] + "-android"
    directory = "/".join([_REPOSITORY] + _BASE_PACKAGE.split(".") + ["shared"] + parts[:-1] + [artifact])
    package = ".".join([_BASE_PACKAGE, "shared"] + parts)

    if parts[0] == "feature" and len(parts) == 2:
        default_visibility = ["//android/feature/%s:__pkg__" % parts[1], "//android/app:__pkg__"]
        plugin_deps = _COMMON_DEPS + _FEATURE_DEPS
    elif parts[0] == "core" and len(parts) == 2:
        default_visibility = ["//android:__subpackages__"]
        plugin_deps = _COMMON_DEPS
    elif parts == ["umbrella"]:
        default_visibility = ["//android/app:__pkg__"]
        plugin_deps = _COMMON_DEPS
    else:
        fail("'%s' is not a shared module: expected core/<name>, feature/<name> or umbrella." % module)
    if visibility == None:
        visibility = default_visibility

    aars = native.glob(["%s/*/%s-*.aar" % (directory, artifact)], allow_empty = True)
    if len(aars) != 1:
        _missing_aar(
            name = name,
            count = len(aars),
            path = native.package_name() + "/" + directory,
            visibility = visibility,
        )
        return

    # The sources, for the IDE and the debugger.
    sources = native.glob(["%s/*/%s-*-sources.jar" % (directory, artifact)], allow_empty = True)
    aar_import(
        name = name,
        aar = aars[0],
        package = package,
        srcjar = sources[0] if sources else None,
        visibility = visibility,
        # A label given twice would make Bazel refuse the list.
        deps = plugin_deps + [dep for dep in deps if dep not in plugin_deps],
    )

def _missing_aar_impl(ctx):
    fail("""

{path} holds {count} Android libraries of this module, where Bazel expects one.
They are built from the Kotlin sources and are not committed. Publish them, then run Bazel again:

    tools/shared/sync --android
""".format(path = ctx.attr.path, count = ctx.attr.count))

_missing_aar = rule(
    implementation = _missing_aar_impl,
    doc = "Stands for an AAR that was not published yet: it fails the build with the command to run.",
    attrs = {
        "count": attr.int(doc = "Number of AARs found."),
        "path": attr.string(mandatory = True, doc = "Directory of the AAR, from the root of the repository."),
    },
)
