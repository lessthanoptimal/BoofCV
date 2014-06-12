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

package boofcv.examples.features;

import boofcv.abst.flow.DenseOpticalFlow;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.flow.FactoryDenseOpticalFlow;
import boofcv.gui.PanelGridPanel;
import boofcv.gui.feature.VisualizeOpticalFlow;
import boofcv.gui.image.AnimatePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * Demonstration of how to compute the dense optical flow between two images.  Dense optical flow of an image
 * describes how each pixel moves from one image to the next.  The results is visualized in a color image.  The
 * color indicates the direction of motion and the intensity the magnitude.
 *
 * @author Peter Abeles
 */
public class ExampleDenseOpticalFlow {

	public static void main(String[] args) {
		MediaManager media = DefaultMediaManager.INSTANCE;

//		String fileName0 = "../data/applet/denseflow/dogdance07.png";
//		String fileName1 = "../data/applet/denseflow/dogdance08.png";

		String fileName0 = "../data/applet/denseflow/Urban2_07.png";
		String fileName1 = "../data/applet/denseflow/Urban2_08.png";

//		String fileName0 = "../data/applet/denseflow/Grove2_07.png";
//		String fileName1 = "../data/applet/denseflow/Grove2_09.png";

		DenseOpticalFlow<ImageFloat32> denseFlow =
//				FactoryDenseOpticalFlow.flowKlt(null, 6, ImageFloat32.class, null);
//				FactoryDenseOpticalFlow.region(null,ImageFloat32.class);
//				FactoryDenseOpticalFlow.hornSchunck(20, 1000, ImageFloat32.class);
//				FactoryDenseOpticalFlow.hornSchunckPyramid(null,ImageFloat32.class);
				FactoryDenseOpticalFlow.broxWarping(null, ImageFloat32.class);

		BufferedImage buff0 = media.openImage(fileName0);
		BufferedImage buff1 = media.openImage(fileName1);

		ImageFloat32 full = new ImageFloat32(buff0.getWidth(),buff0.getHeight());

		// Dense optical flow is very computationally expensive.  Just process the image at 1/2 resolution
		ImageFloat32 previous = new ImageFloat32(full.width/2,full.height/2);
		ImageFloat32 current = new ImageFloat32(previous.width,previous.height);
		ImageFlow flow = new ImageFlow(previous.width,previous.height);

		ConvertBufferedImage.convertFrom(buff0,full);
		DistortImageOps.scale(full, previous, TypeInterpolate.BILINEAR);
		ConvertBufferedImage.convertFrom(buff1, full);
		DistortImageOps.scale(full,current, TypeInterpolate.BILINEAR);

		// compute dense motion
		denseFlow.process(previous, current, flow);

		// Visualize the results
		PanelGridPanel gui = new PanelGridPanel(1,2);

		BufferedImage converted0 = new BufferedImage(current.width,current.height,BufferedImage.TYPE_INT_RGB);
		BufferedImage converted1 = new BufferedImage(current.width,current.height,BufferedImage.TYPE_INT_RGB);
		BufferedImage visualized = new BufferedImage(current.width,current.height,BufferedImage.TYPE_INT_RGB);

		ConvertBufferedImage.convertTo(previous, converted0, true);
		ConvertBufferedImage.convertTo(current, converted1, true);
		VisualizeOpticalFlow.colorized(flow, 10, visualized);

		AnimatePanel animate = new AnimatePanel(150,converted0,converted1);
		gui.add(animate);
		gui.add(visualized);
		animate.start();

		ShowImages.showWindow(gui,"Dense Optical Flow");
	}
}
