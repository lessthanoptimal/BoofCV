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

package boofcv.alg.shapes.polyline.keypoint;

import boofcv.alg.shapes.polyline.splitmerge.SplitMergeLineFit;
import boofcv.alg.shapes.polyline.splitmerge.SplitMergeLineFitLoop;
import boofcv.alg.shapes.polyline.splitmerge.SplitMergeLineFitSegment;
import boofcv.struct.ConfigLength;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses contour interest points as a seed to fit a polygon to the contour.
 *
 * @author Peter Abeles
 */
public class ContourInterestThenSplitMerge {

	ContourInterestPointDetector detector;
	SplitMergeLineFit splitMerge;

	List<Point2D_I32> points = new ArrayList<>();
	GrowQueue_I32 pointsVertexes = new GrowQueue_I32();

	public ContourInterestThenSplitMerge(boolean loop, ConfigLength period, double threshold,
										 double splitFraction , int maxIterations) {
		detector = new ContourInterestPointDetector(loop, period, threshold);
		if( loop ) {
			splitMerge = new SplitMergeLineFitLoop(splitFraction,period,maxIterations);
		} else {
			splitMerge = new SplitMergeLineFitSegment(splitFraction,period,maxIterations);
		}
	}

	public boolean process(List<Point2D_I32> contour , GrowQueue_I32 vertexes ) {

		vertexes.reset();

		// detect features in the contour
		detector.process(contour);
		detector.getInterestPoints(contour,points);

		// apply split merget o the contour
		if( !splitMerge.process(points, pointsVertexes) ) {
			return false;
		}

		// translate indexes back into indexes in the contour list
		vertexes.resize(pointsVertexes.size);
		GrowQueue_I32 detectorVertexes = detector.indexes;

		for (int i = 0; i < pointsVertexes.size; i++) {
			int a = detectorVertexes.get(pointsVertexes.get(i));
			vertexes.set(i,a);
		}

		return true;
	}

}
