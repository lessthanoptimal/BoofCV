/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.interest;

/**
 * Features can belong to multiple set. A feature set indicates that the features were some how detected using
 * mutually exclusive methods. A classical example comes from blob detectors where there will naturally be
 * two sets composed of dark and white blobs.
 *
 * @author Peter Abeles
 */
public interface FeatureSets {
	/**
	 * The number of feature sets.
	 *
	 * @return number of feature sets
	 */
	int getNumberOfSets();

	/**
	 * Returns the set that a feature belongs in
	 * @param index Which feature
	 */
	int getSet(int index);
}
