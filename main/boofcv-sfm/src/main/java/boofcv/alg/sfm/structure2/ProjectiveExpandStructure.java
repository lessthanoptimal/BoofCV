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
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.Motion;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ProjectiveExpandStructure {

	List<View> seedConnections = new ArrayList<>();

	ProjectiveInitializeAllCommon computeStructure;

	public void process( LookupSimilarImages db ,
						 View seed , GrowQueue_I32 connIdx , GrowQueue_I32 featIdx,
						 SceneStructureProjective structure ) {

		// create a list of initial views that are connected to the seed
		seed.addConnections(connIdx.data,connIdx.size,seedConnections);

		for (int motionIdxB = 0; motionIdxB < seed.connections.size; motionIdxB++) {
			Motion motionB = seed.connections.get(motionIdxB);

			// Only process nodes which are 3D
			if( !motionB.is3D )
				continue;

			// Skip if already in the current scene's structure
			if( connIdx.contains(motionIdxB))
				continue;

			// Find best 3D Connection between this node and one of the initial connections
			View viewB = motionB.other(seed);
			Motion conn_B_to_C = findConnectedSeed(viewB);
			if( conn_B_to_C == null )
				continue;

			// Find features with tracks that go from seed to B to C to seed
			View viewC = conn_B_to_C.other(viewB);
			int motionIdxC = seed.findMotionIdx(viewC);
			if( motionIdxC < 0 )
				throw new RuntimeException("BUG!");

			// Robustly find initial camera matrices
			if( !computeStructure.projectiveCameras3( db , seed, motionIdxB, motionIdxC ) )
				continue;

			// TODO Convert found into compatible camera matrices and add to scene

			// TODO Triangulate new points


			// TODO Add new camera matrix

			// TODO Triangulate points which are common across all 3-views

			// TODO Update scene structure

			// TODO Run bundle adjustment
		}
	}

	/**
	 * Finds the seed view with the most inliers and a 3D connection. Returns null if none exist
	 */
	public Motion findConnectedSeed( View v ) {
		int bestScore = 0;
		Motion bestView = null;

		for (int i = 0; i < v.connections.size; i++) {
			Motion m = v.connections.get(i);
			if( !m.is3D )
				continue;
			if( !seedConnections.contains(m.other(v)) )
				continue;

			if( m.inliers.size > bestScore ) {
				bestScore = m.inliers.size;
				bestView = m;
			}
		}

		return bestView;
	}

	/**
	 * Create a set of inlier tracks that connect all 3 nodes together
	 * @param seed
	 * @param seedToB
	 * @param bToC
	 */
	public void createTracks( View seed , Motion seedToB , Motion bToC ) {

	}
}
