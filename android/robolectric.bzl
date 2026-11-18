"""`robolectric_runtime`: the Android runtime of Robolectric, for tests that run offline.

Robolectric downloads the Android jar of each SDK level the first time it needs it. Bazel tests have
no network, so rules_android starts them with `-Drobolectric.offline=true` and the properties file
of `robolectric_properties_file`, which maps each jar to a file. This rule writes that file for the
jar of one SDK level, a pinned Maven dependency, and stages the jar next to it.
"""

def _robolectric_runtime_impl(ctx):
    jars = [
        jar
        for jar in ctx.files.jar
        if jar.extension == "jar" and not jar.basename.endswith("-sources.jar")
    ]
    if len(jars) != 1:
        fail("%s gives %d jars, where one Android runtime was expected." % (ctx.attr.jar.label, len(jars)))

    # Robolectric resolves the path of the properties file from the directory of that file.
    file_name = "android-all-instrumented-%s.jar" % ctx.attr.version
    directory = ctx.actions.declare_directory(ctx.label.name)
    ctx.actions.run_shell(
        inputs = jars,
        outputs = [directory],
        arguments = [directory.path, jars[0].path, file_name],
        command = 'mkdir -p "$1" && cp "$2" "$1/$3"',
        mnemonic = "RobolectricRuntime",
        progress_message = "Staging the Robolectric runtime %s" % ctx.attr.version,
    )
    ctx.actions.write(
        output = ctx.outputs.properties,
        content = "org.robolectric\\:android-all-instrumented\\:%s=%s/%s\n" % (
            ctx.attr.version,
            ctx.label.name,
            file_name,
        ),
    )

    files = [ctx.outputs.properties, directory]
    return [DefaultInfo(files = depset(files), runfiles = ctx.runfiles(files = files))]

robolectric_runtime = rule(
    implementation = _robolectric_runtime_impl,
    doc = "Writes a robolectric-deps.properties file for one Android runtime jar, and stages the jar.",
    attrs = {
        "jar": attr.label(mandatory = True, allow_files = True, doc = "The android-all-instrumented jar."),
        "properties": attr.output(mandatory = True, doc = "The properties file to write."),
        "version": attr.string(mandatory = True, doc = "Version of the jar, as Robolectric names it."),
    },
)
