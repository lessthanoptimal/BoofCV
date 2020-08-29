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

import boofcv.abst.geo.TriangulateNViewsMetric;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.selfcalib.TwoViewToCalibratingHomography;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.Motion;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.ConfigConverge;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.FastQueue;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * Expands a metric {@link SceneWorkingGraph scene} by one view (the taget) using the geometric relationship between
 * the target and two known metric views.
 *
 * <ol>
 *     <li>Input: A seed view and the known graph</li>
 *     <li>Selects two other views with known camera matrices</li>
 *     <li>Finds features in common with all three views</li>
 *     <li>Trifocal tensor and RANSAC to find the unknown seed camera matrix</li>
 *     <li>Estimate calibrating homography from found projective scene and known metric values</li>
 *     <li>Elevate target from projective to metric</li>
 *     <li>Bundle Adjustment to refine estimate</li>
 *     <li>Convert from local coordinates to world / scene coordinates</li>
 *     <li>Add found metric view for target to scene</li>
 * </ol>
 *
 * <p>The initial projective scene is found independently using common observations in an attempt to reduce the
 * influence of past mistakes. To mitigate past mistakes, the intrisic parameters for the known views are
 * optimized inside of bundle adjustment even though they are "known". Only the found intrinsic and Se3 for
 * the target view will be added/modified in the scene graph.</p>
 *
 * @author Peter Abeles
 */
public class MetricExpandByOneView extends ExpandByOneView {

	// Finds the calibrating homography when metric parameters are known for two views
	protected TwoViewToCalibratingHomography projectiveHomography = new TwoViewToCalibratingHomography();

	/** The estimated scene structure. This the final estimated scene state */
	protected final @Getter SceneStructureMetric structure = new SceneStructureMetric(true);
	protected final @Getter SceneObservations observations = new SceneObservations();
	protected @Getter @Setter BundleAdjustment<SceneStructureMetric> sba = FactoryMultiView.bundleSparseMetric(null);
	protected @Getter @Setter TriangulateNViewsMetric triangulator = FactoryMultiView.triangulateNViewCalibrated((ConfigTriangulation)null);

	protected final List<Point2D_F64> pixelNorms = BoofMiscOps.createListFilled(3,Point2D_F64::new);
	protected final List<Se3_F64> listMotion = new ArrayList<>();
	protected final PinholePtoN_F64 normalize1 = new PinholePtoN_F64();
	protected final PinholePtoN_F64 normalize2 = new PinholePtoN_F64();
	protected final PinholePtoN_F64 normalize3 = new PinholePtoN_F64();

	//------------------------- Local work space

	// Storage fort he two selected connections with known cameras
	List<Motion> connections = new ArrayList<>();

	// Fundamental matrix between view-2 and view-3 in triplet
	DMatrixRMaj F21 = new DMatrixRMaj(3,3);
	// Storage for intrinsic camera matrices in view-2 and view-3
	DMatrixRMaj K1 = new DMatrixRMaj(3,3);
	DMatrixRMaj K2 = new DMatrixRMaj(3,3);
	FastQueue<AssociatedPair> pairs = new FastQueue<>(AssociatedPair::new);

	// Found Se3 from view-1 to target
	Se3_F64 view1_to_view1 = new Se3_F64();
	Se3_F64 view1_to_view2 = new Se3_F64();
	Se3_F64 view1_to_target = new Se3_F64();
	Se3_F64 view1_to_view2H = new Se3_F64(); // found with calibrating homography
	// K calibration matrix for target view
	DMatrixRMaj K_target = new DMatrixRMaj(3,3);

	public MetricExpandByOneView() {
		listMotion.add(view1_to_view1);
		listMotion.add(view1_to_view2);
		listMotion.add(view1_to_target);
	}

