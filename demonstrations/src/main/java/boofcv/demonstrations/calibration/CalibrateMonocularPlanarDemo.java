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
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Launches calibration application with example data
 *
 * @author Peter Abeles
 */
public class CalibrateMonocularPlanarDemo {
	public static void main( String[] args ) {
		List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("chessboard", UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess")));
		examples.add(new PathLabel("square", UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Square")));
		examples.add(new PathLabel("circle hexagonal", UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_CircleHexagonal")));
		examples.add(new PathLabel("circle regular", UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_CircleRegular")));
		examples.add(new PathLabel("fisheye chessboard", UtilIO.pathExample("calibration/fisheye/chessboard")));
		examples.add(new PathLabel("fisheye square grid", UtilIO.pathExample("calibration/fisheye/square_grid")));

		SwingUtilities.invokeLater(() -> {
			var app = new CalibrateMonocularPlanarApp();
			app.addExamples(examples);

			app.window = ShowImages.showWindow(app, "Monocular Planar Calibration", true);
			app.window.setJMenuBar(app.menuBar);

			new Thread(() -> app.processDirectory(new File(examples.get(0).path[0]))).start();
		});
	}
}
