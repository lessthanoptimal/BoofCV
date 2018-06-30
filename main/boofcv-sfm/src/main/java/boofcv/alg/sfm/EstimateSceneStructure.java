/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm;

import boofcv.abst.geo.bundle.BundleAdjustmentObservations;
import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.struct.image.ImageBase;
import org.ddogleg.struct.Stoppable;

/**
 * Provides an initial estimate of a scenes structure for {@link boofcv.abst.geo.bundle.BundleAdjustment}.
 *
 * @author Peter Abeles
 */
public interface EstimateSceneStructure<T extends ImageBase<T>> extends Stoppable {

	/**
	 * Adds a camera with unknown intrinsic parameters.
	 *
	 * @param cameraName Camera identifier.
	 */
	void addCamera( String cameraName );

	/**
	 * Adds a camera with known intrinsic parameters.
	 * @param cameraName Identifier for this camera.
	 * @param intrinsic If null then camera intrinsics is assumed to be unknown
	 * @param width Camera's width and height
	 * @param height Camera's width and height
	 */
	void addCamera( String cameraName , LensDistortionNarrowFOV intrinsic , int width, int height );

	void add( T image , String cameraName );

	boolean estimate();

	/**
	 * Returns the scene structure. Camera models will not be specified since that requires additional information
	 * than is already available
	 *
	 * @return scene
	 */
	BundleAdjustmentSceneStructure getSceneStructure();

	/**
	 * Observations from each view
	 * @return observations
	 */
	BundleAdjustmentObservations getObservations();

	/**
	 * Forgets all added views and cameras
	 */
	void reset();
}
