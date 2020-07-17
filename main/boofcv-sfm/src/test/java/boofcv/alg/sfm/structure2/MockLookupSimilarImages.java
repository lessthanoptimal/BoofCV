/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure2;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.util.PrimitiveArrays;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates dummy data for testing algorithms. All views see the same scene and are similar to each other
 *
 * @author Peter Abeles
 */
class MockLookupSimilarImages implements LookupSimilarImages{
	CameraPinhole intrinsic = new CameraPinhole(400,410,0,420,420,800,800);
	int numFeatures = 100;

	Random rand;
	List<String> viewIds = new ArrayList<>();
	List<List<Point2D_F64>> viewObs = new ArrayList<>();
	// look up table from view index to feature index
	List<int[]> viewToFeat = new ArrayList<>();
	List<int[]> featToView = new ArrayList<>();
	public List<Point3D_F64> feats3D;

	public List<Se3_F64> listOriginToView = new ArrayList<>();
	public List<DMatrixRMaj> listCameraMatrices = new ArrayList<>();

	public PairwiseImageGraph2 graph = new PairwiseImageGraph2();

	public MockLookupSimilarImages( int numViews , long seed) {
		this.rand = new Random(seed);

		for (int i = 0; i < numViews; i++) {
			viewIds.add("View "+i);
		}

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(intrinsic,(DMatrixRMaj)null);

		// 3D location of points in view 0 reference frame
		feats3D = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 1), -0.5, 0.5, numFeatures, rand);

		// render pixel coordinates of all points
		for (int i = 0; i < numViews; i++) {
			Se3_F64 view0_to_viewi = SpecialEuclideanOps_F64.eulerXyz(
					0.01 + 0.1*i,0,rand.nextGaussian()*0.03,
					rand.nextGaussian()*0.03,rand.nextGaussian()*0.03,rand.nextGaussian()*0.1,null);

			// first view is the origin
			if( i == 0 )
				view0_to_viewi.reset();

			// Create the camera matrix P
			DMatrixRMaj P = PerspectiveOps.createCameraMatrix(view0_to_viewi.R, view0_to_viewi.T,K,null);

			// save information on the view
			listOriginToView.add(view0_to_viewi);
			listCameraMatrices.add(P);

			// Observed features in the view
			List<Point2D_F64> viewPixels = new ArrayList<>();
			viewObs.add(viewPixels);

			// create look up table from view to feature
			// we don't want features to have same index because that's not realistic and would hide bugs
			int[] v2f = PrimitiveArrays.fillCounting(numFeatures);
			if( i > 0 )
				PrimitiveArrays.shuffle(v2f,0, numFeatures,rand);
			viewToFeat.add(v2f);

			// save reverse table for fast lookup later
			int[] f2v = new int[numFeatures];
			for (int j = 0; j < numFeatures; j++) {
				f2v[v2f[j]] = j;
			}
			featToView.add(f2v);

			// note the featIdx is the index of the feature in the view
			for (int viewIdx = 0; viewIdx < feats3D.size(); viewIdx++) {
				Point3D_F64 X = feats3D.get(v2f[viewIdx]);
				viewPixels.add(PerspectiveOps.renderPixel(view0_to_viewi,intrinsic,X, null));
			}
		}

		constructGraph(numViews);
	}

	private void constructGraph(int numViews) {
		for (int i = 0; i < numViews; i++) {
			PairwiseImageGraph2.View v = graph.createNode(viewIds.get(i));
			v.totalObservations = numFeatures;
		}
		// connect all views to each other
		for (int i = 0; i < numViews; i++) {
			String nameI = viewIds.get(i);
			for (int j = i+1; j < numViews; j++) {
				String nameJ = viewIds.get(j);

				PairwiseImageGraph2.Motion edge = new PairwiseImageGraph2.Motion();
				edge.is3D = true;
				// swap src and dst to exercise more edge cases
				if( j%2 == 0 ) {
					edge.src = graph.mapNodes.get(nameI);
					edge.dst = graph.mapNodes.get(nameJ);
				} else {
					edge.src = graph.mapNodes.get(nameJ);
					edge.dst = graph.mapNodes.get(nameI);
				}
				edge.countH = 10;
				edge.countF = numFeatures;

				int[] tableI = featToView.get(i);
				int[] tableJ = featToView.get(j);

				for (int k = 0; k < numFeatures; k++) {
					if( j%2 == 0 ) {
						edge.inliers.grow().setAssociation(tableI[k], tableJ[k], 0.0);
					} else {
						edge.inliers.grow().setAssociation(tableJ[k], tableI[k], 0.0);
					}
				}

				edge.src.connections.add(edge);
				edge.dst.connections.add(edge);

			}
		}
	}

	@Override
	public List<String> getImageIDs() {
		return viewIds;
	}

	@Override
	public void findSimilar(String target, List<String> similar) {
		similar.clear();
		for (int i = 0; i < viewIds.size(); i++) {
			if( !viewIds.get(i).equals(target) ) {
				similar.add(viewIds.get(i));
			}
		}
	}

	@Override
	public void lookupPixelFeats(String target, FastQueue<Point2D_F64> features) {
		int index = viewIds.indexOf(target);
		List<Point2D_F64> l = viewObs.get(index);
		features.reset();
		for (int i = 0; i < l.size(); i++) {
			features.grow().set(l.get(i));
		}
	}

	@Override
	public boolean lookupMatches(String viewA, String viewB, FastQueue<AssociatedIndex> pairs)
	{
		int[] tableA = featToView.get(indexOfView(viewA));
		int[] tableB = featToView.get(indexOfView(viewB));

		pairs.reset();
		for (int i = 0; i < numFeatures; i++) {
			pairs.grow().setAssociation(tableA[i],tableB[i],0);
		}

		return true;
	}

	@Override
	public void lookupShape(String target, ImageDimension shape) {
		shape.set(intrinsic.width,intrinsic.height);
	}

	public int indexOfView( String name ) {
		for (int i = 0; i < viewIds.size(); i++) {
			if( name.equals(viewIds.get(i)))
				return i;
		}
		return -1;
	}
}