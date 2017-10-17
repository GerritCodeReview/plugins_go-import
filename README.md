This plugin allows you to perform `go get` requests against a Gerrit project.

## Packages
Go packages may be imported by specifying the package's _folder_ after the repository name.
For example, "import github.com/bob/my-project/package1" will download the repository from
`github.com/bob/my-project` and then include the package `package1`.

For Gerrit, this is still possible, but it's a little bit more ambiguous.
On GitHub, all projects have a depth of two: (1) group and (2) repository.
On Gerrit, any project may have an arbitrary depth.
Thus, the following project names are valid in Gerrit:

1. `bob`
1. `bob/my-project`
1. `bob/my-project/some-other-project`
1. `tom`
1. `tom/my-project`

When a user requests a package via `go get`, this plugin will attempt to match the _most specific_
project and return that.

Using our previous examples of existing projects, this plugin will return the following projects
for the given requests:

| Request                            | Project          |
| ---------------------------------- | ---------------- |
| `/bob`                             | `bob`            |
| `/bob/package1`                    | `bob`            |
| `/bob/my-project`                  | `bob/my-project` |
| `/bob/my-project/package1`         | `bob/my-project` |
| `/bob/my-project/package1/folder2` | `bob/my-project` |

