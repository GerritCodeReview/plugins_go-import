This plugin provides support of
[remote import paths](https://golang.org/cmd/go/#hdr-Remote_import_paths) and
[go-get command](https://golang.org/cmd/go/).

### Usage

* Go-get command

```
go get example.org/foo
```

* Remote import paths

```
import "example.org/foo"
```

### Limitation
Folder of the repository is not supported in the context of Gerrit up to
ambiguity of repository name. E.g., _example.org/foo/bar_ has two potential meanings:

* Folder _bar_ in repository _foo_, or
* repository _foo/bar_.

