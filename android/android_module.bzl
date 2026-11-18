"""`android_module` and `android_application`: the Android modules, built by Bazel.

A module is a directory with this layout:

    BUILD.bazel                     one `android_module(...)` call
    src/main/kotlin/**/*.kt
    src/main/res/**                 optional: strings and other resources
    src/test/kotlin/**/*Test.kt     optional: Robolectric tests, one test target per file

The macro configures what Gradle's convention plugins configure in prototype A: the Android
namespace, which comes from the path (`android/feature/login` is `com.example.modular.feature.login`),
the SDK levels of gradle/libs.versions.toml, the Compose compiler and the common dependencies.

Targets:
    <name>          the Kotlin Android library, named after its directory
    <TestClass>     one Robolectric test per file of src/test, such as `LoginScreenTest`

    bazel test //android/feature/login:all     tests of one module
"""

load("@android_catalog//:catalog.bzl", "MIN_SDK", "TARGET_SDK")
load("@rules_android//android:rules.bzl", "android_binary")
load("@rules_kotlin//kotlin:android.bzl", "kt_android_library", "kt_android_local_test")

# Base package of the project, as BASE_PACKAGE in build-logic. Placeholder, like the application id.
BASE_PACKAGE = "com.example.modular"

COMPOSE_COMPILER_PLUGIN = "//android:compose_compiler_plugin"

# Compose dependencies of every Android module. Versions come from the Compose BOM of the catalog.
_COMPOSE_DEPS = [
    "@maven//:androidx_compose_material3_material3",
    "@maven//:androidx_compose_ui_ui",
    "@maven//:androidx_compose_ui_ui_tooling_preview",
]

# What a screen needs to observe a shared ViewModel.
_SCREEN_DEPS = [
    "@maven//:androidx_lifecycle_lifecycle_runtime_compose",
    "@maven//:androidx_lifecycle_lifecycle_viewmodel_compose",
    "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_android",
    "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
]

_APPLICATION_DEPS = _SCREEN_DEPS + [
    "@maven//:androidx_activity_activity_compose",
    # Runtime of androidx.concurrent, which profileinstaller uses at startup: see the catalog.
    "@maven//:com_google_guava_listenablefuture",
]

_TEST_DEPS = [
    "@maven//:androidx_compose_ui_ui_test_junit4",
    # Declares the empty activity that hosts the screen under test.
    "@maven//:androidx_compose_ui_ui_test_manifest",
    "@maven//:androidx_test_ext_junit",
    "@maven//:junit_junit",
    "@maven//:org_robolectric_robolectric",
]

# The Android runtime of Robolectric, found offline: //android:robolectric_runtime.
_ROBOLECTRIC_RUNTIME = "//android:robolectric_runtime"
_ROBOLECTRIC_PROPERTIES = "//android:robolectric-deps.properties"

# Robolectric reflects into the JDK, which Java 17 and later close by default.
_ROBOLECTRIC_JVM_FLAGS = [
    "--add-opens=java.base/java.io=ALL-UNNAMED",
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.net=ALL-UNNAMED",
    "--add-opens=java.base/java.security=ALL-UNNAMED",
    "--add-opens=java.base/java.text=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
    "--add-opens=java.base/jdk.internal.util=ALL-UNNAMED",
]

_KINDS = ["core", "feature"]

