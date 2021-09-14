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

package boofcv.demonstrations.calibration;

import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;

import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * Launches calibration application with example data
 *
 * @author Peter Abeles
 */
public class CalibrateStereoPlanarDemo {
	public static void main( String[] args ) {
		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess");

		java.util.List<String> leftImages = UtilIO.listByPrefix(directory, "left", null);
		List<String> rightImages = UtilIO.listByPrefix(directory, "right", null);

		SwingUtilities.invokeLater(() -> {
			var app = new CalibrateStereoPlanarApp();

			app.window = ShowImages.showWindow(app, "Planar Stereo Calibration", true);
			app.window.setJMenuBar(app.menuBar);

			app.checkDefaultTarget(new File(directory));
			app.setMenuBarEnabled(false);
			new Thread(() -> app.process(leftImages, rightImages)).start();
		});
	}
}
