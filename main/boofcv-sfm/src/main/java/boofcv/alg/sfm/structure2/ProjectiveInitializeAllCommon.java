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

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.Motion;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.assertBoof;

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
public class ProjectiveInitializeAllCommon implements VerbosePrint {

	/** Common algorithms for reconstructing the projective scene */
	@Getter	@Setter	PairwiseGraphUtils utils = new PairwiseGraphUtils(new ConfigProjectiveReconstruction());

	/**
	 * List of feature indexes in the seed view that are the inliers from robust model matching. This is what was
	 * used to estimate all the camera matrices and
	 */
	protected @Getter final GrowQueue_I32 inlierToSeed = new GrowQueue_I32();

	protected final List<View> viewsByStructureIndex = new ArrayList<>();

	// Indicates if debugging information should be printed
	private PrintStream verbose;

	//-------------- Internal workspace variables
	protected final int[] selectedTriple = new int[2];
	// triangulated 3D homogenous points in seed reference frame
	protected final FastQueue<Point4D_F64> points3D = new FastQueue<>(Point4D_F64::new);
	// Associated pixel observations
	protected final FastQueue<AssociatedPair> assocPixel = new FastQueue<>(AssociatedPair::new);
	protected final ImageDimension shape = new ImageDimension();
	/**
	 * lookup table from feature ID in seed view to structure index. There will only be 3D features for members
	 * of the inlier set.
	 */
	protected final GrowQueue_I32 seedToStructure = new GrowQueue_I32();

	/**
	 * Computes a projective reconstruction. Reconstruction will be relative the 'seed' and only use features
	 * listed in 'common'. The list of views is taken from seed and is specified in 'motions'.
	 *
	 * @param db (Input) Data based used to look up information on each image
	 * @param seed (Input) The seed view that will act as the origin
	 * @param seedFeatsIdx (Input) Indexes of common features in the seed view which are to be used.
	 * @param seedConnIdx (Input) Indexes of motions in the seed view to use when initializing
	 * @return true is successful or false if it failed
	 */
	public boolean projectiveSceneN(LookupSimilarImages db ,
									View seed , GrowQueue_I32 seedFeatsIdx , GrowQueue_I32 seedConnIdx ) {
		// Check preconditions. Exceptions are thrown since these are easily checked and shouldn't be ignored under
		// the assumption that geometry was simply bad
		assertBoof(seedFeatsIdx.size>=6,
				"need at least 6 common features to estimate camera matrix");
		assertBoof(seedConnIdx.size>=2,
				"2-views, a.k.a. stereo, is a special case and requires different logic and isn't yet supported");
		assertBoof(seed.connections.size >= seedConnIdx.size,
				"Can't have more seed connection indexes than actual connections");

		// initialize data structures
		utils.db = db;
		viewsByStructureIndex.clear();
		viewsByStructureIndex.add(seed);

		// find the 3 view combination with the best score
		if( !selectInitialTriplet(seed,seedConnIdx,selectedTriple))
			return false;

		// Find features which are common between all three views
		utils.seed = seed;
		utils.viewB = utils.seed.connections.get(selectedTriple[0]).other(seed);
		utils.viewC = utils.seed.connections.get(selectedTriple[1]).other(seed);
		utils.createThreeViewLookUpTables();
		utils.findCommonFeatures(seedFeatsIdx);

		// Estimate the initial projective cameras using trifocal tensor
		utils.createTripleFromCommon();
		if( !utils.estimateProjectiveCamerasRobustly() )
			return false;

		// look up tables to trace the same feature across different data structures
		createStructureLookUpTables(seed);

		utils.initializeSbaSceneThreeView();
		utils.initializeSbaObservationsThreeView();

		// Estimate projective cameras for each view not in the original triplet
		// This is simple because the 3D coordinate of each point is already known
		if( seedConnIdx.size > 2 ) { // only do if more than 3 views
			initializeStructureForAllViews(db, utils.ransac.getMatchSet().size(), seed, seedConnIdx);

			if (!findRemainingCameraMatrices(db, seed, seedFeatsIdx, seedConnIdx))
				return false;
		} else {
			viewsByStructureIndex.clear();
			viewsByStructureIndex.add(seed);
			viewsByStructureIndex.add(utils.viewB);
			viewsByStructureIndex.add(utils.viewC);
		}

		// create observation data structure for SBA
		createObservationsForBundleAdjustment(seedConnIdx);

		// Refine results with projective bundle adjustment
		return utils.refineWithBundleAdjustment();
	}

