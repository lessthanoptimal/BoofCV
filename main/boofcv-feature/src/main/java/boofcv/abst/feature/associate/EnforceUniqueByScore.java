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

import boofcv.alg.feature.associate.AssociateUniqueByScoreAlg;
import boofcv.alg.feature.associate.FindUnassociated;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;

/**
 * <p>
 * Ensures that the source and/or destination features are uniquely associated by resolving ambiguity using
 * association score and preferring matches with better scores.
 * </p>
 *
 * <p>
 * NOTE: Unassociated matches are recomputed from scratch. It could potentially add to the list created by
 * the original algorithm for a bit more efficiency.
 * </p>
 *
 * @author Peter Abeles
 * @see AssociateUniqueByScoreAlg
 */
public class EnforceUniqueByScore<Desc, A extends Associate<Desc>> implements Associate<Desc> {

	// algorithm for removing ambiguity
	protected AssociateUniqueByScoreAlg uniqueByScore;
	// associates image features together
	protected A association;

	// creates a list of unassociated features from the list of matches
	protected FindUnassociated unassociated = new FindUnassociated();

	// number of features in source and destination list
	protected int numSource;
	protected int numDestination;

	/**
	 * Configures the algorithm to ensure source and/or destination features are unique. The uniqueness of
	 * the input algorithm is checked and if it is already unique it that processing step will be skipped.
	 *
	 * @param association The association algorithm which is being wrapped
	 * @param checkSource Should source features be unique
	 * @param checkDestination Should destination features be unique
	 */
	public EnforceUniqueByScore( A association, boolean checkSource, boolean checkDestination ) {
		this.association = association;

		// make sure it doesn't perform a redundant check
		checkSource = checkSource && !association.uniqueSource();
		checkDestination = checkDestination && !association.uniqueDestination();

		uniqueByScore = new AssociateUniqueByScoreAlg(association.getScoreType(), checkSource, checkDestination);
	}

	@Override
	public void associate() {
		association.associate();
	}

	@Override
	public FastAccess<AssociatedIndex> getMatches() {
		FastAccess<AssociatedIndex> matches = association.getMatches();

		uniqueByScore.process(matches, numSource, numDestination);

		return uniqueByScore.getMatches();
	}

	@Override
	public DogArray_I32 getUnassociatedSource() {
		return unassociated.checkSource(uniqueByScore.getMatches(), numSource);
	}

	@Override
	public DogArray_I32 getUnassociatedDestination() {
		return unassociated.checkDestination(uniqueByScore.getMatches(), numDestination);
	}

	@Override
	public void setMaxScoreThreshold( double score ) {
		association.setMaxScoreThreshold(score);
	}

	@Override
	public MatchScoreType getScoreType() {
		return association.getScoreType();
	}

	@Override
	public boolean uniqueSource() {
		return association.uniqueSource() || uniqueByScore.checkSource();
	}

	@Override
	public boolean uniqueDestination() {
		return association.uniqueDestination() || uniqueByScore.checkDestination();
	}

	@Override public Class<Desc> getDescriptionType() {
		return association.getDescriptionType();
	}

	/**
	 * Implementation of {@link EnforceUniqueByScore} for {@link AssociateDescription}.
	 *
	 * @param <Desc> Feature description type
	 */
	public static class Describe<Desc> extends EnforceUniqueByScore<Desc, AssociateDescription<Desc>>
			implements AssociateDescription<Desc> {
		public Describe( AssociateDescription<Desc> alg, boolean checkSource, boolean checkDestination ) {
			super(alg, checkSource, checkDestination);
		}

		@Override
		public void setSource( FastAccess<Desc> listSrc ) {
			association.setSource(listSrc);
			numSource = listSrc.size;
		}

		@Override
		public void setDestination( FastAccess<Desc> listDst ) {
			association.setDestination(listDst);
			numDestination = listDst.size;
		}
	}

	/**
	 * Implementation of {@link EnforceUniqueByScore} for {@link AssociateDescription2D}.
	 *
	 * @param <Desc> Feature description type
	 */
	public static class Describe2D<Desc> extends EnforceUniqueByScore<Desc, AssociateDescription2D<Desc>>
			implements AssociateDescription2D<Desc> {
		public Describe2D( AssociateDescription2D<Desc> alg, boolean checkSource, boolean checkDestination ) {
			super(alg, checkSource, checkDestination);
		}

		@Override
		public void initialize( int imageWidth, int imageHeight ) {
			association.initialize(imageWidth, imageHeight);
		}

		@Override
		public void setSource( FastAccess<Point2D_F64> location, FastAccess<Desc> listSrc ) {
			association.setSource(location, listSrc);
			numSource = listSrc.size;
		}

		@Override
		public void setDestination( FastAccess<Point2D_F64> location, FastAccess<Desc> listDst ) {
			association.setDestination(location, listDst);
			numDestination = listDst.size;
		}
	}
}
