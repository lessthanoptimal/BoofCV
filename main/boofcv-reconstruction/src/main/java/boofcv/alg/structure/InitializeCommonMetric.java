/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.MetricCameras;
import boofcv.alg.structure.PairwiseImageGraph.Motion;
import boofcv.alg.structure.PairwiseImageGraph.View;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.AssociatedIndex;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Given a set of views and a set of features which are visible in all views, estimate their metric structure.
 *
 * <ol>
 *     <li>Select the three best 3 views</li>
 *     <li>Association between all 3 views</li>
 *     <li>Robust self calibration</li>
 * </ol>
 *
 * TODO handle 2-view case
 * TODO use information for when there's a single camera
 *
 * The output is contained in SBA structure. See {@link #getStructure()} and
 * {@link #getPairwiseGraphViewByStructureIndex(int)}
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class InitializeCommonMetric implements VerbosePrint {
	/** Common algorithms for reconstructing the projective scene */
	public @Getter @Setter PairwiseGraphUtils utils;

	/**
	 * List of feature indexes for each view that are part of the inlier set. The seed view is at index 0. The other
	 * indexes are in order of 'seedConnIdx'.
	 */
	protected @Getter final DogArray<DogArray_I32> inlierIndexes =
			new DogArray<>(DogArray_I32::new, DogArray_I32::reset);

	// Retrieve a Pairwise.View given it's index in the SBA structure
	protected @Getter final FastArray<View> viewsByStructureIndex = new FastArray<>(View.class);

	//-------------- Internal workspace variables
	private final DogArray_I32 selectedTriple = new DogArray_I32(2);
	/**
	 * lookup table from feature ID in seed view to structure index. There will only be 3D features for members
	 * of the inlier set.
	 */
	protected final DogArray_I32 seedToStructure = new DogArray_I32();

	// Indexes of observations in the seed view which are common to the selected adjacent views
	DogArray_I32 seedFeatsIdx = new DogArray_I32();

	// Indicates if debugging information should be printed
	private @Nullable PrintStream verbose;

	public InitializeCommonMetric( ConfigProjectiveReconstruction configProjective ) {
		utils = new PairwiseGraphUtils(configProjective);
	}

	public InitializeCommonMetric() {
		this(new ConfigProjectiveReconstruction());
	}

	/**
	 * Computes a metric reconstruction. Reconstruction will be relative the 'seed' and only use features
	 * listed in 'common'. The list of views is taken from seed and is specified in 'motions'.
	 *
	 * @param dbSimilar (Input) Data based used to look up information on each image
	 * @param seed (Input) The seed view that will act as the origin
	 * @param seedConnIdx (Input) Indexes of motions in the seed view to use when initializing
	 * @param results (Output) Found metric reconstruction for seed and selected connected views
	 * @return true is successful or false if it failed
	 */
	public boolean metricScene( LookUpSimilarImages dbSimilar,
								LookUpCameraInfo dbCams,
								View seed, DogArray_I32 seedConnIdx,
								MetricCameras results ) {
		results.reset();
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

		// Find observations which are common between the selected views
		utils.findAllConnectedSeed(seed, selectedTriple, seedFeatsIdx);

		// Find features which are common between all three views
		utils.seed = seed;
		utils.viewB = utils.seed.connections.get(selectedTriple.data[0]).other(seed);
		utils.viewC = utils.seed.connections.get(selectedTriple.data[1]).other(seed);
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
		utils.pixelToMetric3.singleCamera = false; // TODO make this configured centrally better some how
		// TODO move out camera estimation from pairwise utils?
		if (!utils.estimateMetricCamerasRobustly()) {
			if (verbose != null) verbose.println("FAILED: Create metric views from initial triplet");
			return false;
		}

		// look up tables to trace the same feature across different data structures
		createStructureLookUpTables(seed);

		saveInlierObservationsConnectedViews();

		viewsByStructureIndex.resize(3);
		viewsByStructureIndex.set(0, utils.seed);
		viewsByStructureIndex.set(1, utils.viewB);
		viewsByStructureIndex.set(2, utils.viewC);

		// sanity check for bugs
		viewsByStructureIndex.forIdx(( i, o ) -> BoofMiscOps.checkTrue(o != null));

		// Save results
		for (int viewIdx = 0; viewIdx < 3; viewIdx++) {
			results.intrinsics.grow().setTo(utils.pixelToMetric3.listPinhole.get(viewIdx));
			// First view is implicit
			if (viewIdx != 0)
				results.motion_1_to_k.grow().setTo(utils.pixelToMetric3.listWorldToView.get(viewIdx));
		}

		return true;
	}

	/**
	 * Create look up tables to go from seed feature index to structure feature index.
	 * ransac inlier index to seed feature index
	 *
	 * Only points that are in the inlier set are part of the scene's structure.
	 */
	void createStructureLookUpTables( View viewA ) {
		final Ransac<?, ?> ransac = utils.pixelToMetric3.ransac;

		final int numInliers = ransac.getMatchSet().size();
		seedToStructure.resize(viewA.totalObservations);
		seedToStructure.fill(-1); // -1 indicates no match
		DogArray_I32 inlierToSeed = inlierIndexes.get(0);
		inlierToSeed.resize(numInliers);
		for (int i = 0; i < numInliers; i++) {
			int inputIdx = ransac.getInputIndex(i);

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
	boolean selectInitialTriplet( View seed, DogArray_I32 edgeIdxs, DogArray_I32 selected ) {
		selected.resize(2);
		double bestScore = 0; // zero is used for invalid triples
		for (int i = 0; i < edgeIdxs.size; i++) {
			int edgeI = edgeIdxs.get(i);
			View viewB = seed.connections.get(edgeI).other(seed);

			for (int j = i + 1; j < edgeIdxs.size; j++) {
				int edgeJ = edgeIdxs.get(j);
				View viewC = seed.connections.get(edgeJ).other(seed);

				double s = scoreTripleView(seed, viewB, viewC);
				if (s <= bestScore)
					continue;

				bestScore = s;
				selected.data[0] = edgeI;
				selected.data[1] = edgeJ;
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
	 * Save which observations are in the inlier set to the connected views
	 */
	private void saveInlierObservationsConnectedViews() {
		DogArray_I32 inlierToSeed = inlierIndexes.get(0);

		// Save which observations are part the inlier set
		// Now add observations for edges connected to the seed
		for (int motionIdx = 0; motionIdx < selectedTriple.size(); motionIdx++) {
			SceneObservations.View obsView = utils.observations.getView(motionIdx + 1);
			Motion m = utils.seed.connections.get(selectedTriple.get(motionIdx));
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
	 * Returns the estimated scene structure
	 */
	public SceneStructureProjective getStructure() {
		return utils.structurePr;
	}

	/**
	 * Returns the {@link View} given the index of the view in structure
	 */
	public View getPairwiseGraphViewByStructureIndex( int index ) {
		return viewsByStructureIndex.get(index);
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