	/**
	 * Initializes the bundle adjustment structure for all views not just the initial set of 3. The seed view is
	 * view index=0. The other views are in order of `seedConnIdx` after that.
	 */
	private void initializeStructureForAllViews(LookupSimilarImages db, int numberOfFeatures, View seed, GrowQueue_I32 seedConnIdx) {
		utils.structure.initialize(1+seedConnIdx.size,numberOfFeatures);
		db.lookupShape(seed.id,shape);
		utils.structure.setView(0,true, utils.P1,shape.width,shape.height);
		viewsByStructureIndex.clear();
		viewsByStructureIndex.add(seed);
		for (int i = 0; i < 2; i++) {
			Motion motionAB = seed.connections.get(selectedTriple[i]);
			View viewB = motionAB.other(seed);
			db.lookupShape(viewB.id,shape);
			utils.structure.setView(1+seedConnIdx.indexOf(selectedTriple[i]),
					false, i==0?utils.P2:utils.P3,shape.width,shape.height);
			viewsByStructureIndex.add(viewB);
		}
	}

	/**
	 * Create look up tables to go from seed feature index to structure feature index.
	 *                                       ransac inlier index to seed feature index
	 *
	 * Only points that are in the inlier set are part of the scene's structure.
	 */
	void createStructureLookUpTables(View viewA) {
		final int numInliers = utils.ransac.getMatchSet().size();
		seedToStructure.resize(viewA.totalObservations);
		seedToStructure.fill(-1); // -1 indicates no match
		inlierToSeed.resize(numInliers);
		for (int i = 0; i < numInliers; i++) {
			int inputIdx = utils.ransac.getInputIndex(i);

			// table to go from inlier list into seed feature index
			inlierToSeed.data[i] = utils.commonIdx.get(inputIdx);
			// seed feature index into the output structure index
			seedToStructure.data[inlierToSeed.data[i]] = i;
		}
	}

