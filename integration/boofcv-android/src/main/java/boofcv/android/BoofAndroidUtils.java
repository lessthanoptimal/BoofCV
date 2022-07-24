/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.android;

import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.util.Size;
import boofcv.android.camera2.CameraID;

import java.util.ArrayList;
import java.util.List;

/**
 * Miscelaneous utility functions useful with Android development
 *
 * @author Peter Abeles
 */
public class BoofAndroidUtils {
	/**
	 * Computes a transform so that the camera image will fille the displau view, or be centered inside.
	 *
	 * @return true if successful
	 */
	public static boolean videoToDisplayMatrix( int cameraWidth, int cameraHeight, int cameraRotation,
												int displayWidth, int displayHeight, int displayRotation,
												boolean stretchToFill, Matrix imageToView ) {
		// Compute transform from bitmap to view coordinates
		int rotatedWidth = cameraWidth;
		int rotatedHeight = cameraHeight;

		int offsetX = 0, offsetY = 0;

		boolean needToRotateView = (0 == displayRotation || 180 == displayRotation) !=
				(cameraRotation == 0 || cameraRotation == 180);

		if (needToRotateView) {
			rotatedWidth = cameraHeight;
			rotatedHeight = cameraWidth;
			offsetX = (rotatedWidth - rotatedHeight)/2;
			offsetY = (rotatedHeight - rotatedWidth)/2;
		}

		imageToView.reset();
		float scale = Math.min(
				(float)displayWidth/rotatedWidth,
				(float)displayHeight/rotatedHeight);
		if (scale == 0) {
			return false;
		}

		imageToView.postRotate(-displayRotation + cameraRotation, cameraWidth/2, cameraHeight/2);
		imageToView.postTranslate(offsetX, offsetY);
		imageToView.postScale(scale, scale);
		if (stretchToFill) {
			imageToView.postScale(
					displayWidth/(rotatedWidth*scale),
					displayHeight/(rotatedHeight*scale));
		} else {
			imageToView.postTranslate(
					(displayWidth - rotatedWidth*scale)/2,
					(displayHeight - rotatedHeight*scale)/2);
		}
		return true;
	}

	/**
	 * Selects the camera resolution from the list of possible values. By default it picks the
	 * resolution which best fits the texture's aspect ratio. If there's a tie the area is
	 * maximized.
	 *
	 * @param widthTexture Width of the texture the preview is displayed inside of. <= 0 if no view
	 * @param heightTexture Height of the texture the preview is displayed inside of. <= 0 if no view
	 * @param resolutions array of possible resolutions
	 * @return index of the resolution
	 */
	public static int selectAspectRatio( int widthTexture, int heightTexture, Size[] resolutions ) {
		int bestIndex = -1;
		double bestAspect = Double.MAX_VALUE;
		double bestArea = 0;

		double textureAspect = widthTexture > 0 ? widthTexture/(double)heightTexture : 0;

		for (int i = 0; i < resolutions.length; i++) {
			Size s = resolutions[i];
			int width = s.getWidth();
			int height = s.getHeight();

			double aspectScore = widthTexture > 0 ? Math.abs(width - height*textureAspect)/width : 1;

			if (aspectScore < bestAspect) {
				bestIndex = i;
				bestAspect = aspectScore;
				bestArea = width*height;
			} else if (Math.abs(aspectScore - bestArea) <= 1e-8) {
				bestIndex = i;
				double area = width*height;
				if (area > bestArea) {
					bestArea = area;
				}
			}
		}

		return bestIndex;
	}

	/**
	 * Finds all cameras, including physical cameras that are part of a logical camera.
	 */
	public static List<CameraID> getAllCameras( CameraManager manager ) throws CameraAccessException {
		List<CameraID> allCameras = new ArrayList<>();
		for (String id : manager.getCameraIdList()) {
			allCameras.add(new CameraID(id));
			CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);

			// getPhysicalCameraIds() does not exist in older android devices
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P)
				continue;

			int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
			for (int i = 0; i < capabilities.length; i++) {
				if (capabilities[i] != CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) {
					continue;
				}
				for (String physicalID : characteristics.getPhysicalCameraIds()) {
					allCameras.add(new CameraID(physicalID, id));
				}
				break;
			}
		}
		return allCameras;
	}
}