	/**
	 * Attempts to estimate the camera model in the global projective space for the specified view
	 *
	 * @param db (Input) image data base
	 * @param workGraph (Input/Output) scene graph. On input it will have the known scene and if successful the metric
	 *                  information for the target view.
	 * @param target (Input) The view that needs its projective camera estimated and the graph is being expanded into
	 * @return true if successful target view has an estimated calibration matrix and pose, which have already been
	 * added to "workGraph"
	 */
	public boolean process( LookupSimilarImages db ,
							SceneWorkingGraph workGraph ,
							View target )
	{
		assertBoof(!workGraph.isKnown(target),"Target shouldn't already be in the workGraph");
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
		utils.seed = connections.get(0).other(target);
		utils.viewB = connections.get(1).other(target);
		utils.viewC = target; // easier if target is viewC when doing metric elevation
		utils.createThreeViewLookUpTables();
		utils.findCommonFeatures();

		if( verbose != null ) {
			verbose.println( "Expanding to view='"+target.id+"' using views ( '"+utils.seed.id+"' , '"+utils.viewB.id+
					"') common="+utils.commonIdx.size+" valid.size="+validCandidates.size());
		}

		// Estimate trifocal tensor using three view observations
		utils.createTripleFromCommon();
		if( !utils.estimateProjectiveCamerasRobustly() )
			return false;
		if( verbose != null ) verbose.println( "Trifocal RANSAC inliers.size="+utils.inliersThreeView.size());

		// Using known camera information elevate to a metric scene
		if (!computeCalibratingHomography())
			return false;

		// Now that the metric upgrade is known add it to work graph
		SceneWorkingGraph.View wtarget = workGraph.addView(utils.viewC);

		// Find the metric upgrade of the target
		upgradeToMetric(wtarget,projectiveHomography.getCalibrationHomography());

		// Refine using bundle adjustment, if configured to do so
		if( utils.configConvergeSBA.maxIterations > 0 ) {
			refineWithBundleAdjustment(workGraph);
		}

		// Match the local coordinate system's scale to the global coordinate system's scale
		int which = UtilPoint3D_F64.axisLargestAbs(view1_to_view2.T);
		double scale = view1_to_view2.T.getIdx(which)/view1_to_view2H.T.getIdx(which);
		view1_to_target.T.scale(scale);

		// Convert local coordinate into world coordinates for the view's pose
		Se3_F64 world_to_view1 = workGraph.views.get(utils.seed.id).world_to_view;
		world_to_view1.concat(view1_to_target, wtarget.world_to_view);

		if( verbose != null ) {
			verbose.printf("Rescaled Local T={%.1f %.1f %.1f) scale=%f\n",
					view1_to_target.T.x, view1_to_target.T.y, view1_to_target.T.z,scale);
			verbose.printf("Final Global   T={%.1f %.1f %.1f)\n",
					wtarget.world_to_view.T.x, wtarget.world_to_view.T.y, wtarget.world_to_view.T.z);
		}

		return true;
	}

	/**
	 * Use previously computed calibration homography to upgrade projective scene to metric
	 */
	private void upgradeToMetric(SceneWorkingGraph.View wview, DMatrixRMaj H_cal) {
		// Compute metric upgrade from found projective camera matrices
		MultiViewOps.projectiveToMetric(utils.P2,H_cal, view1_to_view2H,K_target); // discard K_target
		MultiViewOps.projectiveToMetric(utils.P3,H_cal, view1_to_target,K_target);
		wview.intrinsic.set(K_target);

		// Normalize the scale so that it's close to 1.0
		double normTarget = view1_to_target.T.norm();
		view1_to_target.T.divide(normTarget);
		view1_to_view2H.T.divide(normTarget);

		// Let's use the "known" view1_to_view2, but to do that we will need to resolve the scale ambiguity
		SceneWorkingGraph.View wview1 = workGraph.lookupView(utils.seed.id);
		SceneWorkingGraph.View wview2 = workGraph.lookupView(utils.viewB.id);
		wview1.world_to_view.invert(null).concat(wview2.world_to_view,view1_to_view2);

		// use the largest axis (which should not be equal to 0.0) to determine the scale + sign
		int which = UtilPoint3D_F64.axisLargestAbs(view1_to_view2.T);
		double scale = view1_to_view2H.T.getIdx(which)/view1_to_view2.T.getIdx(which);
		if(UtilEjml.isUncountable(scale)) {
			scale = 0.0; // if pathological fail gracefully
		}

		if( verbose != null ) {
			// print the found view 1 to view 2 using local information only
			verbose.printf("L View 1 to 2     T={%.1f %.1f %.1f)\n",
					view1_to_view2H.T.x, view1_to_view2H.T.y, view1_to_view2H.T.z);
		}

		// Now set the global view-1 to view-2 at the local scale
		view1_to_view2H.set(view1_to_view2);
		view1_to_view2H.T.scale(scale);

		if( verbose != null ) {
			verbose.printf("G View 1 to 2     T={%.1f %.1f %.1f)\n",
					view1_to_view2H.T.x, view1_to_view2H.T.y, view1_to_view2H.T.z);
			verbose.printf("Initial fx=%6.1f k1=%6.3f k2=%6.3f T={%.1f %.1f %.1f)\n",
					wview.intrinsic.f, wview.intrinsic.k1, wview.intrinsic.k2,
					view1_to_target.T.x, view1_to_target.T.y, view1_to_target.T.z);
		}
	}

