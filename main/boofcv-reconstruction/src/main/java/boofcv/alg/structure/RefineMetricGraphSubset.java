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

import boofcv.abst.geo.bundle.MetricBundleAdjustmentUtils;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;

/**
 * Used to refine only part of a {@link SceneWorkingGraph}. All the views which are to be optimized are first
 * specified. Then (optionally) views which are fixed and considered known are specified.
 *
 * 1) Specifiy the subset by calling setSubset()
 * 2) Mark views as known, if applicable
 * 3) Call process to optimize
 *
 * Internally it changes the coordinate system from the scene's world to view[0] and rescales it so the largest
 * translation has a norm of 1. This is to improve optimization performance which works best with numbers close to 1.0.
 *
 * @author Peter Abeles
 */
public class RefineMetricGraphSubset implements VerbosePrint {

	/** Used to refine the scene */
	@Getter RefineMetricWorkingGraph refiner = new RefineMetricWorkingGraph();

	// The sub scene that's going to be optimized
	@Getter SceneWorkingGraph subgraph = new SceneWorkingGraph();

	// look up table to go from the (original) src scene to the subset/copy
	Map<String, SceneWorkingGraph.View> srcToCpy = new HashMap<>();
	// Look up table that indicates which views in the subgraph are fixed and should not be optimized
	DogArray_B viewsFixed = new DogArray_B();

	// Reference to the passed in list of views
	final List<SceneWorkingGraph.View> srcViews = new ArrayList<>();

	// Transform from view0 to the world frame
	@Getter Se3_F64 local_to_world = new Se3_F64();
	@Getter Se3_F64 world_to_local = new Se3_F64();
	// scale factor from local to world
	@Getter double scaleLocalToWorld;

	PrintStream verbose;

	List<CameraPinholeBrown> cameraPriors = new ArrayList<>();

	// Internal workspace
	Se3_F64 tmp = new Se3_F64();

	public RefineMetricGraphSubset() {
		// Turn off view specific information by default since there can be a lot of views while merging
		refiner.verboseViewInfo = false;
	}

	/**
	 * Creates a sub-graph from a single view and add all its inlier sets. This will also require adding the
	 * views referenced in the inlier sets
	 */
	public void setSubset( SceneWorkingGraph src, SceneWorkingGraph.View srcView) {
		cameraPriors.clear();
		srcViews.clear();
		srcViews.add(srcView);

		viewsFixed.resetResize(srcViews.size(), false);
		srcToCpy.clear();

		// First copy the view's intrinsics/Intrinsics
		subgraph.reset();

		copyIntrinsicsExtrinsics(srcView);
		for (int infoIdx = 0; infoIdx < srcView.inliers.size; infoIdx++) {
			copyViewAndInlierSet(src, srcView, infoIdx);
		}

		// Sanity checks
		BoofMiscOps.checkEq(cameraPriors.size(), subgraph.listViews.size());
	}

	/**
	 * Creates a subset of the scene using the provided views. If a view references other views not in this list
	 * in its inlier sets then those views are added to the subview but marked as known so that they aren't updated
	 *
	 * @param src The scene
	 * @param srcViews The views in the scene which compose the sub-scene
	 */
	public void setSubset( SceneWorkingGraph src, List<SceneWorkingGraph.View> srcViews ) {
		cameraPriors.clear();
		this.srcViews.clear();
		this.srcViews.addAll(srcViews);

		copySubGraph(src, srcViews);
		rescaleLocalCoordinateSystem();

		if (verbose != null)
			verbose.printf("subviews.size=%d subgraph.size=%d scale=%.2e\n",
					srcViews.size(), subgraph.listViews.size(), scaleLocalToWorld);
	}

	/**
	 * Copies the subgraph of the source scene into a new scene.
	 */
	private void copySubGraph( SceneWorkingGraph src, List<SceneWorkingGraph.View> srcViews ) {
		// Mark all views as dynamic
		viewsFixed.resetResize(srcViews.size(), false);
		srcToCpy.clear();

		// First copy the view's intrinsics/Intrinsics
		subgraph.reset();
		for (int listIdx = 0; listIdx < srcViews.size(); listIdx++) {
			copyIntrinsicsExtrinsics(srcViews.get(listIdx));
		}

		// Copy the inlier sets. If a view is not in the subset, add it so we can triangulate the points
		for (int listIdx = 0; listIdx < srcViews.size(); listIdx++) {
			SceneWorkingGraph.View srcView = srcViews.get(listIdx);

			BoofMiscOps.checkTrue(!srcView.inliers.isEmpty(), "BUG no inliers");

			for (int infoIdx = 0; infoIdx < srcView.inliers.size; infoIdx++) {
				copyViewAndInlierSet(src, srcView, infoIdx);
			}
		}

		// Sanity checks
		BoofMiscOps.checkEq(cameraPriors.size(), subgraph.listViews.size());
	}

