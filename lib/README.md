# Local build dependency

The build expects one jar in this directory:

```
Slimefun-United-4.10-alpha-19+050decd.jar
```

SHA-256:

```
62821aa5fdcbb77788430a5a08d958f8b52fa74c12c3b33c21ef9b5756ad6fe7
```

It is referenced from `pom.xml` with `system` scope and is **not** bundled into
the finished addon jar — the server supplies Slimefun itself at runtime.

The binary is omitted from this bundle because it is a third-party build and not
this project's to redistribute. Get it from whoever runs the target server, or
build the matching Slimefun-United commit (`050decd`) yourself, then verify the
hash above.

## Why not upstream Slimefun4?

Slimefun-United 4.10-alpha uses a database-backed `BlockStorage` API. Upstream
Slimefun4 RC-37 does not. The two are source-compatible enough that a substitution
compiles, then fails at runtime with `NoSuchMethodError` once blocks are placed
or read. If you swap the dependency, audit every `BlockStorage` call site and
`com.wwsf.integration.SlimefunStorageAccess` first.

A JitPack coordinate for this fork was tried and does not resolve
(`io.github.slimefun-united:slimefun-united:4.10-alpha+050decd` returns HTTP 400),
which is why the jar is vendored rather than fetched.
