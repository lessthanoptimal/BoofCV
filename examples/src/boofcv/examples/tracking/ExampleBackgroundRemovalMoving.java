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

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.alg.background.BackgroundModelMoving;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.core.image.GConvertImage;
import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.ConfigBackgroundGaussian;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.sfm.FactoryMotion2D;
import boofcv.gui.image.ImageBinaryPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.homography.Homography2D_F32;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.homography.UtilHomography;

/**
 * @author Peter Abeles
 */
// TODO simplify the creation image motion estimation
// TODO Visualization.  Show input image in a window,  Difference + color in another
public class ExampleBackgroundRemovalMoving {
	public static void main(String[] args) {

		String fileName = "../data/applet/background/horse_jitter.mjpg";
//		String fileName = "../data/applet/shake.mjpeg";

		ImageType imageType = ImageType.single(ImageFloat32.class);
//		ImageType imageType = ImageType.ms(3, ImageFloat32.class);
//		ImageType imageType = ImageType.il(3, InterleavedF32.class);
//		ImageType imageType = ImageType.il(3, InterleavedU8.class);

		// Configure the feature detector
		ConfigGeneralDetector confDetector = new ConfigGeneralDetector();
		confDetector.threshold = 10;
		confDetector.maxFeatures = 300;
		confDetector.radius = 6;

		// Use a KLT tracker
		PointTracker tracker = FactoryPointTracker.klt(new int[]{1, 2, 4, 8}, confDetector, 3,
				ImageFloat32.class, null);

		// This estimates the 2D image motion
		// An Affine2D_F64 model also works quite well.
		ImageMotion2D<ImageFloat32,Homography2D_F64> motion2D =
				FactoryMotion2D.createMotion2D(500, 0.5, 3, 100, 0.6, 0.5, false, tracker, new Homography2D_F64());

		ConfigBackgroundBasic configBasic = new ConfigBackgroundBasic(30, 0.05f);

		ConfigBackgroundGaussian configGaussian = new ConfigBackgroundGaussian(40,0.001f);
		configGaussian.initialVariance = 64;
		configGaussian.minimumDifference = 5;

		BackgroundModelMoving background =
//				FactoryBackgroundModel.movingBasic(configBasic, new PointTransformHomography_F32(), imageType);
				FactoryBackgroundModel.movingGaussian(configGaussian, new PointTransformHomography_F32(), imageType);


		MediaManager media = DefaultMediaManager.INSTANCE;
		SimpleImageSequence video = media.openVideo(fileName, background.getImageType());

		ImageUInt8 segmented = new ImageUInt8(1,1);
		ImageFloat32 grey = new ImageFloat32(1,1);

		Homography2D_F32 firstToCurrent32 = new Homography2D_F32();

		ImageBinaryPanel gui = null;

		double fps = 0;
		double alpha = 0.01; // smoothing factor for FPS

		while( video.hasNext() ) {
			ImageBase input = video.next();

			if( segmented.width != input.width ) {
				segmented.reshape(input.width,input.height);
				grey.reshape(input.width,input.height);
				Homography2D_F32 homeToWorld = new Homography2D_F32();
				homeToWorld.a13 = input.width/2;
				homeToWorld.a23 = input.height/2;
				background.initialize(input.width*2,input.height*2,homeToWorld);

				gui = new ImageBinaryPanel(segmented);
				ShowImages.showWindow(gui,"Detections",true);
			}

			long before = System.nanoTime();
			GConvertImage.convert(input, grey);

			if( !motion2D.process(grey) )
				throw new RuntimeException("Failed!");

			Homography2D_F64 firstToCurrent64 = motion2D.getFirstToCurrent();
			UtilHomography.convert(firstToCurrent64,firstToCurrent32);

			background.segment(firstToCurrent32,input,segmented);
			background.updateBackground(firstToCurrent32,input);
			long after = System.nanoTime();

			fps = (1.0-alpha)*fps + alpha*(1.0/((after-before)/1e9));

			gui.setBinaryImage(segmented);
			gui.repaint();
			System.out.println("FPS = "+fps);

			try {Thread.sleep(5);} catch (InterruptedException e) {}
		}
	}
}
