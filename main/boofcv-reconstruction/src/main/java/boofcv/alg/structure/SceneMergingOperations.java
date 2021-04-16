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

import boofcv.alg.structure.MetricFromUncalibratedPairwiseGraph.PairwiseViewScenes;
import boofcv.alg.structure.MetricFromUncalibratedPairwiseGraph.ViewScenes;
import boofcv.misc.BoofMiscOps;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.AverageRotationMatrix_F64;
import org.ddogleg.sorting.QuickSort_S32;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

/**
 * <p>Contains operations used to merge all the connected spawned scenes in {@link MetricFromUncalibratedPairwiseGraph}
 * into a single scene.</p>
 *
 * First call  {@link #countCommonViews} as it will initialize all data structures. Then all the other operations
 * can be called.
 *
 * @author Peter Abeles
 */
public class SceneMergingOperations {
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

	// These are used to compute the average rigid body transform between two views
	AverageRotationMatrix_F64 averageRotation = new AverageRotationMatrix_F64();
	DogArray<DMatrixRMaj> listRotation = new DogArray<>(() -> new DMatrixRMaj(3, 3));
	Point3D_F64 sumTranslation = new Point3D_F64();

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

		// Go through each view and count the common views between the scenes
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
		return scene1.workingViews.size() < scene2.workingViews.size();
	}

	/**
	 * Merges the 'src' scene into the 'dst' scene. All views which are not in dst already are added after applying
	 * the transform to the coordinate system.
	 *
	 * @param src (Input) The source scene which which is merged into 'dst'
	 * @param dst (Input/Output) The destination scene
	 * @param src_to_dst Known transform from the coordinate system of src to dst.
	 */
	public static void mergeViews( SceneWorkingGraph src, SceneWorkingGraph dst, Se3_F64 src_to_dst ) {
		for (int srcViewIdx = 0; srcViewIdx < src.workingViews.size(); srcViewIdx++) {
			SceneWorkingGraph.View srcView = src.workingViews.get(srcViewIdx);

			// If it already has this view skip it
			if (dst.views.containsKey(srcView.pview.id))
				continue;

			SceneWorkingGraph.View dstView = dst.addView(srcView.pview);

			dstView.imageDimension.setTo(srcView.imageDimension);
			dstView.intrinsic.setTo(srcView.intrinsic);
			dstView.inliers.setTo(srcView.inliers);
			srcView.world_to_view.concat(src_to_dst, dstView.world_to_view);
		}
	}

	/**
	 * Using the common views between the scenes, compute the average transform to go from 'src' to 'dst' coordinate
	 * system
	 */
	public boolean findTransformSe3( SceneWorkingGraph src, SceneWorkingGraph dst, Se3_F64 src_to_dst ) {

		// We will sum up the translation to find the average
		sumTranslation.setTo(0, 0, 0);

		// "Average" rotation is a bit more complex. This only works well if the rotations are all similar
		listRotation.reset();

		// Go through all the views in the src list
		for (int srcViewIdx = 0; srcViewIdx < src.workingViews.size(); srcViewIdx++) {
			SceneWorkingGraph.View srcView = src.workingViews.get(srcViewIdx);

			// If this view is not common, skip it
			if (!dst.views.containsKey(srcView.pview.id))
				continue;

			SceneWorkingGraph.View dstView = dst.views.get(srcView.pview.id);

			double dx = dstView.world_to_view.T.x - srcView.world_to_view.T.x;
			double dy = dstView.world_to_view.T.y - srcView.world_to_view.T.y;
			double dz = dstView.world_to_view.T.z - srcView.world_to_view.T.z;

			System.out.println("    Delta Translation (" + dx+" , "+dy+" , "+dz+" )");
			if (!srcView.inliers.isEmpty() && !dstView.inliers.isEmpty()) {
				System.out.println("      Inliers src=" + srcView.inliers.getInlierCount() +
						" dst=" + dstView.inliers.getInlierCount());
			}

			// Compute the transform from src to dst and add it to the average calculation
			sumTranslation.x += dstView.world_to_view.T.x - srcView.world_to_view.T.x;
			sumTranslation.y += dstView.world_to_view.T.y - srcView.world_to_view.T.y;
			sumTranslation.z += dstView.world_to_view.T.z - srcView.world_to_view.T.z;

			CommonOps_DDRM.multTransA(srcView.world_to_view.R, dstView.world_to_view.R, listRotation.grow());
		}

		if (!averageRotation.process(listRotation.toList(), src_to_dst.R))
			return false;

		src_to_dst.T.x = sumTranslation.x/listRotation.size;
		src_to_dst.T.y = sumTranslation.y/listRotation.size;
		src_to_dst.T.z = sumTranslation.z/listRotation.size;

		return true;
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
		target.workingViews.forEach(( wv ) -> {
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
		target.workingViews.forEach(( wv ) -> {
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
