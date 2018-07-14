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

package boofcv.alg.sfm.structure;

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * Graph describing the relationship between image features and 3D points.
 *
 * @author Peter Abeles
 */
public class PairwiseImageGraph {

	public List<CameraView> nodes = new ArrayList<>();
	public List<CameraMotion> edges = new ArrayList<>();
	public List<Feature3D> features3D = new ArrayList<>();

	static class CameraView {
		public String camera;
		public int index;
		public Se3_F64 viewToWorld = new Se3_F64();
		public ViewState state = ViewState.UNPROCESSED;

		public List<PairwiseImageGraph.CameraMotion> connections = new ArrayList<>();

		// feature descriptor of all features in this image
		public FastQueue<TupleDesc> descriptions;
		// observed location of all features in pixels
		public FastQueue<Point2D_F64> observationPixels = new FastQueue<>(Point2D_F64.class, true);
		public FastQueue<Point2D_F64> observationNorm = new FastQueue<>(Point2D_F64.class, true);

		// Estimated 3D location for SOME of the features
		public Feature3D[] features3D;

		public CameraView(int index, FastQueue<TupleDesc> descriptions ) {
			this.index = index;
			this.descriptions = descriptions;
		}
	}

	enum ViewState {
		UNPROCESSED,
		PENDING,
		PROCESSED
	}

	static class CameraMotion {
		// if the transform of both views is known then this will be scaled to be in world units
		// otherwise it's in arbitrary units
		public Se3_F64 a_to_b = new Se3_F64();

		// Which features are associated with each other and in the inlier set
		public List<AssociatedIndex> associated = new ArrayList<>();

		// 3D features triangulated from this motion alone. Features are in reference frame src
		public List<Feature3D> stereoTriangulations = new ArrayList<>();

		public PairwiseImageGraph.CameraView viewSrc;
		public PairwiseImageGraph.CameraView viewDst;

		// Average angle of features in this motion for triangulation
		double triangulationAngle;

		/**
		 * Score how well this motion can be used to provide an initial set of triangulated feature points.
		 * More features the better but you want the epipolar estimate to be a better model than homography
		 * since the epipolar includes translation.
		 * @return the score
		 */
		public double scoreTriangulation() {
			return associated.size()*triangulationAngle;
		}

		public Se3_F64 motionSrcToDst( PairwiseImageGraph.CameraView src ) {
			if( src == viewSrc) {
				return a_to_b.copy();
			} else if( src == viewDst){
				return a_to_b.invert(null);
			} else {
				throw new RuntimeException("BUG!");
			}
		}

		public PairwiseImageGraph.CameraView destination(PairwiseImageGraph.CameraView src ) {
			if( src == viewSrc) {
				return viewDst;
			} else if( src == viewDst){
				return viewSrc;
			} else {
				throw new RuntimeException("BUG!");
			}
		}
	}

	static class Feature3D {
		// estimate 3D position of the feature in world frame
		public Point3D_F64 worldPt = new Point3D_F64();
		// The acute angle between the two orientations it was triangulated from
		public double triangulationAngle;
		// Index of the observation in the corresponding view which the feature is visible in
		public GrowQueue_I32 obsIdx = new GrowQueue_I32();
		// List of views this feature is visible in
		public List<PairwiseImageGraph.CameraView> views = new ArrayList<>();
		public int mark = -1;
	}
}
