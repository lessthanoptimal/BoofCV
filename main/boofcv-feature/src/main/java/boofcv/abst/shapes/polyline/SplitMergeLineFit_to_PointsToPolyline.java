/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.shapes.polyline.SplitMergeLineFitLoop;
import boofcv.alg.shapes.polyline.SplitMergeLineFitSegment;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Wrapper around {@link boofcv.alg.shapes.polyline.SplitMergeLineFit} for {@link PointsToPolyline}.
 *
 * @author Peter Abeles
 */
public class SplitMergeLineFit_to_PointsToPolyline implements PointsToPolyline {

	SplitMergeLineFitLoop algLoop;
	SplitMergeLineFitSegment algSegments;

	public SplitMergeLineFit_to_PointsToPolyline(double splitFraction,
												 double minimumSplitFraction,
												 int maxIterations)
	{
		algLoop = new SplitMergeLineFitLoop(splitFraction, minimumSplitFraction, maxIterations);
		algSegments = new SplitMergeLineFitSegment(splitFraction, minimumSplitFraction, maxIterations);
	}


	@Override
	public boolean process(List<Point2D_I32> input, boolean loop, GrowQueue_I32 vertexes) {
		if( loop )
			return algLoop.process(input,vertexes);
		else
			return algSegments.process(input,vertexes);
	}

	@Override
	public void setMaxVertexes(int maximum) {
		algLoop.setAbortSplits(maximum);
		algSegments.setAbortSplits(maximum);
	}

	@Override
	public int getMaxVertexes() {
		return algLoop.getAbortSplits();
	}
}
