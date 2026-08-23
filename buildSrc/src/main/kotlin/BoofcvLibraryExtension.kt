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

import org.gradle.api.provider.Property

/**
 * Lets a published module declare its role beyond simply "published".
 *
 * Declared here rather than inside the convention script so the root build can reference
 * the type: a type declared in a precompiled script plugin is not importable elsewhere.
 */
interface BoofcvLibraryExtension {
    /**
     * Whether this module's compiled output and source belong in the combined artifacts:
     * `oneJarBin`, `alljavadoc` and `createLibraryDirectory`.
     *
     * True for real library modules. False for dependency-aggregator POMs like boofcv-core
     * and boofcv-all, which carry no API of their own, and for the runnable app modules,
     * which are products rather than library API.
     */
    val includeInAggregates: Property<Boolean>
}
