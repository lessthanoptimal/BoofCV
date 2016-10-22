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

import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

/**
 * Interface for detecting fiducials and estimating their 6-DOF pose.  The camera must be calibrated by
 * specifying {@link CameraPinholeRadial}.  When one or more fiducials are detected their IDs and pose
 * are returned.
 *
 * @author Peter Abeles
 */
public interface FiducialDetector<T extends ImageBase>
{
	/**
	 * Detects fiducials inside the image
	 *
	 * @param input Input image.  Not modified.
	 */
	void detect( T input );

	/**
	 * The total number of targets found
	 * @return number of targets found
	 */
	int totalFound();

	/**
	 * Returns where in the image the fiducial is.  Typically this will be the fiducial's visual center.  Note that
	 * the visual center is unlikely to be the projection of the 3D geometric center.  To find the former you need
	 * to reproject it using the found fiducialToCamera.
	 *
	 * <p>NOTE: The reprojected center might not be the same as the location returned here.</p>
	 *
	 * @param which Fiducial's index
	 * @param location (output) Storage for the transform. modified.
	 */
	void getImageLocation(int which , Point2D_F64 location );

	/**
	 * If applicable, returns the ID of the fiducial found.
	 * @param which Fiducial's index
	 * @return ID of the fiducial
	 */
	long getId( int which );

	/**
	 * Type of input image
	 */
	ImageType<T> getInputType();
}
