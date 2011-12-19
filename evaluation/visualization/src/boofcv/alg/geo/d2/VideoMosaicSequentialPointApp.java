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

import boofcv.alg.geo.d2.stabilization.MotionMosaicPointKey;
import boofcv.alg.geo.d2.stabilization.UtilImageMotion;
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
import georegression.struct.homo.Homography2D_F32;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.transform.ConvertTransform_F64;
import georegression.transform.homo.HomographyPointOps;

import java.awt.image.BufferedImage;

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
		extends ImageMotionBaseApp<I,D,T> implements ProcessInput
{
	private final static int maxFeatures = 250;
	private final static int maxIterations = 100;
	
	public VideoMosaicSequentialPointApp(Class<I> imageType, Class<D> derivType) {
		super(false,imageType,2);

		// size of the mosaic image
		setOutputSize(1000,600);

		PkltManagerConfig<I, D> config =
				PkltManagerConfig.createDefault(imageType,derivType);
		config.maxFeatures = maxFeatures;
		config.featureRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		addAlgorithm(0, "KLT", FactoryPointSequentialTracker.klt(config));
		addAlgorithm(0, "BRIEF", FactoryPointSequentialTracker.brief(300, 200, 10, imageType));
		addAlgorithm(0, "SURF", FactoryPointSequentialTracker.surf(300, 200, 2, imageType));
		
		addAlgorithm(1,"Affine", new Affine2D_F64());
		addAlgorithm(1,"Homography", new Homography2D_F64());
	}

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
		HomographyPointOps.transform(currToWorld, pt, pt);

		
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
			distortAlg.refocus(oldToNew);
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
		tracker.dropTracks();
		createModelMatcher(maxIterations,4);
		distortAlg = new MotionMosaicPointKey<I,T>(tracker,modelMatcher,fitModel,40,0.3,0.8);
		T initTran = ConvertTransform_F64.convert(createInitialTransform(), fitModel.createInstance());
		distortAlg.setInitialTransform(initTran);
		totalKeyFrames = 0;
		motionRender.clear();

		startWorkerThread();
	}

	public static void main( String args[] ) {
		VideoMosaicSequentialPointApp app = new VideoMosaicSequentialPointApp(ImageFloat32.class, ImageFloat32.class);

		VideoListManager manager = new VideoListManager(ImageFloat32.class);
		manager.add("Plane 1", "MJPEG", "../data/applet/mosaic/airplane01.mjpeg");
		manager.add("Plane 2", "MJPEG", "../data/applet/mosaic/airplane02.mjpeg");
		manager.add("Shake", "MJPEG", "../data/applet/shake.mjpeg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Video Image Mosaic");
	}
}
