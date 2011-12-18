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

import boofcv.alg.geo.AssociatedPair;
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
import georegression.struct.homo.Homography2D_F32;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.transform.ConvertTransform_F64;
import georegression.transform.homo.HomographyPointOps;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 *
 *
 * @author Peter Abeles
 * @param <I> Input image type
 * @param <D> Image derivative type
 */
// TODO change scale
// todo create stabilize app using common code
// TODO get SURF tracker to stop spawning so many new frames
// TODO SURF should support drop track
// TODO Make a copy of features?  or synchronize
public class ImageMosaicApp <I extends ImageSingleBand, D extends ImageSingleBand, T extends InvertibleTransform<T>>
		extends ImageDistortBaseApp<I,D,T> implements ProcessInput
{
	private final static int maxFeatures = 250;

	private final static int maxIterations = 80;

	private static int thresholdChange = 80;

	int absoluteMinimumTracks = 40;
	double respawnTrackFraction = 0.7;
	// if less than this fraction of the window is convered by features switch views
	double respawnCoverageFraction = 0.8;
	// coverage right after spawning new features
	double maxCoverage;
	
	public ImageMosaicApp( Class<I> imageType , Class<D> derivType) {
		super(false,imageType,2);

		setOutputSize(1000,600);

		PkltManagerConfig<I, D> config =
				PkltManagerConfig.createDefault(imageType,derivType);
		config.maxFeatures = maxFeatures;
		config.featureRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		addAlgorithm(0, "KLT", FactoryPointSequentialTracker.klt(config));
//		addAlgorithm(0, "BRIEF", FactoryPointSequentialTracker.brief(300, 200, 20, imageType));
		addAlgorithm(0, "SURF", FactoryPointSequentialTracker.surf(300, 200, 2, imageType));
		
		addAlgorithm(1,"Affine", new Affine2D_F64());
		addAlgorithm(1,"Homography", new Homography2D_F64());
	}

	private Affine2D_F64 createInitialTransform() {
		float scale = 0.8f;

		Affine2D_F64 H = new Affine2D_F64(scale,0,0,scale,outputWidth/4, outputHeight/4);
		return H.invert(null);
	}

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
	
	private boolean closeToBorder( int x , int y , int tolerance , Homography2D_F32 currToWorld) {

		Point2D_F32 pt = new Point2D_F32(x,y);
		HomographyPointOps.transform(currToWorld, pt, pt);

		
		if( pt.x < tolerance || pt.y < tolerance )
			return true;
		return( pt.x >= outputWidth-tolerance || pt.y >= outputHeight-tolerance );
	}

	@Override
	protected void handleFatalError() {
		mosaicRender.clear();
		distortAlg.reset();
	}

	@Override
	protected void checkStatus( PixelTransform_F32 pixelTran , I frame , BufferedImage buffImage  ) {

		boolean keyframe = false;
		int matchSetSize = modelMatcher.getMatchSet().size();
		if( matchSetSize < distortAlg.getTotalSpawned()* respawnTrackFraction  || matchSetSize < absoluteMinimumTracks ) {
			keyframe = true;
		}

		double fractionCovered = 1;//imageCoverageFraction(frame.width,frame.height,pairs);
		if( fractionCovered < respawnCoverageFraction *maxCoverage ) {
			keyframe = true;
		}

		if( keyframe ) {
			distortAlg.changeKeyFrame();
			totalKeyFrames++;
		}

//		maxCoverage = imageCoverageFraction(width, height,tracker.getActiveTracks());
//
//		// for some trackers like KLT, they keep old features and these features can get squeezed together
//		// this will remove some of the really close features
//		if( maxCoverage < respawnCoverageFraction) {
//			// prune some of the ones which are too close
//			pruneClose.process(tracker);
//			// see if it can find some more in diverse locations
//			tracker.spawnTracks();
//			maxCoverage = imageCoverageFraction(width, height,tracker.getActiveTracks());
//		}
	}

	private double imageCoverageFraction( int width , int height , List<AssociatedPair> tracks ) {
		double x0 = width;
		double x1 = 0;
		double y0 = height;
		double y1 = 0;

		for( AssociatedPair p : tracks ) {
			if( p.currLoc.x < x0 )
				x0 = p.currLoc.x;
			if( p.currLoc.x >= x1 )
				x1 = p.currLoc.x;
			if( p.currLoc.y < y0 )
				y0 = p.currLoc.y;
			if( p.currLoc.y >= y1 )
				y1 = p.currLoc.y;
		}
		return ((x1-x0)*(y1-y0))/(width*height);
	}

	@Override
	protected void updateAlgGUI(I frame, BufferedImage imageGUI, final double fps) {

		// reset the world coordinate system to the current key frame
		if( infoPanel.resetRequested() || closeToImageBounds(frame.width,frame.height,30)) {
			T oldToNew = fitModel.createInstance();
			distortAlg.refocus(oldToNew);
			PixelTransform_F32 pixelTran = createPixelTransform(oldToNew);
			mosaicRender.distortMosaic(pixelTran);
		}

		super.updateAlgGUI(frame,imageGUI,fps);
	}

	@Override
	protected void startEverything() {
		// make sure there is nothing left over from before
		tracker.dropTracks();
		createModelMatcher(maxIterations,4);
		distortAlg = new ImageDistortPointKey<I,T>(tracker,modelMatcher,fitModel);
		T initTran = ConvertTransform_F64.convert(createInitialTransform(), fitModel.createInstance());
		distortAlg.setInitialTransform(initTran);
		totalKeyFrames = 0;
		mosaicRender.clear();

		startWorkerThread();
	}

	public static void main( String args[] ) {
		ImageMosaicApp app = new ImageMosaicApp(ImageFloat32.class, ImageFloat32.class);

		VideoListManager manager = new VideoListManager(ImageFloat32.class);
		manager.add("Plane 1", "MJPEG", "../applet/data/mosaic/airplane01.mjpeg");
		manager.add("Plane 2", "MJPEG", "../applet/data/mosaic/airplane02.mjpeg");
		manager.add("Shake", "MJPEG", "../applet/data/shake.mjpeg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Video Image Mosaic");
	}
}
