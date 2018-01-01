---
layout: book-article
title: Release Build
---
<div class="alert alert-info">
  To understand the following instructions, please make sure you have read the <a href="../intro/overview.html">overview to the build process</a>. Then install the required tools for your development host. For instructions choose your build host platform from the Menu.
</div>

A release build includes all neccessary artifacts to run rxtx on all officially supported platforms (see [the build matrix](../intro/matrix.html)).

This is a three step process:

 1. Build all platform independend code and pack the native source code.
 2. Build each native library on a supported build host. This means you might have to compile code on multiple machines. (For a painless build process on a single host, a [cross toolchain setup](../platforms/ubuntu.html) on Ubuntu Linux is recommended.)
 3. Assemble the distribution JARs.

First, run the platform independend build phase:

    mvn -Dmake.release=prepare install

This will build the

* cross-toolchain-wrapper,
* rxtx-api,
* rxtxSerial-java and
* rxtxSerial-native

artifacts and the *[lib]rxtxSerial-os-arch* binary matching your build platform. If your build host supports additional target platforms via cross compilers, you can include them in the build by enabling their respective maven profile. e.g. on a [prepared](../platforms/ubuntu.html) Ubuntu Linux you can include all artifacts by running:

    mvn -Dmake.release=prepare -Pwith-linux-x86,with-linux-x86_64,with-windows-x86,with-windows-x86_64,with-osx-x86_64 install

If your build host does not support the build of all targets, you have to continue the process on one or more other systems, until all native targets are built. To do that, you have to run above command (with respective profiles) on the machines. Then collect the binaries from the *target* directories and copy them to one machine. [Deploy](http://maven.apache.org/guides/mini/guide-3rd-party-jars-remote.html) the binaries to the local maven repository on one machine. Then continue on this particular machine.

Finally the previously build artifacts are packed to a release by running
    
    mvn -Dmake.release=bundle install

When the build finishes, the rxtx api JAR and rxtxSerial driver JAR are installed in your local maven repository. To use the JARs with *ant* or other build systems, you might copy them from the following directories:

    /somewhere/rxtx/rxtx-api/target
    /somewhere/rxtx/rxtxSerial/target
    

