/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.fiducial.square.DetectFiducialSquareHamming;
import boofcv.struct.image.ImageGray;

/**
 * Wrapper around {@link DetectFiducialSquareHamming} for {@link FiducialDetector}
 *
 * @author Peter Abeles
 */
public class SquareHamming_to_FiducialDetector<T extends ImageGray<T>>
		extends SquareBase_to_FiducialDetector<T, DetectFiducialSquareHamming<T>> {
	private final double targetWidth;

	public SquareHamming_to_FiducialDetector( DetectFiducialSquareHamming<T> detector ) {
		super(detector);
		this.targetWidth = detector.description.targetWidth;
	}

	@Override
	public double getSideWidth( int which ) {
		return targetWidth;
	}

	@Override
	public double getSideHeight( int which ) {
		return targetWidth;
	}

	@Override
	public double getWidth( int which ) {
		return targetWidth;
	}
}
