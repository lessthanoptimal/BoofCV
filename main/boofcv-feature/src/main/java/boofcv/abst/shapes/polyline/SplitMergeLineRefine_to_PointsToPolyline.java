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

import boofcv.alg.shapes.polyline.MinimizeEnergyPrune;
import boofcv.alg.shapes.polyline.RefinePolyLineCorner;
import boofcv.alg.shapes.polyline.splitmerge.SplitMergeLineFit;
import boofcv.alg.shapes.polyline.splitmerge.SplitMergeLineFitLoop;
import boofcv.alg.shapes.polyline.splitmerge.SplitMergeLineFitSegment;
import boofcv.factory.shape.ConfigSplitMergeLineFit;
import boofcv.struct.ConfigLength;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Wrapper around {@link SplitMergeLineFit} and other refinement algorithms for {@link PointsToPolyline}.
 *
 * @author Peter Abeles
 */
@Deprecated
public class SplitMergeLineRefine_to_PointsToPolyline implements PointsToPolyline {

	// reject the number of sides found is greater than this amount
	int maxVertexes = Integer.MAX_VALUE;
	int minVertexes = 3;

	// standard split merge algorithm
	SplitMergeLineFit splitMerge;
	// refine corner location
	RefinePolyLineCorner refine;
	// removes extra corners
	private GrowQueue_I32 pruned = new GrowQueue_I32(); // corners after pruning
	private MinimizeEnergyPrune pruner;

	boolean convex = true;

	Polygon2D_F64 tmp = new Polygon2D_F64();

	public SplitMergeLineRefine_to_PointsToPolyline(ConfigSplitMergeLineFit config)
	{
		ConfigLength minimumSplit = config.minimumSide.copy();
		if( config.loop ) {
			splitMerge = new SplitMergeLineFitLoop(config.splitFraction, minimumSplit, config.iterations);
		} else {
			splitMerge = new SplitMergeLineFitSegment(config.splitFraction, minimumSplit, config.iterations);
		}

		if( config.refine > 0 ) {
			refine = new RefinePolyLineCorner(config.loop,config.refine);
		}

		if( config.pruneSplitPenalty > 0 ) {
			pruner = new MinimizeEnergyPrune(config.pruneSplitPenalty);
		}

		convex = config.convex;
		minVertexes = config.minimumSides;
		maxVertexes = config.maximumSides;
	}


	@Override
	public boolean process(List<Point2D_I32> input, GrowQueue_I32 vertexes) {
		if (!splitMerge.process(input, vertexes)) {
			return false;
		}

		if (refine != null && !refine.fit(input, vertexes)) {
			return false;
		}

		if (pruner != null && pruner.prune(input, vertexes, pruned)) {
			vertexes.setTo(pruned);
		}

		if (vertexes.size > maxVertexes || vertexes.size < minVertexes)
			return false;

		tmp.vertexes.resize(vertexes.size);
		for (int i = 0; i < vertexes.size; i++) {
			Point2D_I32 p = input.get(vertexes.get(i));
			tmp.set(i, p.x, p.y);
		}

		return !convex || UtilPolygons2D_F64.isConvex(tmp);
	}

	@Override
	public void setMinimumSides(int minimum) {
		this.minVertexes = minimum;
	}

	@Override
	public int getMinimumSides() {
		return minVertexes;
	}

	@Override
	public void setMaximumSides(int maximum) {
		this.maxVertexes = maximum;
		// detect more than the max. Prune will reduce the number of corners later on
		splitMerge.setAbortSplits(maximum*2);
	}

	@Override
	public int getMaximumSides() {
		return maxVertexes;
	}

	@Override
	public boolean isLoop() {
		return splitMerge instanceof SplitMergeLineFitLoop;
	}

	@Override
	public void setConvex(boolean convex) {
		this.convex = convex;
	}

	@Override
	public boolean isConvex() {
		return convex;
	}
}
