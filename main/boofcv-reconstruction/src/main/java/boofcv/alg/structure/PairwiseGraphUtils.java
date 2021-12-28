/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.structure;

import boofcv.abst.geo.TriangulateNViewsProjective;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.ScaleSceneStructure;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.pose.PoseFromPairLinear6;
import boofcv.alg.structure.PairwiseImageGraph.Motion;
import boofcv.alg.structure.PairwiseImageGraph.View;
import boofcv.alg.structure.SceneWorkingGraph.InlierInfo;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.ConfigConverge;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import lombok.Getter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Various utility functions for dealing with {@link PairwiseImageGraph}
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class PairwiseGraphUtils {

	public LookUpSimilarImages dbSimilar;
	public LookUpCameraInfo dbCams;

	public DogArray_B visibleAll = new DogArray_B();
	public DogArray_B visibleMotion = new DogArray_B();

	//-------------------------------- Configurations. Must call fixate() if modified
	/** Converge tolerance for SBA */
	public ConfigConverge configConvergeSBA = new ConfigConverge(1e-8, 1e-8, 50);
	/** Toggles scaling inputs for SBA */
	public boolean configScaleSBA = false;

	/** The estimated scene structure. This the final estimated scene state */
	protected final @Getter SceneStructureProjective structure = new SceneStructureProjective(true);
	protected final @Getter SceneObservations observations = new SceneObservations();

	protected @Getter ModelMatcher<TrifocalTensor, AssociatedTriple> ransac;
	protected @Getter TriangulateNViewsProjective triangulator;
	protected @Getter PoseFromPairLinear6 poseEstimator = new PoseFromPairLinear6();
	protected @Getter BundleAdjustment<SceneStructureProjective> sba;
	protected @Getter ScaleSceneStructure scaler = new ScaleSceneStructure();

	/** The three views used in three view algorithms */
	public View seed, viewB, viewC;
	/** Prior calibration information for each view's camera */
	public CameraPinholeBrown priorCamA = new CameraPinholeBrown(2);
	public CameraPinholeBrown priorCamB = new CameraPinholeBrown(2);
	public CameraPinholeBrown priorCamC = new CameraPinholeBrown(2);

	public final DogArray<AssociatedTriple> matchesTriple = new DogArray<>(AssociatedTriple::new);
	/** Inliers from robust fitting of trifocal tensor. Pixels */
	public final FastArray<AssociatedTriple> inliersThreeView = new FastArray<>(AssociatedTriple.class);
	/** Which features the inliers correspond to */
	public final DogArray_I32 inlierIdx = new DogArray_I32();

	/** Projective camera matrices for 3-View reconstruction. P1 is always identity */
	public final DMatrixRMaj P1 = new DMatrixRMaj(3, 4);
	public final DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
	public final DMatrixRMaj P3 = new DMatrixRMaj(3, 4);

	// Location of features in the image. Pixels
	public final DogArray<Point2D_F64> featsA = new DogArray<>(Point2D_F64::new);
	public final DogArray<Point2D_F64> featsB = new DogArray<>(Point2D_F64::new);
	public final DogArray<Point2D_F64> featsC = new DogArray<>(Point2D_F64::new);

	// look up tables for feature indexes from one view to another
	public final DogArray_I32 table_A_to_B = new DogArray_I32();
	public final DogArray_I32 table_A_to_C = new DogArray_I32();
	public final DogArray_I32 table_B_to_C = new DogArray_I32();
	public final DogArray_I32 table_C_to_A = new DogArray_I32();
	// List indexes in the seed view that are common among all the views
	public final DogArray_I32 commonIdx = new DogArray_I32();

	public PairwiseGraphUtils( TriangulateNViewsProjective triangulator,
							   ModelMatcher<TrifocalTensor, AssociatedTriple> ransac,
							   BundleAdjustment<SceneStructureProjective> sba ) {
		this.triangulator = triangulator;
		this.ransac = ransac;
		this.sba = sba;
	}

	public PairwiseGraphUtils( ConfigProjectiveReconstruction config ) {
		config.checkValidity();
		triangulator = FactoryMultiView.triangulateNViewProj(ConfigTriangulation.GEOMETRIC());
		ransac = FactoryMultiViewRobust.trifocalRansac(
				config.ransacTrifocal, config.ransacError, config.ransac);
		sba = FactoryMultiView.bundleSparseProjective(config.sba);
		configConvergeSBA.setTo(config.sbaConverge);
		configScaleSBA = config.sbaScale;
	}

	public PairwiseGraphUtils() {}

	/**
	 * Finds the indexes of tracks which are common to all views and are inliers.
	 *
	 * @param seed (Input) The view which is used as the reference point
	 * @param connectIdx (Input) Indexes of connections in the seed view that will be searched for common connections
	 * @param commonIdx (Output) Indexes of observation in seed that are visible in connected views
	 */
	public void findAllConnectedSeed( View seed, DogArray_I32 connectIdx, DogArray_I32 commonIdx ) {
		if (connectIdx.size < 1)
			throw new RuntimeException("Called when there are no connections");

		// if true then it is visible in all tracks
		visibleAll.resetResize(seed.totalObservations, true);
		// used to keep track of which features are visible in the current motion
		visibleMotion.resize(seed.totalObservations);

		// Only look at features in the motions that were used to compute the score
		for (int idxMotion = 0; idxMotion < connectIdx.size; idxMotion++) {
			// Mark features which are inside the inlier set connecting these two views
			Motion m = seed.connections.get(connectIdx.get(idxMotion));
			boolean seedIsSrc = m.src == seed;
			visibleMotion.fill(false);
			for (int i = 0; i < m.inliers.size; i++) {
				AssociatedIndex a = m.inliers.get(i);
				visibleMotion.data[seedIsSrc ? a.src : a.dst] = true;
			}

			// This will mark features that were not visible as false
			for (int i = 0; i < seed.totalObservations; i++) {
				visibleAll.data[i] &= visibleMotion.data[i];
			}
		}

		// Make a list of indexes of features that are common for all views
		commonIdx.reset();
		for (int i = 0; i < seed.totalObservations; i++) {
			if (visibleAll.data[i]) {
				commonIdx.add(i);
			}
		}
	}

	/**
	 * Creates three feature look up tables: A -> B, A -> C, and B -> C
	 */
	public void createThreeViewLookUpTables() {
		Motion connAB = Objects.requireNonNull(seed.findMotion(viewB));
		Motion connAC = Objects.requireNonNull(seed.findMotion(viewC));
		Motion connBC = Objects.requireNonNull(viewB.findMotion(viewC));

		PairwiseGraphUtils.createTableViewAtoB(seed, connAB, table_A_to_B);
		PairwiseGraphUtils.createTableViewAtoB(seed, connAC, table_A_to_C);
		PairwiseGraphUtils.createTableViewAtoB(viewB, connBC, table_B_to_C);
		PairwiseGraphUtils.createTableViewAtoB(viewC, connAC, table_C_to_A);
	}

	/**
	 * Finds which features are common between all three views using the look up tables. Results are stored in
	 * {@link #commonIdx} by index of feature in {@link #seed}.
	 */
	public void findFullyConnectedTriple() {
		final int N = seed.totalObservations;
		commonIdx.reset();
		for (int featureIdxA = 0; featureIdxA < N; featureIdxA++) {
			// this feature must be visible in all 3 views
			if (table_A_to_B.data[featureIdxA] < 0 || table_A_to_C.data[featureIdxA] < 0)
				continue;
			// There also must be a connection from B to C for this feature
			int featureIdxB = table_A_to_B.data[featureIdxA];
			int featureIdxC = table_B_to_C.data[featureIdxB];
			if (featureIdxC < 0 || table_C_to_A.data[featureIdxC] != featureIdxA)
				continue;
			commonIdx.add(featureIdxA);
		}
	}

	/**
	 * Same as {@link #findFullyConnectedTriple()} but with a list that specified a subset of features in {@link #seed}.
	 *
	 * @param selectedIdxA List of feature indexes in A to consider
	 */
	public void findFullyConnectedTriple( DogArray_I32 selectedIdxA ) {
		commonIdx.reset();
		for (int selectedIdx = 0; selectedIdx < selectedIdxA.size; selectedIdx++) {
			final int featureIdxA = selectedIdxA.get(selectedIdx);

			// There also must be a connection from B to C for this feature
			int featureIdxB = table_A_to_B.data[featureIdxA];
			int featureIdxC = table_B_to_C.data[featureIdxB];
			if (featureIdxC < 0 || table_C_to_A.data[featureIdxC] != featureIdxA)
				continue;
			commonIdx.add(featureIdxA);
		}
	}

	/**
	 * Convert triple from indexes into coordinates
	 */
	public void createTripleFromCommon( @Nullable PrintStream verbose ) {
		// Camera info for each view
		dbCams.lookupCalibration(dbCams.viewToCamera(seed.id), priorCamA);
		dbCams.lookupCalibration(dbCams.viewToCamera(viewB.id), priorCamB);
		dbCams.lookupCalibration(dbCams.viewToCamera(viewC.id), priorCamC);

		if (verbose != null) {
			verbose.printf("prior: A={fx=%.1f cx=%.1f %.1f} B={fx=%.1f cx=%.1f %.1f} C={fx=%.1f cx=%.1f %.1f}\n",
					priorCamA.fx, priorCamA.cx, priorCamA.cy,
					priorCamB.fx, priorCamB.cx, priorCamB.cy,
					priorCamC.fx, priorCamC.cx, priorCamC.cy);
		}

		// Get coordinates of features in each view
		dbSimilar.lookupPixelFeats(seed.id, featsA);
		dbSimilar.lookupPixelFeats(viewB.id, featsB);
		dbSimilar.lookupPixelFeats(viewC.id, featsC);

		// Make the pixels zero centered
		BoofMiscOps.offsetPixels(featsA.toList(), -priorCamA.cx, -priorCamA.cy);
		BoofMiscOps.offsetPixels(featsB.toList(), -priorCamB.cx, -priorCamB.cy);
		BoofMiscOps.offsetPixels(featsC.toList(), -priorCamC.cx, -priorCamC.cy);

		// pre-declare memory
		matchesTriple.reset();
		matchesTriple.resize(commonIdx.size);

		// Copy coordinates into triples list
		for (int i = 0; i < commonIdx.size; i++) {
			int indexA = commonIdx.get(i);
			int indexB = table_A_to_B.data[indexA];
			int indexC = table_A_to_C.data[indexA];

			matchesTriple.get(i).setTo(featsA.get(indexA), featsB.get(indexB), featsC.get(indexC));
		}
	}

	/**
	 * Robustly estimates the trifocal tensor and extracts canonical camera matrices
	 */
	public boolean estimateProjectiveCamerasRobustly() {
		// Robustly fit trifocal tensor to 3-view
		if (!ransac.process(matchesTriple.toList()))
			return false;

		// Extract camera matrices
		TrifocalTensor model = ransac.getModelParameters();
		CommonOps_DDRM.setIdentity(P1);
		MultiViewOps.trifocalToCameraMatrices(model, P2, P3);

		inliersThreeView.reset();
		inliersThreeView.addAll(ransac.getMatchSet());
		inlierIdx.reset();
		inlierIdx.resize(inliersThreeView.size);
		for (int i = 0; i < inliersThreeView.size; i++) {
			inlierIdx.data[i] = ransac.getInputIndex(i);
		}
		return true;
	}

	/**
	 * Saves which features were used as inliers.
	 *
	 * @param inlierIdx Which features in 'commonIdx' are inliers and should be added to the set
	 * @param view Which view should be the first view in the list and have its inliers updated.
	 */
	public InlierInfo saveRansacInliers( SceneWorkingGraph.View view, DogArray_I32 inlierIdx ) {
		int viewIdx = seed == view.pview ? 0 : viewB == view.pview ? 1 : viewC == view.pview ? 2 : -1;

		int[] order = switch (viewIdx) {
			case 0 -> new int[]{0, 1, 2};
			case 1 -> new int[]{1, 0, 2};
			case 2 -> new int[]{2, 1, 0};
			default -> throw new RuntimeException("Passed in view is not any of the three expected");
		};

		int numInliers = inlierIdx.size();
		checkTrue(view.inliers.size == 0, "Inliers should not have already been set for this view");
		final InlierInfo info = view.inliers.grow();

		info.observations.reset();
		info.observations.resize(3);
		final DogArray_I32 indexesA = info.observations.get(order[0]);
		final DogArray_I32 indexesB = info.observations.get(order[1]);
		final DogArray_I32 indexesC = info.observations.get(order[2]);

		for (int inlierCnt = 0; inlierCnt < numInliers; inlierCnt++) {
			int indexA = commonIdx.get(inlierIdx.get(inlierCnt));
			indexesA.add(indexA);
			indexesB.add(table_A_to_B.data[indexA]);
			indexesC.add(table_A_to_C.data[indexA]);
		}

		info.views.resize(3);
		for (int i = 0; i < 3; i++) {
			View v = switch (i) {
				case 0 -> seed;
				case 1 -> viewB;
				case 2 -> viewC;
				default -> throw new RuntimeException("BUG");
			};
			info.views.set(order[i], v);
		}

		return info;
	}

	/**
	 * Initializes projective reconstruction from 3-views.
	 *
	 * 1) RANSAC to fit a trifocal tensor
	 * 2) Extract camera matrices that have a common projective space
	 * 3) Triangulate location of 3D homogenous points
	 *
	 * @param fixedSeed if true the seed is fixed and other views are not. If false then the inverse happens.
	 */
	public void initializeSbaSceneThreeView( boolean fixedSeed ) {
		// Initialize the 3D scene structure, stored in a format understood by bundle adjustment
		structure.initialize(3, inliersThreeView.size());

		// specify the found projective camera matrices
		dbCams.lookupCalibration(dbCams.viewToCamera(seed.id), priorCamA);
		dbCams.lookupCalibration(dbCams.viewToCamera(viewB.id), priorCamB);
		dbCams.lookupCalibration(dbCams.viewToCamera(viewC.id), priorCamC);

		// The first view is assumed to be the coordinate system's origin and is identity by definition
		structure.setView(0, fixedSeed, P1, priorCamA.width, priorCamA.height);
		structure.setView(1, !fixedSeed, P2, priorCamB.width, priorCamB.height);
		structure.setView(2, !fixedSeed, P3, priorCamC.width, priorCamC.height);

		// triangulate homogenous coordinates for each point in the inlier set
		triangulateFeatures();
	}

	/**
	 * Triangulates the location of each features in homogenous space and save to bundle adjustment scene
	 */
	protected void triangulateFeatures() {
		checkTrue(structure.views.size > 0, "Must initialize the structure first");
		checkTrue(structure.points.size == inliersThreeView.size(),
				"Number of inliers must match the number of points in the scene");

		// TODO Normalize camera matrices for better numerics?
		List<DMatrixRMaj> cameraMatrices = new ArrayList<>();
		cameraMatrices.add(P1);
		cameraMatrices.add(P2);
		cameraMatrices.add(P3);

		// need elements to be non-empty so that it can use set(). probably over optimization
		List<Point2D_F64> triangObs = new ArrayList<>();
		triangObs.add(null);
		triangObs.add(null);
		triangObs.add(null);

		Point4D_F64 X = new Point4D_F64();
		for (int i = 0; i < inliersThreeView.size(); i++) {
			AssociatedTriple t = inliersThreeView.get(i);

			triangObs.set(0, t.p1);
			triangObs.set(1, t.p2);
			triangObs.set(2, t.p3);

			// triangulation can fail if all 3 views have the same pixel value. This has been observed in
			// simulated 3D scenes
			if (triangulator.triangulate(triangObs, cameraMatrices, X)) {
				structure.points.data[i].set(X.x, X.y, X.z, X.w);
			} else {
				throw new RuntimeException("Failed to triangulate a point in the inlier set?! Handle if this is common");
			}
		}
	}

	/**
	 * Converts the set of inlier observations from the three view inliers into observations that bundle adjustment
	 * can understand
	 */
	public void initializeSbaObservationsThreeView() {
		observations.initialize(3);

		SceneObservations.View view1 = observations.getView(0);
		SceneObservations.View view2 = observations.getView(1);
		SceneObservations.View view3 = observations.getView(2);

		for (int i = 0; i < inliersThreeView.size(); i++) {
			AssociatedTriple t = inliersThreeView.get(i);

			view1.add(i, (float)t.p1.x, (float)t.p1.y);
			view2.add(i, (float)t.p2.x, (float)t.p2.y);
			view3.add(i, (float)t.p3.x, (float)t.p3.y);
		}
	}

	/**
	 * Last step is to refine the current initial estimate with bundle adjustment
	 */
	public boolean refineWithBundleAdjustment() {
		if (configConvergeSBA.maxIterations <= 0)
			return true;

		if (configScaleSBA) {
			scaler.applyScale(structure, observations);
		}

		sba.setParameters(structure, observations);
		sba.configure(configConvergeSBA.ftol, configConvergeSBA.gtol, configConvergeSBA.maxIterations);

		if (!sba.optimize(structure)) {
			return false;
		}

		if (configScaleSBA) {
			scaler.undoScale(structure, observations);
		}
		return true;
	}

	/**
	 * Creates a look up table for converting features indexes from view A to view B from the list of inliers.
	 *
	 * @param viewA (input) The view that is the src in the table
	 * @param edge (input) Edge connect view A to B, this contains the dst in the table
	 * @param table_a_to_b (output) The resulting lookup table
	 */
	public static void createTableViewAtoB( View viewA,
											Motion edge,
											DogArray_I32 table_a_to_b ) {
		table_a_to_b.resetResize(viewA.totalObservations, -1);
		boolean src_is_A = edge.src == viewA;
		for (int i = 0; i < edge.inliers.size; i++) {
			AssociatedIndex assoc = edge.inliers.get(i);
			table_a_to_b.data[src_is_A ? assoc.src : assoc.dst] = src_is_A ? assoc.dst : assoc.src;
		}
	}
}
