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

import org.gradle.api.publish.maven.MavenPublication

/**
 * Shared POM metadata for every published BoofCV artifact.
 *
 * Lives here rather than inside the publishing convention because the android module cannot
 * apply the Java conventions yet must publish an identical POM.
 */
fun MavenPublication.boofcvPom() {
    pom {
        name.set("BoofCV")
        description.set(
            "BoofCV is an open source Java library for real-time computer vision and " +
                "robotics applications."
        )
        url.set("https://boofcv.org")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("pabeles")
                name.set("Peter Abeles")
                email.set("peter.abeles@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/lessthanoptimal/BoofCV.git")
            developerConnection.set("scm:git:git://github.com/lessthanoptimal/BoofCV.git")
            url.set("https://github.com/lessthanoptimal/BoofCV")
        }
    }
}
