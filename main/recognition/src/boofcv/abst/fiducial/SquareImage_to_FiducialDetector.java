/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import georegression.struct.se.Se3_F64;

/**
 * Wrapper around {@link boofcv.alg.fiducial.DetectFiducialSquareImage} for {@link FiducialDetector}
 *
 * @author Peter Abeles
 */
public class SquareImage_to_FiducialDetector<T extends ImageSingleBand>
	implements FiducialDetector<T>
{
	DetectFiducialSquareImage<T> alg;

	double targetWidth;
	ImageType<T> type;

	public SquareImage_to_FiducialDetector(DetectFiducialSquareImage<T> alg, double targetWidth) {
		this.alg = alg;
		this.targetWidth = targetWidth;
		this.type = ImageType.single(alg.getInputType());
	}

	/**
	 * Add a new target to the list
	 */
	public void addTarget( T target , double threshold ) {
		alg.addImage(target,threshold);
	}

	@Override
	public void detect(T input) {
		alg.process(input);
	}

	@Override
	public void setIntrinsic(IntrinsicParameters intrinsic) {
		alg.configure(targetWidth, intrinsic);
	}

	@Override
	public int totalFound() {
		return alg.getFound().size;
	}

	@Override
	public void getFiducialToWorld(int which, Se3_F64 fiducialToSensor ) {
		fiducialToSensor.set(alg.getFound().get(which).targetToSensor);
	}

	@Override
	public int getId( int which ) {
		return alg.getFound().get(which).index;
	}

	@Override
	public ImageType<T> getInputType() {
		return type;
	}

	public double getTargetWidth() {
		return targetWidth;
	}
}
