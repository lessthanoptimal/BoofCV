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

package boofcv.alg.sfm.structure;

import boofcv.alg.mvs.CreateCloudFromDisparityImages;
import boofcv.alg.mvs.DisparityParameters;
import boofcv.alg.mvs.MultiViewToFusedDisparity;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
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

	@Nullable PrintStream verbose = null;

	// Used to retrieve images
	final LookUpImages imageLookUp;

	CreateCloudFromDisparityImages disparityCloud;
	MultiViewToFusedDisparity computeDisparity;

	public MultiViewStereoFromSceneGraph( LookUpImages imageLookUp ) {
		this.imageLookUp = imageLookUp;
	}

	/**
	 * Selects views from the scene and
	 *
	 * @param scene (Input) Used to relationship between views and there parameters.
	 */
	public void process( SceneWorkingGraph scene ) {
		// TODO go through each view and compute score for use as a "left" stereo image

		// TODO compute total area covered by stereo pairs.
		// Alternative score for each view: (dot project of +z axis)*(area covered)*(baseline length)

		// TODO Select a subset of views to compute a disparity image for

		// TODO For each of those views compute the fused disparity image and then convert that into a point cloud

		// TODO Optionally retrieve the color of each point in the cloud
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
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
