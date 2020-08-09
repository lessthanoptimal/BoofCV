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
public class ProjectiveExpandByOneView extends ExpandByOneView {

	// Used to make two sets of equivalent projective transforms compatible with each other
	CompatibleProjectiveHomography findCompatible = new CompatibleProjectiveHomography();

	//------------------------- Local work space

	// Storage fort he two selected connections with known cameras
	List<Motion> connections = new ArrayList<>();

	// homography to convert local camera matrices into ones in global
	DMatrixRMaj localToGlobal = new DMatrixRMaj(4,4);
	DMatrixRMaj globalToLocal = new DMatrixRMaj(4,4);

	// Storage for camera matrices
	List<DMatrixRMaj> camerasLocal = new ArrayList<>();
	List<DMatrixRMaj> camerasGlobal = new ArrayList<>();

	/**
	 * Attempts to estimate the camera model in the global projective space for the specified view
	 *
	 * @param db (Input) image data base
	 * @param workGraph (Input) scene graph
	 * @param target (Input) The view that needs its projective camera estimated and the graph is being expanded into
	 * @param cameraMatrix (output) the found camera matrix
	 * @return true if successful and the camera matrix computed
	 */
	public boolean process( LookupSimilarImages db ,
							SceneWorkingGraph workGraph ,
							View target ,
							DMatrixRMaj cameraMatrix )
	{
		this.workGraph = workGraph;
		this.utils.db = db;

		// Select two known connected Views
		if( !selectTwoConnections(target,connections) ) {
			if( verbose != null ) {
				verbose.println( "Failed to expand because two connections couldn't be found. valid.size=" +
						validCandidates.size());
				for (int i = 0; i < validCandidates.size(); i++) {
					verbose.println("   valid view.id='"+validCandidates.get(i).other(target).id+"'");
				}
			}
			return false;
		}

		// Find features which are common between all three views
		utils.seed = target;
		utils.viewB = connections.get(0).other(target);
		utils.viewC = connections.get(1).other(target);
		utils.createThreeViewLookUpTables();
		utils.findCommonFeatures();

		if( verbose != null ) {
			verbose.println( "Expanding to view='"+target.id+"' using views ( '"+utils.viewB.id+"' , '"+utils.viewC.id+
					"') common="+utils.commonIdx.size+" valid.size="+validCandidates.size());
		}

		// Estimate trifocal tensor using three view observations
		utils.createTripleFromCommon();
		if( !utils.estimateProjectiveCamerasRobustly() )
			return false;

		// Compute the conversion which will make the two frames compatible
		if (!computeConversionHomography()) // TODO refine by minimizing reprojection error
			return false;

		// Improve the fit using bundle adjustment. This will reduce the rate at which errors are built up since
		// It's important to optimize in the local frame since numbers involved will not be too large or small
		// NOTE: Still might not be a bad idea to adjust the scale of everything first
		// The lines below convert the known camera frames from global into local frame
		CommonOps_DDRM.invert(localToGlobal,globalToLocal);
		CommonOps_DDRM.mult(workGraph.lookupView(utils.viewB.id).projective,globalToLocal,utils.P2);
		CommonOps_DDRM.mult(workGraph.lookupView(utils.viewC.id).projective,globalToLocal,utils.P3);

		// fix cameras P2 and P3 and let everything else float
		utils.initializeSbaSceneThreeView(false);
		utils.initializeSbaObservationsThreeView();
		utils.refineWithBundleAdjustment();

		// Convert the refined results into global projective frame
		CommonOps_DDRM.mult(utils.structure.getViews().get(0).worldToView,localToGlobal,cameraMatrix);

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
		camerasGlobal.add(workGraph.lookupView(utils.viewB.id).projective);
		camerasGlobal.add(workGraph.lookupView(utils.viewC.id).projective);

		return findCompatible.fitCameras(camerasLocal, camerasGlobal, localToGlobal);
	}
}
