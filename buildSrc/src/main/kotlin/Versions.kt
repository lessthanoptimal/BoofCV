/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** Single source of truth for third party versions. Previously `project.ext.*` in build.gradle. */
object Versions {
    const val DDOGLEG = "0.25.3"
    const val GEOREGRESSION = "0.30.1"
    const val COMMONS_IO = "2.16.1"
    const val DEEPBOOF = "0.5.3"
    const val LOMBOK = "1.18.42"
    const val JABEL = "1.0.1-1"
    const val GUAVA = "33.2.0-jre"
    const val ARGS4J = "2.33"
    const val JUNIT = "5.11.4"
    const val ERRORPRONE = "2.41.0"
    const val NULLAWAY = "0.12.10"
    const val AUTO64TO32 = "3.3.0"
    const val SNAKEYAML = "2.3"
    const val JMH = "1.36"
    const val JETBRAINS_ANNOTATIONS = "23.0.0"
    const val JSR250 = "1.0"
    const val JSR305 = "3.0.2"
    const val TROVE4J = "3.0.3"
    const val PDFBOX = "3.0.4"

    // JavaCPP presets. These are the versions pinned by JavaCV 1.5.14; keep them in step.
    const val JAVACV = "1.5.14"
    const val OPENCV = "4.14.0-1.5.14"
    const val OPENBLAS = "0.3.34-1.5.14"
    const val FFMPEG = "8.1.2-1.5.14"

    // Forced versions. Not declared directly, only pinned via resolutionStrategy.
    const val BYTEBUDDY = "1.17.7"
    const val CHECKER_QUAL = "2.10.0"
    const val COMMONS_LANG3 = "3.14.0"

    // NOTE: the Kotlin version is NOT here. It lives in gradle.properties as `kotlinVersion`
    // because settings.gradle.kts pluginManagement needs it and cannot read buildSrc.
    // Keeping a copy here would create a second source of truth.

    // Android
    const val ANDROID_GRADLE_PLUGIN = "8.9.0"
}

/** Dependency coordinates. Single-string notation; multi-string fails in Gradle 10. */
object Libs {
    const val DDOGLEG = "org.ddogleg:ddogleg:${Versions.DDOGLEG}"
    const val GEOREGRESSION = "org.georegression:georegression:${Versions.GEOREGRESSION}"
    const val TROVE4J = "net.sf.trove4j:trove4j:${Versions.TROVE4J}"
    const val COMMONS_IO = "commons-io:commons-io:${Versions.COMMONS_IO}"

    const val AUTOFLOAT = "com.peterabeles:autofloat:${Versions.AUTO64TO32}"
    const val AUTOCONCURRENT = "com.peterabeles:autoconcurrent:${Versions.AUTO64TO32}"
    const val REGRESSION = "com.peterabeles:regression:${Versions.AUTO64TO32}"
    const val LANGUAGE = "com.peterabeles:language:${Versions.AUTO64TO32}"

    const val LOMBOK = "org.projectlombok:lombok:${Versions.LOMBOK}"
    const val JABEL = "com.pkware.jabel:jabel-javac-plugin:${Versions.JABEL}"
    const val JETBRAINS_ANNOTATIONS = "org.jetbrains:annotations:${Versions.JETBRAINS_ANNOTATIONS}"
    const val JSR250 = "javax.annotation:jsr250-api:${Versions.JSR250}"
    const val JSR305 = "com.google.code.findbugs:jsr305:${Versions.JSR305}"
    const val GUAVA = "com.google.guava:guava:${Versions.GUAVA}"

    const val JUNIT_API = "org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}"
    const val JUNIT_ENGINE = "org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}"
    const val JUNIT_LAUNCHER = "org.junit.platform:junit-platform-launcher"

    const val ERRORPRONE_CORE = "com.google.errorprone:error_prone_core:${Versions.ERRORPRONE}"
    const val ERRORPRONE_ANNOTATIONS = "com.google.errorprone:error_prone_annotations:${Versions.ERRORPRONE}"
    const val NULLAWAY = "com.uber.nullaway:nullaway:${Versions.NULLAWAY}"

    const val JMH_CORE = "org.openjdk.jmh:jmh-core:${Versions.JMH}"
    const val JMH_ANNPROCESS = "org.openjdk.jmh:jmh-generator-annprocess:${Versions.JMH}"

    const val ARGS4J = "args4j:args4j:${Versions.ARGS4J}"
    const val PDFBOX = "org.apache.pdfbox:pdfbox:${Versions.PDFBOX}"
    const val SNAKEYAML = "org.yaml:snakeyaml:${Versions.SNAKEYAML}"

    const val JAVACV = "org.bytedeco:javacv:${Versions.JAVACV}"
    const val FFMPEG = "org.bytedeco:ffmpeg:${Versions.FFMPEG}"
    const val OPENCV = "org.bytedeco:opencv:${Versions.OPENCV}"
    const val OPENBLAS = "org.bytedeco:openblas:${Versions.OPENBLAS}"

    const val BYTEBUDDY = "net.bytebuddy:byte-buddy:${Versions.BYTEBUDDY}"
    const val BYTEBUDDY_AGENT = "net.bytebuddy:byte-buddy-agent:${Versions.BYTEBUDDY}"
    const val CHECKER_QUAL = "org.checkerframework:checker-qual:${Versions.CHECKER_QUAL}"
    const val COMMONS_LANG3 = "org.apache.commons:commons-lang3:${Versions.COMMONS_LANG3}"
}
