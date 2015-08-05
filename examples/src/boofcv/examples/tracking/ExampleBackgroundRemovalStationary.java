/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import boofcv.factory.background.ConfigBackgroundGaussian;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.gui.image.ImageBinaryPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
// TODO simplify the creation image motion estimation
// TODO Visualization.  Show input image in a window,  Difference + color in another
public class ExampleBackgroundRemovalStationary {
	public static void main(String[] args) {

		String fileName = "../data/applet/background/horse_jitter.mjpg";

		ImageType imageType = ImageType.single(ImageFloat32.class);
//		ImageType imageType = ImageType.ms(3, ImageFloat32.class);
//		ImageType imageType = ImageType.il(3, InterleavedF32.class);
//		ImageType imageType = ImageType.il(3, InterleavedU8.class);

		ConfigBackgroundGaussian configGaussian = new ConfigBackgroundGaussian(20,0.001f);
		configGaussian.initialVariance = 64;
		configGaussian.minimumDifference = 5;

		BackgroundModelStationary background =
				FactoryBackgroundModel.stationaryBasic(new ConfigBackgroundBasic(35, 0.005f), imageType);
//				FactoryBackgroundModel.stationaryGaussian(configGaussian, imageType);


		MediaManager media = DefaultMediaManager.INSTANCE;
		SimpleImageSequence video = media.openVideo(fileName, background.getImageType());

		ImageUInt8 segmented = new ImageUInt8(1,1);

		ImageBinaryPanel gui = null;

		double fps = 0;
		double alpha = 0.01; // smoothing factor for FPS

		while( video.hasNext() ) {
			ImageBase input = video.next();

			if( segmented.width != input.width ) {
				segmented.reshape(input.width,input.height);
				gui = new ImageBinaryPanel(segmented);
				ShowImages.showWindow(gui,"Detections",true);
			}

			long before = System.nanoTime();
			background.segment(input,segmented);
			background.updateBackground(input);
			long after = System.nanoTime();

			fps = (1.0-alpha)*fps + alpha*(1.0/((after-before)/1e9));

			gui.setBinaryImage(segmented);
			gui.repaint();
			System.out.println("FPS = "+fps);

			try {Thread.sleep(5);} catch (InterruptedException e) {}
		}
		System.out.println("done!");
	}
}
