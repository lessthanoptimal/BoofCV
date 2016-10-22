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

package boofcv.abst.fiducial;

import boofcv.alg.fiducial.square.DetectFiducialSquareImage;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

/**
 * Wrapper around {@link DetectFiducialSquareImage} for {@link FiducialDetector}
 *
 * @author Peter Abeles
 */
public class SquareImage_to_FiducialDetector<T extends ImageGray>
	extends SquareBase_to_FiducialDetector<T,DetectFiducialSquareImage<T>>
{
	DetectFiducialSquareImage<T> alg;

	public SquareImage_to_FiducialDetector(DetectFiducialSquareImage<T> alg) {
		super(alg);
		this.alg = alg;
	}

	/**
	 * Add a new pattern to be detected.  This function takes in a raw gray scale image and thresholds it.
	 *
	 * @param pattern Gray scale image of the pattern
	 * @param threshold Threshold used to convert it into a binary image
	 * @param lengthSide Length of a side on the square in world units.
	 */
	public void addPatternImage(T pattern, double threshold, double lengthSide) {
		GrayU8 binary = new GrayU8(pattern.width,pattern.height);
		GThresholdImageOps.threshold(pattern,binary,threshold,false);
		alg.addPattern(binary, lengthSide);
	}

	/**
	 * Add a new pattern to be detected.
	 *
	 * @param binary Binary image of the pattern.  0 = black, 1 = white.
	 * @param lengthSide Length of a side on the square in world units.
	 */
	public void addPatternBinary(GrayU8 binary, double lengthSide) {
		alg.addPattern(binary, lengthSide);
	}

	@Override
	public double getWidth(int which) {
		int index = (int)alg.getFound().get(which).id;
		return alg.getTargets().get(index).lengthSide;
	}
}
