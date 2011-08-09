/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.abst.detect.interest;

import gecv.struct.image.ImageBase;
import jgrl.struct.point.Point2D_I32;

/**
 * Interface for automatic interest point detection in an image.  Provides optional
 * support for additional point features.
 *
 * @author Peter Abeles
 */
public interface InterestPointDetector< T extends ImageBase> {

	/**
	 * Detects and returns a list of interest points in the input image.
	 *
	 * @param input Input image.
	 * @return List of interest points.
	 */
	void detect( T input );

	int getNumberOfFeatures();

	Point2D_I32 getLocation( int featureIndex );

	double getScale( int featureIndex );

	double getOrientation( int featureIndex );

	public boolean hasScale();

	public boolean hasOrientation();
}
