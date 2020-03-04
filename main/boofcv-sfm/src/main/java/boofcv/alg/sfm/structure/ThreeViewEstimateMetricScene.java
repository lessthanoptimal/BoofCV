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

package boofcv.alg.sfm.structure;

import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.abst.geo.RefineThreeViewProjective;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.PruneStructureFromSceneMetric;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.selfcalib.EstimatePlaneAtInfinityGivenK;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic;
import boofcv.factory.geo.*;
import boofcv.misc.ConfigConverge;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.geo.MultiViewOps.triangulatePoints;

/**
 * Estimates the metric scene's structure given a set of sparse features associations from three views. This is
 * intended to give the best possible solution from the sparse set of matching features. Its internal
 * methods are updated as better strategies are found.
 *
 * Assumptions:
 * <ul>
 *     <li>Principle point is zero</li>
 *     <li>Zero skew</li>
 *     <li>fx = fy approximately</li>
 * </ul>
 *
 * The zero principle point is enforced prior to calling {@link #process} by subtracting the image center from
 * each pixel observations.
 *
 * Steps:
 * <ol>
 *     <li>Fit Trifocal tensor using RANSAC</li>
 *     <li>Get and refine camera matrices</li>
 *     <li>Compute dual absolute quadratic</li>
 *     <li>Estimate intrinsic parameters from DAC</li>
 *     <li>Estimate metric scene structure</li>
 *     <li>Sparse bundle adjustment</li>
 *     <li>Tweak parameters and sparse bundle adjustment again</li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class ThreeViewEstimateMetricScene {

	// Make all configurations public for ease of manipulation
	public ConfigRansac configRansac = new ConfigRansac();
	public ConfigTrifocal configTriRansac = new ConfigTrifocal();
	public ConfigTrifocal configTriFit = new ConfigTrifocal();
	public ConfigTrifocalError configError = new ConfigTrifocalError();
	public ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
	public ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
	public ConfigConverge convergeSBA = new ConfigConverge(1e-6,1e-6,100);

	// estimating the trifocal tensor and storing which observations are in the inlier set
	public Ransac<TrifocalTensor,AssociatedTriple> ransac;
	public List<AssociatedTriple> inliers;
	public Estimate1ofTrifocalTensor trifocalEstimator;

	// how much and where it should print to
	private PrintStream verbose;
	private int verboseLevel;

	// Projective camera matrices
	protected DMatrixRMaj P1 = CommonOps_DDRM.identity(3,4);
	protected DMatrixRMaj P2 = new DMatrixRMaj(3,4);
	protected DMatrixRMaj P3 = new DMatrixRMaj(3,4);

	// storage for pinhole cameras
	protected List<CameraPinhole> listPinhole = new ArrayList<>();

	// Refines the structure
	public BundleAdjustment<SceneStructureMetric> bundleAdjustment;

	// Bundle adjustment data structure and tuning parameters
	public SceneStructureMetric structure;
	public SceneObservations observations;

	// If a positive number the focal length will be assumed to be that
	public double manualFocalLength=-1;

	// How many features it will keep when pruning
	public double pruneFraction = 0.7;

	// shape of input images.
	private int width, height; // TODO Get size for each image individually

	// metric location of each camera. The first view is always identity
	protected List<Se3_F64> worldToView = new ArrayList<>();

	/**
	 * Sets configurations to their default value
	 */
	public ThreeViewEstimateMetricScene() {
		configRansac.maxIterations = 500;
		configRansac.inlierThreshold = 1;

		configError.model = ConfigTrifocalError.Model.REPROJECTION_REFINE;

		configTriFit.which = EnumTrifocal.ALGEBRAIC_7;
		configTriFit.converge.maxIterations = 100;

		configLM.dampeningInitial = 1e-3;
		configLM.hessianScaling = false;
		configSBA.configOptimizer = configLM;

		for (int i = 0; i < 3; i++) {
			worldToView.add( new Se3_F64());
		}
	}

	/**
	 * Determines the metric scene. The principle point is assumed to be zero in the passed in pixel coordinates.
	 * Typically this is done by subtracting the image center from each pixel coordinate for each view.
	 *
	 * @param associated List of associated features from 3 views. pixels
	 * @param width width of all images
	 * @param height height of all images
	 * @return true if successful or false if it failed
	 */
	public boolean process(List<AssociatedTriple> associated , int width , int height ) {
		init(width, height);

		// Fit a trifocal tensor to the input observations
		if (!robustFitTrifocal(associated) )
			return false;

		// estimate the scene's structure
		if( !estimateProjectiveScene())
			return false;

		if( !projectiveToMetric() )
			return false;

		// Run bundle adjustment while make sure a valid solution is found
		setupMetricBundleAdjustment(inliers);

		bundleAdjustment = FactoryMultiView.bundleSparseMetric(configSBA);
		findBestValidSolution(bundleAdjustment);

		// Prune outliers and run bundle adjustment one last time
		pruneOutliers(bundleAdjustment);

		return true;
	}

	private void init( int width , int height) {
		this.width = width;
		this.height = height;
		ransac = FactoryMultiViewRobust.trifocalRansac(configTriRansac,configError,configRansac);
		trifocalEstimator = FactoryMultiView.trifocal_1(configTriFit);
		structure = null;
		observations = null;
	}

	/**
	 * Fits a trifocal tensor to the list of matches features using a robust method
	 */
	private boolean robustFitTrifocal(List<AssociatedTriple> associated) {
		// Fit a trifocal tensor to the observations robustly
		ransac.process(associated);

		inliers = ransac.getMatchSet();
		TrifocalTensor model = ransac.getModelParameters();
		if( verbose != null )
			verbose.println("Remaining after RANSAC "+inliers.size()+" / "+associated.size());

		// estimate using all the inliers
		// No need to re-scale the input because the estimator automatically adjusts the input on its own
		if( !trifocalEstimator.process(inliers,model) ) {
			if( verbose != null ) {
				verbose.println("Trifocal estimator failed");
			}
			return false;
		}
		return true;
	}

	/**
	 * Prunes the features with the largest reprojection error
	 */
	private void pruneOutliers(BundleAdjustment<SceneStructureMetric> bundleAdjustment) {
		// see if it's configured to not prune
		if( pruneFraction == 1.0 )
			return;
		PruneStructureFromSceneMetric pruner = new PruneStructureFromSceneMetric(structure,observations);
		pruner.pruneObservationsByErrorRank(pruneFraction);
		pruner.pruneViews(10);
		pruner.prunePoints(1);
		bundleAdjustment.setParameters(structure,observations);
		bundleAdjustment.optimize(structure);

		if( verbose != null ) {
			verbose.println("\nCamera");
			for (int i = 0; i < structure.cameras.size; i++) {
				verbose.println(structure.cameras.data[i].getModel().toString());
			}
			verbose.println("\n\nworldToView");
			for (int i = 0; i < structure.views.size; i++) {
				verbose.println(structure.views.data[i].worldToView.toString());
			}
			verbose.println("Fit Score: " + bundleAdjustment.getFitScore());
		}
	}

	/**
	 * Tries a bunch of stuff to ensure that it can find the best solution which is physically possible
	 */
	private void findBestValidSolution(BundleAdjustment<SceneStructureMetric> bundleAdjustment) {
		// prints out useful debugging information that lets you know how well it's converging
		if( verbose != null && verboseLevel > 0 )
			bundleAdjustment.setVerbose(verbose,0);

		// Specifies convergence criteria
		bundleAdjustment.configure(convergeSBA.ftol, convergeSBA.gtol, convergeSBA.maxIterations);

		bundleAdjustment.setParameters(structure,observations);
		bundleAdjustment.optimize(structure);

		// ensure that the points are in front of the camera and are a valid solution
		if( checkBehindCamera(structure) ) {
			if( verbose != null )
				verbose.println("  flipping view");
			flipAround(structure,observations);
			bundleAdjustment.setParameters(structure,observations);
			bundleAdjustment.optimize(structure);
		}

		double bestScore = bundleAdjustment.getFitScore();
		List<Se3_F64> bestPose = new ArrayList<>();
		List<BundlePinholeSimplified> bestCameras = new ArrayList<>();
		for (int i = 0; i < structure.views.size; i++) {
			BundlePinholeSimplified c = structure.cameras.data[i].getModel();
			bestPose.add(structure.views.data[i].worldToView.copy());
			bestCameras.add( c.copy());
		}

		for (int i = 0; i < structure.cameras.size; i++) {
			BundlePinholeSimplified c = structure.cameras.data[i].getModel();
			c.f = listPinhole.get(i).fx;
			c.k1 = c.k2 = 0;
		}
		// flip rotation assuming that it was done wrong
		for (int i = 1; i < structure.views.size; i++) {
			CommonOps_DDRM.transpose(structure.views.data[i].worldToView.R);
		}
		triangulatePoints(structure,observations);

		bundleAdjustment.setParameters(structure,observations);
		bundleAdjustment.optimize(structure);

		if( checkBehindCamera(structure) ) {
			if( verbose != null )
				verbose.println("  flipping view");
			flipAround(structure,observations);
			bundleAdjustment.setParameters(structure,observations);
			bundleAdjustment.optimize(structure);
		}

		// revert to old settings
		if( verbose != null )
			verbose.println(" ORIGINAL / NEW = " + bestScore+" / "+bundleAdjustment.getFitScore());
		if( bundleAdjustment.getFitScore() > bestScore ) {
			if( verbose != null )
				verbose.println("  recomputing old structure");
			for (int i = 0; i < structure.cameras.size; i++) {
				BundlePinholeSimplified c = structure.cameras.data[i].getModel();
				c.set(bestCameras.get(i));
				structure.views.data[i].worldToView.set(bestPose.get(i));
			}
			triangulatePoints(structure,observations);
			bundleAdjustment.setParameters(structure,observations);
			bundleAdjustment.optimize(structure);
			if( verbose != null )
				verbose.println("  score = "+bundleAdjustment.getFitScore());
		}
	}

	/**
	 * Estimate the projective scene from the trifocal tensor
	 */
	private boolean estimateProjectiveScene() {
		List<AssociatedTriple> inliers = ransac.getMatchSet();
		TrifocalTensor model = ransac.getModelParameters();

		MultiViewOps.extractCameraMatrices(model,P2,P3);

		// Most of the time this makes little difference, but in some edges cases this enables it to
		// converge correctly
		RefineThreeViewProjective refineP23 = FactoryMultiView.threeViewRefine(null);
		if( !refineP23.process(inliers,P2,P3,P2,P3) ) {
			if( verbose != null ) {
				verbose.println("Can't refine P2 and P3!");
			}
			return false;
		}
		return true;
	}

	/**
	 * Using the initial metric reconstruction, provide the initial configurations for bundle adjustment
	 */
	private void setupMetricBundleAdjustment(List<AssociatedTriple> inliers) {
		// Construct bundle adjustment data structure
		structure = new SceneStructureMetric(false);
		structure.initialize(3,3,inliers.size());
		observations = new SceneObservations();
		observations.initialize(3);

		for (int i = 0; i < listPinhole.size(); i++) {
			CameraPinhole cp = listPinhole.get(i);
			BundlePinholeSimplified bp = new BundlePinholeSimplified();

			bp.f = cp.fx;

			structure.setCamera(i,false,bp);
			structure.setView(i,i==0,worldToView.get(i));
			structure.connectViewToCamera(i,i);
		}
		for (int i = 0; i < inliers.size(); i++) {
			AssociatedTriple t = inliers.get(i);

			observations.getView(0).add(i,(float)t.p1.x,(float)t.p1.y);
			observations.getView(1).add(i,(float)t.p2.x,(float)t.p2.y);
			observations.getView(2).add(i,(float)t.p3.x,(float)t.p3.y);

			structure.connectPointToView(i,0);
			structure.connectPointToView(i,1);
			structure.connectPointToView(i,2);
		}
		// Initial estimate for point 3D locations
		triangulatePoints(structure,observations);
	}

	/**
	 * Estimates the transform from projective to metric geometry
	 * @return true if successful
	 */
	boolean projectiveToMetric() {

		// homography from projective to metric
		DMatrixRMaj H = new DMatrixRMaj(4,4);
		listPinhole.clear();

		if( manualFocalLength <= 0 ) {
			// Estimate calibration parameters
			SelfCalibrationLinearDualQuadratic selfcalib = new SelfCalibrationLinearDualQuadratic(1.0);
			selfcalib.addCameraMatrix(P1);
			selfcalib.addCameraMatrix(P2);
			selfcalib.addCameraMatrix(P3);

			GeometricResult result = selfcalib.solve();
			if (GeometricResult.SOLVE_FAILED != result && selfcalib.getSolutions().size() == 3) {
				for (int i = 0; i < 3; i++) {
					SelfCalibrationLinearDualQuadratic.Intrinsic c = selfcalib.getSolutions().get(i);
					CameraPinhole p = new CameraPinhole(c.fx, c.fy, 0, 0, 0, width, height);
					listPinhole.add(p);
				}
			} else {
				// TODO Handle this better
				System.out.println("Self calibration failed!");
				for (int i = 0; i < 3; i++) {
					CameraPinhole p = new CameraPinhole(width / 2, width / 2, 0, 0, 0, width, height);
					listPinhole.add(p);
				}
			}
			// convert camera matrix from projective to metric
			if( !MultiViewOps.absoluteQuadraticToH(selfcalib.getQ(),H) ) {
				if( verbose != null ) {
					verbose.println("Projective to metric failed");
				}
				return false;
			}
		} else {
			// Assume all cameras have a fixed known focal length
			EstimatePlaneAtInfinityGivenK estimateV = new EstimatePlaneAtInfinityGivenK();
			estimateV.setCamera1(manualFocalLength,manualFocalLength,0,0,0);
			estimateV.setCamera2(manualFocalLength,manualFocalLength,0,0,0);

			Vector3D_F64 v = new Vector3D_F64(); // plane at infinity
			if( !estimateV.estimatePlaneAtInfinity(P2,v))
				throw new RuntimeException("Failed!");

			DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(manualFocalLength,manualFocalLength,0,0,0);
			MultiViewOps.createProjectiveToMetric(K,v.x,v.y,v.z,1,H);

			for (int i = 0; i < 3; i++) {
				CameraPinhole p = new CameraPinhole(manualFocalLength,manualFocalLength, 0, 0, 0, width, height);
				listPinhole.add(p);
			}
		}

		if( verbose != null ) {
			for (int i = 0; i < 3; i++) {
				CameraPinhole r = listPinhole.get(i);
				verbose.println("fx=" + r.fx + " fy=" + r.fy + " skew=" + r.skew);
			}
			verbose.println("Projective to metric");
		}

		DMatrixRMaj K = new DMatrixRMaj(3,3);

		// ignore K since we already have that
		MultiViewOps.projectiveToMetric(P1,H,worldToView.get(0),K);
		MultiViewOps.projectiveToMetric(P2,H,worldToView.get(1),K);
		MultiViewOps.projectiveToMetric(P3,H,worldToView.get(2),K);

		// scale is arbitrary. Set max translation to 1
		double maxT = 0;
		for( Se3_F64 p : worldToView ) {
			maxT = Math.max(maxT,p.T.norm());
		}
		for( Se3_F64 p : worldToView ) {
			p.T.scale(1.0/maxT);
			if( verbose != null ) {
				verbose.println(p);
			}
		}

		return true;
	}

	/**
	 * Checks to see if a solution was converged to where the points are behind the camera. This is
	 * pysically impossible
	 */
	private boolean checkBehindCamera(SceneStructureMetric structure ) {

		int totalBehind = 0;
		Point3D_F64 X = new Point3D_F64();
		for (int i = 0; i < structure.points.size; i++) {
			structure.points.data[i].get(X);
			if( X.z < 0 )
				totalBehind++;
		}

		if( verbose != null ) {
			verbose.println("points behind "+totalBehind+" / "+structure.points.size);
		}

		return totalBehind > structure.points.size/2;
	}

	/**
	 * Flip the camera pose around. This seems to help it converge to a valid solution if it got it backwards
	 * even if it's not technically something which can be inverted this way
	 */
	private static void flipAround(SceneStructureMetric structure, SceneObservations observations) {
		// The first view will be identity
		for (int i = 1; i < structure.views.size; i++) {
			Se3_F64 w2v = structure.views.data[i].worldToView;
			w2v.set(w2v.invert(null));
		}
		triangulatePoints(structure,observations);
	}

	public void setVerbose( PrintStream verbose , int level ) {
		this.verbose = verbose;
		this.verboseLevel = level;
	}

	public SceneStructureMetric getStructure() {
		return structure;
	}
}
