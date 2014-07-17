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

package boofcv.alg.sfm.d2;

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homography.Homography2D_F64;

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
public class VideoStabilizeSequentialPointApp<I extends ImageSingleBand, D extends ImageSingleBand,
		T extends InvertibleTransform<T>>
		extends VideoStitchBaseApp<I,T>
{
	private int maxFeatures = 250;
	
	int largeMotionThreshold = 5000;

	public VideoStabilizeSequentialPointApp(Class<I> imageType, Class<D> derivType) {
		super(2,imageType,true,new Stabilize2DPanel());

		PkltConfig config = new PkltConfig();
		config.templateRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		ConfigFastHessian configFH = new ConfigFastHessian();
		configFH.maxFeaturesPerScale = 200;
		configFH.initialSampleSize = 2;

		addAlgorithm(0, "KLT", FactoryPointTracker.klt(config, new ConfigGeneralDetector(maxFeatures, 1, 3),
				imageType,derivType));
		addAlgorithm(0, "ST-BRIEF", FactoryPointTracker.
				dda_ST_BRIEF(100, new ConfigGeneralDetector(400, 1, 10), imageType, derivType));
		// size of the description region has been increased to improve quality.
		addAlgorithm(0, "ST-NCC", FactoryPointTracker.
				dda_ST_NCC(new ConfigGeneralDetector(500, 3, 10), 5, imageType, derivType));
		addAlgorithm(0, "FH-SURF", FactoryPointTracker.dda_FH_SURF_Fast(configFH, null, null, imageType));
		addAlgorithm(0, "ST-SURF-KLT", FactoryPointTracker.
				combined_ST_SURF_KLT(new ConfigGeneralDetector(400, 3, 1),
						config, 50, null, null, imageType, derivType));
		addAlgorithm(0, "FH-SURF-KLT", FactoryPointTracker.combined_FH_SURF_KLT(
				config, 50, configFH, null, null, imageType));

		addAlgorithm(1,"Affine", new Affine2D_F64());
		addAlgorithm(1,"Homography", new Homography2D_F64());

		absoluteMinimumTracks = 40;
		respawnTrackFraction = 0.3;
		respawnCoverageFraction = 0.5;
		maxJumpFraction = 0.3;
		inlierThreshold = 4;
	}

	@Override
	protected void init(int inputWidth, int inputHeight) {
		setStitchImageSize(inputWidth,inputHeight);
		((Stabilize2DPanel)gui).setInputSize(inputWidth,inputHeight);
		alg.configure(inputWidth,inputHeight,fitModel.createInstance());
	}

	@Override
	protected boolean checkLocation(StitchingFromMotion2D.Corners corners) {
		return false;
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

	@Override
	protected void handleRunningStatus(int status) {
	}
}
