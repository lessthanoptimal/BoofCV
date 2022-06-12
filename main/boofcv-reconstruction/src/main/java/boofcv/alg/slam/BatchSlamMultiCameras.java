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

package boofcv.alg.slam;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.robust.ModelMatcherMultiview;
import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.alg.structure.PairwiseImageGraph;
import boofcv.alg.structure.SceneWorkingGraph;
import boofcv.factory.geo.ConfigPnP;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.Point2D3D;
import georegression.geometry.UtilPoint4D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.*;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Batch Simultaneous Location and Mapping (SLAM) system which assumed a known multi camera system is viewing the world.
 * A feature based approach is used were first salient image features are found then matched to each other using
 * descriptors.
 *
 * @author Peter Abeles
 */
public class BatchSlamMultiCameras implements VerbosePrint {

	// Handling known baseline in multi-camera systems
	// - TLDR for now ignore this constraint
	// - TODO How to handle that the baseline between cameras in multi-camera system is assumed to be known?
	// - TODO if the location of one view is known then the location of all views from camera system is known

	// TODO force it to use simplified camera model for now?

	int countConsideredConnections = 3;

	MultiCameraSlamUtils slamUtils = new MultiCameraSlamUtils();

	ModelMatcherMultiview<Se3_F64, Point2D3D> robustPnP;

	GeneratePairwiseGraphFromMultiCameraSystem generatePairwise;

	DogArray<SeedInfo> seeds = new DogArray<>(SeedInfo::new, SeedInfo::reset);
	DogArray_B viewUsed = new DogArray_B();

	DogArray<SceneWorkingGraph> scenes = new DogArray<>(SceneWorkingGraph::new, SceneWorkingGraph::reset);

	Se3_F64 viewA_to_viewB = new Se3_F64();
	DogArray<Point2D_F64> pixelsA = new DogArray<>(Point2D_F64::new, Point2D_F64::zero);
	DogArray<Point2D_F64> pixelsB = new DogArray<>(Point2D_F64::new, Point2D_F64::zero);

	private final List<PairwiseImageGraph.View> valid = new ArrayList<>();

	MultiCameraSystem sensors;
	ViewToCamera viewToCamera;

	// If a scene as a known scale, which means the seed has a pair of views with a known extrinsic relationship
	boolean activeSceneKnownScale;

	PrintStream verbose;

	public void initialize() {
		var configRansac = new ConfigRansac();
		var configPnP = new ConfigPnP();

//		configRansac.inlierThreshold =
		robustPnP = FactoryMultiViewRobust.pnpRansac(configPnP, configRansac);
	}

	public void process( MultiCameraSystem sensors, LookUpSimilarImages similarImages ) {
		// Print verbose from utils as if it was from this class, to keep indentations down
		slamUtils.verbose = this.verbose;

		// Learn how much geometric information is available between views
		generatePairwise.process(sensors, similarImages);

		// Decide which views are preferred as seeds
		scoreViewsAsSeeds();

		// All views can be used as a seed or added to a scene
		viewUsed.resetResize(seeds.size, true);

		// Select seeds and perform reconstructions
		PairwiseImageGraph pairwise = generatePairwise.getPairwise();
		while (true) {
			SeedInfo seed = selectSeedForScene(pairwise);
			if (seed == null)
				return;

			if (!initializeNewScene(pairwise, seed, similarImages)) {
				// TODO abort if it fails X times in a row
				continue;
			}

			SceneWorkingGraph scene = scenes.getTail();

			while (scene.open.size > 0) {
				// Select the view with the most informative connections to known views in the scene
				PairwiseImageGraph.View target = selectViewToExpandInto(scene);

				// Couldn't find a valid view to add
				if (target == null)
					break;

				// Try adding this view to the scene
				if (!expandIntoView(scene, target, similarImages)) {
					continue;
				}

				// TODO sometimes refine the entire scene

			}

			// TODO refine the entire scene
		}
	}

