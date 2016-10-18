/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.associate.AssociateGreedy;
import boofcv.alg.feature.associate.FindUnassociated;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;


/**
 * Wrapper around algorithms contained inside of {@link boofcv.alg.feature.associate.AssociateGreedy}.
 *
 * @author Peter Abeles
 */
public class WrapAssociateGreedy<T> implements AssociateDescription<T> {

	AssociateGreedy<T> alg;

	FastQueue<AssociatedIndex> matches = new FastQueue<>(10, AssociatedIndex.class, true);

	// reference to input list
	FastQueue<T> listSrc;
	FastQueue<T> listDst;

	// indexes of unassociated features
	GrowQueue_I32 unassocSrc = new GrowQueue_I32();
	// creates a list of unassociated features from the list of matches
	FindUnassociated unassociated = new FindUnassociated();

	/**
	 *
	 * @param alg
	 */
	public WrapAssociateGreedy( AssociateGreedy<T> alg ) {
		this.alg = alg;
	}

	@Override
	public void setSource(FastQueue<T> listSrc) {
		this.listSrc = listSrc;
	}

	@Override
	public void setDestination(FastQueue<T> listDst) {
		this.listDst = listDst;
	}

	@Override
	public FastQueue<AssociatedIndex> getMatches() {
		return matches;
	}

	@Override
	public void associate() {
		if( listSrc == null )
			throw new IllegalArgumentException("source features not specified");
		if( listDst == null )
			throw new IllegalArgumentException("destination features not specified");

		unassocSrc.reset();
		alg.associate(listSrc,listDst);

		int pairs[] = alg.getPairs();
		double score[] = alg.getFitQuality();

		matches.reset();
		for( int i = 0; i < listSrc.size; i++ ) {
			int dst = pairs[i];
			if( dst >= 0 )
				matches.grow().setAssociation(i,dst,score[i]);
			else
				unassocSrc.add(i);
		}
	}

	@Override
	public GrowQueue_I32 getUnassociatedSource() {
		return unassocSrc;
	}

	@Override
	public GrowQueue_I32 getUnassociatedDestination() {
		return unassociated.checkDestination(matches,listDst.size);
	}

	@Override
	public void setThreshold(double score) {
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
}
