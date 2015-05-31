/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.fiducial.DetectFiducialSquareImage;
import boofcv.struct.image.ImageSingleBand;

/**
 * Wrapper around {@link boofcv.alg.fiducial.DetectFiducialSquareImage} for {@link FiducialDetector}
 *
 * @author Peter Abeles
 */
public class SquareImage_to_FiducialDetector<T extends ImageSingleBand>
	extends BaseSquare_FiducialDetector<T,DetectFiducialSquareImage<T>>
{
	public SquareImage_to_FiducialDetector(DetectFiducialSquareImage<T> alg) {
		super(alg);
	}

	/**
	 * Add a new target to the list.
	 *
	 * @param target Gray scale image of the target
	 * @param threshold Threshold used to convert it into a binary image
	 * @param lengthSide Length of a side on the square in world units.
	 */
	public void addTarget( T target , double threshold , double lengthSide ) {
		alg.addImage(target, threshold, lengthSide);
	}

	@Override
	public double getWidth(int which) {
		int index = alg.getFound().get(which).index;
		return alg.getTargets().get(index).lengthSide;
	}
}