	/**
	 * Score each view as a potential seed.
	 */
	void scoreViewsAsSeeds() {
		FastAccess<PairwiseImageGraph.View> views = generatePairwise.getPairwise().nodes;
		seeds.resetResize(views.size);
		for (int viewIdx = 0; viewIdx < views.size; viewIdx++) {
			PairwiseImageGraph.View v = views.get(viewIdx);
			SeedInfo s = seeds.get(viewIdx);
			s.viewIndex = viewIdx;
			scoreViewAsSeed(v, s);
		}
	}

	/**
	 * Scores a view as a seed based on the scores of the best N connected motion. Motions are selected which are
	 * discintive from other already selected motions
	 */
	void scoreViewAsSeed( PairwiseImageGraph.View target, SeedInfo info ) {
		// Select the motion with the best score
		double bestScore = 0;
		int bestIndex = -1;
		for (int i = 0; i < target.connections.size; i++) {
			PairwiseImageGraph.Motion m = target.connections.get(i);

			if (!m.is3D || m.score3D <= bestScore)
				continue;

			bestScore = m.score3D;
			bestIndex = i;
		}
		// Nothing was selected, this is a horrible seed
		if (bestIndex == -1)
			return;

		addNeighbor(info, target, target.connections.get(bestIndex), bestScore);

		// The remaining neighbors are selected by finding the neighbor which the best score which is the minimum
		// of the score to the target and any of the already connected. This avoids adds two very similar view
		// that happen to have a high score to the target
		for (int considerIdx = 1; considerIdx < countConsideredConnections; considerIdx++) {
			bestScore = 0;
			bestIndex = -1;
			for (int connIdx = 0; connIdx < target.connections.size; connIdx++) {
				PairwiseImageGraph.Motion m = target.connections.get(connIdx);
				PairwiseImageGraph.View mview = m.other(target);

				// Only consider motions which have not been selected and could have a better score
				if (!m.is3D || m.score3D <= bestScore || info.neighbors.contains(m))
					continue;

				double score = m.score3D;
				for (int nghIdx = 0; nghIdx < info.neighbors.size(); nghIdx++) {
					PairwiseImageGraph.View nview = info.neighbors.get(nghIdx).other(target);
					@Nullable PairwiseImageGraph.Motion m2n = mview.findMotion(nview);
					if (m2n == null)
						continue;
					if (m2n.is3D && m2n.score3D < score)
						score = m2n.score3D;
				}

				if (score <= bestScore)
					continue;

				bestScore = score;
				bestIndex = connIdx;
			}

			if (bestIndex == -1)
				return;

			addNeighbor(info, target, target.connections.get(bestIndex), bestScore);
		}
	}

	public @Nullable SeedInfo selectSeedForScene( PairwiseImageGraph pairwise ) {
		for (int seedIdx = 0; seedIdx < seeds.size; seedIdx++) {
			// See if this view can be used
			SeedInfo candidate = seeds.get(seedIdx);
			if (viewUsed.get(candidate.viewIndex))
				continue;

			// Remove views which can't be seeds
			for (int removeIdx = seedIdx; removeIdx >= 0; removeIdx--) {
				seeds.removeSwap(removeIdx);
			}

			return candidate;
		}
		return null;
	}