	private void copyViewAndInlierSet( SceneWorkingGraph src, SceneWorkingGraph.View srcView, int infoIdx ) {
		SceneWorkingGraph.View cpyView = srcToCpy.get(srcView.pview.id);

		BoofMiscOps.checkTrue(!srcView.inliers.isEmpty(), "BUG no inliers");

		SceneWorkingGraph.InlierInfo srcInfo = srcView.inliers.get(infoIdx);
		SceneWorkingGraph.InlierInfo cpyInfo = cpyView.inliers.grow();

		for (int infoViewsIdx = 0; infoViewsIdx < srcInfo.views.size; infoViewsIdx++) {
			PairwiseImageGraph.View pview = srcInfo.views.get(infoViewsIdx);

			SceneWorkingGraph.View infoView = srcToCpy.get(pview.id);
			if (infoView == null) {
				// Add a new view to the subgraph. Mark it as fixed since the user isn't interested in it
				copyIntrinsicsExtrinsics(src.lookupView(pview.id));
				viewsFixed.add(true);
			}

			// Copy everything over
			cpyInfo.views.add(pview);
			cpyInfo.scoreGeometric = srcInfo.scoreGeometric;
			cpyInfo.observations.grow().setTo(srcInfo.observations.get(infoViewsIdx));

			// NOTE: Could just reference the original inlier sets once all new views have been added
		}
	}

	private void copyIntrinsicsExtrinsics( SceneWorkingGraph.View srcView ) {
		SceneWorkingGraph.View cpyView = subgraph.addView(srcView.pview);

		cpyView.world_to_view.setTo(srcView.world_to_view);
		cpyView.intrinsic.setTo(srcView.intrinsic);
		cpyView.priorCamera.setTo(srcView.priorCamera);

		cameraPriors.add(cpyView.priorCamera);

		BoofMiscOps.checkTrue(null == srcToCpy.put(srcView.pview.id, cpyView));
	}

	/**
	 * Makes view[0] the original and determines what the local scale should be
	 */
	void rescaleLocalCoordinateSystem() {
		// Make view[0] the origin
		world_to_local.setTo(subgraph.listViews.get(0).world_to_view);
		world_to_local.invert(local_to_world);

		scaleLocalToWorld = 0.0;

		for (int i = 0; i < subgraph.listViews.size(); i++) {
			Se3_F64 world_to_view = subgraph.listViews.get(i).world_to_view;
			local_to_world.concat(world_to_view, tmp);
			world_to_view.setTo(tmp);
			scaleLocalToWorld = Math.max(scaleLocalToWorld, tmp.T.norm());
		}

		// Rescale translation
		for (int i = 0; i < subgraph.listViews.size(); i++) {
			subgraph.listViews.get(i).world_to_view.T.divide(scaleLocalToWorld);
		}
	}

	/**
	 * Specifies the scene as being known.
	 */
	public void setViewKnown( String id ) {
		viewsFixed.set(subgraph.views.get(id).index, true);
	}

	/**
	 * Optimizes the subscene specified earlier and copies over the results
	 *
	 * @return true if it finished without issuez
	 */
	public boolean process( LookUpSimilarImages db ) {
		// Refine the scene and mark views as known
		if (!refiner.process(db, subgraph, this::markKnownParameters))
			return false;

		// copy the results back over into the original views
		for (int listIdx = 0; listIdx < srcViews.size(); listIdx++) {
			SceneWorkingGraph.View srcView = srcViews.get(listIdx);
			SceneWorkingGraph.View cpyView = srcToCpy.get(srcView.pview.id);

			// Skip if it's static and can't be optimized
			if (viewsFixed.get(cpyView.index))
				continue;

			localToGlobal(cpyView.world_to_view, srcView.world_to_view);
			srcView.intrinsic.setTo(cpyView.intrinsic);
		}

		return true;
	}

	/**
	 * Undoes the scale and coordinate system change
	 */
	protected void localToGlobal( Se3_F64 cpy_to_view, Se3_F64 src_to_view ) {
		tmp.setTo(cpy_to_view);
		tmp.T.scale(scaleLocalToWorld);
		world_to_local.concat(tmp, src_to_view);
	}

	/**
	 * If a view is known then mark its extrinsics and intrinsics as known
	 */
	protected void markKnownParameters( MetricBundleAdjustmentUtils utils ) {
		BoofMiscOps.checkEq(viewsFixed.size, utils.structure.views.size);

		for (int viewIdx = 0; viewIdx < viewsFixed.size; viewIdx++) {
			// skip if not fixed
			if (!viewsFixed.get(viewIdx))
				continue;
			utils.structure.motions.get(viewIdx).known = true;
			utils.structure.cameras.get(viewIdx).known = true;
		}
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(out, configuration, refiner);
	}
}
