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
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.d2.stabilization.MosaicImagePointKey;
import boofcv.alg.geo.d2.stabilization.RenderImageMosaic;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.gui.ProcessInput;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.VideoListManager;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
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
// todo create stabilize app using common code
// TODO get SURF tracker to stop spawning so many new frames
public class ImageMosaicApp <I extends ImageSingleBand, D extends ImageSingleBand, O extends ImageBase>
		extends VideoProcessAppBase<I,D> implements ProcessInput
{
	private int maxFeatures = 250;
	private static int thresholdChange = 80;

	private int totalKeyFrames = 0;

	private ImagePointTracker<I> tracker;
	private ModelMatcher<Object,AssociatedPair> modelMatcher;

	private MosaicImagePointKey<I> mosaicAlg;
	private RenderImageMosaic<I,?> mosaicRender;

	private StabilizationInfoPanel infoPanel;
	private ImagePanel gui = new ImagePanel();

	private int mosaicWidth = 1000;
	private int mosaicHeight = 600;

	public ImageMosaicApp( Class<I> imageType , Class<D> derivType , boolean colorOutput ) {
		super(1);

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

		mosaicRender = new RenderImageMosaic<I,ImageBase>(mosaicWidth,mosaicHeight,imageType,colorOutput);
		

		final BufferedImage out = new BufferedImage(mosaicWidth,mosaicHeight,BufferedImage.TYPE_INT_BGR);

		gui.setBufferedImage(out);
		gui.setPreferredSize(new Dimension(mosaicWidth, mosaicHeight));
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
	protected void updateAlg(I frame, BufferedImage buffImage) {
		if( mosaicAlg == null )
			return;

		mosaicAlg.process(frame);

		mosaicRender.update(frame,buffImage,mosaicAlg.getWorldToCurr());
	}

	private Homography2D_F32 createInitialTransform() {
		float scale = 0.5f;

		Homography2D_F32 H = new Homography2D_F32(scale,0, mosaicWidth/4,
				0,scale, mosaicHeight/4,
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
		Homography2D_F32 worldToCurr = mosaicAlg.getWorldToCurr();
		Homography2D_F32 currToWorld = worldToCurr.invert(null);

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
		return( pt.x >= mosaicWidth-tolerance || pt.y >= mosaicHeight-tolerance );
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

		if( mosaicAlg.isKeyFrameChanged() )
			totalKeyFrames++;
		
		// reset the world coordinate system to the current key frame
		if( infoPanel.resetRequested() || closeToImageBounds(frame.width,frame.height,30)) {
			Homography2D_F32 oldToNew = new Homography2D_F32();
			mosaicAlg.refocus(oldToNew);

			mosaicRender.distortMosaic(oldToNew);
		}

		Homography2D_F32 worldToCurr = mosaicAlg.getWorldToCurr();
		final Homography2D_F32 currToWorld = worldToCurr.invert(null);

		final int width = frame.width;
		final int height = frame.height;

		final int numAssociated = modelMatcher.getMatchSet().size();
		final int numFeatures = tracker.getActiveTracks().size();

		// update GUI
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				infoPanel.setFPS(fps);
				infoPanel.setNumInliers(numAssociated);
				infoPanel.setNumTracks(numFeatures);
				infoPanel.setKeyFrames(totalKeyFrames);
				infoPanel.repaint();
				
				BufferedImage out = gui.getImage();
				ConvertBufferedImage.convertTo(mosaicRender.getMosaic(), out);

				Graphics2D g2 = out.createGraphics();

				drawImageBounds(width,height, currToWorld, g2);
				drawFeatures(currToWorld, g2);

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
		mosaicRender.clear();

		startWorkerThread();
	}

	public static void main( String args[] ) {
		ImageMosaicApp app = new ImageMosaicApp(ImageFloat32.class, ImageFloat32.class, true);

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
