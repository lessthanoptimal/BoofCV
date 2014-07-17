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
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a mosaic from an image sequence using tracked point features.  Each the input window
 * moaes toward the mosaic image's boundary it is automatically reset.  When reset the current
 * image is put in the initial position and the mosaic distorted accordingly.
 *
 * @author Peter Abeles
 * @param <I> Input image type
 * @param <D> Image derivative type
 */
// TODO add support for color again
// TODO comment and clean up code
public class VideoMosaicSequentialPointApp<I extends ImageSingleBand, D extends ImageSingleBand,
		IT extends InvertibleTransform>
		extends VideoStitchBaseApp<I,IT>
{
	private static int maxFeatures = 250;

	public VideoMosaicSequentialPointApp(Class<I> imageType, Class<D> derivType) {
		super(2,imageType,true,new Mosaic2DPanel());

		PkltConfig config = new PkltConfig();
		config.templateRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		ConfigFastHessian configFH = new ConfigFastHessian();
		configFH.initialSampleSize = 2;
		configFH.maxFeaturesPerScale = 200;

		addAlgorithm(0, "KLT", FactoryPointTracker.klt(config, new ConfigGeneralDetector(maxFeatures, 3, 1),
				imageType, derivType));
		addAlgorithm(0, "ST-BRIEF", FactoryPointTracker.
				dda_ST_BRIEF(150, new ConfigGeneralDetector(400, 1, 10), imageType, null));
		// size of the description region has been increased to improve quality.
		addAlgorithm(0, "ST-NCC", FactoryPointTracker.
				dda_ST_NCC(new ConfigGeneralDetector(500, 3, 9), 10, imageType, derivType));
		addAlgorithm(0, "FH-SURF", FactoryPointTracker.dda_FH_SURF_Fast(configFH, null, null, imageType));
		addAlgorithm(0, "ST-SURF-KLT", FactoryPointTracker.
				combined_ST_SURF_KLT(new ConfigGeneralDetector(400, 3, 1),
						config, 75, null, null, imageType, derivType));
		addAlgorithm(0, "FH-SURF-KLT", FactoryPointTracker.combined_FH_SURF_KLT(
				config, 75, configFH, null, null, imageType));

		addAlgorithm(1,"Affine", new Affine2D_F64());
		addAlgorithm(1,"Homography", new Homography2D_F64());

		absoluteMinimumTracks = 40;
		respawnTrackFraction = 0.3;
		respawnCoverageFraction = 0.8;
		maxJumpFraction = 0.3;
		inlierThreshold = 4;
	}

	private IT createInitialTransform() {
		float scale = 0.8f;

		if( fitModel instanceof Affine2D_F64 ) {
			Affine2D_F64 H = new Affine2D_F64(scale,0,0,scale, stitchWidth /4, stitchHeight /4);
			return (IT)H.invert(null);
		} else if( fitModel instanceof Homography2D_F64 ) {
			Homography2D_F64 H = new Homography2D_F64(scale,0,stitchWidth /4,0,scale,stitchHeight /4,0,0,1 );
			return (IT)H.invert(null);
		} else {
			throw new RuntimeException("Need to support this model type: "+fitModel.getClass().getSimpleName());
		}
	}

	@Override
	protected void init(int inputWidth, int inputHeight) {
		setStitchImageSize(1000, 600);
		((Mosaic2DPanel)gui).setMosaicSize(stitchWidth, stitchHeight);
		alg.configure(stitchWidth, stitchHeight,createInitialTransform());
	}

	@Override
	protected boolean checkLocation(StitchingFromMotion2D.Corners corners) {
		if( closeToBorder(corners.p0) )
			return true;
		if( closeToBorder(corners.p1) )
			return true;
		if( closeToBorder(corners.p2) )
			return true;
		if( closeToBorder(corners.p3) )
			return true;

		return false;
	}

	private boolean closeToBorder( Point2D_F64 pt ) {
		if( pt.x < borderTolerance || pt.y < borderTolerance)
			return true;
		return( pt.x >= stitchWidth - borderTolerance || pt.y >= stitchHeight - borderTolerance);
	}

	public static void main( String args[] ) {
		Class type = ImageFloat32.class;
		Class derivType = type;

//		Class type = ImageUInt8.class;
//		Class derivType = ImageSInt16.class;

		VideoMosaicSequentialPointApp app = new VideoMosaicSequentialPointApp(type,derivType);

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("Plane 1", "../data/applet/mosaic/airplane01.mjpeg"));
		inputs.add(new PathLabel("Plane 2", "../data/applet/mosaic/airplane02.mjpeg"));
		inputs.add(new PathLabel("Shake", "../data/applet/shake.mjpeg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Video Image Mosaic");
	}

	@Override
	protected void handleRunningStatus(int status) {}
}
