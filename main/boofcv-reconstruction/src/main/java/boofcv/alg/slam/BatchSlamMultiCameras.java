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

import boofcv.abst.geo.Triangulate2PointingMetricH;
import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.alg.structure.PairwiseImageGraph;
import boofcv.alg.structure.SceneWorkingGraph;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.distort.Point2Transform3_F64;
import boofcv.struct.feature.AssociatedIndex;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
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

	GeneratePairwiseGraphFromMultiCameraSystem generatePairwise;

	DogArray<SeedInfo> seeds = new DogArray<>(SeedInfo::new, SeedInfo::reset);
	DogArray_B viewUsed = new DogArray_B();

	DogArray<SceneWorkingGraph> scenes = new DogArray<>(SceneWorkingGraph::new, SceneWorkingGraph::reset);

	Triangulate2PointingMetricH triangulator2 = FactoryMultiView.triangulate2PointingMetricH(null);
	Se3_F64 viewA_to_viewB = new Se3_F64();
	DogArray<Point2D_F64> pixelsA = new DogArray<>(Point2D_F64::new, Point2D_F64::zero);
	DogArray<Point2D_F64> pixelsB = new DogArray<>(Point2D_F64::new, Point2D_F64::zero);

	// Observations converting into pointing vectors
	Point3D_F64 pointA = new Point3D_F64();
	Point3D_F64 pointB = new Point3D_F64();

	private final List<PairwiseImageGraph.View> valid = new ArrayList<>();

	MultiCameraSystem sensors;
	ViewToCamera viewToCamera;

	// If a scene as a known scale, which means the seed has a pair of views with a known extrinsic relationship
	boolean activeSceneKnownScale;

	PrintStream verbose;

	public void process( MultiCameraSystem sensors, LookUpSimilarImages similarImages ) {
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
				if (!expandIntoView(scene, target)) {
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

		Point2Transform3_F64 pixelToPointingA = sensors.lookupCamera(pseed.id).intrinsics.undistortPtoS_F64();

		if (activeSceneKnownScale) {
			SceneWorkingGraph.Camera camSeed = lookupCamera(scene, pseed);
			SceneWorkingGraph.View wseed = scene.addView(pseed, camSeed);
			SceneWorkingGraph.InlierInfo inliers = wseed.inliers.grow();
			wseed.featureIDs.resetResize(pseed.totalObservations, -1);


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
				wdst.featureIDs.resetResize(dst.totalObservations, -1);

				similarImages.lookupPixelFeats(dst.id, pixelsB);
				Point2Transform3_F64 pixelToPointingB = sensors.lookupCamera(dst.id).intrinsics.undistortPtoS_F64();

				// Look up known extrinsics
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
					pixelToPointingA.compute(pixelA.x, pixelA.y, pointA);
					pixelToPointingB.compute(pixelB.x, pixelB.y, pointB);

					// Estimate 3D location in homogeneous coordinates
					if (!triangulator2.triangulate(pointA, pointB, viewA_to_viewB, location)) {
						scene.listLandmarks.removeTail();
						continue;
					}

					// note which features these line up with
					wseed.featureIDs.set(a.src, landmarkID);
					wdst.featureIDs.set(a.dst, landmarkID);
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
		for (int obsIdx = 0; obsIdx < wseed.featureIDs.size; obsIdx++) {
			if (wseed.featureIDs.get(obsIdx) >= 0)
				continue;
			wseed.featureIDs.set(obsIdx, scene.listLandmarks.size);
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
	protected boolean expandIntoView( SceneWorkingGraph scene, PairwiseImageGraph.View ptarget ) {
		List<String> knownViews = new ArrayList<>();

		// Find all known views in the scene which are connected to the target
		for (int midx = 0; midx < ptarget.connections.size; midx++) {
			PairwiseImageGraph.Motion m = ptarget.connections.get(midx);
			if (scene.isKnown(m.other(ptarget))) {
				knownViews.add(m.other(ptarget).id);
			}
		}

		// For each observation, this contains a list of feature IDs neighboring views thing it belongs to
		DogArray<DogArray_I32> listVotes = new DogArray<>(DogArray_I32::new, DogArray_I32::reset);

		// Accumulate votes for each observation
		listVotes.resetResize(ptarget.totalObservations);
		for (int midx = 0; midx < ptarget.connections.size; midx++) {
			PairwiseImageGraph.Motion m = ptarget.connections.get(midx);
			if (!scene.isKnown(m.other(ptarget))) {
				continue;
			}
			PairwiseImageGraph.View ov = m.other(ptarget);
			SceneWorkingGraph.View sv = scene.lookupView(ov.id);

		}
		// TODO Robust PNP to find pose of new view

		// TODO Triangulate features which don't have a known location

		// TODO assign a unique ID to all observations that don't have a feature ID

		// NOTE: Future might want to triangulate from local views too and compare solutions. This is done to prevent
		//       earlier mistakes from screwing up things now

		return true;
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
