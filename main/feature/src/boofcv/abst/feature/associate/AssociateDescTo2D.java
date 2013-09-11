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

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Wrapper around {@link AssociateDescription} that allows it to be used inside of {@link AssociateDescription2D}
 *
 * @author Peter Abeles
 */
public class AssociateDescTo2D<D> implements AssociateDescription2D<D> {

	AssociateDescription<D> alg;

	public AssociateDescTo2D(AssociateDescription<D> alg) {
		this.alg = alg;
	}

	@Override
	public void setSource(FastQueue<Point2D_F64> location, FastQueue<D> descriptions) {
		alg.setSource(descriptions);
	}

	@Override
	public void setDestination(FastQueue<Point2D_F64> location, FastQueue<D> descriptions) {
		alg.setDestination(descriptions);
	}

	@Override
	public void associate() {
		alg.associate();
	}

	@Override
	public FastQueue<AssociatedIndex> getMatches() {
		return alg.getMatches();
	}

	@Override
	public GrowQueue_I32 getUnassociatedSource() {
		return alg.getUnassociatedSource();
	}

	@Override
	public GrowQueue_I32 getUnassociatedDestination() {
		return alg.getUnassociatedDestination();
	}

	@Override
	public void setThreshold(double score) {
		alg.setThreshold(score);
	}

	@Override
	public MatchScoreType getScoreType() {
		return alg.getScoreType();
	}

	@Override
	public boolean uniqueSource() {
		return alg.uniqueSource();
	}

	@Override
	public boolean uniqueDestination() {
		return alg.uniqueDestination();
	}
}
