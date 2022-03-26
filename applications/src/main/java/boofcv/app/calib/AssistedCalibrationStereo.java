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

package boofcv.app.calib;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;

import java.awt.image.BufferedImage;

/**
 * Adaptation of {@link AssistedCalibrationMono} for a stereo image pair.
 *
 * @author Peter Abeles
 */
public class AssistedCalibrationStereo {
	/** Pixel where the divide between left and right stereo images are */
	int stereoSplitPx;

	public void init( int imageWidth, int imageHeight, int stereoSplitPx ) {

	}

	public void process( GrayF32 gray, BufferedImage image ) {
		BoofMiscOps.checkTrue(stereoSplitPx > 0, "Did you call init? stereoSplitPx not set");
	}
}
