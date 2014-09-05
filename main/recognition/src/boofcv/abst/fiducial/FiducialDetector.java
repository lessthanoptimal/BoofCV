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

import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.se.Se3_F64;

/**
 * Interface for detecting fiducials.  If the {@link boofcv.struct.calib.IntrinsicParameters} specifies lens
 * distortion then it will be automatically removed and if there is no lens distortion then it will skip that step.
 *
 * @author Peter Abeles
 */
public interface FiducialDetector<T extends ImageBase>
{
	/**
	 * Call to detect the fiducial inside the image.  Must call
	 * {@link #setIntrinsic(boofcv.struct.calib.IntrinsicParameters)} first.
	 * @param input Input image.  Not modified.
	 */
	public void detect( T input );

	/**
	 * Specifies the intrinsic camera parameters.  Allows for the euclidean geometry of be extracted from
	 * a single image
	 *
	 * @param intrinsic The camera's intrinsic parmeters
	 */
	public void setIntrinsic( IntrinsicParameters intrinsic );

	/**
	 * The total number of targets found
	 * @return number of targets found
	 */
	public int totalFound();

	/**
	 * Used to retrieve the transformation from the fiducial's reference frame to the sensor's refernece frame.
	 *
	 * @param which Fiducial's index
	 * @param fiducialToSensor (output) Storage for the transform. modified.
	 */
	public void getFiducialToWorld(int which, Se3_F64 fiducialToSensor );

	/**
	 * If applicable, returns the ID of the fiducial found.
	 * @param which Fiducial's index
	 * @return ID of the fiducial
	 */
	public int getId( int which );

	/**
	 * Type of input image
	 */
	public ImageType<T> getInputType();
}
