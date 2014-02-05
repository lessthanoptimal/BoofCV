/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * <p>
 * Common interface for associating features between two images.  Found associations are returned in a list of
 * {@link AssociatedIndex} which specifies the index and score of the matching pair.  Implementing classes can
 * optionally ensure that a unique pairing is found from source to destination and/or the reverse.  See
 * functions {@link #uniqueSource()} and {@link #uniqueDestination()}.  Indexes refer to the index in the input
 * list for source and destination lists.  Inputs are not specified in this interface but are specified in a child
 * interface.
 * </p>
 *
 * <p>
 * DESIGN NOTES:<br>
 * <b>Indexes</b> of matching features are used instead of the descriptions because descriptions are often separated
 * from another more complex data structure and the index can be easily matched to that data.<br>
 * <b>Unassociated feature</b> lists can be easily computed using the returned set of associations.  This functionality
 * is provided since in some cases it can be computed at virtually no cost during association.<br>
 * </p>
 *
 * @author Peter Abeles
 */
public interface Associate {

	/**
	 * Finds the best match for each item in the source list with an item in the destination list.
	 */
	public void associate();

	/**
	 * List of associated features.  Indexes refer to the index inside the input lists.
	 *
	 * @return List of associated features.
	 */
	public FastQueue<AssociatedIndex> getMatches();

	/**
	 * Indexes of features in the source set which are not associated.
	 *
	 * WARNING: In some implementations the unassociated list is recomputed each time this function is invoked.  In
	 * other implementations it was found virtually for free while the matches are found.
	 *
	 * @return List of unassociated source features by index.
	 */
	public GrowQueue_I32 getUnassociatedSource();

	/**
	 * Indexes of features in the destination set which are not associated.
	 *
	 * WARNING: In some implementations the unassociated list is recomputed each time this function is invoked.  In
	 * other implementations it was found virtually for free while the matches are found.
	 *
	 * @return List of unassociated destination features by index.
	 */
	public GrowQueue_I32 getUnassociatedDestination();

	/**
	 * Associations are only considered if their score is less than or equal to the specified threshold.  To remove
	 * any threshold test set this value to Double.MAX_VALUE
	 *
	 * @param score The threshold.
	 */
	public void setThreshold( double score );

	/**
	 * Specifies the type of score which is returned.
	 *
	 * @return Type of association score.
	 */
	public MatchScoreType getScoreType();

	/**
	 * If at most one match is returned for each source feature.
	 *
	 * @return true for unique source association
	 */
	public boolean uniqueSource();

	/**
	 * If at most one match is returned for each destination feature.
	 *
	 * @return true for unique destination association
	 */
	public boolean uniqueDestination();
}
