Setup
To use Room in your app, add the following dependencies to your app's build.gradle file.

Note: Choose only one of ksp or annotationProcessor. Don't include both.
Kotlin
Groovy

dependencies {
    val room_version = "2.8.4"

    implementation("androidx.room:room-runtime:$room_version")

    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project
    ksp("androidx.room:room-compiler:$room_version")

    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    annotationProcessor("androidx.room:room-compiler:$room_version")

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")

    // optional - RxJava2 support for Room
    implementation("androidx.room:room-rxjava2:$room_version")

    // optional - RxJava3 support for Room
    implementation("androidx.room:room-rxjava3:$room_version")

    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation("androidx.room:room-guava:$room_version")

    // optional - Test helpers
    testImplementation("androidx.room:room-testing:$room_version")

    // optional - Paging 3 Integration
    implementation("androidx.room:room-paging:$room_version")
}
Declaring dependencies
To add a dependency on Room, you must add the Google Maven repository to your project. Read Google's Maven repository for more information.

Dependencies for Room include testing Room migrations and Room RxJava

Add the dependencies for the artifacts you need in the build.gradle file for your app or module:

Kotlin
Groovy

dependencies {
    val room_version = "2.8.4"

    implementation("androidx.room:room-runtime:$room_version")

    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project
    ksp("androidx.room:room-compiler:$room_version")

    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    annotationProcessor("androidx.room:room-compiler:$room_version")

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")

    // optional - RxJava2 support for Room
    implementation("androidx.room:room-rxjava2:$room_version")

    // optional - RxJava3 support for Room
    implementation("androidx.room:room-rxjava3:$room_version")

    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation("androidx.room:room-guava:$room_version")

    // optional - Test helpers
    testImplementation("androidx.room:room-testing:$room_version")

    // optional - Paging 3 Integration
    implementation("androidx.room:room-paging:$room_version")
}
For information on using the KAPT plugin, see the KAPT documentation.

For information on using the KSP plugin, see the KSP quick-start documentation.

For information on using Kotlin extensions, see the ktx documentation.

For more information about dependencies, see Add Build Dependencies.

Optionally, for non-Android libraries (i.e. Java or Kotlin only Gradle modules) you can depend on androidx.room:room-common to use Room annotations.

Configuring Compiler Options
Room has the following annotation processor options.

room.schemaLocation	directory
Enables exporting database schemas into JSON files in the given directory. See Room Migrations for more information.
room.incremental	boolean
Enables Gradle incremental annotation processor. Default value is true.
room.generateKotlin	boolean
Generate Kotlin source files instead of Java. Requires KSP. Default value is true as of version 2.7.0. See version 2.6.0 notes, when it was introduced, for more details.
Use the Room Gradle Plugin
With Room version 2.6.0 and higher, you can use the Room Gradle Plugin to configure options for the Room compiler. The plugin configures the project such that generated schemas (which are an output of the compile tasks and are consumed for auto-migrations) are correctly configured to have reproducible and cacheable builds.

To add the plugin, in your top-level Gradle build file, define the plugin and its version.

Groovy
Kotlin

plugins {
    id("androidx.room") version "$room_version" apply false
}
In the module-level Gradle build file, apply the plugin and use the room extension.

Groovy
Kotlin

plugins {
    id("androidx.room")
}

android {
    ...
    room {
        schemaDirectory("$projectDir/schemas")
    }
}
Setting a schemaDirectory is required when using the Room Gradle Plugin. This will configure the Room compiler and the various compile tasks and its backends (javac, KAPT, KSP) to output schema files into flavored folders, for example schemas/flavorOneDebug/com.package.MyDatabase/1.json. These files should be checked into the repository to be used for validation and auto-migrations.

Some options cannot be configured in all versions of the Room Gradle Plugin, even though they are supported by the Room compiler. The table below lists each option and shows the version of the Room Gradle Plugin that added support for configuring that option using the room extension. If your version is lower, or if the option is not supported yet, you can use annotation processor options instead.

Option	Since version
room.schemaLocation (required)	2.6.0
room.incremental	-
room.generateKotlin	-
Use annotation processor options
If you aren't using the Room Gradle Plugin, or if the option you want isn't supported by your version of the plugin, you can configure Room using annotation processor options, as described in Add build dependencies. How you specify annotation options depends on whether you use KSP or KAPT for Room.

Groovy
Kotlin

// For KSP
ksp {
    arg("option_name", "option_value")
    // other options...
}

// For javac and KAPT
android {
    ...
    defaultConfig {
        ...
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "option_name" to "option_value",
                    // other options...
                )
            }
        }
    }
}
Because room.schemaLocation is a directory and not a primitive type, it is necessary to use a CommandLineArgumentsProvider when adding this option so that Gradle knows about this directory when conducting up-to-date checks. Migrate your Room database shows a complete implementation of CommandLineArgumentsProvider that provides the schema location.

---
To get started using WorkManager, first import the library into your Android project.

Add the following dependencies to your app's build.gradle file:

Groovy
Kotlin

dependencies {
    val work_version = "2.11.1"

    // (Java only)
    implementation("androidx.work:work-runtime:$work_version")

    // Kotlin + coroutines
    implementation("androidx.work:work-runtime-ktx:$work_version")

    // optional - RxJava2 support
    implementation("androidx.work:work-rxjava2:$work_version")

    // optional - GCMNetworkManager support
    implementation("androidx.work:work-gcm:$work_version")

    // optional - Test helpers
    androidTestImplementation("androidx.work:work-testing:$work_version")

    // optional - Multiprocess support
    implementation("androidx.work:work-multiprocess:$work_version")
}
---
To use coroutines in your Android project, add the following dependency to your app's build.gradle file:

Groovy
Kotlin

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")
}
---
The source code to the Retrofit, its samples, and this website is available on GitHub.

GradleSection titled “Gradle”
implementation("com.squareup.retrofit2:retrofit:3.1.0-SNAPSHOT")

MavenSection titled “Maven”
<dependency>
  <groupId>com.squareup.retrofit2</groupId>
  <artifactId>retrofit</artifactId>
  <version>3.1.0-SNAPSHOT</version>
</dependency>

Retrofit requires at minimum Java 8+ or Android API 21+.
---
The latest release is available on Maven Central.

implementation("com.squareup.okhttp3:okhttp:5.3.0")
Snapshot builds are available. R8 and ProGuard rules are available.

Also, we have a bill of materials (BOM) available to help you keep OkHttp artifacts up to date and be sure about version compatibility.

    dependencies {
       // define a BOM and its version
       implementation(platform("com.squareup.okhttp3:okhttp-bom:5.3.0"))

       // define any required OkHttp artifacts without version
       implementation("com.squareup.okhttp3:okhttp")
       implementation("com.squareup.okhttp3:logging-interceptor")
    }