/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.interest;

import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;

/**
 * Interface for automatic interest point detection in an image.  Optional support is
 * provided for scale and orientation.
 *
 * @author Peter Abeles
 */
// TODO Change scale into size in pixels. Or maybe radius in pixels.
// todo make scale a manditory feature.  even corner detectors have "scale" its the detector region size
public interface InterestPointDetector< T extends ImageBase> {

	/**
	 * Detects interest points inside the provided image.
	 *
	 * @param input Input features are detected inside of.
	 */
	void detect( T input );

	/**
	 * Returns the number of interest points found.
	 *
	 * @return Number of interest points.
	 */
	int getNumberOfFeatures();

	/**
	 * The center location of the feature inside the image.
	 *
	 * @param featureIndex The feature's index.
	 * @return Location of the feature in image pixels.
	 */
	Point2D_F64 getLocation( int featureIndex );

	/**
	 * The
	 * @param featureIndex
	 * @return
	 */
	double getScale( int featureIndex );

	double getOrientation( int featureIndex );

	public boolean hasScale();

	public boolean hasOrientation();
}
