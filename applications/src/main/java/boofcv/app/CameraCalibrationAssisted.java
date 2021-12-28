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

package boofcv.app;

import boofcv.io.webcamcapture.OpenWebcamDialog;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Lets the user select the webcam then opens the assisted calibration
 *
 * @author Peter Abeles
 */
public class CameraCalibrationAssisted {
	public static void main( @Nullable String[] args ) {
		SwingUtilities.invokeLater(() -> {
			// Let the user select which camera and at what resolution
			final OpenWebcamDialog.Selection selected = OpenWebcamDialog.showDialog(null);
			if (selected == null)
				return;

			// now user the mono app from here
			var app = new CameraCalibrationMono();
			app.desiredWidth = selected.width;
			app.desiredHeight = selected.height;
			app.cameraName = selected.camera.getName();
			app.configTarget.type = null; // tells it use the default
			new Thread(app::handleWebcam).start();
		});
	}
}
