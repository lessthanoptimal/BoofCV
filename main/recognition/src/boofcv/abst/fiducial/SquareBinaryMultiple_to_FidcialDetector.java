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

import boofcv.alg.fiducial.BaseDetectFiducialSquare;
import boofcv.alg.fiducial.DetectFiducialSquareBinary;
import boofcv.alg.fiducial.FoundFiducial;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;


/**
 * Enables detection with more than one Binary Fiducial algorithm.
 * Useful if you want to be able to detect several things at the same time.
 *
 * @author Nathan Pahucki, <a href="mailto:npahucki@gmail.com"> npahucki@gmail.com</a>
 */
public final class SquareBinaryMultiple_to_FidcialDetector<T extends ImageSingleBand,Detector extends BaseDetectFiducialSquare<T>>
	implements FiducialDetector<T> {

	private final List<DetectFiducialSquareBinary<T>> algs;
	private double targetWidth;
	private ImageType<T> type;



	public SquareBinaryMultiple_to_FidcialDetector(List<DetectFiducialSquareBinary<T>> algs, double targetWidth) {
		if(algs.isEmpty()) throw new IllegalArgumentException("At least one DetectFiducialSquareBinary must be provided");
		// Sanity check
		final Class inputClass = algs.get(0).getInputType();
		for(DetectFiducialSquareBinary<T> alg : algs) {
			if(!inputClass.equals(alg.getInputType())) {
				throw new IllegalArgumentException("All elements in array must have same InputType");
			}
		}

		this.type = ImageType.single(algs.get(0).getInputType());
		this.algs = new ArrayList(algs); // make a copy no one can change it mid iteration
		this.targetWidth = targetWidth;

	}


	@Override
	public void detect(T input) {
		for(DetectFiducialSquareBinary<T> alg : algs) {
			alg.process(input);
		}
	}

	@Override
	public boolean computeStability(int which, double disturbance, FiducialStability results) {
		throw new RuntimeException("Not supported yet");
	}

	@Override
	public void setIntrinsic(IntrinsicParameters intrinsic) {
		for(DetectFiducialSquareBinary<T> alg : algs) {
			alg.setLengthSide(targetWidth);
			alg.configure(intrinsic, true);
		}

	}

	@Override
	public int totalFound() {
		int total = 0;
		for(DetectFiducialSquareBinary<T> alg : algs) {
			total += alg.getFound().size();
		}
		return total;
	}

	@Override
	public void getFiducialToCamera(int which, Se3_F64 fiducialToCamera) {
		fiducialToCamera.set(findByIndex(which).targetToSensor);
	}

	@Override
	public int getId(int which) {
		return findByIndex(which).index;
	}

	@Override
	public double getWidth(int which) {
		return targetWidth;
	}

	@Override
	public ImageType<T> getInputType() {
		return this.type;
	}


	private FoundFiducial findByIndex(int idx) {
		int lastIdx = 0;
		for (DetectFiducialSquareBinary<T> alg : algs) {
			int foundInCurrent = alg.getFound().size();
			if (foundInCurrent > 0 && idx < lastIdx + foundInCurrent) {
				return alg.getFound().get(idx - lastIdx);
			} else {
				lastIdx += foundInCurrent;
			}
		}
		throw new IllegalArgumentException("Invalid index " + idx);
	}

}