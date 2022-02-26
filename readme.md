# Prerequisites
Install Java JDK 11 or 17 on your computer via preferred method. 

The easy way for most platforms is downloading and installing prebuilt package here: https://adoptium.net/?variant=openjdk11

# Run
Go to *dist* folder and execute:
```java -jar hugs-1.0.jar```

Application has sensible default configuration, but you may want to change list of hug receivers. To do that - follow the instructions after the launch

Latest configuration options:
```
  Options:
    -e, -endless
      Keep hugs even if receiver doesn't respond, which usually indicates a 
      success of passionate hugs. Use false to share hugs with more receivers. 
      Use true to concentrate on few receivers
      Default: false
    -p, -port
      Port to knock on
      Default: 80
    -r, -receiver
      Hug receiver (for one hugger only)
    -rs, -receivers
      URL or file path to the list of targets to hug
      Default: https://raw.githubusercontent.com/david-l-books/storage/main/urls.txt
    -s, -sockets
      Number of sockets to use per receiver
      Default: 10
    -th, -threads
      Number of threads to use
      Default: 220
    -t, -time
      How long to perform hugs (in minutes). Leave at 0 to never stop hugging
      Default: 0
```

# Contribute
TBD

## For native builds:
Download latest GraalVM for your platform here:
https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-22.0.0.2

Installation instructions:
https://www.graalvm.org/22.0/docs/getting-started/linux/

Add the native images and llvm:
gu install native-image
gu install llvm-toolchain

Configure for Gradle:
https://docs.gradle.org/7.1.1/userguide/toolchains.html
https://graalvm.github.io/native-build-tools/0.9.4/gradle-plugin.html

sudo apt-get install build-essential libz-dev zlib1g-dev
