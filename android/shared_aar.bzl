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

def shared_aar(name, module, deps = [], visibility = None):
    """Imports the AAR of one shared module.

    Visibility follows the module rules: a shared feature is visible to its own Android feature and to
    the application, the umbrella to the application only, a shared core module to every Android module.

    Args:
        name: name of the target.
        module: path of the Gradle module under shared/, such as "feature/login".
        deps: what the code of the module uses, as in its build.gradle.kts: other shared modules and
            Maven libraries. An AAR does not declare its dependencies to Bazel.
        visibility: overrides the visibility described above.
    """
    parts = module.split("/")
    artifact = parts[-1] + "-android"
    directory = "/".join([_REPOSITORY] + _BASE_PACKAGE.split(".") + ["shared"] + parts[:-1] + [artifact])
    package = ".".join([_BASE_PACKAGE, "shared"] + parts)

    if visibility == None:
        if parts[0] == "feature" and len(parts) == 2:
            visibility = ["//android/feature/%s:__pkg__" % parts[1], "//android/app:__pkg__"]
        elif parts[0] == "core" and len(parts) == 2:
            visibility = ["//android:__subpackages__"]
        elif parts == ["umbrella"]:
            visibility = ["//android/app:__pkg__"]
        else:
            fail("'%s' is not a shared module: expected core/<name>, feature/<name> or umbrella." % module)

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
        deps = deps,
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
