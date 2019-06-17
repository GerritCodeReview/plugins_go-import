load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

gerrit_plugin(
    name = "go-import",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: go-import",
        "Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/go-import",
        "Implementation-Title: go-import plugin",
        "Implementation-Vendor: Ericsson",
        "Gerrit-HttpModule: com.ericsson.gerrit.plugins.goimport.HttpModule",
    ],
    resources = glob(["src/main/resources/**/*"]),
)

junit_tests(
    name = "go-import_tests",
    testonly = 1,
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["go-import"],
    deps = [
        ":go-import__plugin_test_deps",
    ],
)

java_library(
    name = "go-import__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":go-import__plugin",
    ],
)
