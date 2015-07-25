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
import boofcv.alg.background.stationary.BackgroundStationaryBasic_SB;
import boofcv.alg.misc.ImageStatistics;
import boofcv.gui.image.ImageBinaryPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.wrapper.images.LoadFileImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
// TODO simplify the creation image motion estimation
// TODO Visualization.  Show input image in a window,  Difference + color in another
public class ExampleBackgroundRemovalStationary {
	public static void main(String[] args) {


		BackgroundModelStationary background =
				new BackgroundStationaryBasic_SB(0.005f,30,ImageFloat32.class);
//				new BackgroundStationaryBasic_MS(0.005f,30, ImageType.ms(3, ImageFloat32.class));
//		BackgroundStationaryGaussian background =
//				new BackgroundStationaryGaussian_SB(0.001f,10, ImageFloat32.class);
//				new BackgroundStationaryGaussian_MS(0.001f,30,ImageType.ms(3, ImageFloat32.class));
//		background.setInitialVariance(64);
//		background.setMinimumDifference(12);

//		MediaManager media = DefaultMediaManager.INSTANCE;
//		String fileName = "../data/applet/shake.mjpeg";
//		SimpleImageSequence video = media.openVideo(fileName, background.getImageType());
		LoadFileImageSequence video = new LoadFileImageSequence(background.getImageType(),"/home/pabeles/romotive/Vision/DetectLanding/output","jpg");

		video.setLoop(true);
//		video.setIndex(500);

		ImageUInt8 segmented = new ImageUInt8(1,1);

		ImageBinaryPanel gui = null;

		double fps = 0;
		double alpha = 0.01;

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

			System.out.println("sum = " + ImageStatistics.sum(segmented) + " " + ImageStatistics.max(segmented)+"  "+fps);
			gui.setBinaryImage(segmented);
			gui.repaint();
			System.out.println("Processed!!");

//			try {
//				Thread.sleep(5);
//			} catch (InterruptedException e) {
//
//			}
		}
	}
}
