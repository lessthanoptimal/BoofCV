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
import boofcv.alg.background.moving.BackgroundMovingBasic_IL;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.GConvertImage;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.sfm.FactoryMotion2D;
import boofcv.gui.image.ImageBinaryPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.*;
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
		// Configure the feature detector
		ConfigGeneralDetector confDetector = new ConfigGeneralDetector();
		confDetector.threshold = 5;
		confDetector.maxFeatures = 400;
		confDetector.radius = 4;

		// Use a KLT tracker
		PointTracker<ImageFloat32> tracker = FactoryPointTracker.klt(new int[]{1, 2, 4, 8}, confDetector, 3,
				ImageFloat32.class, ImageFloat32.class);

		// This estimates the 2D image motion
		// An Affine2D_F64 model also works quite well.
		ImageMotion2D<ImageFloat32,Homography2D_F64> motion2D =
				FactoryMotion2D.createMotion2D(250, 2, 2, 30, 0.6, 0.5, false, tracker, new Homography2D_F64());

		// TODO IL and MS don't produce identical results
		BackgroundModelMoving background =
//				new BackgroundMovingBasic_SB(0.05f,30,new PointTransformHomography_F32(),
//						TypeInterpolate.BILINEAR, ImageType.single(ImageFloat32.class));
				new BackgroundMovingBasic_IL(0.1f,30,new PointTransformHomography_F32(),
						TypeInterpolate.BILINEAR, ImageType.il(3, InterleavedF32.class));
//				new BackgroundMovingBasic_MS(0.1f,30,new PointTransformHomography_F32(),
//						TypeInterpolate.BILINEAR, ImageType.ms(3, ImageFloat32.class));
//		BackgroundMovingGaussian background =
//				new BackgroundMovingGaussian_SB(0.01f,10,new PointTransformHomography_F32(),
//						TypeInterpolate.BILINEAR, ImageType.single(ImageFloat32.class));
//				new BackgroundMovingGaussian_MS(0.01f,40,new PointTransformHomography_F32(),
//						TypeInterpolate.BILINEAR, ImageType.ms(3, ImageFloat32.class));
//		background.setInitialVariance(64);

		MediaManager media = DefaultMediaManager.INSTANCE;
		String fileName = "../data/applet/shake.mjpeg";
		SimpleImageSequence video = media.openVideo(fileName, background.getImageType());

		ImageUInt8 segmented = new ImageUInt8(1,1);
		ImageFloat32 grey = new ImageFloat32(1,1);

		Homography2D_F32 firstToCurrent32 = new Homography2D_F32();

		ImageBinaryPanel gui = null;

		double fps = 30;
		double alpha = 0.05;

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

//			System.out.println("sum = "+ ImageStatistics.sum(segmented)+" "+ImageStatistics.max(segmented));
			gui.setBinaryImage(segmented);
			gui.repaint();
			System.out.println("Processed!!  fps = "+fps);

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {

			}
		}
	}
}
