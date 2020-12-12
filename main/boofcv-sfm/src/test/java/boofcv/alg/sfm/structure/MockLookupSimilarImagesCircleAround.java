/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.BoofTesting;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.sfm.structure.PairwiseImageGraph.View;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastArray;
import org.ddogleg.util.PrimitiveArrays;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Creates a scene were all views are pointing towards the center and the camera travels around in a circle
 *
 * @author Peter Abeles
 */
class MockLookupSimilarImagesCircleAround implements LookUpSimilarImages {
	public CameraPinhole intrinsic = new CameraPinhole(400, 410, 0, 420, 420, 800, 800);
	public int numFeatures = 100;
	public Random rand = BoofTesting.createRandom(3);
	List<String> viewIds = new ArrayList<>();
	List<List<Point2D_F64>> viewObs = new ArrayList<>();
	// look up table from view index to feature index
	List<int[]> viewToFeat = new ArrayList<>();
	List<int[]> featToView = new ArrayList<>();
	public List<Point3D_F64> feats3D;

	public FastArray<Se3_F64> listOriginToView = new FastArray<>(Se3_F64.class);
	public FastArray<DMatrixRMaj> listCameraMatrices = new FastArray<>(DMatrixRMaj.class);
	// camera matrices with the (cx,cy) = (0,0)
	public FastArray<DMatrixRMaj> listCameraMatricesZeroPrinciple = new FastArray<>(DMatrixRMaj.class);

	public PairwiseImageGraph graph = new PairwiseImageGraph();

	public MockLookupSimilarImagesCircleAround() {}

	public MockLookupSimilarImagesCircleAround setIntrinsic( CameraPinhole intrinsic ) {
		this.intrinsic = intrinsic;
		return this;
	}

	public MockLookupSimilarImagesCircleAround setFeatures( int numFeatures ) {
		this.numFeatures = numFeatures;
		return this;
	}

	public MockLookupSimilarImagesCircleAround setSeed( long seed ) {
		rand = BoofTesting.createRandom(seed);
		return this;
	}

