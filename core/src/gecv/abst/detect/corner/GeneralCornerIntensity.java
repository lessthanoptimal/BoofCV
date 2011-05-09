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

package gecv.abst.detect.corner;

import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

/**
 * Extracts corners from a the image and or its gradient.  This is a generalized interface and lacks some of the functionality
 * of more specialized classes.
 *
 * @see gecv.alg.detect.corner
 * @see gecv.abst.detect.extract
 *
 * @author Peter Abeles
 */
public interface GeneralCornerIntensity<I extends ImageBase,D extends ImageBase > {

	/**
	 *
	 * @param image
	 * @param derivX
	 * @param derivY
	 */
	public void process( I image , D derivX , D derivY );

	public ImageFloat32 getIntensity();

	/**
	 * Optional: Returns a list of candidate locations for corners.  All other pixels are assumed to not be corners.
	 *
	 * @return List of potential corners.
	 */
	public QueueCorner getCandidates();

	/**
	 * If the image gradient is required for calculations.
	 *
	 * @return true if the image gradient is required.
	 */
	public boolean getRequiresGradient();

	/**
	 * If true a list of candidate corners is returned.
	 */
	public boolean hasCandidates();
}
