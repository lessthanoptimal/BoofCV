/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d2;

import boofcv.alg.geo.d2.stabilization.ImageDistortPointKey;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.gui.ProcessInput;
import boofcv.gui.image.ShowImages;
import boofcv.io.video.VideoListManager;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homo.Homography2D_F64;

import java.awt.image.BufferedImage;

/**
 * Attempts to remove camera jitter across multiple video frames by detecting point features inside the image
 * and tracking their motion.  Models are then fit to the feature's motion and the inverse transform
 * computer and rendered.  RANSAC is used internally to remove noise.  Different feature descriptors and motion
 * models can be used. Both the unstabilized input and stabilized output are shown in a window.
 *
 * @author Peter Abeles
 * @param <I> Input image type
 * @param <D> Image derivative type
 */
public class ImageStabilizeApp<I extends ImageSingleBand, D extends ImageSingleBand, T extends InvertibleTransform<T>>
		extends ImageDistortBaseApp<I,D,T> implements ProcessInput
{
	private int maxFeatures = 250;
	private static int thresholdKeyFrame = 80;
	private static int thresholdReset = 20;
	private static int maxIterations = 80;
	
	int largeMotionThreshold = 5000;

	public ImageStabilizeApp(Class<I> imageType, Class<D> derivType ) {
		super(true,imageType,2);


		PkltManagerConfig<I, D> config =
				PkltManagerConfig.createDefault(imageType,derivType);
		config.maxFeatures = maxFeatures;
		config.featureRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		addAlgorithm(0, "KLT", FactoryPointSequentialTracker.klt(config));
		addAlgorithm(0, "BRIEF", FactoryPointSequentialTracker.brief(300, 200, 20, imageType));
		addAlgorithm(0, "SURF", FactoryPointSequentialTracker.surf(300, 200, 2, imageType));
		// size of the description region has been increased to improve quality.
		addAlgorithm(0, "NCC", FactoryPointSequentialTracker.pixelNCC(500,11,11,20,imageType,derivType));

		addAlgorithm(1,"Affine", new Affine2D_F64());
		addAlgorithm(1,"Homography", new Homography2D_F64());
	}

	/**
	 * Updates the GUI and response to user requests.
	 */
	@Override
	protected void updateAlgGUI(I frame, BufferedImage imageGUI, final double fps) {

		// put it into its initial state
		if( infoPanel.resetRequested() ) {
			doRefreshAll();
		}

		// perform standard update
		super.updateAlgGUI(frame,imageGUI,fps);
	}

	@Override
	protected void handleFatalError() {
		mosaicRender.clear();
		distortAlg.reset();
	}

	@Override
	protected void checkStatus(  PixelTransform_F32 keyToCurr , I frame , BufferedImage buffImage ) {
		
		int inliers = distortAlg.getModelMatcher().getMatchSet().size();
		
		if( inliers < thresholdReset ) {
			totalKeyFrames++;
			distortAlg.reset();
			renderCurrentTransform(frame, buffImage);
		} else if( inliers < thresholdKeyFrame ) {
			distortAlg.changeKeyFrame();
		}
		// sudden very large movements tend to be divergence
		// check for four corners for large changes
		int w = frame.width;
		int h = frame.height;
		if( checkLargeDistortion(0,0,keyToCurr) ||
				checkLargeDistortion(w,0,keyToCurr) ||
				checkLargeDistortion(w,h,keyToCurr) ||
				checkLargeDistortion(0,h,keyToCurr))
		{
			totalFatalErrors++;
			distortAlg.reset();
			renderCurrentTransform(frame, buffImage);
		}
	}
	
	private boolean checkLargeDistortion( int x , int y , PixelTransform_F32 tran )
	{
		tran.compute(x,y);
		
		if( Math.abs(tran.distX-x) > largeMotionThreshold || Math.abs(tran.distY-y) > largeMotionThreshold ) {
			return true;
		}
		return false;
	}

	@Override
	protected void startEverything() {
		// make sure there is nothing left over from before
		tracker.dropTracks();
		createModelMatcher(maxIterations,4);
		distortAlg = new ImageDistortPointKey<I,T>(tracker,modelMatcher,fitModel);
//		distortAlg.setInitialTransform(createInitialTransform());

		totalKeyFrames = 0;

		I image = sequence.next();
		setOutputSize(image.width,image.height);
		mosaicRender.clear();

		startWorkerThread();
	}

	public static void main( String args[] ) {
		ImageStabilizeApp app = new ImageStabilizeApp(ImageFloat32.class, ImageFloat32.class);

		VideoListManager manager = new VideoListManager(ImageFloat32.class);
		manager.add("Shake", "MJPEG", "../applet/data/shake.mjpeg");
		manager.add("Zoom", "MJPEG", "../applet/data/zoom.mjpeg");
		manager.add("Rotate", "MJPEG", "../applet/data/rotate.mjpeg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Video Image Stabilize");
	}
}
