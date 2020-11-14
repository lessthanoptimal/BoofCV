/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.mvs;

import boofcv.alg.sfm.structure.SceneWorkingGraph;
import boofcv.struct.image.*;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

/**
 * Creates a dense point cloud using a {@link SceneWorkingGraph} and Multi View Stereo to fuse data from multiple
 * images.
 *
 * @author Peter Abeles
 */
public class MultiViewStereoFromSceneGraph<T extends ImageGray<T>> implements VerbosePrint {

	// Select views for being fused based on area covered from neighbors
	// Only add a new disparity when it is it adds more than X fraction new compared to neighbor disparity?

	// Used to retrieve images
	final LookUpImages imageLookUp;

	final FastQueue<ViewInfo> viewScores = new FastQueue<>(ViewInfo::new, ViewInfo::reset);

	final @Getter ScoreRectifiedViewCoveragePixels scoreCoverage = new ScoreRectifiedViewCoveragePixels();

	BundleToRectificationStereoParameters computeRectification = new BundleToRectificationStereoParameters();

	CreateCloudFromDisparityImages disparityCloud;
	MultiViewToFusedDisparity computeDisparity;

	@Nullable PrintStream verbose = null;

	public MultiViewStereoFromSceneGraph( LookUpImages imageLookUp ) {
		this.imageLookUp = imageLookUp;
	}

	/**
	 * Selects views from the scene and
	 *
	 * @param scene (Input) Used to relationship between views and there parameters.
	 */
	public void process( SceneWorkingGraph scene, StereoPairGraph pairs ) {
		// Go through each view and compute score for use as a common / "left" stereo image
		scoreViewsSelectStereoPairs(scene, pairs);

		// Greedily select common views
		Collections.sort(viewScores.toList(), Comparator.comparingDouble(a -> a.score));
		while( viewScores.size>0) {
			ViewInfo info = viewScores.removeSwap(0); // TODO this will mess up the ordering
		}

		// TODO Select a subset of views to compute a disparity image for

		// TODO For each of those views compute the fused disparity image and then convert that into a point cloud

		// TODO Optionally retrieve the color of each point in the cloud
	}

	private void scoreViewsSelectStereoPairs( SceneWorkingGraph scene,
											  StereoPairGraph pairs )
	{
		Se3_F64 view1_to_view2 = new Se3_F64();
		viewScores.resize(scene.viewList.size());
		viewScores.reset();
		for (int candIdx = 0; candIdx < scene.viewList.size(); candIdx++) {
			SceneWorkingGraph.View candidate = scene.viewList.get(candIdx);
			StereoPairGraph.View node = pairs.views.get(candidate.pview.id);
			Objects.requireNonNull(node,"Can't find view in StereoPairGraph");

			ImageDimension candidateD = candidate.imageDimension;
			computeRectification.setView1(candidate.intrinsic, candidateD.width, candidateD.height);

			scoreCoverage.initialize(candidateD.width, candidateD.height, computeRectification.view1_dist_to_undist);

			for (int pairIdx = 0; pairIdx < node.pairs.size(); pairIdx++) {
				StereoPairGraph.Pair pair = node.pairs.get(pairIdx);
				SceneWorkingGraph.View v = scene.views.get(pair.id);
				Objects.requireNonNull(v,"Can't find view in SceneWorkingGraph");
				ImageDimension viewD = v.imageDimension;

				// Compute rectification then apply converage with geometric score
				computeRectification.processView2(v.intrinsic, view1_to_view2);
				scoreCoverage.addView(viewD.width, viewD.height, computeRectification.rect2, (float)pair.quality3D);
			}

			scoreCoverage.process();

			ViewInfo info = viewScores.grow();
			info.view = candidate;
			info.score = scoreCoverage.getScore();
		}
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}


	public static class ViewInfo {
		SceneWorkingGraph.View view;
		int index;
		double score;
		boolean used;

		void reset() {
			view = null;
			score = -1;
			used = false;
		}
	}

	/** Used to look up images as needed for disparity calculation. */
	public interface LookUpImages {
		<LT extends ImageGray<LT>> LT lookup( String name, ImageType<LT> type );
	}

	/** Used to capture intermediate results */
	public interface Listener {
		/**
		 * After a regular disparity image has been computed from a pair, this function is called and the results
		 * passed in
		 */
		void handlePairDisparity( String left, String right, GrayF32 disparity, GrayU8 mask,
								  DisparityParameters parameters );

		/**
		 * After a fused disparity image has been computed, this function is called and the results passed in
		 */
		void handleFusedDisparity( String name, GrayF32 disparity, GrayU8 mask, DisparityParameters parameters );
	}
}
