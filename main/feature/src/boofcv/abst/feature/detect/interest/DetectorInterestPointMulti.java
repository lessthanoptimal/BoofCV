/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.ImageBase;

/**
 * Interface for automatically detecting multiple types of interest points in images.  Each type of interest point
 * was detected using a different technique and should only be associated with interest points of the same type. The
 * number and type of sets is fixed.
 *
 * @author Peter Abeles
 */
// Development Note:  If MSER is added then it might make sense to abstract out this class so that different
//                    types of detected features can be returned using generics
// rename to DetectorMulti<T,Type> ?
public interface DetectorInterestPointMulti<T extends ImageBase> {

	/**
	 * Detects interest points inside the provided image.
	 *
	 * @param input Input features are detected inside of.
	 */
	void detect( T input );

	/**
	 * The number of families.
	 *
	 * @return number of families
	 */
	public int getNumberOfSets();

	/**
	 * Returns the most recently detected features for a specific set.  Each time
	 * {@link #detect(boofcv.struct.image.ImageBase)} is called the results are modified.
	 *
	 * @param set Which set of detected features.
	 * @return Results for a set.
	 */
	public FoundPointSO getFeatureSet(int set);
}
