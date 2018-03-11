/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.shapes.polyline;

import boofcv.alg.shapes.polyline.RefinePolyLineCorner;
import boofcv.alg.shapes.polyline.splitmerge.PolylineSplitMerge;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class NewSplitMerge_to_PointsToPolyline implements PointsToPolyline {

	// The base algorithm
	PolylineSplitMerge alg;

	// refine corner location
	RefinePolyLineCorner refine;

	public NewSplitMerge_to_PointsToPolyline( ConfigPolylineSplitMerge config ){

		alg = new PolylineSplitMerge();
		alg.setMinimumSideLength(config.minimumSideLength);
		alg.setMaxNumberOfSideSamples(config.maxNumberOfSideSamples);
		alg.setMaxSides(config.maximumSides);
		alg.setMinSides(config.minimumSides);
		alg.setExtraConsider(config.extraConsider);
		alg.setConvex(config.convex);
		alg.setThresholdSideSplitScore(config.thresholdSideSplitScore);
		alg.setCornerScorePenalty(config.cornerScorePenalty);
		alg.setConvexTest(config.convexTest);
		alg.setMaxSideError(config.maxSideError);
		alg.setLoops(config.loops);

		if( config.refineIterations > 0 ) {
			refine = new RefinePolyLineCorner(config.loops,config.refineIterations);
		}
	}

	@Override
	public boolean process(List<Point2D_I32> input, GrowQueue_I32 vertexes) {
		if( !alg.process(input) )
			return false;

		PolylineSplitMerge.CandidatePolyline best = alg.getBestPolyline();

		if( best == null ) {
			return false;
		}

		vertexes.setTo(best.splits);

		if( refine != null && !refine.fit(input,vertexes)) {
			return false;
		}

		return true;
	}

	@Override
	public void setMinimumSides(int minimum) {
		alg.setMinSides(minimum);
	}

	@Override
	public int getMinimumSides() {
		return alg.getMinSides();
	}

	@Override
	public void setMaximumSides(int maximum) {
		alg.setMaxSides(maximum);
	}

	@Override
	public int getMaximumSides() {
		return alg.getMaxSides();
	}

	@Override
	public boolean isLoop() {
		return alg.isLoops();
	}

	@Override
	public void setConvex(boolean convex) {
		alg.setConvex(convex);
	}

	@Override
	public boolean isConvex() {
		return alg.isConvex();
	}
}
