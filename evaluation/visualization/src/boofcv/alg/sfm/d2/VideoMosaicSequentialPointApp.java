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

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.tracker.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homo.Homography2D_F32;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.transform.ConvertTransform_F64;
import georegression.transform.homo.HomographyPointOps_F32;

import java.awt.image.BufferedImage;
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
// TODO change scale in info panel
public class VideoMosaicSequentialPointApp<I extends ImageSingleBand, D extends ImageSingleBand, T extends InvertibleTransform<T>>
		extends ImageMotionBaseApp<I,T>
{
	private final static int maxFeatures = 250;
	private final static int maxIterations = 100;
	private final static int pruneThreshold = 10;
	
	public VideoMosaicSequentialPointApp(Class<I> imageType, Class<D> derivType) {
		super(false,imageType,2);

		// size of the mosaic image
		setOutputSize(1000,600);

		PkltConfig<I, D> config =
				PkltConfig.createDefault(imageType, derivType);
		config.maxFeatures = maxFeatures;
		config.featureRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		ConfigFastHessian configFH = new ConfigFastHessian();
		configFH.maxFeaturesPerScale = 200;

		addAlgorithm(0, "KLT", FactoryPointSequentialTracker.klt(config,1,3,1,1));
		addAlgorithm(0, "ST-BRIEF", FactoryPointSequentialTracker.dda_ST_BRIEF(400, 150, 1, 10, imageType, null));
		// size of the description region has been increased to improve quality.
		addAlgorithm(0, "ST-NCC", FactoryPointSequentialTracker.
				dda_ST_NCC(500, 3, 9, 10, imageType, derivType));
		addAlgorithm(0, "FH-SURF", FactoryPointSequentialTracker.dda_FH_SURF_Fast(400, configFH,null,null, imageType));
		addAlgorithm(0, "ST-SURF-KLT", FactoryPointSequentialTracker.combined_ST_SURF_KLT(400, 3, 1, 3,
				config.pyramidScaling, 50, null,null, imageType, derivType));
		addAlgorithm(0, "FH-SURF-KLT", FactoryPointSequentialTracker.combined_FH_SURF_KLT(400,3,
				config.pyramidScaling,50, configFH,null,null,imageType));

		addAlgorithm(1,"Affine", new Affine2D_F64());
		addAlgorithm(1,"Homography", new Homography2D_F64());
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	private Affine2D_F64 createInitialTransform() {
		float scale = 0.8f;

		Affine2D_F64 H = new Affine2D_F64(scale,0,0,scale,outputWidth/4, outputHeight/4);
		return H.invert(null);
	}

	/**
	 * Checks to see if one of the four corners is near the mosaic's border.
	 *
	 * @param frameWidth width of input image
	 * @param frameHeight height of input image
	 * @param tol how close to the border it needs to be to return true
	 * @return If it is near the image border
	 */
	private boolean closeToImageBounds( int frameWidth , int frameHeight, int tol )  {
		T worldToCurr = distortAlg.getWorldToCurr();

		Homography2D_F32 currToWorld = convertToHomography(worldToCurr.invert(null));

		if( closeToBorder(0,0,tol,currToWorld) )
			return true;
		if( closeToBorder(frameWidth,0,tol,currToWorld) )
			return true;
		if( closeToBorder(frameWidth,frameHeight,tol,currToWorld) )
			return true;
		if( closeToBorder(0,frameHeight,tol,currToWorld) )
			return true;

		return false;
	}

	/**
	 * Transforms the point and sees if it is near the border
	 */
	private boolean closeToBorder( int x , int y , int tolerance , Homography2D_F32 currToWorld) {

		Point2D_F32 pt = new Point2D_F32(x,y);
		HomographyPointOps_F32.transform(currToWorld, pt, pt);

		
		if( pt.x < tolerance || pt.y < tolerance )
			return true;
		return( pt.x >= outputWidth-tolerance || pt.y >= outputHeight-tolerance );
	}

	@Override
	protected void handleFatalError() {
		motionRender.clear();
		distortAlg.reset();
	}

	@Override
	protected void updateAlgGUI(I frame, BufferedImage imageGUI, final double fps) {

		// reset the world coordinate system to the current key frame
		if( infoPanel.resetRequested() || closeToImageBounds(frame.width,frame.height,30)) {
			T oldToNew = fitModel.createInstance();
			distortAlg.changeWorld(oldToNew);
			PixelTransform_F32 pixelTran = UtilImageMotion.createPixelTransform(oldToNew);
			motionRender.distortMosaic(pixelTran);
		}

		MotionMosaicPointKey alg = (MotionMosaicPointKey)distortAlg;

		if( alg.isKeyFrame() ) {
			totalKeyFrames++;
		}

		super.updateAlgGUI(frame,imageGUI,fps);
	}

	@Override
	protected void startEverything() {
		// make sure there is nothing left over from before
		tracker.dropAllTracks();
		createAssistedTracker(maxIterations,4);
		distortAlg = new MotionMosaicPointKey<I,T>(trackerModel,fitModel,40,0.3,pruneThreshold,0.8);
		T initTran = ConvertTransform_F64.convert(createInitialTransform(), fitModel.createInstance());
		distortAlg.setInitialTransform(initTran);
		totalKeyFrames = 0;
		motionRender.clear();

		startWorkerThread();
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
}
