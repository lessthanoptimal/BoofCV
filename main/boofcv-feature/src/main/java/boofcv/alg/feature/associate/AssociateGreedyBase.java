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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.ScoreAssociation;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;

/**
 * <p>
 * Performs association by greedily assigning matches to the src list from the dst list if they minimize a score
 * function. Optional additional checks can be done. Backwards Validation sees of the dst feature would also
 * select the src feature as its best match. Ratio Test sees if the second best match has a similar score to the
 * first. If it does it's likely that the best match is ambiguous. Matches can also be rejected if they exceed
 * a maximum fit score limit. In practice, forward-backwards and ratio test pruning are very effective.
 * </p>
 *
 * <p>Internally it compare a score matrix while performing the greedy src to dst assignments. This score matrix
 * is then used to quickly perform a look up when doing forwards-backwards validation.</p>
 *
 * @param <D> Feature description type.
 * @author Peter Abeles
 */
public abstract class AssociateGreedyBase<D> {

	/** computes association score */
	@Getter ScoreAssociation<D> score;
	/** worst allowed fit score to associate */
	@Getter double maxFitError = Double.MAX_VALUE;
	/** Fit score for each assigned pair */
	@Getter DogArray_F64 fitQuality = new DogArray_F64(100);
	/** Look up table with the index of dst features that have been assigned to src features. pairs[src] = dst */
	@Getter DogArray_I32 pairs = new DogArray_I32(100);
	// Score matrix in row-major format. rows = src.size, cols = dst.size
	@Getter DMatrixRMaj scoreMatrix = new DMatrixRMaj(1, 1);
	/**
	 * if true forwards-backwards validation is. For a match to be accepted it must be the best match in both directions
	 */
	@Getter @Setter boolean backwardsValidation = false;
	/**
	 * For a solution to be accepted the second best score must be better than the best score by this ratio.
	 * A value &ge; 1.0 will effective turn this test off
	 */
	@Getter @Setter double ratioTest = 1.0;

	/**
	 * Configure association
	 *
	 * @param score Computes the association score.
	 */
	AssociateGreedyBase( ScoreAssociation<D> score ) {
		this.score = score;
	}

	/**
	 * Clears and allocates memory before association starts.
	 *
	 * @param sizeSrc size of src list
	 * @param sizeDst size of dst list
	 */
	protected void setupForAssociate( int sizeSrc, int sizeDst ) {
		fitQuality.reset();
		pairs.reset();

		pairs.resize(sizeSrc);
		fitQuality.resize(sizeSrc);
		scoreMatrix.reshape(sizeSrc, sizeDst);
	}

	/**
	 * Uses score matrix to validate the assignment of src feature `indexSrc`
	 *
	 * @param indexSrc Index of source feature being validated
	 * @param sizeSrc size of src list
	 * @param sizeDst size of dst list
	 */
	public final void forwardsBackwards( final int indexSrc, final int sizeSrc, final int sizeDst ) {
		// Look up the index that this src feature was matched with
		final int indexDst = pairs.data[indexSrc];
		if (indexDst == -1)
			return;

		double scoreToBeat = scoreMatrix.data[indexSrc*sizeDst + indexDst];
		int indexScore = indexDst;

		// compare the score against all the other possible matches in source
		for (int indexSrcCmp = 0; indexSrcCmp < sizeSrc; indexSrcCmp++, indexScore += sizeDst) {
			if (scoreMatrix.data[indexScore] <= scoreToBeat && indexSrcCmp != indexSrc) {
				pairs.data[indexSrc] = -1;
				fitQuality.data[indexSrc] = Double.MAX_VALUE;
				break;
			}
		}
	}

	public void setMaxFitError( double maxFitError ) {
		if (maxFitError <= 0.0)
			this.maxFitError = Double.MAX_VALUE;
		else
			this.maxFitError = maxFitError;
	}
}
