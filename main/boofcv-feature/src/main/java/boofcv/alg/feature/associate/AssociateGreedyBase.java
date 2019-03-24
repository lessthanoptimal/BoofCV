/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * <p>
 * Brute force greedy association for objects described by a {@link TupleDesc_F64}.  An
 * object is associated with whichever object has the best fit score and every possible combination
 * is examined.  If there are a large number of features this can be quite slow.
 * </p>
 *
 * <p>
 * Optionally, backwards validation can be used to reduce the number of false associations.
 * Backwards validation works by checking to see if two objects are mutually the best association
 * for each other.  First an association is found from src to dst, then the best fit in dst is
 * associated with feature in src.
 * </p>
 *
 * @param <D> Feature description type.
 *
 * @author Peter Abeles
 */
public abstract class AssociateGreedyBase<D> {

	// computes association score
	ScoreAssociation<D> score;
	// worst allowed fit score to associate
	double maxFitError = Double.MAX_VALUE;
	// stores the quality of fit score
	GrowQueue_F64 fitQuality = new GrowQueue_F64(100);
	// stores indexes of associated
	GrowQueue_I32 pairs = new GrowQueue_I32(100);
	// various
	GrowQueue_F64 workBuffer = new GrowQueue_F64(100);
	// if true backwardsValidation is done
	boolean backwardsValidation;

	/**
	 * Configure association
	 *
	 * @param score Computes the association score.
	 * @param backwardsValidation If true then backwards validation is performed.
	 */
	AssociateGreedyBase(ScoreAssociation<D> score,
							   boolean backwardsValidation) {
		this.score = score;
		this.backwardsValidation = backwardsValidation;
	}

	/**
	 * Associates the two sets objects against each other by minimizing fit score.
	 *
	 * @param src Source list.
	 * @param dst Destination list.
	 */
	public abstract void associate( FastQueue<D> src , FastQueue<D> dst );

	/**
	 * Returns a list of association pairs.  Each element in the returned list corresponds
	 * to an element in the src list.  The value contained in the index indicate which element
	 * in the dst list that object was associated with.  If a value of -1 is stored then
	 * no association was found.
	 *
	 * @return Array containing associations by src index.
	 */
	public int[] getPairs() {
		return pairs.data;
	}

	/**
	 * Quality of fit scores for each association.  Lower fit scores are better.
	 *
	 * @return Array of fit sources by src index.
	 */
	public double[] getFitQuality() {
		return fitQuality.data;
	}

	public void setMaxFitError(double maxFitError) {
		this.maxFitError = maxFitError;
	}

	public ScoreAssociation<D> getScore() {
		return score;
	}

	public boolean isBackwardsValidation() {
		return backwardsValidation;
	}
}
