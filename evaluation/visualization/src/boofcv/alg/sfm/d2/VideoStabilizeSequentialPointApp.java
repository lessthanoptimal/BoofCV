/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.d2;

import boofcv.abst.feature.tracker.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homo.Homography2D_F64;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

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
public class VideoStabilizeSequentialPointApp<I extends ImageSingleBand, D extends ImageSingleBand, T extends InvertibleTransform<T>>
		extends ImageMotionBaseApp<I,T>
{
	private int maxFeatures = 250;
	private static int thresholdKeyFrame = 80;
	private static int thresholdReset = 20;
	private static int maxIterations = 80;
	
	int largeMotionThreshold = 5000;

	public VideoStabilizeSequentialPointApp(Class<I> imageType, Class<D> derivType) {
		super(true,imageType,2);

		PkltConfig<I, D> config =
				PkltConfig.createDefault(imageType, derivType);
		config.maxFeatures = maxFeatures;
		config.featureRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		addAlgorithm(0, "KLT", FactoryPointSequentialTracker.klt(config,1,1));
		addAlgorithm(0, "BRIEF", FactoryPointSequentialTracker.
				dda_ShiTomasi_BRIEF(400, 100, 1, 10, imageType, derivType));
		addAlgorithm(0, "SURF", FactoryPointSequentialTracker.dda_FH_SURF(300, 200, 2, imageType));
		// size of the description region has been increased to improve quality.
		addAlgorithm(0, "NCC", FactoryPointSequentialTracker.
				dda_ShiTomasi_NCC(500, 11, 11, 10, imageType, derivType));
		addAlgorithm(0, "SURF-KLT", FactoryPointSequentialTracker.combined_FH_SURF_KLT(300, 200, 2,
				config.config,config.pyramidScaling,100,imageType));

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

		MotionStabilizePointKey alg = (MotionStabilizePointKey)distortAlg;

		if( alg.isReset() ) {
			totalKeyFrames++;
		}

		// perform standard update
		super.updateAlgGUI(frame,imageGUI,fps);
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	protected void handleFatalError() {
		motionRender.clear();
		distortAlg.reset();
	}

	@Override
	protected void startEverything() {
		// make sure there is nothing left over from before
		tracker.dropAllTracks();
		createModelMatcher(maxIterations,4);
		distortAlg = new MotionStabilizePointKey<I,T>(tracker,modelMatcher,modelRefiner,fitModel,
				thresholdKeyFrame,thresholdReset,largeMotionThreshold);
//		distortAlg.setInitialTransform(createInitialTransform());

		totalKeyFrames = 0;

		I image = sequence.next();
		setOutputSize(image.width,image.height);
		motionRender.clear();

		startWorkerThread();
	}

	public static void main( String args[] ) {
		VideoStabilizeSequentialPointApp app = new VideoStabilizeSequentialPointApp(ImageFloat32.class, ImageFloat32.class);

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("Shake", "../data/applet/shake.mjpeg"));
		inputs.add(new PathLabel("Zoom", "../data/applet/zoom.mjpeg"));
		inputs.add(new PathLabel("Rotate", "../data/applet/rotate.mjpeg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Video Image Stabilize");
	}
}
