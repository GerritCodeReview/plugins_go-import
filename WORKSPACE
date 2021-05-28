workspace(name = "go_import")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "7ff4605f48db148197675a0d2ea41ee07cb72fd3"
    #local_path = "/home/<user>/projects/bazlets",
)

load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
    "gerrit_api",
)

gerrit_api()
