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

package boofcv.abst.feature.associate;

import boofcv.struct.feature.MatchScoreType;

/**
 * Scores the fit quality between two feature descriptions.  A lower score always indicate a better match a larger one.
 * Thus scoreA < scoreB will return true if scoreA is a better score than scoreB.
 * The range of possible scores is not specified by this interface.  For example, correlation based scores can
 * take on both positive and negative values while Euclidean will always be positive or zero.
 *
 * NOTES: To ensure that lower is better, correlation scores undergo a sign flip.
 *
 * @param <Desc> Feature description type.
 *
 * @author Peter Abeles
 */
public interface ScoreAssociation<Desc> {

	/**
	 * Compute the fit score between the two features.  A better fit score will have a lower value.
	 *
	 * @param a first feature
	 * @param b second feature
	 * @return Quality of fit score.  Lower is better.
	 */
	public double score( Desc a , Desc b );

	/**
	 * Specifies the type of score which is returned.
	 *
	 * @return Type of association score.
	 */
	public MatchScoreType getScoreType();
}
