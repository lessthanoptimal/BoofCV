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

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformHomography_F32;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.d2.stabilization.MosaicImagePointKey;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.ProcessInput;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.VideoListManager;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.homo.Homography2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.transform.homo.HomographyPointOps;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO change scale
// TODO turn off looping
// TODO KLT Issue;  When zooming out features get clumped together, Need to prune features!
// todo improve processing of multi-special images
public class ImageMosaicApp <I extends ImageSingleBand, D extends ImageSingleBand>
		extends VideoProcessAppBase<I,D> implements ProcessInput
{
	int maxFeatures = 250;
	static int thresholdChange = 80;

	int totalKeyFrames = 0;

	Class<I> imageType;

	ImagePointTracker<I> tracker;
	ModelMatcher<Object,AssociatedPair> modelMatcher;

	MosaicImagePointKey<I> mosaicAlg;
	MultiSpectral<I> imageMosaic;
	MultiSpectral<I> tempMosaic;

	MultiSpectral<I> frameMulti;
	
	PixelTransformHomography_F32 distort = new PixelTransformHomography_F32();
	ImageDistort<I> distorter;

	StabilizationInfoPanel infoPanel;
	ImagePanel gui = new ImagePanel();

	public ImageMosaicApp( Class<I> imageType , Class<D> derivType ) {
		super(1);

		this.imageType = imageType;

		ModelFitterAffine2D modelFitter = new ModelFitterAffine2D();
		DistanceAffine2DSq distance = new DistanceAffine2DSq();
//		ModelFitterLinearHomography modelFitter = new ModelFitterLinearHomography();
//		DistanceHomographySq distance = new DistanceHomographySq();

		int numSample =  modelFitter.getMinimumPoints();

		PkltManagerConfig<I, D> config =
				PkltManagerConfig.createDefault(imageType,derivType);
		config.maxFeatures = maxFeatures;
		config.featureRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		addAlgorithm(0, "KLT", FactoryPointSequentialTracker.klt(config));
//		addAlgorithm(0, "BRIEF", FactoryPointSequentialTracker.brief(300, 200, 20, imageType));
		addAlgorithm(0, "SURF", FactoryPointSequentialTracker.surf(300, 200, 2, imageType));

		modelMatcher = new SimpleInlierRansac(123123,
				modelFitter,distance,50,numSample,numSample,10000,4.0);

		InterpolatePixel<I> interp = FactoryInterpolation.createPixel(0, 255, TypeInterpolate.BILINEAR, imageType);
		
		imageMosaic = new MultiSpectral<I>(imageType,1000,600,3);
		tempMosaic = new MultiSpectral<I>(imageType,1000,600,3);
		frameMulti = new MultiSpectral<I>(imageType,1,1,3);
		distorter = DistortSupport.createDistort(imageType,null,interp,null);

		final BufferedImage out = ConvertBufferedImage.convertTo(imageMosaic,null);

		gui.setBufferedImage(out);
		gui.setPreferredSize(new Dimension(imageMosaic.width, imageMosaic.height));
		gui.setMinimumSize(gui.getPreferredSize());

		infoPanel = new StabilizationInfoPanel();
		infoPanel.setMaximumSize(infoPanel.getPreferredSize());
		gui.addMouseListener(this);

		add(infoPanel, BorderLayout.WEST);
		setMainGUI(gui);
	}

	@Override
	protected void process(SimpleImageSequence<I> sequence) {
		if( !sequence.hasNext() )
			return;
		stopWorker();

		this.sequence = sequence;

		doRefreshAll();
	}

	@Override
	protected void updateAlg(I frame) {
		if( mosaicAlg == null )
			return;

		mosaicAlg.process(frame);
		
		// todo move convert back here
	}

	private Homography2D_F32 createInitialTransform() {
		float scale = 0.5f;

		Homography2D_F32 H = new Homography2D_F32(scale,0, imageMosaic.width/4,
				0,scale, imageMosaic.height/4,
				0,0,1);
		return H.invert(null);
	}

	private void drawImageBounds(int width , int height, Homography2D_F32 currToGlobal, Graphics g2) {
		Point2D_F32 a = new Point2D_F32(0,0);
		Point2D_F32 b = new Point2D_F32(width,0);
		Point2D_F32 c = new Point2D_F32(width,height);
		Point2D_F32 d = new Point2D_F32(0,height);

		HomographyPointOps.transform(currToGlobal, a, a);
		HomographyPointOps.transform(currToGlobal,b,b);
		HomographyPointOps.transform(currToGlobal,c,c);
		HomographyPointOps.transform(currToGlobal,d,d);

		g2.setColor(Color.RED);
		g2.drawLine((int)a.x,(int)a.y,(int)b.x,(int)b.y);
		g2.drawLine((int)b.x,(int)b.y,(int)c.x,(int)c.y);
		g2.drawLine((int)c.x,(int)c.y,(int)d.x,(int)d.y);
		g2.drawLine((int)d.x,(int)d.y,(int)a.x,(int)a.y);
	}
	
	public boolean closeToImageBounds( int frameWidth , int frameHeight, int tol )  {
		Homography2D_F32 currToWorld = mosaicAlg.getCurrToWorld();
		Homography2D_F32 worldToCurr = currToWorld.invert(null); // TODO these transform are skewed up

		if( closeToBorder(0,0,tol,worldToCurr) )
			return true;
		if( closeToBorder(frameWidth,0,tol,worldToCurr) )
			return true;
		if( closeToBorder(frameWidth,frameHeight,tol,worldToCurr) )
			return true;
		if( closeToBorder(0,frameHeight,tol,worldToCurr) )
			return true;
		
		return false;
	}
	
	private boolean closeToBorder( int x , int y , int tolerance , Homography2D_F32 currToWorld) {

		Point2D_F32 pt = new Point2D_F32(x,y);
		HomographyPointOps.transform(currToWorld, pt, pt);
		
		int w = imageMosaic.width;
		int h = imageMosaic.height;
		
		if( pt.x < tolerance || pt.y < tolerance )
			return true;
		return( pt.x >= w-tolerance || pt.y >= h-tolerance );
	}

	private void drawFeatures( Homography2D_F32 currToGlobal, Graphics2D g2 ) {

		Point2D_F32 currPt = new Point2D_F32();

		if( infoPanel.getShowAll() ) {
			List<AssociatedPair> all = tracker.getActiveTracks();

			for( AssociatedPair p : all ) {
				currPt.set((float)p.currLoc.x,(float)p.currLoc.y);
				HomographyPointOps.transform(currToGlobal,currPt,currPt);

				VisualizeFeatures.drawPoint(g2,(int)currPt.x,(int)currPt.y,Color.RED);
			}
		}

		if( infoPanel.getShowInliers() ) {
			List<AssociatedPair> inlier = modelMatcher.getMatchSet();

			for( AssociatedPair p : inlier ) {
				currPt.set((float)p.currLoc.x,(float)p.currLoc.y);
				HomographyPointOps.transform(currToGlobal,currPt,currPt);

				VisualizeFeatures.drawPoint(g2,(int)currPt.x,(int)currPt.y,Color.BLUE);
			}
		}
	}

	@Override
	protected void updateAlgGUI(I frame, BufferedImage imageGUI, final double fps) {

		frameMulti.reshape(imageGUI.getWidth(),imageGUI.getHeight());
		ConvertBufferedImage.convertFromMulti(imageGUI,frameMulti,imageType);
		
		// render the mosaic
		Homography2D_F32 currToWorld = mosaicAlg.getCurrToWorld();

		distort.set(currToWorld);

		distorter.setModel(distort);
		for( int i = 0; i < imageMosaic.getNumBands(); i++ )
			distorter.apply(frameMulti.getBand(i), imageMosaic.getBand(i));
		
		if( mosaicAlg.isKeyFrameChanged() )
			totalKeyFrames++;
		
		// reset the world coordinate system to the current key frame
		if( infoPanel.resetRequested() || closeToImageBounds(frame.width,frame.height,30)) {
			Homography2D_F32 oldToNew = new Homography2D_F32();
			mosaicAlg.refocus(oldToNew);

			distort.set(oldToNew);
			distorter.setModel(distort);
			for( int i = 0; i < imageMosaic.getNumBands(); i++ ) {
				GeneralizedImageOps.fill(tempMosaic.getBand(i),0);
				distorter.apply(imageMosaic.getBand(i), tempMosaic.getBand(i));
			}
			// swap the two images
			MultiSpectral<I> s = imageMosaic;
			imageMosaic = tempMosaic;
			tempMosaic = s;
		}

		currToWorld = mosaicAlg.getCurrToWorld();
		final Homography2D_F32 foo = currToWorld.invert(null);
//			distort.set(a);

		final int width = frame.width;
		final int height = frame.height;

		final int numAssociated = modelMatcher.getMatchSet().size();
		final int numFeatures = tracker.getActiveTracks().size();

		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				infoPanel.setFPS(fps);
				infoPanel.setNumInliers(numAssociated);
				infoPanel.setNumTracks(numFeatures);
				infoPanel.setKeyFrames(totalKeyFrames);
				infoPanel.repaint();
				
				BufferedImage out = gui.getImage();
				ConvertBufferedImage.convertTo(imageMosaic, out);

				Graphics2D g2 = out.createGraphics();

				drawImageBounds(width,height, foo, g2);
				drawFeatures(foo, g2);

				gui.repaint();
			}
		});
	}

	@Override
	public boolean getHasProcessedImage() {
		return mosaicAlg != null && mosaicAlg.getHasProcessedImage();
	}

	@Override
	public void refreshAll(Object[] cookies) {
		stopWorker();

		tracker = (ImagePointTracker<I>)cookies[0];

		startEverything();
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( sequence == null || modelMatcher == null )
			return;

		stopWorker();

		switch( indexFamily ) {
			case 0:
				tracker = (ImagePointTracker<I>)cookie;
				break;
		}

		// restart the video
		sequence.reset();

		startEverything();
	}

	private void startEverything() {
		// make sure there is nothing left over from before
		tracker.dropTracks();
		mosaicAlg = new MosaicImagePointKey<I>(tracker,modelMatcher);
		mosaicAlg.setInitialTransform(createInitialTransform());

		totalKeyFrames = 0;
		// clear the mosaic
		for( int i = 0; i < imageMosaic.getNumBands(); i++ )
			GeneralizedImageOps.fill(imageMosaic.getBand(i),0);

		startWorkerThread();
	}

	public static void main( String args[] ) {
		ImageMosaicApp app = new ImageMosaicApp(ImageFloat32.class, ImageFloat32.class);

		VideoListManager manager = new VideoListManager(ImageFloat32.class);
		manager.add("Plane 1", "MJPEG", "/home/pja/a/foo15.mjpeg");
		manager.add("Plane 2", "MJPEG", "/home/pja/a/foo6.mjpeg");
		manager.add("Plane 3", "MJPEG", "/home/pja/a/foo20.mjpeg");
		manager.add("Plane 4", "MJPEG", "/home/pja/a/foo21.mjpeg");

		manager.add("Shake", "MJPEG", "../applet/data/shake.mjpeg");
		manager.add("Zoom", "MJPEG", "../applet/data/zoom.mjpeg");
		manager.add("Rotate", "MJPEG", "../applet/data/rotate.mjpeg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Video Image Mosaic");
	}
}