	public boolean initializeNewScene( PairwiseImageGraph pairwise, SeedInfo seed, LookUpSimilarImages similarImages ) {
		SceneWorkingGraph scene = scenes.grow();

		// Create a camera for every camera in the multi camera system
		for (int i = 0; i < sensors.cameras.size(); i++) {
			scene.addCamera(i);
		}

		activeSceneKnownScale = !seed.knownScale;

		PairwiseImageGraph.View pseed = pairwise.nodes.get(seed.viewIndex);

		similarImages.lookupPixelFeats(pseed.id, pixelsA);

		setCameraA(sensors.lookupCamera(pseed.id));

		if (activeSceneKnownScale) {
			SceneWorkingGraph.Camera camSeed = lookupCamera(scene, pseed);
			SceneWorkingGraph.View wseed = scene.addView(pseed, camSeed);
			SceneWorkingGraph.InlierInfo inliers = wseed.inliers.grow();
			wseed.landmarkIDs.resetResize(pseed.totalObservations, -1);

			for (int i = 0; i < seed.neighbors.size(); i++) {
				PairwiseImageGraph.Motion m = seed.neighbors.get(i);
				PairwiseImageGraph.View dst = m.other(pseed);
				if (!sensors.isStereo(pseed.id, dst.id)) {
					continue;
				}

				// Don't add a view which has already been used again
				if (viewUsed.get(dst.index))
					continue;

				SceneWorkingGraph.Camera camDst = lookupCamera(scene, dst);
				SceneWorkingGraph.View wdst = scene.addView(dst, camDst);
				wdst.landmarkIDs.resetResize(dst.totalObservations, -1);

				similarImages.lookupPixelFeats(dst.id, pixelsB);
				setCameraB(sensors.lookupCamera(dst.id));

				// Look up known extrinsics between the stereo pair
				sensors.computeSrcToDst(pseed.id, dst.id, viewA_to_viewB);

				// Triangulate common observations from inliers
				for (int inlierIdx = 0; inlierIdx < m.inliers.size; inlierIdx++) {
					// identifier for the landmark and storage for estimated location
					int landmarkID = scene.listLandmarks.size;
					Point4D_F64 location = scene.listLandmarks.grow();

					// Look up which image features have been paired for this landmark
					AssociatedIndex a = m.inliers.get(inlierIdx);

					// Get pixel observations
					Point2D_F64 pixelA = pixelsA.get(a.src);
					Point2D_F64 pixelB = pixelsB.get(a.dst);

					// Convert to pointing vectors
					if (!slamUtils.triangulate(pixelA, pixelB, viewA_to_viewB, location)) {
						scene.listLandmarks.removeTail();
						continue;
					}

					// Sanity checks
					if (!slamUtils.checkObservationAngle(viewA_to_viewB, location)) {
						scene.listLandmarks.removeTail();
						continue;
					}

					if (!slamUtils.checkReprojection(pixelA, pixelB, viewA_to_viewB, location)) {
						scene.listLandmarks.removeTail();
						continue;
					}

					// note which features these line up with
					wseed.landmarkIDs.set(a.src, landmarkID);
					wdst.landmarkIDs.set(a.dst, landmarkID);
				}

				// Add remaining unknown observations to landmark list, both views
				addUnknownLandmarks(scene, wseed);
				addUnknownLandmarks(scene, wdst);

				inliers.views.add(pseed);
				inliers.views.add(dst);

				// Mark these two views as being used
				viewUsed.set(pseed.index, true);
				viewUsed.set(dst.index, true);
				break;
			}

			// See if it failed to find a valid view. Probably a bug
			if (scene.listViews.size() == 1)
				return false;

			// Add all neighbors which are connected to the seed to the open list
			for (int i = 0; i < seed.neighbors.size(); i++) {
				PairwiseImageGraph.Motion m = seed.neighbors.get(i);
				PairwiseImageGraph.View dst = m.other(pseed);

				if (viewUsed.get(dst.index))
					continue;

				scene.open.add(dst);
			}
			return true;
		} else {
			throw new RuntimeException("Handle situation where there is no known scale");
		}
	}

	private void addUnknownLandmarks( SceneWorkingGraph scene, SceneWorkingGraph.View wseed ) {
		for (int obsIdx = 0; obsIdx < wseed.landmarkIDs.size; obsIdx++) {
			if (wseed.landmarkIDs.get(obsIdx) >= 0)
				continue;
			wseed.landmarkIDs.set(obsIdx, scene.listLandmarks.size);
			scene.listLandmarks.grow();
		}
	}

