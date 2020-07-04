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

import boofcv.alg.geo.pose.CompatibleProjectiveHomography;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.Motion;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import lombok.Getter;
import lombok.Setter;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.ArrayList;
import java.util.List;

/**
 * Expands an existing projective scene to include a new view. At least two neighbors with a known projective transform
 * and a 3D relationship connecting all three is required.
 *
 * <ol>
 *     <li>Input: A seed view and the known graph</li>
 *     <li>Selects two other views with known camera matrices</li>
 *     <li>Finds features in common with all three views</li>
*      <li>Trifocal tensor and RANSAC to find the unknown seed camera matrix</li>
 *     <li>Bundle Adjustment to refine estimate</li>
 *     <li>Makes the new camera matrix compatible with the existing ones</li>
 * </ol>
 *
 * Previously computed 3D scene points are not used in an effort to avoid propagating errors. The trifocal tensor
 * computed independently of previous calculations won't be influenced by previous errors. Past errors only influence
 * the current projective estimate when making the two scenes compatible.
 *
 * @author Peter Abeles
 */
public class ProjectiveExpandByOneView {

	// Used to make two sets of equivalent projective transforms compatible with each other
	CompatibleProjectiveHomography findCompatible = new CompatibleProjectiveHomography();

	// Reference to the working scene graph
	SceneWorkingGraph workGraph;

	/** Common algorithms for reconstructing the projective scene */
	@Getter	@Setter PairwiseGraphUtils utils = new PairwiseGraphUtils(new ConfigProjectiveReconstruction());

	//------------------------- Local work space

	// Storage fort he two selected connections with known cameras
	List<Motion> connections = new ArrayList<>();

	// homography to convert local camera matrices into ones in global
	DMatrixRMaj localToGlobal = new DMatrixRMaj(4,4);

	// Storage for camera matrices
	List<DMatrixRMaj> camerasLocal = new ArrayList<>();
	List<DMatrixRMaj> camerasGlobal = new ArrayList<>();

	// candidates for being used as known connections
	List<Motion> validCandidates = new ArrayList<>();

	/**
	 * Attempts to estimate the camera model in the global projective space for the specified view
	 *
	 * @param db (Input) image data base
	 * @param workGraph (Input) scene graph
	 * @param seed (Input) The view that needs its projective camera estimated
	 * @param cameraMatrix (output) the found camera matrix
	 * @return true if successful and the camera matrix computed
	 */
	public boolean process( LookupSimilarImages db ,
							SceneWorkingGraph workGraph ,
							View seed ,
							DMatrixRMaj cameraMatrix )
	{
		this.workGraph = workGraph;
		this.utils.db = db;

		// Select two known connected Views
		if( !selectTwoConnections(seed,connections) )
			return false;

		// Find features which are common between all three views
		utils.seed = seed;
		utils.viewB = connections.get(0).other(seed);
		utils.viewC = connections.get(1).other(seed);
		utils.createThreeViewLookUpTables();
		utils.findCommonFeatures();

		// Estimate trifocal tensor using three view observations
		utils.createTripleFromCommon();
		if( !utils.estimateProjectiveCamerasRobustly() )
			return false;

		// Bundle adjustment
		utils.initializeSbaSceneThreeView();
		utils.initializeSbaObservationsThreeView();
		utils.refineWithBundleAdjustment();

		// Make the two projective frames compatible
		if (!computeConversionHomography())
			return false;

		// Add the view to the scene graph and save the found projective camera
		CommonOps_DDRM.mult(utils.P1,localToGlobal,cameraMatrix);

		return true;
	}

	/**
	 * Computes the transform needed to go from one projective space into another
	 */
	boolean computeConversionHomography() {
		camerasLocal.clear();
		camerasGlobal.clear();
		camerasLocal.add(utils.P2);
		camerasLocal.add(utils.P3);
		camerasGlobal.add(workGraph.lookupView(utils.viewB.id).camera);
		camerasGlobal.add(workGraph.lookupView(utils.viewC.id).camera);

		return findCompatible.fitCameras(camerasLocal, camerasGlobal, localToGlobal);
	}

	/**
	 * Selects two views which are connected to the target by maximizing a score function. The two selected
	 * views must have 3D information, be connected to each other, and have a known camera matrix. These three views
	 * will then be used to estimate a trifocal tensor
	 *
	 * @param target (input) A view
	 * @param connections (output) the two selected connected views to the target
	 * @return true if successful or false if it failed
	 */
	public boolean selectTwoConnections( View target , List<Motion> connections )
	{
		connections.clear();

		// Create a list of connections in the target that can be used
		createListOfValid(target, validCandidates);

		double bestScore = 0.0;
		for (int connectionIdx = 0; connectionIdx < validCandidates.size(); connectionIdx++) {
			Motion connectB = validCandidates.get(connectionIdx);
			Motion connectC = findBestCommon(target,connectB, validCandidates);
			if( connectC == null )
				continue; // no common connection could be found

			double score = utils.scoreMotion.score(connectB) + utils.scoreMotion.score(connectC);
			if( score > bestScore ) {
				bestScore = score;
				connections.clear();
				connections.add(connectB);
				connections.add(connectC);
			}
		}

		return !connections.isEmpty();
	}

	/**
	 * Finds all the connections from the target view which are 3D and have known other views
	 * @param target (input)
	 * @param validConnections (output)
	 */
	void createListOfValid(View target, List<Motion> validConnections) {
		validConnections.clear();
		for (int connectionIdx = 0; connectionIdx < target.connections.size; connectionIdx++) {
			Motion connectB = target.connections.get(connectionIdx);
			if( !connectB.is3D || !workGraph.isKnown(connectB.other(target)))
				continue;
			validConnections.add(connectB);
		}
	}

	/**
	 * Selects the view C which has the best connection from A to C and B to C. Best is defined using the
	 * scoring function and being 3D.
	 *
	 * @param viewA (input) The root node all motions must connect to
	 * @param connAB (input) A connection from view A to view B
	 * @param validConnections (input) List of connections that are known to be valid potential solutions
	 * @return The selected common view. null if none could be found
	 */
	public Motion findBestCommon( View viewA, Motion connAB , List<Motion> validConnections)
	{
		double bestScore = 0.0;
		Motion bestConnection = null;

		View viewB = connAB.other(viewA);

		for (int connIdx = 0; connIdx < validConnections.size(); connIdx++) {
			Motion connAC = validConnections.get(connIdx);
			if( connAC == connAB )
				continue;
			View viewC = connAC.other(viewA);

			// The views must form a complete loop with 3D information
			Motion connBC = viewB.findMotion(viewC);
			if( null == connBC || !connBC.is3D )
				continue;

			// Maximize worst case 3D information
			double score = Math.min(utils.scoreMotion.score(connAC) , utils.scoreMotion.score(connBC));

			if( score > bestScore ) {
				bestScore = score;
				bestConnection = connAC;
			}
		}

		return bestConnection;
	}
}
