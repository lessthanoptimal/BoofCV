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

package boofcv.factory.sfm;

import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.EnumPNP;
import boofcv.struct.Configuration;

/**
 * Stereo configuration for {@link boofcv.alg.sfm.d3.VisOdomPixelDepthPnP}
 *
 * @author Peter Abeles
 */
public class ConfigVisOdomDepthPnP implements Configuration {

	/** Configuration for Bundle Adjustment */
	public ConfigBundleAdjustment sba = new ConfigBundleAdjustment();
	/** Maximum number of iterations to do with sparse bundle adjustment. &le; 0 to disable. */
	public int maxBundleIterations = 3;
	/** Drop tracks if they have been outliers for this many frames in a row */
	public int dropOutlierTracks = 2;
	/** Maximum number of key frames it will save */
	public int maxKeyFrames = 5;
	/** Number of RANSAC iterations to perform when estimating motion using PNP */
	public int ransacIterations = 200;
	/** RANSAC inlier tolerance in Pixels */
	public double ransacInlierTol = 1.5;
	/** Seed for the random number generator used by RANSAC */
	public long ransacSeed = 0xDEADBEEF;
	/** Number of iterations to perform when refining the initial frame-to-frame motion estimate. Disable &le; 0 */
	public int pnpRefineIterations = 50;
	/** Which PNP solution to use */
	public EnumPNP pnp = EnumPNP.P3P_GRUNERT;

	@Override
	public void checkValidity() {
	}
}