	/**
	 * Exhaustively look at all triplets that connect with the seed view
	 * @param edgeIdxs (input) List of edges in seed it will consider
	 * @param selected (output) Indexes of the two selected edges going out of `seed`
	 */
	boolean selectInitialTriplet( View seed , GrowQueue_I32 edgeIdxs , int[] selected ) {
		assertBoof(selected.length==2);
		double bestScore = 0; // zero is used for invalid triples
		for (int i = 0; i < edgeIdxs.size; i++) {
			int edgeI = edgeIdxs.get(i);
			View viewB = seed.connections.get(edgeI).other(seed);

			for (int j = i+1; j < edgeIdxs.size; j++) {
				int edgeJ = edgeIdxs.get(j);
				View viewC = seed.connections.get(edgeJ).other(seed);

				double s = scoreTripleView(seed,viewB,viewC);
				if( s > bestScore ) {
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
	 * @return higher is better. zero means worthless
	 */
	double scoreTripleView(View seedA, View viewB , View viewC ) {
		Motion motionAB = seedA.findMotion(viewB);
		Motion motionAC = seedA.findMotion(viewC);
		Motion motionBC = viewB.findMotion(viewC);
		if( motionBC == null )
			return 0;

		double score = 0;
		score += utils.scoreMotion.score(motionAB);
		score += utils.scoreMotion.score(motionAC);
		score += utils.scoreMotion.score(motionBC);

		return score;
	}

	/**
	 * Uses the triangulated points and observations in the root view to estimate the camera matrix for
	 * all the views which are remaining. We are assuming that outliers have already been removed.
	 *
	 * @return true if successful or false if not
	 */
	boolean findRemainingCameraMatrices(LookupSimilarImages db, View seed,
										GrowQueue_I32 seedFeatsIdx, GrowQueue_I32 seedConnIdx) {
		points3D.reset(); // points in 3D
		for (int i = 0; i < utils.structure.points.size; i++) {
			utils.structure.points.data[i].get(points3D.grow());
		}

		// contains associated pairs of pixel observations
		// save a call to db by using the previously loaded points
		assocPixel.reset();
		for (int i = 0; i < inlierToSeed.size; i++) {
			// inliers from triple RANSAC
			// each of these inliers was declared a feature in the world reference frame
			assocPixel.grow().p1.set(utils.matchesTriple.get(i).p1);
		}

		var cameraMatrix = new DMatrixRMaj(3,4);
		for (int motionIdx = 0; motionIdx < seedConnIdx.size; motionIdx++) {
			int connectionIdx = seedConnIdx.get(motionIdx);
			// skip views already in the scene's structure
			if( connectionIdx == selectedTriple[0] || connectionIdx == selectedTriple[1])
				continue;
			Motion edge = seed.connections.get(connectionIdx);
			View viewI = edge.other(seed);

			// Lookup pixel locations of features in the connected view
			db.lookupPixelFeats(viewI.id,utils.featsB);

			if ( !computeCameraMatrix(seed, seedFeatsIdx, edge,utils.featsB,cameraMatrix) ) {
				if( verbose != null ) verbose.println("Pose estimator failed! motionIdx="+motionIdx);
				return false;
			}
			db.lookupShape(edge.other(seed).id,shape);
			utils.structure.setView(motionIdx+1,false,cameraMatrix,shape.width,shape.height);
			viewsByStructureIndex.add(viewI);
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
	boolean computeCameraMatrix(View seed, GrowQueue_I32 seedFeatsIdx,
								Motion edge, FastQueue<Point2D_F64> featsB, DMatrixRMaj cameraMatrix ) {
		// how to convert a feature in the seed to one in viewI
		PairwiseGraphUtils.createTableViewAtoB(seed, edge, utils.table_A_to_B);

		// Get the features in the second view
		for (int i = 0; i < seedFeatsIdx.size; i++) {
			int seedIdx = seedFeatsIdx.get(i);
			int dstIdx = utils.table_A_to_B.data[seedIdx];
			// Assume that p1 from the seed view has already been set
			assocPixel.get(i).p2.set( featsB.get(dstIdx) );
		}

		// Estimate the camera matrix given homogenous pixel observations
		if( utils.poseEstimator.processHomogenous(assocPixel.toList(), points3D.toList()) ) {
			cameraMatrix.set(utils.poseEstimator.getProjective());
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Convert observations into a format which bundle adjustment will understand
	 * @param seedConnIdx Which edges in seed to use
	 */
	protected void createObservationsForBundleAdjustment(GrowQueue_I32 seedConnIdx) {
		// seed view + the motions
		utils.observations.initialize(seedConnIdx.size+1);

		// Observations for the seed view are a special case
		{
			SceneObservations.View obsView = utils.observations.getView(0);
			for (int i = 0; i < inlierToSeed.size; i++) {
				int id = inlierToSeed.data[i];
				Point2D_F64 o = utils.featsA.get(id); // featsA is never modified after initially loaded

				id = seedToStructure.data[id];
				obsView.add(id, (float) o.x, (float) o.y);
			}
		}

		// Now add observations for edges connected to the seed
		for (int motionIdx = 0; motionIdx < seedConnIdx.size(); motionIdx++) {
			SceneObservations.View obsView = utils.observations.getView(motionIdx+1);
			Motion m = utils.seed.connections.get(seedConnIdx.get(motionIdx));
			View v = m.other(utils.seed);
			boolean seedIsSrc = m.src == utils.seed;
			utils.db.lookupPixelFeats(v.id,utils.featsB);
			for (int epipolarInlierIdx = 0; epipolarInlierIdx < m.inliers.size; epipolarInlierIdx++) {
				AssociatedIndex a = m.inliers.get(epipolarInlierIdx);
				// See if the feature is one of inliers computed from 3-view RANSAC
				int structId = seedToStructure.data[seedIsSrc?a.src:a.dst];
				if( structId < 0 )
					continue;
				Point2D_F64 o = utils.featsB.get(seedIsSrc?a.dst:a.src);
				obsView.add(structId,(float)o.x,(float)o.y);
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
	 * Returns the {@link PairwiseImageGraph2.View} given the index of the view in structure
	 */
	public View getPairwiseGraphViewByStructureIndex( int index ) {
		return viewsByStructureIndex.get(index);
	}

	@Override
	public void setVerbose(@Nullable PrintStream out, @Nullable Set<String> configuration) {
		this.verbose = out;
	}
}