def android_module(name, deps = [], test_deps = [], visibility = None):
    """Declares an Android module of `android/core` or `android/feature`: a library and its tests.

    Visibility follows the module rules: a feature is visible to the application only, so a feature
    can never depend on another feature. A core module is visible to every Android module.

    Args:
        name: name of the module. It must be the name of its directory.
        deps: what the module uses: other Android modules (`//android/core/<name>`), the shared code
            (`//android/shared:<module>`) and Maven libraries (`@maven//:<library>`).
        test_deps: what the tests use on top of `deps`.
        visibility: overrides the visibility described above.
    """
    package = native.package_name()
    parts = package.split("/")
    if len(parts) != 3 or parts[0] != "android" or parts[1] not in _KINDS or parts[2] != name:
        fail("An Android module lives in android/core/<name> or android/feature/<name>, and is named " +
             "after its directory: '%s' cannot be declared in '%s'." % (name, package))
    kind = parts[1]
    namespace = ".".join([BASE_PACKAGE, kind, name])

    if visibility == None:
        visibility = ["//android/app:__pkg__"] if kind == "feature" else ["//android:__subpackages__"]

    manifest = name + "_manifest"
    _library_manifest(
        name = manifest,
        package = namespace,
        min_sdk = MIN_SDK,
        visibility = ["//visibility:private"],
    )

    library_deps = _COMPOSE_DEPS + (_SCREEN_DEPS if kind == "feature" else [])
    kt_android_library(
        name = name,
        srcs = native.glob(["src/main/kotlin/**/*.kt"]),
        custom_package = namespace,
        manifest = ":" + manifest,
        plugins = [COMPOSE_COMPILER_PLUGIN],
        resource_files = native.glob(["src/main/res/**"], allow_empty = True),
        visibility = visibility,
        deps = _unique(deps + library_deps),
    )

    for source in native.glob(["src/test/kotlin/**/*Test.kt"], allow_empty = True):
        test_class = source.removeprefix("src/test/kotlin/").removesuffix(".kt").replace("/", ".")
        kt_android_local_test(
            name = test_class.split(".")[-1],
            srcs = [source],
            associates = [":" + name],
            custom_package = namespace,
            data = [_ROBOLECTRIC_RUNTIME],
            jvm_flags = _ROBOLECTRIC_JVM_FLAGS,
            manifest = ":" + manifest,
            # Robolectric runs the Android version of targetSdk, like the application.
            manifest_values = {
                "minSdkVersion": MIN_SDK,
                "targetSdkVersion": TARGET_SDK,
            },
            plugins = [COMPOSE_COMPILER_PLUGIN],
            robolectric_properties_file = _ROBOLECTRIC_PROPERTIES,
            test_class = test_class,
            deps = _unique(deps + test_deps + library_deps + _TEST_DEPS),
        )

def android_application(name, application_id, version_code, version_name, deps = []):
    """Declares the Android application of `android/app`: its code and its APK.

    It is the only Android target that may depend on a feature and on the umbrella of the shared code.

    Targets:
        <name>              the debug APK: bazel build //android/app
        <name>_library      the Kotlin code and the resources of the application

    Args:
        name: name of the APK target.
        application_id: the application id.
        version_code: the version code, as a string.
        version_name: the version name.
        deps: the modules the application assembles.
    """
    if native.package_name() != "android/app":
        fail("android_application is the application of android/app only.")
    namespace = BASE_PACKAGE + ".app"

    manifest = name + "_library_manifest"
    _library_manifest(
        name = manifest,
        package = namespace,
        min_sdk = MIN_SDK,
        visibility = ["//visibility:private"],
    )

    library = name + "_library"
    kt_android_library(
        name = library,
        srcs = native.glob(["src/main/kotlin/**/*.kt"]),
        custom_package = namespace,
        manifest = ":" + manifest,
        plugins = [COMPOSE_COMPILER_PLUGIN],
        resource_files = native.glob(["src/main/res/**"], allow_empty = True),
        visibility = ["//visibility:private"],
        deps = _unique(deps + _COMPOSE_DEPS + _APPLICATION_DEPS),
    )

    android_binary(
        name = name,
        custom_package = namespace,
        manifest = "src/main/AndroidManifest.xml",
        manifest_values = {
            "applicationId": application_id,
            "minSdkVersion": MIN_SDK,
            "targetSdkVersion": TARGET_SDK,
            "versionCode": version_code,
            "versionName": version_name,
        },
        deps = [":" + library],
    )

def _unique(labels):
    """Removes repeated labels, which Bazel refuses in a list attribute."""
    result = []
    for label in labels:
        if label not in result:
            result.append(label)
    return result

def _library_manifest_impl(ctx):
    manifest = ctx.actions.declare_file(ctx.label.name + "/AndroidManifest.xml")
    ctx.actions.write(
        output = manifest,
        content = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{package}">
    <uses-sdk android:minSdkVersion="{min_sdk}" />
</manifest>
""".format(package = ctx.attr.package, min_sdk = ctx.attr.min_sdk),
    )
    return [DefaultInfo(files = depset([manifest]))]

_library_manifest = rule(
    implementation = _library_manifest_impl,
    doc = """Writes the manifest of a library. Gradle writes it from the namespace: modules have none.""",
    attrs = {
        "min_sdk": attr.string(mandatory = True, doc = "minSdkVersion of the library."),
        "package": attr.string(mandatory = True, doc = "Android namespace of the library."),
    },
)
