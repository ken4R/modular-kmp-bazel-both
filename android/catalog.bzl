"""Gives BUILD files the values of gradle/libs.versions.toml that are not Maven dependencies.

MODULE.bazel declares the extension, and the repository @android_catalog holds the values:

    load("@android_catalog//:catalog.bzl", "MIN_SDK", "TARGET_SDK")

Gradle reads the same file, so the shared libraries and the applications use the same SDK levels.
"""

load("@toml.bzl", "toml")

# Name in catalog.bzl -> key of [versions] in gradle/libs.versions.toml.
_VERSIONS = {
    "COMPILE_SDK": "android-compileSdk",
    "MIN_SDK": "android-minSdk",
    "TARGET_SDK": "android-targetSdk",
    "ROBOLECTRIC_ANDROID_ALL": "robolectric-android-all",
}

def _android_catalog_repository_impl(rctx):
    versions = toml.decode(rctx.read(rctx.path(rctx.attr.catalog))).get("versions", {})
    lines = [
        '"""Values of gradle/libs.versions.toml, written by //android:catalog.bzl. Do not edit."""',
        "",
    ]
    for name, key in _VERSIONS.items():
        if key not in versions:
            fail("gradle/libs.versions.toml has no version '%s' in [versions]." % key)
        lines.append("%s = %s" % (name, repr(str(versions[key]))))
    rctx.file("BUILD.bazel", "exports_files([\"catalog.bzl\"])\n")
    rctx.file("catalog.bzl", "\n".join(lines) + "\n")

_android_catalog_repository = repository_rule(
    implementation = _android_catalog_repository_impl,
    doc = "Writes catalog.bzl with the values listed in _VERSIONS.",
    attrs = {
        "catalog": attr.label(mandatory = True, doc = "The version catalog."),
    },
)

def _android_catalog_impl(mctx):
    _android_catalog_repository(
        name = "android_catalog",
        catalog = Label("//gradle:libs.versions.toml"),
    )
    return mctx.extension_metadata(reproducible = True)

android_catalog = module_extension(
    implementation = _android_catalog_impl,
    doc = "Declares @android_catalog, read from gradle/libs.versions.toml.",
)