	/**
	 * Optimize the three view metric local scene using SBA
	 */
	private void refineWithBundleAdjustment(SceneWorkingGraph workGraph) {
		// Look up known information
		SceneWorkingGraph.View wview1 = workGraph.lookupView(utils.seed.id);
		SceneWorkingGraph.View wview2 = workGraph.lookupView(utils.viewB.id);
		SceneWorkingGraph.View wview3 = workGraph.lookupView(utils.viewC.id);

		// configure camera pose and intrinsics
		List<AssociatedTriple> triples = utils.inliersThreeView;
		final int numFeatures = triples.size();
		structure.initialize(3,3,numFeatures);
		observations.initialize(3);

		observations.getView(0).resize(numFeatures);
		observations.getView(1).resize(numFeatures);
		observations.getView(2).resize(numFeatures);

		// TODO change to pinhole simplified? (0,0) is assumed at this point
		// Camera parameters and Se3 for all views can change to mitigate past mistakes
		// if there was a past mistake the idea is that it can be recovered from here, however if a mistake is
		// made here it will be compensated for by being ignored in future calls.
		structure.setCamera(0,true, wview1.intrinsic);
		structure.setCamera(1,true, wview2.intrinsic);
		structure.setCamera(2,false,wview3.intrinsic);
		structure.setView(0,true,view1_to_view1);
		structure.setView(1,false,view1_to_view2H);
		structure.setView(2,false,view1_to_target);
		structure.connectViewToCamera(0,0);
		structure.connectViewToCamera(1,1);
		structure.connectViewToCamera(2,2);

		// Add observations and 3D feature locations
		normalize1.set(wview1.intrinsic.f,wview1.intrinsic.f,0,0,0);
		normalize2.set(wview2.intrinsic.f,wview2.intrinsic.f,0,0,0);
		normalize3.set(wview3.intrinsic.f,wview3.intrinsic.f,0,0,0);

		SceneObservations.View viewObs1 = observations.getView(0);
		SceneObservations.View viewObs2 = observations.getView(1);
		SceneObservations.View viewObs3 = observations.getView(2);

		Point3D_F64 foundX = new Point3D_F64();
		for (int featIdx = 0; featIdx < numFeatures; featIdx++) {
			AssociatedTriple a = triples.get(featIdx);
			viewObs1.set(featIdx,featIdx,(float)a.p1.x, (float)a.p1.y);
			viewObs2.set(featIdx,featIdx,(float)a.p2.x, (float)a.p2.y);
			viewObs3.set(featIdx,featIdx,(float)a.p3.x, (float)a.p3.y);

			normalize1.compute(a.p1.x,a.p1.y,pixelNorms.get(0));
			normalize2.compute(a.p2.x,a.p2.y,pixelNorms.get(1));
			normalize3.compute(a.p3.x,a.p3.y,pixelNorms.get(2));

			if( !triangulator.triangulate(pixelNorms,listMotion,foundX) ) {
				throw new RuntimeException("This should be handled");
			}
			// TODO also check to see if it appears behind the camera
			structure.setPoint(featIdx,foundX.x,foundX.y,foundX.z,1.0);
			structure.connectPointToView(featIdx,0);
			structure.connectPointToView(featIdx,1);
			structure.connectPointToView(featIdx,2);
		}

		// Refine using bundle adjustment
		ConfigConverge converge = utils.configConvergeSBA;
		sba.configure(converge.ftol,converge.gtol,converge.maxIterations);
		sba.setParameters(structure,observations);
		if( !sba.optimize(structure) )
			throw new RuntimeException("Handle this");

		// copy results for output
		wview3.intrinsic.set((BundlePinholeSimplified)structure.cameras.get(2).model);
		view1_to_view2H.set(structure.views.get(1).worldToView);
		view1_to_target.set(structure.views.get(2).worldToView);

		if( verbose != null ) {
			verbose.printf("G View 1 to 2     T={%.1f %.1f %.1f)\n",
					view1_to_view2H.T.x, view1_to_view2H.T.y, view1_to_view2H.T.z);
			verbose.printf("Refined fx=%6.1f k1=%6.3f k2=%6.3f T={%.1f %.1f %.1f)\n",
					wview3.intrinsic.f, wview3.intrinsic.k1, wview3.intrinsic.k2,
					view1_to_target.T.x, view1_to_target.T.y, view1_to_target.T.z);
		}
	}

	/**
	 * Computes the transform needed to go from one projective space into another
	 */
	boolean computeCalibratingHomography() {

		// convert everything in to the correct data format
		MultiViewOps.projectiveToFundamental(utils.P2, F21);
		projectiveHomography.initialize(F21,utils.P2);

		workGraph.lookupView(utils.seed.id).intrinsic.convertTo(K1);
		workGraph.lookupView(utils.viewB.id).intrinsic.convertTo(K2);

		FastQueue<AssociatedTriple> triples = utils.matchesTriple;
		pairs.resize(triples.size());
		for (int idx = 0; idx < triples.size(); idx++) {
			AssociatedTriple a = triples.get(idx);
			pairs.get(idx).set(a.p1,a.p2);
		}

		return projectiveHomography.process(K1, K2,pairs.toList());
	}
}
