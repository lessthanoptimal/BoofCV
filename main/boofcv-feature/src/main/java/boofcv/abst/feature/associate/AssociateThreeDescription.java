/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.associate;

import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.MatchScoreType;
import org.ddogleg.struct.FastQueue;

/**
 * <p>
 * Common interface for associating features between three images.
 * </p>
 * @author Peter Abeles
 */
public interface AssociateThreeDescription<Desc> {

	/**
	 * Specify descriptors in image A
	 * @param features feature descriptors
	 */
	void setFeaturesA( FastQueue<Desc> features );

	/**
	 * Specify descriptors in image B
	 * @param features feature descriptors
	 */
	void setFeaturesC( FastQueue<Desc> features );

	/**
	 * Specify descriptors in image C
	 * @param features feature descriptors
	 */
	void setFeaturesB( FastQueue<Desc> features );

	/**
	 * Finds the best match for each item in the source list with an item in the destination list.
	 */
	void associate();

	/**
	 * List of associated features.  Indexes refer to the index inside the input lists.
	 *
	 * @return List of associated features.
	 */
	FastQueue<AssociatedTripleIndex> getMatches();


	/**
	 * Associations are only considered if their score is less than or equal to the specified threshold.  To remove
	 * any threshold test set this value to Double.MAX_VALUE
	 *
	 * @param score The threshold.
	 */
	void setMaxScoreThreshold(double score);

	/**
	 * Specifies the type of score which is returned.
	 *
	 * @return Type of association score.
	 */
	MatchScoreType getScoreType();

	/**
	 * If true then each feature is associated at most one time
	 */
	boolean isEachFeatureAssociatedOnlyOnce();
}
