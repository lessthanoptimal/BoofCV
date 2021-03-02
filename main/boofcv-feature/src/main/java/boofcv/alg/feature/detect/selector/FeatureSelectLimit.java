/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.selector;

import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

/**
 * Selects a subset of the features inside the image until it hits the requested number.
 *
 * @author Peter Abeles
 * @see FeatureSelectLimitIntensity
 */
public interface FeatureSelectLimit<Point> {
	/**
	 * Selects features inside the image.
	 *
	 * @param imageWidth Width of the image the features were detected in
	 * @param imageHeight Height of the image the features were detected in
	 * @param prior (Input) Locations of previously detected features
	 * @param detected (Input) Locations of newly detected features
	 * @param limit (Input) The maximum number of new features detected
	 * @param selected (Output) Selected features. Element count not exceed the limit. Reset on every call.
	 */
	void select( int imageWidth, int imageHeight,
				 @Nullable FastAccess<Point> prior,
				 FastAccess<Point> detected, int limit, FastArray<Point> selected );
}
