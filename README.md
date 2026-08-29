# kotlinx-io-tar a multiplatform library tar reader/writter

## What is it?

This repository is a kotlin multiplatform library to read and create a tar. It uses [kotlinx-io](https://github.com/Kotlin/kotlinx-io) for the I/O part. Really basic support for now.

## Why use a tar format?

If you have immutable data that doesn't need to be partially accessed, that's the perfect format! Also a specificity of a tar is that if you add more data to it, you can redownload only the difference as the tar will keep the order.

# Documentation

## Read a tar

```kotlin
SystemFileSystem.source(Path("my.tar"))
    .buffered()
    .tar().use { tar ->
        var nextEntry: TarEntry? = null
        while (tar.nextEntry.also { nextEntry = it } != null) {
            if (nextEntry != null) {
                val headerInfo = nextEntry.header
                val fileDataSource = tar.currentFile()
            }
        }
    }
```

## Write a tar

```kotlin
val folder = "/tmp"
SystemFileSystem.sink(Path("myNew.tar"))
    .buffered()
    .tar().use { tarSink ->
        listOf("file1.txt", "flie2.xml").forEach { fileName ->
            val path = Path("$folder/$fileName")
            if (SystemFileSystem.exists(path)) {
                tarSink.putNextEntry(TarEntry(path, fileName))
                tarSink.write(path)
            }
        }
    }
```

# Maintenance

## Do a release

- git tag x.x.x
- git push origin x.x.x
- then go to [maven](https://central.sonatype.com/publishing/deployments) to validate the deployment
- then on GitHub [create a new release](https://github.com/Pascap-LTD/kotlinx-io-tar/releases/new)

# About

We are [pascap LTD](https://pascap.eu) a small startup, if you want to support us don't hesitate to try our app on the [play store](https://play.google.com/store/apps/details?id=com.pascap.connectedBody), we use that library in production with that application.

No llm has been used for that repository.

# Resources

- based of [ktar](https://github.com/mjdenham/ktar-multiplatform)
- used [gnu specification](https://www.gnu.org/software/tar/manual/html_node/Standard.html) for validation
- also used [apache common-compress](https://commons.apache.org/compress/) to have a reference test
- [guide](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-libraries-to-maven.html) to help publish on maven central
