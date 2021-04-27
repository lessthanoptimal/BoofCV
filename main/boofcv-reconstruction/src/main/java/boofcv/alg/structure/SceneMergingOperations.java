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

import boofcv.alg.distort.brown.RemoveBrownPtoN_F64;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.structure.MetricFromUncalibratedPairwiseGraph.PairwiseViewScenes;
import boofcv.alg.structure.MetricFromUncalibratedPairwiseGraph.ViewScenes;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.ddogleg.sorting.QuickSort_S32;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.ddogleg.util.PrimitiveArrays;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>Contains operations used to merge all the connected spawned scenes in {@link MetricFromUncalibratedPairwiseGraph}
 * into a single scene. Scale ambiguity is resolved by selecting a single view with the "best" geometric
 * information and then feeding that to {@link ResolveSceneScaleAmbiguity}.</p>
 *
 * Usage:<br>
 * First call {@link #countCommonViews} as it will initialize all data structures. Then all the other operations
 * can be called.
 *
 * @author Peter Abeles
 */
public class SceneMergingOperations implements VerbosePrint {
	/**
	 * <p>Storage for the number of common views between the scenes. Look at {@link #countCommonViews} for
	 * how these counts are stored. It's not entirely obvious</p>
	 * commonViewCounts.size == number of scenes<br>
	 * commonViewCounts[i].size == number of scenes that scene[i] has common views AND i < j.<br>
	 */
	DogArray<DogArray<SceneCommonCounts>> commonViewCounts = new DogArray<>(
			() -> new DogArray<>(SceneCommonCounts::new, SceneCommonCounts::reset), DogArray::reset);

	// Predeclare memory for sorting since this will be done a ton
	QuickSort_S32 sorter = new QuickSort_S32();

	/** Estimates the scale and SE3 transform from one scene to another */
	@Getter ResolveSceneScaleAmbiguity resolveScale = new ResolveSceneScaleAmbiguity();

	//----------------------------------------
	// Workspace for resolving the scale ambiguity between the two scenes
	List<Se3_F64> listWorldToViewSrc = new ArrayList<>();
	DogArray<RemoveBrownPtoN_F64> listIntrinsicsSrc = new DogArray<>(RemoveBrownPtoN_F64::new);
	List<Se3_F64> listWorldToViewDst = new ArrayList<>();
	DogArray<RemoveBrownPtoN_F64> listIntrinsicsDst = new DogArray<>(RemoveBrownPtoN_F64::new);
	// List of feature pixels in the zero view that are common between the two scenes
	DogArray<Point2D_F64> zeroViewPixels = new DogArray<>(Point2D_F64::new);
	// storage for image feature pixel coordinates retrieved from the database
	DogArray<Point2D_F64> dbPixels = new DogArray<>(Point2D_F64::new);
	// Conversion from zero view (view[0]) feature index into the index in the common feature list.
	// -1 if it's not int common list
	DogArray_I32 zeroFeatureToCommonIndex = new DogArray_I32();

	PrintStream verbose;

	/**
	 * Creates a sparse data structure that stores the number of views that each scene shares with all
	 * the other scenes.
	 *
	 * @param viewScenes (Input) List of all the views and the scenes that reference them
	 */
	public void countCommonViews( PairwiseViewScenes viewScenes, int numScenes ) {
		BoofMiscOps.checkTrue(viewScenes.views.size > 0, "There are no views");
		BoofMiscOps.checkTrue(numScenes > 0, "There are no scenes");

		// Initializes data structures
		commonViewCounts.reset();
		commonViewCounts.resize(numScenes);

		// Go through each view and count the number of scenes that contain that view
		for (int viewsIdx = 0; viewsIdx < viewScenes.views.size; viewsIdx++) {
			ViewScenes v = viewScenes.views.get(viewsIdx);

			// Put it in ascending order by index. This 'owner' of a count is the scene with a lower index
			v.viewedBy.sort(sorter);

			// Go through the now ordered list of scenes which viewed this view and increment the counter
			// by definition all scenes in this view share the view
			for (int srcIdx = 0; srcIdx < v.viewedBy.size; srcIdx++) {
				int sceneSrc = v.viewedBy.get(srcIdx);

				DogArray<SceneCommonCounts> countsSrc = commonViewCounts.get(sceneSrc);

				// Counts are only stored in one of the views, the view with a lower index
				for (int dstIdx = srcIdx + 1; dstIdx < v.viewedBy.size; dstIdx++) {
					int sceneDst = v.viewedBy.get(dstIdx);

					findViewCounts(countsSrc, sceneDst).counts++;
				}
			}
		}
	}

	/**
	 * Selects the two scenes with the most common views to merge together.
	 *
	 * @param selected (Output) Selected scenes to merge
	 * @return true if it could find two valid scenes to merge
	 */
	public boolean selectViewsToMerge( SelectedScenes selected ) {
		int bestCommon = 0;

		for (int sceneIndexA = 0; sceneIndexA < commonViewCounts.size; sceneIndexA++) {
			DogArray<SceneCommonCounts> list = commonViewCounts.get(sceneIndexA);
			for (int j = 0; j < list.size; j++) {
				SceneCommonCounts overlap = list.get(j);
				if (overlap.counts <= bestCommon)
					continue;

				bestCommon = overlap.counts;
				selected.sceneA = sceneIndexA;
				selected.sceneB = overlap.sceneIndex;
			}
		}

		return bestCommon > 0;
	}

	/**
	 * Returns true if the first scene should be merged into the second scene or the reverse. For now this simply
	 * looks at the number of views in each scene.
	 */
	public boolean decideFirstIntoSecond( SceneWorkingGraph scene1, SceneWorkingGraph scene2 ) {
		return scene1.listViews.size() < scene2.listViews.size();
	}

	/**
	 * Merges the 'src' scene into the 'dst' scene. All views which are not in dst already are added after applying
	 * the transform to the coordinate system.
	 *
	 * @param src (Input) The source scene which which is merged into 'dst'
	 * @param dst (Input/Output) The destination scene
	 * @param src_to_dst Known transform from the coordinate system of src to dst.
	 */
	public void mergeViews( SceneWorkingGraph src, SceneWorkingGraph dst, ScaleSe3_F64 src_to_dst ) {
		Se3_F64 src_to_view = new Se3_F64();

		// src_to_dst is between the world coordinate systems. We need dst_to_src * src_to_view
		Se3_F64 transform_dst_to_src = src_to_dst.transform.invert(null);

		for (int srcViewIdx = 0; srcViewIdx < src.listViews.size(); srcViewIdx++) {
			SceneWorkingGraph.View srcView = src.listViews.get(srcViewIdx);

			SceneWorkingGraph.View dstView;
			if (dst.views.containsKey(srcView.pview.id)) {
				// If both scenes have the same view, go with the one that has be most reliable estimate
				dstView = dst.views.get(srcView.pview.id);

				// TODO handle this later
				// Ignore the special case where one of them is in the seed set of views
				if (srcView.inliers.isEmpty() || dstView.inliers.isEmpty())
					continue;

				// For now we will go with the number of inliers as a test of quality
				boolean replace = srcView.inliers.getInlierCount() > dstView.inliers.getInlierCount();

				if (verbose != null) {
					verbose.printf("%-7s view='%s' inliers: %d vs %d src.f=%.1f dst.f=%.1f\n", replace ? "REPLACE" : "SKIP",
							srcView.pview.id, srcView.inliers.getInlierCount(), dstView.inliers.getInlierCount(),
							srcView.intrinsic.f, dstView.intrinsic.f);
				}

				// Do nothing if we are not replacing values in the dst view
				if (!replace)
					continue;
			} else {
				if (verbose != null)
					verbose.printf("%-7s view='%s' f=%.1f\n", "NEW", srcView.pview.id, srcView.intrinsic.f);
				// Create a new view in the dst scene
				dstView = dst.addView(srcView.pview);
			}

			dstView.imageDimension.setTo(srcView.imageDimension);
			dstView.intrinsic.setTo(srcView.intrinsic);
			dstView.inliers.setTo(srcView.inliers);

			// Copy the src transform since we need to modify it
			src_to_view.setTo(srcView.world_to_view);
			// Convert it to the same scale as the dst
			src_to_view.T.scale(src_to_dst.scale);
			// Apply te coordinate transform
			transform_dst_to_src.concat(src_to_view, dstView.world_to_view);
		}
	}

	/**
	 * Using the common views between the scenes, compute the average transform to go from 'src' to 'dst' coordinate
	 * system
	 */
	public boolean findTransform( LookUpSimilarImages db,
								  SceneWorkingGraph src, SceneWorkingGraph dst, ScaleSe3_F64 src_to_dst ) {
		double bestScore = 0.0;
		SceneWorkingGraph.View selectedSrc = null;
		SceneWorkingGraph.View selectedDst = null;

		// Number of common views that can be used for merging
		int totalCommon = 0;

		// Go through all the views in the src list
		for (int srcViewIdx = 0; srcViewIdx < src.listViews.size(); srcViewIdx++) {
			SceneWorkingGraph.View srcView = src.listViews.get(srcViewIdx);
			SceneWorkingGraph.View dstView = dst.views.get(srcView.pview.id);

			// Skip if the view is unknown in dst or if either of them are from the seed set
			if (dstView == null || srcView.inliers.isEmpty() || dstView.inliers.isEmpty())
				continue; // TODO this doesn't really check if seed set

			totalCommon++;

			// Evaluate the quality of 3D information
			double score = Math.min(srcView.inliers.getInlierCount(), dstView.inliers.getInlierCount());
			if (score <= bestScore)
				continue;

			bestScore = score;
			selectedSrc = srcView;
			selectedDst = dstView;
		}

		if (selectedSrc == null || selectedDst == null) {
			if (verbose != null) verbose.println("Select merge views: Failed.");
			return false;
		}

		if (verbose != null)
			verbose.println("Select merge views: candidates=" + totalCommon + " bestScore=" + bestScore +
					" id='" + selectedSrc.pview.id + "'");

		return computeSceneToSceneTransform(db, src, dst, selectedSrc, selectedDst, src_to_dst);
	}

	private boolean computeSceneToSceneTransform( LookUpSimilarImages db,
												  SceneWorkingGraph src, SceneWorkingGraph dst,
												  SceneWorkingGraph.View selectedSrc,
												  SceneWorkingGraph.View selectedDst,
												  ScaleSe3_F64 src_to_dst ) {
		// Sanity check
		BoofMiscOps.checkTrue(selectedSrc.pview == selectedDst.pview);

		if (verbose != null) printInlierViews(selectedSrc, selectedDst);

		// Get the set feature indexes for the selected view that were inliers in each scene
		DogArray_I32 zeroSrcIdx = selectedSrc.inliers.observations.get(0);
		DogArray_I32 zeroDstIdx = selectedDst.inliers.observations.get(0);

		// Find features in the target view that are common between the two scenes inlier feature sets
		int numCommon = findCommonInliers(zeroSrcIdx, zeroDstIdx, zeroFeatureToCommonIndex);

		// Load observation of common features in view[0]
		loadViewZeroCommonObservations(db, selectedSrc.imageDimension, numCommon, selectedSrc.pview.id);

		List<DogArray<Point2D_F64>> listViewPixelsSrc = getCommonFeaturePixelsViews(db, src, selectedSrc.inliers);
		List<DogArray<Point2D_F64>> listViewPixelsDst = getCommonFeaturePixelsViews(db, dst, selectedDst.inliers);

		// Load the extrinsics and convert the intrinsics into a usable format
		loadExtrinsicsIntrinsics(src, selectedSrc.inliers, listWorldToViewSrc, listIntrinsicsSrc);
		loadExtrinsicsIntrinsics(dst, selectedDst.inliers, listWorldToViewDst, listIntrinsicsDst);

		if (verbose != null) verbose.println("commonInliers.size=" + numCommon + " src.size=" + zeroSrcIdx.size +
				" dst.size=" + zeroDstIdx.size);

		// Pass in everything to the scale resolving algorithm
		resolveScale.initialize(zeroViewPixels.size);
		resolveScale.setScene1(
				( viewIdx, featureIdx, pixel ) -> {
					if (viewIdx == 0)
						pixel.setTo(zeroViewPixels.get(featureIdx));
					else
						pixel.setTo(listViewPixelsSrc.get(viewIdx - 1).get(featureIdx));
				},
				listWorldToViewSrc, (List)listIntrinsicsSrc.toList());
		resolveScale.setScene2(
				( viewIdx, featureIdx, pixel ) -> {
					if (viewIdx == 0)
						pixel.setTo(zeroViewPixels.get(featureIdx));
					else
						pixel.setTo(listViewPixelsDst.get(viewIdx - 1).get(featureIdx));
				},
				listWorldToViewDst, (List)listIntrinsicsDst.toList());

		return resolveScale.process(src_to_dst);
	}

	/**
	 * Prints debugging information about which views are in the inlier sets
	 */
	private void printInlierViews( SceneWorkingGraph.View selectedSrc, SceneWorkingGraph.View selectedDst ) {
		verbose.print("src.inliers.views = { ");
		for (int i = 0; i < selectedSrc.inliers.views.size; i++) {
			verbose.print("'" + selectedSrc.inliers.views.get(i).id + "' ");
		}
		verbose.println("}");
		verbose.print("dst.inliers.views = { ");
		for (int i = 0; i < selectedDst.inliers.views.size; i++) {
			verbose.print("'" + selectedDst.inliers.views.get(i).id + "' ");
		}
		verbose.println("}");
	}

	/**
	 * Creates the set of pixels in the target view that are common between the two scenes.
	 *
	 * @param db Storage with feature pixel coordinates
	 * @param numCommon Number of features that are common between the two scenes in this view
	 * @param viewID The ID of the view
	 */
	private void loadViewZeroCommonObservations( LookUpSimilarImages db,
												 ImageDimension imageShape,
												 int numCommon,
												 String viewID ) {
		db.lookupPixelFeats(viewID, dbPixels);

		// camera model assumes pixels have been recentered
		double cx = imageShape.width/2;
		double cy = imageShape.height/2;

		zeroViewPixels.resetResize(numCommon);
		for (int featureIdx = 0; featureIdx < zeroFeatureToCommonIndex.size; featureIdx++) {
			// See if only one of the scenes had this feature as an inlier
			int commonIdx = zeroFeatureToCommonIndex.get(featureIdx);
			if (commonIdx == -1) {
				continue;
			}
			Point2D_F64 p = zeroViewPixels.get(commonIdx);
			p.setTo(dbPixels.get(featureIdx));
			p.x -= cx;
			p.y -= cy;
		}
	}

	/**
	 * Loads information about the view's intrinsics and estimated intrinsics in the specified scene
	 *
	 * @param scene Which scene is being considered
	 * @param inliers Information on the views and inlier set used to estimate the target view
	 * @param listWorldToViewSrc (Output) Extrinsics
	 * @param listIntrinsicsSrc (Output) Intrinsics
	 */
	private void loadExtrinsicsIntrinsics( SceneWorkingGraph scene, SceneWorkingGraph.InlierInfo inliers,
										   List<Se3_F64> listWorldToViewSrc,
										   DogArray<RemoveBrownPtoN_F64> listIntrinsicsSrc ) {
		// Clear lists
		listWorldToViewSrc.clear();
		listIntrinsicsSrc.reset();

		// Go through each view and extract it's SE3 and
		for (int viewIdx = 0; viewIdx < inliers.views.size; viewIdx++) {
			PairwiseImageGraph.View pview = inliers.views.get(viewIdx);
			SceneWorkingGraph.View wview = scene.views.get(pview.id);
			BundlePinholeSimplified cam = wview.intrinsic;

			// Save the view's pose
			listWorldToViewSrc.add(wview.world_to_view);

			// Convert the intrinsics model to one that can be used to go from pixel to normalized
			RemoveBrownPtoN_F64 pixelToNorm = listIntrinsicsSrc.grow();
			pixelToNorm.setK(cam.f, cam.f, 0, 0, 0);
			pixelToNorm.setDistortion(cam.k1, cam.k2);
		}
	}

	/**
	 * Retrieves the pixel coordinates for all the other views in InlierInfo, excludes the first/target.
	 *
	 * @param db Used to look up image feature pixel coordinates
	 * @param inliers List of image features that were part of the inlier set in all the views
	 * @return List of observations in each view (expet the target) that are part of the inler and common set
	 */
	private List<DogArray<Point2D_F64>> getCommonFeaturePixelsViews( LookUpSimilarImages db,
																	 SceneWorkingGraph workingGraph,
																	 SceneWorkingGraph.InlierInfo inliers ) {
		List<DogArray<Point2D_F64>> listViewPixels = new ArrayList<>();

		// Which features are inliers in view[0] / common view
		DogArray_I32 viewZeroInlierIdx = inliers.observations.get(0);

		// View 0 = common view and is skipped here
		int numViews = inliers.observations.size;
		for (int viewIdx = 1; viewIdx < numViews; viewIdx++) {
			// Retrieve the feature pixel coordinates for this view
			db.lookupPixelFeats(inliers.views.get(viewIdx).id, dbPixels);
			// Which features are part of the inlier set
			DogArray_I32 inlierIdx = inliers.observations.get(viewIdx);
			BoofMiscOps.checkEq(viewZeroInlierIdx.size, inlierIdx.size, "Inliers count should be the same");

			// Create storage for all the pixels in each view
			DogArray<Point2D_F64> viewPixels = new DogArray<>(Point2D_F64::new);
			listViewPixels.add(viewPixels);
			viewPixels.resize(zeroViewPixels.size);

			// camera model assumes pixels have been recentered
			ImageDimension imageShape = workingGraph.views.get(inliers.views.get(viewIdx).id).imageDimension;
			double cx = imageShape.width/2;
			double cy = imageShape.height/2;

			// Add the inlier pixels from this view to the array in the correct order
			for (int idx = 0; idx < inlierIdx.size; idx++) {
				// feature ID in view[0]
				int viewZeroIdx = viewZeroInlierIdx.get(idx);

				// feature ID in the common list
				int commonFeatureIdx = zeroFeatureToCommonIndex.get(viewZeroIdx);
				if (commonFeatureIdx < 0)
					continue;

				// Copy the pixel from this view into the appropriate location
				Point2D_F64 p = viewPixels.get(commonFeatureIdx);
				p.setTo(dbPixels.get(inlierIdx.get(idx)));
				p.x -= cx;
				p.y -= cy;
			}
		}

		return listViewPixels;
	}

	/**
	 * Given the indexes of image features that were found to be inliers in each scene, find the set of image features
	 * that were selected in both scenes. The output is a look up table that takes in the feature's index and
	 * outputs -1 if the feature was not seen in both scenes or its new index in a common list
	 *
	 * @param indexesA List of features that are inliers in a scene
	 * @param indexesB List of features that are inliers in a scene
	 * @param zeroFeatureToCommonIndex Output. Look up table from feature in view[0] to index in common list
	 * @return Number of common features
	 */
	static int findCommonInliers( DogArray_I32 indexesA, DogArray_I32 indexesB,
								  DogArray_I32 zeroFeatureToCommonIndex ) {
		// Create a histogram of occurrences. Each index should appear once in each set
		int maxValue = PrimitiveArrays.max(indexesA.data, 0, indexesA.size);
		maxValue = Math.max(maxValue, PrimitiveArrays.max(indexesB.data, 0, indexesB.size));

		zeroFeatureToCommonIndex.resetResize(maxValue + 1, 0);
		indexesA.forEach(v -> zeroFeatureToCommonIndex.data[v]++);
		indexesB.forEach(v -> zeroFeatureToCommonIndex.data[v]++);

		// Convert the histogram into a look up table from feature index to a new sorted array with just the common
		// elements between the two sets
		int count = 0;
		for (int i = 0; i < maxValue; i++) {
			if (zeroFeatureToCommonIndex.data[i] == 2)
				zeroFeatureToCommonIndex.data[i] = count++;
			else
				zeroFeatureToCommonIndex.data[i] = -1;
		}
		return count;
	}

	/**
	 * Common function for either adding or removing counts from the list of common counts that involve the
	 * specified scene
	 *
	 * @param target Scene that will have its counts modified.
	 * @param views List of views + scenes that views them
	 * @param addCounts true to add and false to remove
	 */
	public void adjustSceneCounts( SceneWorkingGraph target, PairwiseViewScenes views, boolean addCounts ) {
		int amount = addCounts ? 1 : -1;

		// Go through all the views which are part of this scene
		target.listViews.forEach(( wv ) -> {
			ViewScenes v = views.getView(wv.pview);

			for (int srcIdx = 0; srcIdx < v.viewedBy.size; srcIdx++) {
				int sceneSrc = v.viewedBy.get(srcIdx);
				DogArray<SceneCommonCounts> countsSrc = commonViewCounts.get(sceneSrc);

				if (sceneSrc == target.index) {
					// the scene is the one being removed. So all the ones that come after it will be in its list
					for (int dstIdx = srcIdx + 1; dstIdx < v.viewedBy.size; dstIdx++) {
						int sceneDst = v.viewedBy.get(dstIdx);

						findViewCounts(countsSrc, sceneDst).counts += amount;
					}
				} else if (sceneSrc < target.index) {
					// This scene is before the target so it owns the counter
					findViewCounts(countsSrc, target.index).counts += amount;
				}
			}
		});
	}

	/**
	 * Removes the scene from the list of views
	 *
	 * @param target The scene that is to be removed
	 * @param viewScenes List of views and the scenes that see them
	 */
	public static void removeScene( SceneWorkingGraph target, PairwiseViewScenes viewScenes ) {
		// Go through all the views which are part of this scene
		target.listViews.forEach(( wv ) -> {
			ViewScenes v = viewScenes.getView(wv.pview);

			// Remove this view from the scene. Notice the counts are not adjusted. It's
			// assumed that its counts have already been removed
			boolean found = false;
			for (int srcIdx = 0; srcIdx < v.viewedBy.size; srcIdx++) {
				int sceneSrc = v.viewedBy.get(srcIdx);
				if (sceneSrc != target.index)
					continue;

				found = true;
				v.viewedBy.remove(srcIdx);
				break;
			}

			BoofMiscOps.checkTrue(found, "Failed sanity check!");
		});

		// reset the view so that it's clear it shouldn't be used any more
		target.reset();
	}

	/**
	 * Finds the targeted scene in the list of common counts. If it is not in the list then a new scene is added
	 */
	static SceneCommonCounts findViewCounts( DogArray<SceneCommonCounts> list, int targetScene ) {
		for (int i = 0; i < list.size; i++) {
			if (list.get(i).sceneIndex == targetScene) {
				return list.get(i);
			}
		}
		SceneCommonCounts match = list.grow();
		match.sceneIndex = targetScene;
		return match;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(verbose, configuration, resolveScale);
	}

	/**
	 * Specifies how many views two scenes have in common. The first scene will have a list of this and is
	 * implicitly the 'other' scene, which is why only one is contained here.
	 */
	static class SceneCommonCounts {
		// Index of the second scene that shares common views with the first
		public int sceneIndex;
		// Number of common views between the two scenes
		public int counts;

		public void reset() {
			sceneIndex = -1;
			counts = 0;
		}
	}

	/**
	 * Specifies which two scenes should be merged together
	 */
	public static class SelectedScenes {
		public int sceneA;
		public int sceneB;
	}
}