	protected @Nullable PairwiseImageGraph.View selectViewToExpandInto( SceneWorkingGraph scene ) {
		int bestIdx = -1;
		double bestScore = 0.0;
		int bestValidCount = 0;

		for (int openIdx = 0; openIdx < scene.open.size; openIdx++) {
			final PairwiseImageGraph.View pview = scene.open.get(openIdx);

			// See which views in the scene pview can connect to
			valid.clear();
			for (int connIdx = 0; connIdx < pview.connections.size; connIdx++) {
				PairwiseImageGraph.Motion m = pview.connections.get(connIdx);
				PairwiseImageGraph.View dst = m.other(pview);
				if (!m.is3D || !scene.isKnown(dst))
					continue;
				valid.add(dst);
			}

			double bestLocalScore = 0.0;
			for (int idx0 = 0; idx0 < valid.size(); idx0++) {
				PairwiseImageGraph.View viewB = valid.get(idx0);
				PairwiseImageGraph.Motion m0 = Objects.requireNonNull(pview.findMotion(viewB));

				for (int idx1 = idx0 + 1; idx1 < valid.size(); idx1++) {
					PairwiseImageGraph.View viewC = valid.get(idx1);
					PairwiseImageGraph.Motion m2 = viewB.findMotion(viewC);

					if (m2 == null || !m2.is3D)
						continue;

					PairwiseImageGraph.Motion m1 = Objects.requireNonNull(pview.findMotion(viewC));

					double s = BoofMiscOps.min(m0.score3D, m1.score3D, m2.score3D);

					bestLocalScore = Math.max(s, bestLocalScore);
				}
			}

			// strongly prefer 3 or more. Technically the above test won't check for this but in the future it will
			// so this test serves as a reminder
			if (Math.min(3, valid.size()) >= bestValidCount && bestLocalScore > bestScore) {
				bestValidCount = Math.min(3, valid.size());
				bestScore = bestLocalScore;
				bestIdx = openIdx;
			}
		}

		if (bestIdx < 0)
			return null;

		return scene.open.removeSwap(bestIdx);
	}

	/**
	 * @param scene The known scene
	 * @param ptarget The view which is to be added to the scene
	 * @return true if successful and the scene was modified. If false the scene was not modified
	 */
	protected boolean expandIntoView( SceneWorkingGraph scene, PairwiseImageGraph.View ptarget, LookUpSimilarImages similarImages ) {
		// Select the known neighbor with the most common connections to the target
		PairwiseImageGraph.Motion bestMotion = null;
		for (int midx = 0; midx < ptarget.connections.size; midx++) {
			PairwiseImageGraph.Motion m = ptarget.connections.get(midx);
			if (!scene.isKnown(m.other(ptarget))) {
				continue;
			}
			if (bestMotion == null || bestMotion.inliers.size < m.inliers.size)
				bestMotion = m;
		}

		if (bestMotion == null)
			return false;

		if (!estimateViewPose(scene, bestMotion, ptarget, similarImages))
			return false;

		assignLandmarksToObservations();

		return true;
	}

