/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.sfm.d2;

import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.AccessPointTracks;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.abst.sfm.d2.PlToGrayMotion2D;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.sfm.d2.StitchingFromMotion2D;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.sfm.FactoryMotion2D;
import boofcv.gui.DemonstrationBase;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent class for applications which estimate image motion based upon fit parameters to a model on extracted
 * point features.  Only gray scale images are processed, but the output can be in gray scale or color.
 *
 * @author Peter Abeles
 */
public abstract class VideoStitchBaseApp<I extends ImageBase<I>, IT extends InvertibleTransform>
		extends DemonstrationBase implements ImageMotionInfoPanel.Listener
{
	// size of the image being stitched into
	int stitchWidth;
	int stitchHeight;

	// show a rectangle around the view be shown
	boolean showImageView;

	int borderTolerance = 30;

	// tracks feature in the video stream
	protected PointTracker<I> tracker;

	BufferedImage stitchOut;

	StitchingFromMotion2D alg;
	StitchingFromMotion2D.Corners corners;

	// number of times stitching has failed and it was reset
	int totalResets;

	private final static int maxIterations = 100;

	protected Motion2DPanel gui;
	protected ImageMotionInfoPanel infoPanel = new ImageMotionInfoPanel(this);

	// stabilization parameters
	protected int absoluteMinimumTracks;
	protected double respawnTrackFraction;
	protected double respawnCoverageFraction;
	protected double maxJumpFraction;
	protected double inlierThreshold;

	private static int maxFeatures = 350;

	private boolean algorithmChanged = false;

	// TODO Specify tracker and motion model in info panel

	public VideoStitchBaseApp(List<?> exampleInputs , Motion2DPanel gui, boolean color , Class imageType ) {
		super(true,true,exampleInputs,
				color ? ImageType.pl(3, imageType) : ImageType.single(imageType));

		this.gui = gui;
		gui.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				streamPaused = !streamPaused;
			}
		});

		infoPanel.setMaximumSize(infoPanel.getPreferredSize());
		add(infoPanel, BorderLayout.WEST);
		add(gui,BorderLayout.CENTER);
	}

	protected void setStitchImageSize( int width , int height ) {
		this.stitchWidth = width;
		this.stitchHeight = height;

		stitchOut = new BufferedImage(stitchWidth, stitchHeight,BufferedImage.TYPE_INT_RGB);
	}

	@Override
	protected void configureVideo(int which, SimpleImageSequence sequence) {
		super.configureVideo(which, sequence);
		sequence.setLoop(true);
	}

	protected void handleAlgorithmChange() {
		tracker = createTracker();
		alg = createAlgorithm(tracker);
	}

	protected PointTracker<I> createTracker() {
		PkltConfig config = new PkltConfig();
		config.templateRadius = 3;
		config.pyramidScaling = new int[]{1,2,4,8};

		ConfigFastHessian configFH = new ConfigFastHessian();
		configFH.initialSampleSize = 2;
		configFH.maxFeaturesPerScale = 250;

		ImageType imageType = super.getImageType(0);
		Class imageClass = imageType.getImageClass();
		Class derivClass = GImageDerivativeOps.getDerivativeType(imageClass);

		FeatureTrackerTypes type = FeatureTrackerTypes.values()[infoPanel.tracker];
		switch( type ) {
			case KLT:
				return FactoryPointTracker.klt(config, new ConfigGeneralDetector(maxFeatures, 3, 1),
						imageClass, derivClass);
			case ST_BRIEF:
				return FactoryPointTracker. dda_ST_BRIEF(150,
						new ConfigGeneralDetector(400, 1, 10), imageClass, null);
			case FH_SURF:
				return FactoryPointTracker.dda_FH_SURF_Fast(configFH, null, null, imageClass);
			case ST_SURF_KLT:
				return FactoryPointTracker.combined_FH_SURF_KLT(
						config, 100, configFH, null, null, imageClass);

			default:
				throw new RuntimeException("Unknown tracker: "+type);
		}
	}

	protected StitchingFromMotion2D createAlgorithm( PointTracker<I> tracker ) {

		ImageType imageType = super.getImageType(0);

		IT fitModel = createFitModelStructure();

		if( imageType.getFamily() == ImageType.Family.PLANAR) {
			Class imageClass = imageType.getImageClass();

			ImageMotion2D<I,IT> motion = FactoryMotion2D.createMotion2D(maxIterations,inlierThreshold,2,absoluteMinimumTracks,
					respawnTrackFraction,respawnCoverageFraction,false,tracker,fitModel);

			ImageMotion2D motion2DColor = new PlToGrayMotion2D(motion,imageClass);

			return FactoryMotion2D.createVideoStitch(maxJumpFraction,motion2DColor, imageType);
		} else {
			ImageMotion2D motion = FactoryMotion2D.createMotion2D(maxIterations,inlierThreshold,2,absoluteMinimumTracks,
					respawnTrackFraction,respawnCoverageFraction,false,tracker,fitModel);

			return FactoryMotion2D.createVideoStitch(maxJumpFraction,motion, imageType);
		}
	}

	protected IT createFitModelStructure() {
		IT fitModel;
		switch( infoPanel.motionModels ) {
			case 0: fitModel = (IT)new Affine2D_F64();break;
			case 1: fitModel = (IT)new Homography2D_F64();break;
			default:
				throw new IllegalArgumentException("Unknown motion model");
		}
		return fitModel;
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {
		if( algorithmChanged ) {
			algorithmChanged = false;
			handleAlgorithmChange();
		}

		if( alg == null )
			return;

		long time0 = System.nanoTime();
		if( infoPanel.resetRequested() ) {
			totalResets = 0;
			alg.reset();
		} else if( !alg.process((I)input) ) {
			alg.reset();
			totalResets++;
		}
		long time1 = System.nanoTime();

		updateGUI((I)input, buffered, (time1-time0)*1e-6 );
	}

	void updateGUI(I frame, BufferedImage imageGUI, final double timeMS) {

		corners = alg.getImageCorners(frame.width,frame.height,null);
		ConvertBufferedImage.convertTo(alg.getStitchedImage(), stitchOut,true);

		if( checkLocation(corners) ) {
			// the change will only be visible in the next update
			alg.setOriginToCurrent();
		}

		AccessPointTracks access = (AccessPointTracks)alg.getMotion();

		List<Point2D_F64> tracks = access.getAllTracks();
		List<Point2D_F64> inliers = new ArrayList<>();

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
		SwingUtilities.invokeLater(() -> {
			// update GUI
			infoPanel.setPeriodMS(timeMS);
			infoPanel.setNumInliers(numInliers);
			infoPanel.setNumTracks(numFeatures);
			infoPanel.setKeyFrames(totalResets);
			infoPanel.repaint();

			gui.repaint();
		});
	}

	@Override
	public void handleUserChangeAlgorithm() {
		// start processing the input again and change the tracker
		algorithmChanged = true;
		reprocessInput();
	}

	/**
	 * Checks the location of the stitched region and decides if its location should be reset
	 */
	protected abstract boolean checkLocation( StitchingFromMotion2D.Corners corners );
}
