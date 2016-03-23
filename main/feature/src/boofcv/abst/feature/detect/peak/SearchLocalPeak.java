/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.peak;

import boofcv.struct.image.ImageGray;

/**
 * Interface for searching for local peaks near by a user specified point.
 *
 * @author Peter Abeles
 */
public interface SearchLocalPeak<T extends ImageGray> {

	/**
	 * Specifies the image which is to be searched
	 * @param image input image
	 */
	public void setImage( T image );

	/**
	 * How far around the center it consider when searching for the peak
	 * @param radius Search radius
	 */
	public void setSearchRadius( int radius );

	/**
	 * Initial point for the search
	 * @param x initial x-coordinate
	 * @param y initial y-coordinate
	 */
	public void search( float x , float y );

	/**
	 * Location of the found peak.  x-coordinate
	 * @return x-coordinate
	 */
	public float getPeakX();

	/**
	 * Location of the found peak.  y-coordinate
	 * @return y-coordinate
	 */
	public float getPeakY();

}
