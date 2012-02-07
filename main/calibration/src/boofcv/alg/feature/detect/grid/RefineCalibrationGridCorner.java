/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.grid;

import boofcv.struct.image.ImageFloat32;

import java.util.List;

/**
 * Interface for computing sub-pixel accurate corners given a set of corners with are within a few
 * pixels of the true corners
 *
 * @author Peter Abeles
 */
public interface RefineCalibrationGridCorner {

	/**
	 * Refines the initial corner estimates in the blobs with a sub-pixel estimate.  The sub-pixel
	 * estimates are stored in each {@link SquareBlob}.
	 *
	 * @param squares List of square blobs whose corner estimates need to be refined. Modified
	 * @param image Original image being processed.
	 */
	public void refine( List<SquareBlob> squares , ImageFloat32 image );
}
