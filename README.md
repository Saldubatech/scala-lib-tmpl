# scala-lib-tmpl
Opinionated Template for building Scala enterprise modules/libraries


## Maven Repo for Libraries

This project publishes libraries to Github Packages under the
[Saldubatech/packages](https://github.com/Saldubatech/packages/packages/2012643) repository.

To publish from the desktop:

1. Update the version in the `lib/build.sbt` file
2. execute `sbt lib/publish`

To use the `lib` library in sbt

```sbt
libraryDependencies ++= Seq(
  // [...]
   "com.saldubatech" % "lib" %% "<version>"
  // [...]
)
```
The versions will be available in the [packages](https://github.com/Saldubatech/packages) repository

### GH Actions

T.B.D.

### Reference Materials

- A step-by-step guide to [publishing libraries to GH packages with Sbt](https://medium.com/@supermanue/how-to-publish-a-scala-library-in-github-bfb0fa39c1e4)
- The [sbt plugin](https://github.com/djspiewak/sbt-github-packages) to support publishing
