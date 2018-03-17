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

package boofcv.examples.tracking;

import boofcv.alg.background.BackgroundModelStationary;
import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.ConfigBackgroundGmm;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImageGridPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.awt.image.BufferedImage;

/**
 * Example showing how to perform background modeling when the camera is assumed to be stationary.  This scenario
 * can be computed much faster than the moving camera case and depending on the background model can some times produce
 * reasonable results when the camera has a little bit of jitter.
 *
 * @author Peter Abeles
 */
public class ExampleBackgroundRemovalStationary {
	public static void main(String[] args) {

		String fileName = UtilIO.pathExample("background/street_intersection.mp4");
//		String fileName = UtilIO.pathExample("background/rubixfire.mp4"); // dynamic background
//		String fileName = UtilIO.pathExample("background/horse_jitter.mp4"); // degraded performance because of jitter
//		String fileName = UtilIO.pathExample("tracking/chipmunk.mjpeg"); // Camera moves.  Stationary will fail here

		// Comment/Uncomment to switch input image type
		ImageType imageType = ImageType.single(GrayF32.class);
//		ImageType imageType = ImageType.il(3, InterleavedF32.class);
//		ImageType imageType = ImageType.il(3, InterleavedU8.class);

		ConfigBackgroundGmm configGmm = new ConfigBackgroundGmm();

		// Comment/Uncomment to switch algorithms
		BackgroundModelStationary background =
				FactoryBackgroundModel.stationaryBasic(new ConfigBackgroundBasic(35, 0.005f), imageType);
//				FactoryBackgroundModel.stationaryGmm(configGmm, imageType);

		MediaManager media = DefaultMediaManager.INSTANCE;
		SimpleImageSequence video =
				media.openVideo(fileName, background.getImageType());
//				media.openCamera(null,640,480,background.getImageType());

		// Declare storage for segmented image.  1 = moving foreground and 0 = background
		GrayU8 segmented = new GrayU8(video.getNextWidth(),video.getNextHeight());

		BufferedImage visualized = new BufferedImage(segmented.width,segmented.height,BufferedImage.TYPE_INT_RGB);
		ImageGridPanel gui = new ImageGridPanel(1,2);
		gui.setImages(visualized, visualized);

		ShowImages.showWindow(gui, "Static Scene: Background Segmentation", true);

		double fps = 0;
		double alpha = 0.01; // smoothing factor for FPS

		while( video.hasNext() ) {
			ImageBase input = video.next();

			long before = System.nanoTime();
			background.updateBackground(input,segmented);
			long after = System.nanoTime();

			fps = (1.0-alpha)*fps + alpha*(1.0/((after-before)/1e9));

			VisualizeBinaryData.renderBinary(segmented, false, visualized);
			gui.setImage(0, 0, (BufferedImage)video.getGuiImage());
			gui.setImage(0, 1, visualized);
			gui.repaint();
			System.out.println("FPS = "+fps);

			try {Thread.sleep(5);} catch (InterruptedException e) {}
		}
		System.out.println("done!");
	}
}
