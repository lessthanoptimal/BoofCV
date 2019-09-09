/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.pose.CompatibleProjectiveHomography;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the projective camera for all 3D neighbors of the seed.
 * For node 'B' with a 3D connection to the seed and an unknown camera it:
 * <ol>
 *     <li>Finds another node 'C' which is connected to the seed and has a known camera</li>
 *     <li>Find all features in common with seed, B, C. Splits them into known and unknown sets</li>
 *     <li>Estimate the camera for B from known features</li>
 *     <li>Triangulates using all 3 views the unknown features</li>
 *     <li>Runs projective bundle adjustment</li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class ProjectiveExpandToNeighbors {

//	List<PairwiseImageGraph2.View> seedConnections = new ArrayList<>();
	ProjectiveInitializeAllCommon computeStructure;

	CompatibleProjectiveHomography findCompatible;

	SceneWorkingGraph workGraph;

//	GrowQueue_B knownFeature = new GrowQueue_B();

	public void process( LookupSimilarImages db ,
						 PairwiseImageGraph2.View seed ,
						 SceneWorkingGraph workGraph ,
						 SceneStructureProjective structure )
	{
		this.workGraph = workGraph;
		// create a list of initial views that are connected to the seed
//		seed.getConnections(connIdx.data,connIdx.size,seedConnections);

		SceneWorkingGraph.View wseed = workGraph.lookupView(seed.id);

		// Go through all views connected to the seed and see if they can be added
		for (int motionIdxB = 0; motionIdxB < seed.connections.size; motionIdxB++) {
			PairwiseImageGraph2.Motion motionB = seed.connections.get(motionIdxB);

			// Only process nodes which are 3D
			if( !motionB.is3D )
				continue;

			// Look up the view in the pairwise graph
			PairwiseImageGraph2.View viewB = motionB.other(seed);

			// Skip if already in the current scene's structure
			if( workGraph.isKnown(viewB) )
				continue;

			// TODO Find the two other views in which it has the best geometry and feature set

			// Select the connection to a third view
			PairwiseImageGraph2.Motion conn_B_to_C = selectThirdView(viewB, seed);
			if( conn_B_to_C == null )
				continue;

			PairwiseImageGraph2.View viewC = conn_B_to_C.other(viewB);
			int motionIdxC = seed.findMotionIdx(viewC);
			if( motionIdxC < 0 )
				throw new RuntimeException("BUG!");

			// Estimate projective from scratch using these three views. The alternative it to use known
			// feature locations. This approach won't propagate errors in 3D points and can work off of more
			// points. This also won't be affected by planes, which are a common degenerate geometry in projective space
			if( !computeStructure.projectiveCameras3(db,seed,motionIdxC,motionIdxB) ) {
				continue;
			}

			SceneWorkingGraph.View wviewC = workGraph.lookupView(viewC.id);

			// The found camera matrices will not be in the same projective frame
			List<DMatrixRMaj> cameras1 = new ArrayList<>();
			List<DMatrixRMaj> cameras2 = new ArrayList<>();
			DMatrixRMaj H = new DMatrixRMaj(4,4);
			DMatrixRMaj H_inv = new DMatrixRMaj(4,4);

			cameras1.add(computeStructure.P1); // P1 = eye(3,4)
			cameras1.add(wviewC.camera);
			cameras2.add(computeStructure.P1);
			cameras2.add(computeStructure.P2);

			findCompatible.fitCameras(cameras1,cameras2,H);
			CommonOps_DDRM.invert(H,H_inv);

			// Add the view and its features to the scene
			SceneWorkingGraph.View wviewB = workGraph.addViewAndFeatures(viewB);

			// Set the camera matrix
			// P2 = P2*inv(H)
			CommonOps_DDRM.mult(computeStructure.P2,H_inv,wviewB.camera);

			// Triangulate location of points using all 3 views.
			// 1) Triangulate in the local projective frame
			// 2) Convert to the global projective frame
			// 3) Assign to a global point
			cameras1.add(wviewB.camera);
			int N = computeStructure.ransac.getMatchSet().size();
			for (int i = 0; i < N; i++) {
				//
			}


			// TODO Run bundle adjustment
		}
	}

	void addCameraMatrix( int viewIdx , GrowQueue_I32 featsIdx) {
//		FactoryMultiView.

		// create a list of features coordinate and pixel observations
//		computeStructure.ransac.


	}

	/**
	 * Finds the seed view with the most inliers and a 3D connection. Returns null if none exist
	 */
	public PairwiseImageGraph2.Motion selectThirdView(PairwiseImageGraph2.View target, PairwiseImageGraph2.View seed )
	{
		int bestScore = 0;
		PairwiseImageGraph2.Motion bestView = null;

		for (int i = 0; i < target.connections.size; i++) {
			PairwiseImageGraph2.Motion m = target.connections.get(i);
			if( !m.is3D )
				continue;
			PairwiseImageGraph2.View viewC = m.other(target);
			if( viewC == seed)
				continue;

			// If it's known it will have a known motion and be connected to the seed
			if( !workGraph.isKnown(viewC) )
				continue;

			boolean isSrc = m.src == target;

			// count the number of inliers which have known 3D locations
			int totalKnown = 0;
			for (int j = 0; j < m.inliers.size; j++) {
				SceneWorkingGraph.Feature f;
				if( isSrc ) {
					f = workGraph.lookupFeature(target,m.inliers.get(j).src);
				} else {
					f = workGraph.lookupFeature(target,m.inliers.get(j).dst);
				}
				if( f.known ) {
					totalKnown++;
				}
			}

			if( totalKnown > bestScore ) {
				bestScore = totalKnown;
				bestView = m;
			}
		}

		return bestView;
	}

}
