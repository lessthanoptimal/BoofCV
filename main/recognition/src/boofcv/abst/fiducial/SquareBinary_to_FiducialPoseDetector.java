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

import boofcv.alg.fiducial.DetectFiducialSquareBinary;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageSingleBand;

/**
 * Wrapper around {@link boofcv.alg.fiducial.DetectFiducialSquareBinary} for {@link FiducialPoseDetector}
 *
 * @author Peter Abeles
 */
public class SquareBinary_to_FiducialPoseDetector<T extends ImageSingleBand>
	extends BaseSquare_FiducialPoseDetector<T,DetectFiducialSquareBinary<T>>
{
	double targetWidth;

	public SquareBinary_to_FiducialPoseDetector(DetectFiducialSquareBinary<T> alg, double targetWidth) {
		super(alg);
		this.targetWidth = targetWidth;
	}

	@Override
	public void setIntrinsic(IntrinsicParameters intrinsic) {
		alg.setLengthSide(targetWidth);
		super.setIntrinsic(intrinsic);
	}

	@Override
	public double getWidth(int which) {
		return targetWidth;
	}
}
