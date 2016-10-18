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

package boofcv.alg.geo.robust;

import boofcv.struct.geo.AssociatedPair;
import georegression.fitting.MotionTransformPoint;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import org.ddogleg.fitting.modelset.ModelGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses {@link MotionTransformPoint} to estimate the rigid body motion in 2D between two sets of points
 *
 * @author Peter Abeles
 */
public class GenerateSe2_AssociatedPair implements
		ModelGenerator<Se2_F64,AssociatedPair>
{
	MotionTransformPoint<Se2_F64, Point2D_F64> estimate;

	List<Point2D_F64> from = new ArrayList<>();
	List<Point2D_F64> to = new ArrayList<>();

	public GenerateSe2_AssociatedPair(MotionTransformPoint<Se2_F64, Point2D_F64> estimate) {
		this.estimate = estimate;
	}

	@Override
	public boolean generate(List<AssociatedPair> dataSet, Se2_F64 output) {
		from.clear();
		to.clear();

		for( int i = 0; i < dataSet.size(); i++ ) {
			AssociatedPair p = dataSet.get(i);

			from.add(p.getP1());
			to.add(p.getP2());
		}

		if( !estimate.process(from,to) )
			return false;

		output.set(estimate.getTransformSrcToDst());
		return true;
	}

	@Override
	public int getMinimumPoints() {
		return estimate.getMinimumPoints();
	}
}
