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

package boofcv.alg.structure;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.ImageDimension;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.DogArray_I32;

/**
 * Used to retrieve information about a view's camera. Can be used to figure out if the same camera captured all
 * the views
 *
 * @author Peter Abeles
 */
public class LookUpCameraInfo {

	/** Look up from view id to view index */
	public final TObjectIntMap<String> idToView = new TObjectIntHashMap<>();

	/** Lookup table from view to camera that captured the view */
	public final DogArray_I32 viewToCamera = new DogArray_I32();

	/** List of calibration information for each camera */
	public final DogArray<CameraPinholeBrown> listCalibration = new DogArray<>(() -> new CameraPinholeBrown(2));

	/** If true then a camera is known and should not be estimated */
	public final DogArray_B knownCameras = new DogArray_B();

	/**
	 * Total number of views
	 */
	public int totalViews() {
		return viewToCamera.size;
	}

	/**
	 * Total number of unique cameras
	 */
	public int totalCameras() {
		return listCalibration.size;
	}

	/**
	 * Which camera took the image at this view
	 */
	public int viewToCamera( String viewID ) {
		int viewIdx = idToView.get(viewID);
		return viewToCamera.get(viewIdx);
	}

	/**
	 * Returns the camera's calibration. This could be a crude estimate
	 *
	 * @param cameraIdx (Input) which camera
	 * @param calibration (Output) Storage for the retrieved calibration data
	 */
	public void lookupCalibration( int cameraIdx, CameraPinholeBrown calibration ) {
		calibration.setTo(listCalibration.get(cameraIdx));
	}

	public void lookupCalibration( String viewID, CameraPinholeBrown calibration ) {
		int cameraIdx = viewToCamera(viewID);
		calibration.setTo(listCalibration.get(cameraIdx));
	}

	/**
	 * Returns true if the camera's calibration is known and can be assumed ot be fixed
	 */
	public boolean isCameraKnown( int cameraIdx ) {
		return knownCameras.get(cameraIdx);
	}

	public void lookupViewShape( String viewID, ImageDimension shape ) {
		int cameraIdx = viewToCamera(viewID);
		CameraPinholeBrown camera = listCalibration.get(cameraIdx);
		shape.setTo(camera.width, camera.height);
	}

	/**
	 * Adds a default camera with no lens distortion with the specified field of view
	 *
	 * @param width (Input) Image width
	 * @param height (Input) Image height
	 * @param hfov (Input) Horizontal field-of-view in degrees
	 */
	public void addCameraCanonical( int width, int height, double hfov ) {
		BoofMiscOps.checkTrue(width > 0 && height > 0, "width and height must be more than zero");
		BoofMiscOps.checkTrue(hfov > 0, "hfov must be more than zero degrees");
		PerspectiveOps.createIntrinsic(width, height, hfov, listCalibration.grow());
	}

	/**
	 * Adds a new view and maps it to a camera
	 */
	public void addView( String viewID, int cameraIdx ) {
		idToView.put(viewID, viewToCamera.size);
		viewToCamera.add(cameraIdx);
	}
}
