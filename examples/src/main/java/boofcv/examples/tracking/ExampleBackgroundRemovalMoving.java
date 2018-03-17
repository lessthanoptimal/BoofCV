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

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.alg.background.BackgroundModelMoving;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.core.image.GConvertImage;
import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.ConfigBackgroundGaussian;
import boofcv.factory.background.ConfigBackgroundGmm;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.sfm.FactoryMotion2D;
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
import georegression.struct.homography.Homography2D_F32;
import georegression.struct.homography.Homography2D_F64;
import org.ejml.ops.ConvertMatrixData;

import java.awt.image.BufferedImage;

/**
 * Example showing how to perform background modeling with a moving camera.  Here the camera's motion is explicitly
 * estimated using a motion model.  That motion model is then used to distort the image and generate background.
 * The net affect is a significant reduction in false positives around the objects of images in oscillating cameras
 * and the ability to detect motion in moving scenes.
 *
 * @author Peter Abeles
 */
public class ExampleBackgroundRemovalMoving {
	public static void main(String[] args) {

		// Example with a moving camera.  Highlights why motion estimation is sometimes required
		String fileName = UtilIO.pathExample("tracking/chipmunk.mjpeg");
		// Camera has a bit of jitter in it.  Static kinda works but motion reduces false positives
//		String fileName = UtilIO.pathExample("background/horse_jitter.mp4");

		// Comment/Uncomment to switch input image type
		ImageType imageType = ImageType.single(GrayF32.class);
//		ImageType imageType = ImageType.il(3, InterleavedF32.class);
//		ImageType imageType = ImageType.il(3, InterleavedU8.class);

		// Configure the feature detector
		ConfigGeneralDetector confDetector = new ConfigGeneralDetector();
		confDetector.threshold = 10;
		confDetector.maxFeatures = 300;
		confDetector.radius = 6;

		// Use a KLT tracker
		PointTracker tracker = FactoryPointTracker.klt(new int[]{1, 2, 4, 8}, confDetector, 3,
				GrayF32.class, null);

		// This estimates the 2D image motion
		ImageMotion2D<GrayF32,Homography2D_F64> motion2D =
				FactoryMotion2D.createMotion2D(500, 0.5, 3, 100, 0.6, 0.5, false, tracker, new Homography2D_F64());

		ConfigBackgroundBasic configBasic = new ConfigBackgroundBasic(30, 0.005f);

		// Configuration for Gaussian model.  Note that the threshold changes depending on the number of image bands
		// 12 = gray scale and 40 = color
		ConfigBackgroundGaussian configGaussian = new ConfigBackgroundGaussian(12,0.001f);
		configGaussian.initialVariance = 64;
		configGaussian.minimumDifference = 5;

		// Note that GMM doesn't interpolate the input image. Making it harder to model object edges.
		// However it runs faster because of this.
		ConfigBackgroundGmm configGmm = new ConfigBackgroundGmm();
		configGmm.initialVariance = 1600;
		configGmm.significantWeight = 1e-1f;

		// Comment/Uncomment to switch background mode
		BackgroundModelMoving background =
				FactoryBackgroundModel.movingBasic(configBasic, new PointTransformHomography_F32(), imageType);
//				FactoryBackgroundModel.movingGaussian(configGaussian, new PointTransformHomography_F32(), imageType);
//				FactoryBackgroundModel.movingGmm(configGmm,new PointTransformHomography_F32(), imageType);

		background.setUnknownValue(1);

		MediaManager media = DefaultMediaManager.INSTANCE;
		SimpleImageSequence video =
				media.openVideo(fileName, background.getImageType());
//				media.openCamera(null,640,480,background.getImageType());

		//====== Initialize Images

		// storage for segmented image.  Background = 0, Foreground = 1
		GrayU8 segmented = new GrayU8(video.getNextWidth(),video.getNextHeight());
		// Grey scale image that's the input for motion estimation
		GrayF32 grey = new GrayF32(segmented.width,segmented.height);

		// coordinate frames
		Homography2D_F32 firstToCurrent32 = new Homography2D_F32();
		Homography2D_F32 homeToWorld = new Homography2D_F32();
		homeToWorld.a13 = grey.width/2;
		homeToWorld.a23 = grey.height/2;

		// Create a background image twice the size of the input image.  Tell it that the home is in the center
		background.initialize(grey.width * 2, grey.height * 2, homeToWorld);

		BufferedImage visualized = new BufferedImage(segmented.width,segmented.height,BufferedImage.TYPE_INT_RGB);
		ImageGridPanel gui = new ImageGridPanel(1,2);
		gui.setImages(visualized, visualized);

		ShowImages.showWindow(gui, "Detections", true);

		double fps = 0;
		double alpha = 0.01; // smoothing factor for FPS

		while( video.hasNext() ) {
			ImageBase input = video.next();

			long before = System.nanoTime();
			GConvertImage.convert(input, grey);

			if( !motion2D.process(grey) ) {
				throw new RuntimeException("Should handle this scenario");
			}

			Homography2D_F64 firstToCurrent64 = motion2D.getFirstToCurrent();
			ConvertMatrixData.convert(firstToCurrent64, firstToCurrent32);

			background.segment(firstToCurrent32, input, segmented);
			background.updateBackground(firstToCurrent32,input);
			long after = System.nanoTime();

			fps = (1.0-alpha)*fps + alpha*(1.0/((after-before)/1e9));

			VisualizeBinaryData.renderBinary(segmented,false,visualized);
			gui.setImage(0, 0, (BufferedImage)video.getGuiImage());
			gui.setImage(0, 1, visualized);
			gui.repaint();

			System.out.println("FPS = "+fps);

			try {Thread.sleep(5);} catch (InterruptedException e) {}
		}
	}
}
