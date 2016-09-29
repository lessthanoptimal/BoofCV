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

package boofcv.demonstrations.distort;

import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates recentering of equirectangular images
 *
 * @author Peter Abeles
 */
public class EquirectangularRecenteringApp<T extends ImageBase> extends DemonstrationBase<T> {

	// todo add controls which allow lat and lon to be adjusted
	ImagePanel panelImage;

	public EquirectangularRecenteringApp(List<?> exampleInputs, ImageType<T> imageType) {
		super(exampleInputs, imageType);

//		panelImage
	}

	@Override
	public void processImage(BufferedImage buffered, T input) {

	}

	public static void main(String[] args) {

		ImageType type = ImageType.pl(3, GrayU8.class);

		// todo add actual real images
		List<PathLabel> examples = new ArrayList<PathLabel>();
		examples.add(new PathLabel("WildCat", UtilIO.pathExample("tracking/wildcat_robot.mjpeg")));
		examples.add(new PathLabel("Tree", UtilIO.pathExample("tracking/tree.mjpeg")));
		examples.add(new PathLabel("Book", UtilIO.pathExample("tracking/track_book.mjpeg")));

		EquirectangularRecenteringApp app = new EquirectangularRecenteringApp(examples,type);

		app.openFile(new File(examples.get(0).getPath()));

		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app, "Tracking Rectangle",true);

	}
}
