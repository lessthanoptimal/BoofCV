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

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.structure.PairwiseImageGraph.Motion;
import boofcv.alg.structure.PairwiseImageGraph.View;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.ElevateViewInfo;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.AssociatedTupleDN;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Given a set of views and a set of features which are visible in all views, estimate their structure up to a
 * projective transform. Summary of processing steps:
 *
 * <ol>
 *     <li>Select initial set of 3 views</li>
 *     <li>Association between all 3 views</li>
 *     <li>RANSAC to find Trifocal Tensor</li>
 *     <li>3 Projective from trifocal</li>
 *     <li>Triangulate features</li>
 *     <li>Find remaining projective camera matrices</li>
 *     <li>Refine with bundle adjustment</li>
 * </ol>
 *
 * The output is contained in SBA structure. See {@link #getStructure()} and
 * {@link #getPairwiseGraphViewByStructureIndex(int)}
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ProjectiveInitializeAllCommon implements VerbosePrint {

	/** Common algorithms for reconstructing the projective scene */
	public @Getter @Setter PairwiseGraphUtils utils;

	/**
	 * List of feature indexes for each view that are part of the inlier set. The seed view is at index 0. The other
	 * indexes are in order of 'seedConnIdx'.
	 */
	protected @Getter final DogArray<DogArray_I32> inlierIndexes =
			new DogArray<>(DogArray_I32::new, DogArray_I32::reset);

	// Retrieve a Pairwise.View given it's index in the SBA structure
	protected final FastArray<View> viewsByStructureIndex = new FastArray<>(View.class);

	// Indicates if debugging information should be printed
	private @Nullable PrintStream verbose;

	//-------------- Internal workspace variables
	protected final int[] selectedTriple = new int[2];
	// triangulated 3D homogenous points in seed reference frame
	protected final DogArray<Point4D_F64> points3D = new DogArray<>(Point4D_F64::new);
	// Associated pixel observations
	protected final DogArray<AssociatedPair> assocPixel = new DogArray<>(AssociatedPair::new);
	protected final ImageDimension shape = new ImageDimension();
	/**
	 * lookup table from feature ID in seed view to structure index. There will only be 3D features for members
	 * of the inlier set.
	 */
	protected final DogArray_I32 seedToStructure = new DogArray_I32();

	// Used to reassign camera IDs
	TIntIntMap dbToCamera = new TIntIntHashMap() {{
		no_entry_value = -1;
	}};

	public ProjectiveInitializeAllCommon( ConfigProjectiveReconstruction configProjective ) {
		utils = new PairwiseGraphUtils(configProjective);
	}

	public ProjectiveInitializeAllCommon() {
		this(new ConfigProjectiveReconstruction());
	}

	/**
	 * Computes a projective reconstruction. Reconstruction will be relative the 'seed' and only use features
	 * listed in 'common'. The list of views is taken from seed and is specified in 'motions'.
	 *
	 * @param dbSimilar (Input) Data based used to look up information on each image
	 * @param seed (Input) The seed view that will act as the origin
	 * @param seedFeatsIdx (Input) Indexes of common features in the seed view which are to be used.
	 * @param seedConnIdx (Input) Indexes of motions in the seed view to use when initializing
	 * @return true is successful or false if it failed
	 */
	public boolean projectiveSceneN( LookUpSimilarImages dbSimilar,
									 LookUpCameraInfo dbCams,
									 View seed, DogArray_I32 seedFeatsIdx, DogArray_I32 seedConnIdx ) {
		// Check preconditions. Exceptions are thrown since these are easily checked and shouldn't be ignored under
		// the assumption that geometry was simply bad
		checkTrue(seedFeatsIdx.size >= 6,
				"need at least 6 common features to estimate camera matrix");
		checkTrue(seed.connections.size >= seedConnIdx.size,
				"Can't have more seed connection indexes than actual connections");

		if (verbose != null)
			verbose.println("ENTER projectiveSceneN: seed='" + seed.id + "' common.size=" + seedFeatsIdx.size +
					" conn.size=" + seedConnIdx.size);

		if (seedConnIdx.size < 2) {
			if (verbose != null)
				verbose.println("2-views, a.k.a. stereo, is a special case and requires different" +
						" logic and isn't yet supported");
			return false;
		}

		// initialize data structures
		utils.dbSimilar = dbSimilar;
		utils.dbCams = dbCams;
		viewsByStructureIndex.reset();
		inlierIndexes.resetResize(1 + seedConnIdx.size);

		// find the 3 view combination with the best score
		if (!selectInitialTriplet(seed, seedConnIdx, selectedTriple)) {
			if (verbose != null) verbose.println("FAILED: Select initial triplet");
			return false;
		}

		// Find features which are common between all three views
		utils.seed = seed;
		utils.viewB = utils.seed.connections.get(selectedTriple[0]).other(seed);
		utils.viewC = utils.seed.connections.get(selectedTriple[1]).other(seed);
		utils.createThreeViewLookUpTables();
		utils.findFullyConnectedTriple(seedFeatsIdx);

		if (verbose != null) {
			verbose.println("Selected Triplet: seed='" + utils.seed.id + "' viewB='" + utils.viewB.id + "' viewC='" +
					utils.viewC.id + "' common.size=" + utils.commonIdx.size + " connections.size=" + seedConnIdx.size);
		}

		if (utils.commonIdx.isEmpty()) {
			if (verbose != null) verbose.println("FAILED: No common features found");
			return false;
		}

		// Estimate the initial projective cameras using trifocal tensor
		utils.createTripleFromCommon(verbose);
		if (!utils.estimateProjectiveCamerasRobustly()) {
			if (verbose != null) verbose.println("FAILED: Created projective from initial triplet");
			return false;
		}

		// look up tables to trace the same feature across different data structures
		createStructureLookUpTables(seed);

		// Estimate projective cameras for each view not in the original triplet
		// This is simple because the 3D coordinate of each point is already known
		if (seedConnIdx.size > 2) { // only do if more than 3 views
			initializeStructureForAllViews(dbCams, utils.ransac.getMatchSet().size(), seed, seedConnIdx);

			if (!findRemainingCameraMatrices(dbSimilar, dbCams, seed, seedConnIdx)) {
				if (verbose != null) verbose.println("FAILED: Finding remaining cameras. TODO recover from this");
				return false;
			}
		} else {
			// just optimize the three views
			utils.initializeSbaSceneThreeView(true);
			utils.initializeSbaObservationsThreeView();
			viewsByStructureIndex.resize(3);
			viewsByStructureIndex.set(0, utils.seed);
			viewsByStructureIndex.set(1, utils.viewB);
			viewsByStructureIndex.set(2, utils.viewC);
		}

		// sanity check for bugs
		viewsByStructureIndex.forIdx(( i, o ) -> BoofMiscOps.checkTrue(o != null));

		// create observation data structure for SBA
		createObservationsForBundleAdjustment(seedConnIdx);

		// Refine results with projective bundle adjustment
		return utils.refineWithBundleAdjustment();
	}

	/**
	 * Initializes the bundle adjustment structure for all views not just the initial set of 3. The seed view is
	 * view index=0. The other views are in order of `seedConnIdx` after that.
	 */
	private void initializeStructureForAllViews( LookUpCameraInfo db, int numberOfFeatures, View seed, DogArray_I32 seedConnIdx ) {
		utils.observations.initialize(1 + seedConnIdx.size);
		utils.structure.initialize(1 + seedConnIdx.size, numberOfFeatures);
		viewsByStructureIndex.resize(utils.structure.views.size, null);

		utils.triangulateFeatures();

		// Added the seed view
		db.lookupViewShape(seed.id, shape);
		utils.structure.setView(0, true, utils.P1, shape.width, shape.height);

		// Add the two views connected to it. Note that the index of these views is based on their index
		// in the seedConnIdx list
		int indexSbaViewB = 1 + seedConnIdx.indexOf(selectedTriple[0]);
		int indexSbaViewC = 1 + seedConnIdx.indexOf(selectedTriple[1]);
		checkTrue(indexSbaViewB > 0 && indexSbaViewC > 0, "indexOf() failed");

		for (int i = 0; i < 2; i++) {
			Motion motion = seed.connections.get(selectedTriple[i]);
			View view = motion.other(seed);
			db.lookupViewShape(view.id, shape);
			utils.structure.setView(
					i == 0 ? indexSbaViewB : indexSbaViewC, false,
					i == 0 ? utils.P2 : utils.P3, shape.width, shape.height);
		}

		// create lookup table
		viewsByStructureIndex.set(0, seed);
		viewsByStructureIndex.set(indexSbaViewB, utils.viewB);
		viewsByStructureIndex.set(indexSbaViewC, utils.viewC);

		// Observations for the initial three view
		SceneObservations.View view1 = utils.observations.getView(0);
		SceneObservations.View view2 = utils.observations.getView(indexSbaViewB);
		SceneObservations.View view3 = utils.observations.getView(indexSbaViewC);

		for (int i = 0; i < utils.inliersThreeView.size(); i++) {
			AssociatedTriple t = utils.inliersThreeView.get(i);

			view1.add(i, (float)t.p1.x, (float)t.p1.y);
			view2.add(i, (float)t.p2.x, (float)t.p2.y);
			view3.add(i, (float)t.p3.x, (float)t.p3.y);
		}
	}

	/**
	 * Create look up tables to go from seed feature index to structure feature index.
	 * ransac inlier index to seed feature index
	 *
	 * Only points that are in the inlier set are part of the scene's structure.
	 */
	void createStructureLookUpTables( View viewA ) {
		final int numInliers = utils.ransac.getMatchSet().size();
		seedToStructure.resize(viewA.totalObservations);
		seedToStructure.fill(-1); // -1 indicates no match
		DogArray_I32 inlierToSeed = inlierIndexes.get(0);
		inlierToSeed.resize(numInliers);
		for (int i = 0; i < numInliers; i++) {
			int inputIdx = utils.ransac.getInputIndex(i);

			// table to go from inlier list into seed feature index
			inlierToSeed.data[i] = utils.commonIdx.get(inputIdx);

			// seed feature index into the output structure index
			BoofMiscOps.checkTrue(seedToStructure.data[inlierToSeed.data[i]] == -1);
			seedToStructure.data[inlierToSeed.data[i]] = i;
		}
	}

	/**
	 * Exhaustively look at all triplets that connect with the seed view
	 *
	 * @param edgeIdxs (input) List of edges in seed it will consider
	 * @param selected (output) Indexes of the two selected edges going out of `seed`
	 */
	boolean selectInitialTriplet( View seed, DogArray_I32 edgeIdxs, int[] selected ) {
		BoofMiscOps.checkTrue(selected.length == 2);
		double bestScore = 0; // zero is used for invalid triples
		for (int i = 0; i < edgeIdxs.size; i++) {
			int edgeI = edgeIdxs.get(i);
			View viewB = seed.connections.get(edgeI).other(seed);

			for (int j = i + 1; j < edgeIdxs.size; j++) {
				int edgeJ = edgeIdxs.get(j);
				View viewC = seed.connections.get(edgeJ).other(seed);

				double s = scoreTripleView(seed, viewB, viewC);
				if (s > bestScore) {
					bestScore = s;
					selected[0] = edgeI;
					selected[1] = edgeJ;
				}
			}
		}
		return bestScore != 0;
	}

	/**
	 * Evaluates how well this set of 3-views can be used to estimate the scene's 3D structure
	 *
	 * @return higher is better. zero means worthless
	 */
	double scoreTripleView( View seedA, View viewB, View viewC ) {
		Motion motionAB = Objects.requireNonNull(seedA.findMotion(viewB));
		Motion motionAC = Objects.requireNonNull(seedA.findMotion(viewC));
		Motion motionBC = viewB.findMotion(viewC);
		if (motionBC == null)
			return 0.0;

		double score = 0.0;
		score += motionAB.score3D;
		score += motionAC.score3D;
		score += motionBC.score3D;

		return score;
	}

	/**
	 * Uses the triangulated points and observations in the root view to estimate the camera matrix for
	 * all the views which are remaining. We are assuming that outliers have already been removed.
	 *
	 * @param seedConnIdx (Input) Specifies which connections in 'seed.connections' are to be used.
	 * @return true if successful or false if not
	 */
	boolean findRemainingCameraMatrices( LookUpSimilarImages dbSimilar, LookUpCameraInfo dbCams,
										 View seed, DogArray_I32 seedConnIdx ) {
		int numInliers = inlierIndexes.get(0).size;
		BoofMiscOps.checkTrue(numInliers == utils.inliersThreeView.size());

		// Look up the 3D coordinates of features from the scene's structure previously computed
		points3D.reset(); // points in 3D
		for (int i = 0; i < utils.structure.points.size; i++) {
			utils.structure.points.data[i].get(points3D.grow());
		}

		// contains associated pairs of pixel observations
		// save a call to db by using the previously loaded points for the seed view
		assocPixel.resize(numInliers);
		for (int i = 0; i < numInliers; i++) {
			// inliers from triple RANSAC
			// each of these inliers was declared a feature in the world reference frame
			assocPixel.get(i).p1.setTo(utils.inliersThreeView.get(i).p1);
		}

		var cameraMatrix = new DMatrixRMaj(3, 4);
		for (int motionIdx = 0; motionIdx < seedConnIdx.size; motionIdx++) {
			int connectionIdx = seedConnIdx.get(motionIdx);
			// skip views already in the scene's structure
			if (connectionIdx == selectedTriple[0] || connectionIdx == selectedTriple[1])
				continue;
			Motion edge = seed.connections.get(connectionIdx);
			View viewI = edge.other(seed);

			// Lookup pixel locations of features in the connected view
			dbCams.lookupCalibration(viewI.id, utils.priorCamB);
			dbSimilar.lookupPixelFeats(viewI.id, utils.featsB);
			BoofMiscOps.offsetPixels(utils.featsB.toList(), -utils.priorCamB.cx, -utils.priorCamB.cy);

			if (!computeCameraMatrix(seed, edge, utils.featsB, cameraMatrix)) {
				if (verbose != null) verbose.println("Pose estimator failed! view='" + viewI.id + "'");
				return false; // TODO skip over this view instead
			}
			if (verbose != null) verbose.println("Expanded initial scene to include view='" + viewI.id + "'");

			//---------------------------------------------------------------------------
			// Add all the information from this view to SBA data structure
			int indexSbaView = motionIdx + 1;
			// image information and found camera matrix
			dbCams.lookupViewShape(edge.other(seed).id, shape);
			utils.structure.setView(indexSbaView, false, cameraMatrix, shape.width, shape.height);
			// observation of features
			SceneObservations.View sbaObsView = utils.observations.getView(indexSbaView);
			checkTrue(sbaObsView.size() == 0, "Must be reset to initial state first");
			for (int i = 0; i < numInliers; i++) {
				Point2D_F64 p = assocPixel.get(i).p2;
				sbaObsView.add(i, (float)p.x, (float)p.y);
			}

			viewsByStructureIndex.set(indexSbaView, viewI);
		}

		return true;
	}

	/**
	 * Computes the camera matrix between the seed view and a connected view
	 *
	 * @param seed This will be the source view. It's observations have already been added to assocPixel
	 * @param edge The edge which connects them
	 * @param featsB The features in the dst view
	 * @param cameraMatrix (Output) resulting camera matrix
	 * @return true if successful
	 */
	boolean computeCameraMatrix( View seed, Motion edge, DogArray<Point2D_F64> featsB, DMatrixRMaj cameraMatrix ) {
		DogArray_I32 inlierToSeed = inlierIndexes.get(0);
		BoofMiscOps.checkTrue(assocPixel.size == inlierToSeed.size);

		// how to convert a feature in the seed to one in viewI
		PairwiseGraphUtils.createTableViewAtoB(seed, edge, utils.table_A_to_B);

		// Get the features in the second view
		for (int i = 0; i < inlierToSeed.size; i++) {
			int seedIdx = inlierToSeed.get(i);
			int dstIdx = utils.table_A_to_B.data[seedIdx];
			// Assume that p1 from the seed view has already been set
			assocPixel.get(i).p2.setTo(featsB.get(dstIdx));
		}

		// Estimate the camera matrix given homogenous pixel observations
		if (utils.poseEstimator.processHomogenous(assocPixel.toList(), points3D.toList())) {
			cameraMatrix.setTo(utils.poseEstimator.getProjective());
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Convert observations into a format which bundle adjustment will understand
	 *
	 * @param seedConnIdx Which edges in seed to use
	 */
	protected void createObservationsForBundleAdjustment( DogArray_I32 seedConnIdx ) {
		DogArray_I32 inlierToSeed = inlierIndexes.get(0);
		// seed view + the motions
		utils.observations.initialize(inlierIndexes.size);

		// Observations for the seed view are a special case
		{
			SceneObservations.View obsView = utils.observations.getView(0);
			for (int i = 0; i < inlierToSeed.size; i++) {
				int id = inlierToSeed.data[i];
				Point2D_F64 o = utils.featsA.get(id); // featsA is never modified after initially loaded
				id = seedToStructure.data[id];
				obsView.add(id, (float)o.x, (float)o.y);
			}
		}

		// Now add observations for edges connected to the seed
		for (int motionIdx = 0; motionIdx < seedConnIdx.size(); motionIdx++) {
			SceneObservations.View obsView = utils.observations.getView(motionIdx + 1);
			Motion m = utils.seed.connections.get(seedConnIdx.get(motionIdx));
			View v = m.other(utils.seed);
			boolean seedIsSrc = m.src == utils.seed;
			utils.dbCams.lookupCalibration(utils.dbCams.viewToCamera(v.id), utils.priorCamB);
			utils.dbSimilar.lookupPixelFeats(v.id, utils.featsB);
			BoofMiscOps.offsetPixels(utils.featsB.toList(), -utils.priorCamB.cx, -utils.priorCamB.cy);

			// indicate which observation from this view contributed to which 3D feature
			DogArray_I32 connInlierIndexes = inlierIndexes.get(motionIdx + 1);
			connInlierIndexes.resize(inlierToSeed.size);

			for (int epipolarInlierIdx = 0; epipolarInlierIdx < m.inliers.size; epipolarInlierIdx++) {
				AssociatedIndex a = m.inliers.get(epipolarInlierIdx);
				// See if the feature is one of inliers computed from 3-view RANSAC
				int structId = seedToStructure.data[seedIsSrc ? a.src : a.dst];
				if (structId < 0)
					continue;
				// get the observation in this view to that feature[structId]
				connInlierIndexes.set(structId, seedIsSrc ? a.dst : a.src);
				Point2D_F64 o = utils.featsB.get(seedIsSrc ? a.dst : a.src);
				obsView.add(structId, (float)o.x, (float)o.y);
			}
		}
	}

	/**
	 * Copies results into a format that's useful for projective to metric conversion
	 *
	 * @param viewIds (Output) ID of each view
	 * @param views (Output) Shape of images in each view
	 * @param cameraMatrices (Output) Found camera matrices. view[0] is skipped since it is identity
	 * @param observations (Output) Found observations shifted to have (0,0) center
	 */
	public void lookupInfoForMetricElevation( List<String> viewIds,
											  DogArray<ElevateViewInfo> views,
											  DogArray<DMatrixRMaj> cameraMatrices,
											  DogArray<AssociatedTupleDN> observations ) {
		// Initialize all data structures to the correct size
		final int numViews = utils.structure.views.size;
		viewIds.clear();
		views.resize(numViews);
		cameraMatrices.resize(numViews - 1);
		observations.resize(inlierIndexes.get(0).size);		TIntIntMap dbToCamera = new TIntIntHashMap(){{no_entry_value = -1;}};

		// pre-allocate memory
		for (int obsIdx = 0; obsIdx < observations.size; obsIdx++) {
			observations.get(obsIdx).resize(numViews);
		}

		// Copy results from bundle adjustment data structures
		for (int viewIdx = 0; viewIdx < numViews; viewIdx++) {
			SceneStructureProjective.View pview = utils.structure.views.get(viewIdx);
			if (viewIdx != 0)
				cameraMatrices.get(viewIdx - 1).setTo(pview.worldToView);
			else
				BoofMiscOps.checkTrue(MatrixFeatures_DDRM.isIdentity(pview.worldToView, 1e-8));
			String id = viewsByStructureIndex.get(viewIdx).id;
			viewIds.add(id);

			// See if this camera has already been assigned an index
			int cameraDB = utils.dbCams.viewToCamera(id);
			int cameraIdx = dbToCamera.get(cameraDB);
			if (cameraIdx == -1) {
				// Add this camera to the map since it's unknown
				cameraIdx = dbToCamera.size();
				dbToCamera.put(cameraDB, cameraIdx);
			}

			views.get(viewIdx).setTo(pview.width, pview.height, cameraIdx);

			SceneObservations.View oview = utils.observations.views.get(viewIdx);
			BoofMiscOps.checkTrue(oview.size() == observations.size);

			for (int obsIdx = 0; obsIdx < observations.size; obsIdx++) {
				int featureIdx = oview.getPointId(obsIdx);
				BoofMiscOps.checkTrue(featureIdx != -1, "Every feature should be visible in all views");
				oview.getPixel(obsIdx, observations.get(featureIdx).get(viewIdx));
			}
		}
	}

	/**
	 * Returns the estimated scene structure
	 */
	public SceneStructureProjective getStructure() {
		return utils.structure;
	}

	/**
	 * Returns the {@link PairwiseImageGraph.View} given the index of the view in structure
	 */
	public View getPairwiseGraphViewByStructureIndex( int index ) {
		return viewsByStructureIndex.get(index);
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
