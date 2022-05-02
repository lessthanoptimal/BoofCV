/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.slam;

/**
 * Expands into a new view where some views have a known extrinsic relationship. If there's a 3D relationship
 * between a view in the scene with a known extrinsics than that is used. If there isn't then the 3D relationship
 * is estimated and added using known intrinsics of each view.
 *
 * @author Peter Abeles
 */
public class MetricKnownExtrinsicsExpandByOneView {
}
