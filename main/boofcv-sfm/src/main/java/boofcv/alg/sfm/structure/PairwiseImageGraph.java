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

	public List<CameraView> graphNodes = new ArrayList<>();
	public List<CameraMotion> graphEdges = new ArrayList<>();
	public List<Feature3D> features3D = new ArrayList<>();

	static class CameraView {
		String camera;
		int index;
		Se3_F64 viewToWorld = new Se3_F64();
		EstimateSceneUnordered.ViewState state = EstimateSceneUnordered.ViewState.UNPROCESSED;

		List<EstimateSceneUnordered.CameraMotion> connections = new ArrayList<>();

		// feature descriptor of all features in this image
		FastQueue<TupleDesc> descriptions;
		// observed location of all features in pixels
		FastQueue<Point2D_F64> observationPixels = new FastQueue<>(Point2D_F64.class, true);
		FastQueue<Point2D_F64> observationNorm = new FastQueue<>(Point2D_F64.class, true);

		// Estimated 3D location for SOME of the features
		EstimateSceneUnordered.Feature3D[] features3D;

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
		Se3_F64 a_to_b = new Se3_F64();

		// index
		List<AssociatedIndex> features = new ArrayList<>();

		EstimateSceneUnordered.CameraView viewSrc;
		EstimateSceneUnordered.CameraView viewDst;

		// Average angle of features in this motion for triangulation
		double triangulationAngle;

		/**
		 * Score how well this motion can be used to provide an initial set of triangulated feature points.
		 * More features the better but you want the epipolar estimate to be a better model than homography
		 * since the epipolar includes translation.
		 * @return the score
		 */
		public double scoreTriangulation() {
			return features.size()*triangulationAngle;
		}

		public Se3_F64 motionSrcToDst( EstimateSceneUnordered.CameraView src ) {
			if( src == viewSrc) {
				return a_to_b.copy();
			} else if( src == viewDst){
				return a_to_b.invert(null);
			} else {
				throw new RuntimeException("BUG!");
			}
		}

		public EstimateSceneUnordered.CameraView destination(EstimateSceneUnordered.CameraView src ) {
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
		Point3D_F64 worldPt = new Point3D_F64();
		// Index of the obsrevation in the corresponding view which the feature is visible in
		GrowQueue_I32 obsIdx = new GrowQueue_I32();
		// List of views this feature is visible in
		List<EstimateSceneUnordered.CameraView> views = new ArrayList<>();
		int mark = -1;
	}
}