	boolean estimateViewPose(SceneWorkingGraph scene, PairwiseImageGraph.Motion bestMotion,
							 PairwiseImageGraph.View ptarget, LookUpSimilarImages similarImages) {
		// Look up information on the other view. Now referred to as view 'b'
		PairwiseImageGraph.View pknown = bestMotion.other(ptarget);
		SceneWorkingGraph.View sknown = scene.lookupView(pknown.id);

		// Look up pointing observations in each view
		similarImages.lookupPixelFeats(ptarget.id, pixelsA);
		similarImages.lookupPixelFeats(pknown.id, pixelsB);

		// Create inputs for PNP
		DogArray<Point2D3D> inputPnP = new DogArray<>(Point2D3D::new, Point2D3D::zero);
		DogArray_I32 inputIDs = new DogArray_I32();

		inputPnP.reset();
		inputIDs.reset();

		MultiCameraSystem.Camera cameraA = sensors.lookupCamera(ptarget.id);
		MultiCameraSystem.Camera cameraB = sensors.lookupCamera(pknown.id);

		setCameraA(cameraA);
		setCameraB(cameraB);

		for (int inlierIdx = 0; inlierIdx < bestMotion.inliers.size; inlierIdx++) {
			AssociatedIndex a = bestMotion.inliers.get(inlierIdx);

			int landmarkID = sknown.landmarkIDs.get(bestMotion.src == pknown ? a.src : a.dst);
			if (landmarkID < 0)
				continue;

			// get pointing vector of observation
			Point2D_F64 pixelA = pixelsA.get(bestMotion.src == ptarget ? a.src : a.dst);
			slamUtils.pixelToPointingA.compute(pixelA.x, pixelA.y, slamUtils.pointA);

			// PNP requires normalized image coordinates. It can't handle z=0 pointing vectors
			if (slamUtils.pointA.z == 0.0 || slamUtils.pointB.z == 0.0)
				continue;

			// Convert location to local coordinate system of 'sv'
			Point4D_F64 loc4 = scene.listLandmarks.get(landmarkID);

			// Discard if point is at infinity. PNP can't handle this scenario
			if (loc4.w == 0.0)
				continue;

			inputIDs.add(landmarkID);

			// Put landmark location in view-B reference frame
			// This is an attempt to prevent issues when the world gets very large
			SePointOps_F64.transform(sknown.world_to_view, loc4, slamUtils.locationB);

			// Create view-a observation and 3D location pairs
			// observations will be in normalized image coordinates
			Point2D3D input = inputPnP.grow();
			UtilPoint4D_F64.h_to_e(slamUtils.locationB, input.location);

			input.observation.x = slamUtils.pointA.x/slamUtils.pointA.z;
			input.observation.y = slamUtils.pointA.y/slamUtils.pointA.z;
		}

		// Configure intrinsics for PnP.
		// NOTE: This really should be generalized for pointing observations and homogenous points.
		// NOTE: Also having a threshold relative for each image size...
		var pinholeA = new CameraPinhole();
		var pinholeB = new CameraPinhole();

		MultiViewOps.approximatePinhole(slamUtils.pointingToPixelA, slamUtils.pixelToPointingA, Math.PI*0.9,
				slamUtils.shapeA.width, slamUtils.shapeA.height, pinholeA);
		MultiViewOps.approximatePinhole(slamUtils.pointingToPixelA, slamUtils.pixelToPointingA, Math.PI*0.9,
				slamUtils.shapeA.width, slamUtils.shapeA.height, pinholeB);

		robustPnP.setIntrinsic(0, pinholeA);
		robustPnP.setIntrinsic(1, pinholeB);

		// Estimate location using a robust version of PNP
		if (!robustPnP.process(inputPnP.toList()))
			return false;

		return true;
	}

	void assignLandmarksToObservations() {
		// TODO now that the pose of this camera is known, go through connected views and assign landmark ID's
		//      if triangulation is within tolerance. Do that until all possible assignments have been made
		// TODO if a landmark is triangulated and does not already have a known location set it to what's triangulated
		// TODO create new feature IDs for remaining unknown
	}


	private void setCameraA( MultiCameraSystem.Camera camera ) {
		slamUtils.setCameraA(camera.intrinsics, camera.shape);
	}

	private void setCameraB( MultiCameraSystem.Camera camera ) {
		slamUtils.setCameraB(camera.intrinsics, camera.shape);
	}

	private SceneWorkingGraph.Camera lookupCamera( SceneWorkingGraph scene, PairwiseImageGraph.View pview ) {
		return scene.cameras.get(sensors.lookupCamera(viewToCamera.lookup(pview.id)).index);
	}

	public void addNeighbor( SeedInfo info, PairwiseImageGraph.View target, PairwiseImageGraph.Motion m, double score ) {
		info.knownScale |= sensors.isStereo(target.id, m.other(target).id);
		info.score += score;
		info.neighbors.add(m);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> options ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}

	public static class SeedInfo implements Comparable<SeedInfo> {
		int viewIndex;
		double score;
		// This seed will have a known scale if one of the motions associated with it comes from a known stereo pair
		boolean knownScale;
		// List of motions which connect to a neighboring view used to compute the seed's score
		List<PairwiseImageGraph.Motion> neighbors = new ArrayList<>();

		public void reset() {
			viewIndex = -1;
			score = 0;
			knownScale = false;
			neighbors.clear();
		}

		/**
		 * Prefer a seed with a known scale and higher score
		 */
		@Override public int compareTo( BatchSlamMultiCameras.SeedInfo o ) {
			if (knownScale == o.knownScale) {
				return Double.compare(o.score, score);
			} else if (knownScale) {
				return 1;
			} else {
				return 0;
			}
		}
	}
}
