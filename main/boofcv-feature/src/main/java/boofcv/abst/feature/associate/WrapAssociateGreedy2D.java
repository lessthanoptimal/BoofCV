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

import boofcv.alg.feature.associate.AssociateGreedyBase2D;
import boofcv.alg.feature.associate.FindUnassociated;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;

/**
 * Wrapper around of {@link AssociateGreedyBase2D} for {@link AssociateDescription2D}
 *
 * @author Peter Abeles
 */
public class WrapAssociateGreedy2D<D> implements AssociateDescription2D<D> {
	// The algorithm being wrapped
	AssociateGreedyBase2D<D> alg;

	// Found matches
	DogArray<AssociatedIndex> matches = new DogArray<>(AssociatedIndex::new);

	// indexes of unassociated features
	DogArray_I32 unassocSrc = new DogArray_I32();
	// creates a list of unassociated features from the list of matches
	FindUnassociated unassociated = new FindUnassociated();

	// Number of features in the destination list
	private int sizeDst;

	public WrapAssociateGreedy2D( AssociateGreedyBase2D<D> alg ) {this.alg = alg;}

	@Override
	public void initialize( int imageWidth, int imageHeight ) {
		alg.init(imageWidth, imageHeight);
	}

	@Override
	public void setSource( FastAccess<Point2D_F64> location, FastAccess<D> descriptions ) {
		alg.setSource(location, descriptions);
	}

	@Override
	public void setDestination( FastAccess<Point2D_F64> location, FastAccess<D> descriptions ) {
		alg.setDestination(location, descriptions);
		sizeDst = location.size;
	}

	@Override
	public void associate() {
		unassocSrc.reset();
		alg.associate();

		DogArray_I32 pairs = alg.getPairs();
		DogArray_F64 score = alg.getFitQuality();

		matches.reset();
		for (int i = 0; i < pairs.size; i++) {
			int dst = pairs.data[i];
			if (dst >= 0) matches.grow().setTo(i, dst, score.data[i]);
			else unassocSrc.add(i);
		}
	}

	// @formatter:off
	@Override public DogArray<AssociatedIndex> getMatches() {return matches;}
	@Override public DogArray_I32 getUnassociatedSource() {return unassocSrc;}
	@Override public DogArray_I32 getUnassociatedDestination() {return unassociated.checkDestination(matches, sizeDst);}
	@Override public void setMaxScoreThreshold( double score ) {alg.setMaxFitError(score);}
	@Override public MatchScoreType getScoreType() {return alg.getScore().getScoreType();}
	@Override public boolean uniqueSource() {return true;}
	@Override public boolean uniqueDestination() {return alg.isBackwardsValidation();}
	@Override public Class<D> getDescriptionType() {return alg.getScore().getDescriptorType();}
	// @formatter:on
}
