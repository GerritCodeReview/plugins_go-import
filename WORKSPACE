workspace(name = "go_import")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "10e78cc706760ff24cbc67ba527f9a8e4134d66f"
    #local_path = "/home/<user>/projects/bazlets",
)

load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
    "gerrit_api",
)

gerrit_api()