	/**
	 * Configures the scene
	 *
	 * @param numViews number of views to create
	 * @param numViewConnect Specifies 1/2 the number of views each view will be connected to.
	 */
	public MockLookupSimilarImagesCircleAround init( int numViews, int numViewConnect ) {
		for (int viewCnt = 0; viewCnt < numViews; viewCnt++) {
			viewIds.add("View_" + viewCnt);
		}

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(intrinsic, (DMatrixRMaj)null);
		DMatrixRMaj K_zero = K.copy();
		K_zero.set(0, 2, 0.0);
		K_zero.set(1, 2, 0.0);

		// Randomly add points around the coordinate system's origin
		feats3D = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 0), -0.5, 0.5, numFeatures, rand);

		// Radius of the cameras circling the origin
		double pathRadius = 2;

		// render pixel coordinates of all points
		for (int viewCnt = 0; viewCnt < numViews; viewCnt++) {
			Se3_F64 camera_to_world = new Se3_F64();
			Se3_F64 world_to_camera = new Se3_F64();
			double yaw = 2.0*Math.PI*viewCnt/numViews;

			// camera lie on the (X,Z) plane with +y pointed down.
			// This is done to make the camera coordinate system and the world coordinate system have a more close
			// relationship
			camera_to_world.T.x = Math.cos(yaw)*pathRadius;
			camera_to_world.T.y = rand.nextGaussian()*pathRadius*0.1; // geometric diversity for self calibration
			camera_to_world.T.z = Math.sin(yaw)*pathRadius;

			// camera is pointing in the opposite direction of it's world location
			ConvertRotation3D_F64.rodriguesToMatrix(new Rodrigues_F64(yaw + Math.PI/2, 0, -1, 0), camera_to_world.R);
			camera_to_world.invert(world_to_camera);

			// Create the camera matrix P
			DMatrixRMaj P = PerspectiveOps.createCameraMatrix(world_to_camera.R, world_to_camera.T, K, null);
			DMatrixRMaj P_zero = PerspectiveOps.createCameraMatrix(world_to_camera.R, world_to_camera.T, K_zero, null);

			// save information on the view
			listOriginToView.add(world_to_camera);
			listCameraMatrices.add(P);
			listCameraMatricesZeroPrinciple.add(P_zero);

			// Observed features in the view
			List<Point2D_F64> viewPixels = new ArrayList<>();
			viewObs.add(viewPixels);

			// create look up table from view to feature
			// we don't want features to have same index because that's not realistic and would hide bugs
			int[] v2f = PrimitiveArrays.fillCounting(numFeatures);
			PrimitiveArrays.shuffle(v2f, 0, numFeatures, rand);
			viewToFeat.add(v2f);

			// save reverse table for fast lookup later
			int[] f2v = new int[numFeatures];
			for (int j = 0; j < numFeatures; j++) {
				f2v[v2f[j]] = j;
			}
			featToView.add(f2v);

			// note the featIdx is the index of the feature in the view
			for (int featCnt = 0; featCnt < feats3D.size(); featCnt++) {
				Point3D_F64 X = feats3D.get(v2f[featCnt]);
				Point2D_F64 pixel = PerspectiveOps.renderPixel(world_to_camera, intrinsic, X, null);
				if (pixel == null)
					throw new RuntimeException("Out of FOV");
				viewPixels.add(pixel);
			}
		}

		// Create the pairwise graph
		for (int i = 0; i < numViews; i++) {
			View v = graph.createNode(viewIds.get(i));
			v.totalObservations = numFeatures;
		}

		// Only connect neighbors to each other
		for (int viewIdxI = 0; viewIdxI < numViews; viewIdxI++) {
			View vi = graph.nodes.get(viewIdxI);
			for (int neighborOffset = 1; neighborOffset <= numViewConnect; neighborOffset++) {
				int viewIdxJ = viewIdxI + neighborOffset;
				if (viewIdxJ >= numViews)
					break;
				// next view while wrapping around
				View vj = graph.nodes.get(viewIdxJ);
				// mix of the src/dst to exercise more code during testing
				boolean standardOrder = (viewIdxI + neighborOffset)%2 == 0;
				PairwiseImageGraph.Motion m = standardOrder ? graph.connect(vi, vj) : graph.connect(vj, vi);
				m.countH = numFeatures*5/7;
				m.countF = numFeatures;
				m.is3D = true;

				int[] tableI = featToView.get(viewIdxI);
				int[] tableJ = featToView.get(viewIdxJ);

				for (int i = 0; i < numFeatures; i++) {
					if (standardOrder) {
						m.inliers.grow().setAssociation(tableI[i], tableJ[i], 0.0);
					} else {
						m.inliers.grow().setAssociation(tableJ[i], tableI[i], 0.0);
					}
				}
			}
		}

		return this;
	}

	public SceneWorkingGraph createWorkingGraph() {
		return createWorkingGraph(false);
	}

	public SceneWorkingGraph createWorkingGraph( boolean zeroCP ) {
		var working = new SceneWorkingGraph();
		for (int viewCnt = 0; viewCnt < graph.nodes.size; viewCnt++) {
			PairwiseImageGraph.View pv = graph.nodes.get(viewCnt);
			SceneWorkingGraph.View wv = working.addView(pv);
			if (zeroCP) {
				wv.projective.set(listCameraMatricesZeroPrinciple.get(viewCnt));
			} else {
				wv.projective.set(listCameraMatrices.get(viewCnt));
			}

			wv.inliers.views.add(pv);
			wv.inliers.observations.grow().addAll(featToView.get(viewCnt), 0, numFeatures);
			for (int connCnt = 0; connCnt < pv.connections.size; connCnt++) {
				PairwiseImageGraph.Motion m = pv.connections.get(connCnt);
				PairwiseImageGraph.View mv = m.other(pv);
				int mv_index = graph.nodes.indexOf(mv);
				wv.inliers.views.add(mv);
				wv.inliers.observations.grow().addAll(featToView.get(mv_index), 0, numFeatures);
			}
		}
		return working;
	}

	@Override
	public List<String> getImageIDs() {
		return viewIds;
	}

	@Override
	public void findSimilar( String target, List<String> similar ) {
		similar.clear();
		for (int i = 0; i < viewIds.size(); i++) {
			if (!viewIds.get(i).equals(target)) {
				similar.add(viewIds.get(i));
			}
		}
	}

	@Override
	public void lookupPixelFeats( String target, DogArray<Point2D_F64> features ) {
		int index = viewIds.indexOf(target);
		List<Point2D_F64> l = viewObs.get(index);
		features.reset();
		for (int i = 0; i < l.size(); i++) {
			if (l.get(i) == null)
				throw new RuntimeException("WTF");
			features.grow().setTo(l.get(i));
		}
	}

	@Override
	public boolean lookupMatches( String viewA, String viewB, DogArray<AssociatedIndex> pairs ) {
		int[] tableA = featToView.get(indexOfView(viewA));
		int[] tableB = featToView.get(indexOfView(viewB));

		pairs.reset();
		for (int i = 0; i < numFeatures; i++) {
			pairs.grow().setAssociation(tableA[i], tableB[i], 0);
		}

		return true;
	}

	@Override
	public void lookupShape( String target, ImageDimension shape ) {
		shape.setTo(intrinsic.width, intrinsic.height);
	}

	public int indexOfView( String name ) {
		for (int i = 0; i < viewIds.size(); i++) {
			if (name.equals(viewIds.get(i)))
				return i;
		}
		return -1;
	}
}
