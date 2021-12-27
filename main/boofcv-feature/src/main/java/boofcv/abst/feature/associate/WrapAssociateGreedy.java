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

package boofcv.abst.feature.associate;

import boofcv.alg.feature.associate.AssociateGreedyDesc;
import boofcv.alg.feature.associate.AssociateGreedyDescBase;
import boofcv.alg.feature.associate.FindUnassociated;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;

/**
 * Wrapper around algorithms contained inside of {@link AssociateGreedyDesc}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class WrapAssociateGreedy<T> implements AssociateDescription<T> {

	AssociateGreedyDescBase<T> alg;

	DogArray<AssociatedIndex> matches = new DogArray<>(10, AssociatedIndex::new);

	// reference to input list
	FastAccess<T> listSrc;
	FastAccess<T> listDst;

	// indexes of unassociated features
	DogArray_I32 unassocSrc = new DogArray_I32();
	// creates a list of unassociated features from the list of matches
	FindUnassociated unassociated = new FindUnassociated();

	public WrapAssociateGreedy( AssociateGreedyDescBase<T> alg ) {
		this.alg = alg;
	}

	@Override
	public void setSource( FastAccess<T> listSrc ) {
		this.listSrc = listSrc;
	}

	@Override
	public void setDestination( FastAccess<T> listDst ) {
		this.listDst = listDst;
	}

	@Override
	public DogArray<AssociatedIndex> getMatches() {
		return matches;
	}

	@Override
	public void associate() {
		if (listSrc == null)
			throw new IllegalArgumentException("source features not specified");
		if (listDst == null)
			throw new IllegalArgumentException("destination features not specified");

		unassocSrc.reset();
		alg.associate(listSrc, listDst);

		DogArray_I32 pairs = alg.getPairs();
		DogArray_F64 score = alg.getFitQuality();

		matches.reset();
		for (int i = 0; i < listSrc.size; i++) {
			int dst = pairs.data[i];
			if (dst >= 0)
				matches.grow().setTo(i, dst, score.data[i]);
			else
				unassocSrc.add(i);
		}
	}

	@Override
	public DogArray_I32 getUnassociatedSource() {
		return unassocSrc;
	}

	@Override
	public DogArray_I32 getUnassociatedDestination() {
		return unassociated.checkDestination(matches, listDst.size);
	}

	@Override
	public void setMaxScoreThreshold( double score ) {
		alg.setMaxFitError(score);
	}

	@Override
	public MatchScoreType getScoreType() {
		return alg.getScore().getScoreType();
	}

	@Override
	public boolean uniqueSource() {
		return true;
	}

	@Override
	public boolean uniqueDestination() {
		return alg.isBackwardsValidation();
	}

	@Override public Class<T> getDescriptionType() {
		return alg.getScore().getDescriptorType();
	}
}
