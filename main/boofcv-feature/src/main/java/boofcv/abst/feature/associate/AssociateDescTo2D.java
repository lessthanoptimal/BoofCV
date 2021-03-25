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

import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;

/**
 * Wrapper around {@link AssociateDescription} that allows it to be used inside of {@link AssociateDescription2D}
 *
 * @author Peter Abeles
 */
public class AssociateDescTo2D<D> implements AssociateDescription2D<D> {

	AssociateDescription<D> alg;
	// sanity check to detect bad usage of this interface. Not needed for this implementation
	boolean calledInitialize = false;

	public AssociateDescTo2D( AssociateDescription<D> alg ) {this.alg = alg;}

	@Override public void initialize( int imageWidth, int imageHeight ) {
		calledInitialize = true;
	}

	@Override public void setSource( FastAccess<Point2D_F64> location, FastAccess<D> descriptions ) {
		alg.setSource(descriptions);
	}

	@Override public void setDestination( FastAccess<Point2D_F64> location, FastAccess<D> descriptions ) {
		alg.setDestination(descriptions);
	}

	@Override public void associate() {
		BoofMiscOps.checkTrue(calledInitialize);
		alg.associate();
	}

	// @formatter:off
	@Override public FastAccess<AssociatedIndex> getMatches() {return alg.getMatches();}
	@Override public DogArray_I32 getUnassociatedSource() {return alg.getUnassociatedSource();}
	@Override public DogArray_I32 getUnassociatedDestination() {return alg.getUnassociatedDestination();}
	@Override public void setMaxScoreThreshold( double score ) {alg.setMaxScoreThreshold(score);}
	@Override public MatchScoreType getScoreType() {return alg.getScoreType();}
	@Override public boolean uniqueSource() {return alg.uniqueSource();}
	@Override public boolean uniqueDestination() {return alg.uniqueDestination();}
	@Override public Class<D> getDescriptionType() {return alg.getDescriptionType();}
	// @formatter:on
}
