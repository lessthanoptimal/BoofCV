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

import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.AccessPointTracks;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.abst.sfm.d2.MsToGrayMotion2D;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.sfm.FactoryMotion2D;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.VisualizeApp;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent class for applications which estimate image motion based upon fit parameters to a model on extracted
 * point features.  Only gray scale images are processed, but the output can be in gray scale or color.
 *
 * @author Peter Abeles
 */
public abstract class VideoStitchBaseApp<I extends ImageBase, IT extends InvertibleTransform>
		extends VideoProcessAppBase<I> implements VisualizeApp
{
	// size of input image
	int inputWidth,inputHeight;

	// size of the image being stitched into
	int stitchWidth;
	int stitchHeight;

	// show a rectangle around the view be shown
	boolean showImageView;

	int borderTolerance = 30;

	// data type which is being fit
	IT fitModel;

	// tracks feature in the video stream
	protected PointTracker<I> tracker;

	BufferedImage stitchOut;

	StitchingFromMotion2D alg;

	StitchingFromMotion2D.Corners corners;

	// number of times stitching has failed and it was reset
	int totalResets;

	private final static int maxIterations = 100;
	private final static int pruneThreshold = 10;

	boolean hasProcessedImage = false;

	protected Motion2DPanel gui;
	protected ImageMotionInfoPanel infoPanel = new ImageMotionInfoPanel();

	// stabilization parameters
	protected int absoluteMinimumTracks;
	protected double respawnTrackFraction;
	protected double respawnCoverageFraction;
	protected double maxJumpFraction;
	protected double inlierThreshold;

	public VideoStitchBaseApp(int numAlgFamilies,
							  Class imageType,
							  boolean color ,
							  Motion2DPanel gui) {
		super(numAlgFamilies, color ? ImageType.ms(3, imageType) : ImageType.single(imageType));

		this.gui = gui;
		gui.addMouseListener(this);
		setMainGUI(gui);

		infoPanel.setMaximumSize(infoPanel.getPreferredSize());
		add(infoPanel, BorderLayout.WEST);
	}

	protected void setStitchImageSize( int width , int height ) {
		this.stitchWidth = width;
		this.stitchHeight = height;

		stitchOut = new BufferedImage(stitchWidth, stitchHeight,BufferedImage.TYPE_INT_RGB);
	}

	protected StitchingFromMotion2D createAlgorithm( PointTracker<I> tracker ) {

		if( imageType.getFamily() == ImageType.Family.MULTI_SPECTRAL ) {
			Class imageType = this.imageType.getImageClass();

			ImageMotion2D<I,IT> motion = FactoryMotion2D.createMotion2D(maxIterations,inlierThreshold,2,absoluteMinimumTracks,
					respawnTrackFraction,respawnCoverageFraction,false,tracker,fitModel);

			ImageMotion2D motion2DColor = new MsToGrayMotion2D(motion,imageType);

			return FactoryMotion2D.createVideoStitchMS(maxJumpFraction,motion2DColor,imageType);
		} else {
			ImageMotion2D motion = FactoryMotion2D.createMotion2D(maxIterations,inlierThreshold,2,absoluteMinimumTracks,
					respawnTrackFraction,respawnCoverageFraction,false,tracker,fitModel);

			return FactoryMotion2D.createVideoStitch(maxJumpFraction,motion, imageType.getImageClass());
		}
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return alg != null;
	}

	@Override
	protected void process(SimpleImageSequence<I> sequence) {
		if( !sequence.hasNext() )
			return;
		// stop the image processing code
		stopWorker();

		this.sequence = sequence;
		sequence.setLoop(true);

		// save the input image dimension
		I input = sequence.next();
		inputWidth = input.width;
		inputHeight = input.height;

		// start everything up and resume processing
		doRefreshAll();
	}

	@Override
	protected void updateAlg(I frame, BufferedImage buffImage) {
		if( alg == null )
			return;

		if( !alg.process(frame) ) {
			alg.reset();
			totalResets++;
		}

		hasProcessedImage = true;
	}

	@Override
	protected void updateAlgGUI(I frame, BufferedImage imageGUI, final double fps) {
		if( !hasProcessedImage )
			return;

		corners = alg.getImageCorners(frame.width,frame.height,null);
		ConvertBufferedImage.convertTo(alg.getStitchedImage(), stitchOut,true);

		if( checkLocation(corners) ) {
			// the change will only be visible in the next update
			alg.setOriginToCurrent();
		}

		AccessPointTracks access = (AccessPointTracks)alg.getMotion();

		List<Point2D_F64> tracks = access.getAllTracks();
		List<Point2D_F64> inliers = new ArrayList<Point2D_F64>();

		for( int i = 0; i < tracks.size(); i++ ) {
			if( access.isInlier(i) )
				inliers.add( tracks.get(i) );
		}

		final int numInliers = inliers.size();
		final int numFeatures = tracks.size();

		showImageView = infoPanel.getShowView();

		gui.setImages(imageGUI, stitchOut);
		gui.setShowImageView(infoPanel.getShowView());
		gui.setCorners(corners);

		// toggle on and off showing the active tracks
		if( infoPanel.getShowInliers())
			gui.setInliers(inliers);
		else
			gui.setInliers(null);
		if( infoPanel.getShowAll())
			gui.setAllTracks(tracks);
		else
			gui.setAllTracks(null);

		Homography2D_F64 H = alg.getWorldToCurr(null).invert(null);
		gui.setCurrToWorld(H);

		// update GUI
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// update GUI
				infoPanel.setFPS(fps);
				infoPanel.setNumInliers(numInliers);
				infoPanel.setNumTracks(numFeatures);
				infoPanel.setKeyFrames(totalResets);
				infoPanel.repaint();

				gui.repaint();
			}
		});
	}

	@Override
	public void refreshAll(Object[] cookies) {
		stopWorker();

		tracker = (PointTracker<I>)cookies[0];
		fitModel = (IT)cookies[1];

		startEverything();
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( sequence == null )
			return;

		stopWorker();

		switch( indexFamily ) {
			case 0:
				tracker = (PointTracker<I>)cookie;
				break;

			case 1:
				fitModel = (IT)cookie;
				break;
		}

		// restart the video
		sequence.reset();

		startEverything();
	}

	protected void startEverything() {
		// make sure there is nothing left over from before
		tracker.dropAllTracks();
		alg = createAlgorithm(tracker);
		init(inputWidth,inputHeight);
		totalResets = 0;

		startWorkerThread();
	}

	protected abstract void init( int inputWidth , int inputHeight );

	/**
	 * Checks the location of the stitched region and decides if its location should be reset
	 */
	protected abstract boolean checkLocation( StitchingFromMotion2D.Corners corners );
}
