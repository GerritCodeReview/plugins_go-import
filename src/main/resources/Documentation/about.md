This plugin provides support of
[remote import paths](https://golang.org/cmd/go/#hdr-Remote_import_paths) and
[go-get command](https://golang.org/cmd/go/), which means that you can perform
`go get` (and thus `dep`) requests against a Gerrit project.

### Usage

* Go-get command

```
go get example.org/foo
```

* Remote import paths

```
import "example.org/foo"
```

### Packages
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

### Access (Anonymous vs. Authenticated URLs)
The exact git clone URL served to the client go command will depend on the
project's configured ACLs. If the project is configured to allow anonymous
read access to `refs/heads/*`, an anonymous URL will be served (e.g.
`https://gerrit.example/bob/my-project`). Otherwise, a URL that requires
authentication will be used (e.g. `https://gerrit.example/a/bob/my-project`).
