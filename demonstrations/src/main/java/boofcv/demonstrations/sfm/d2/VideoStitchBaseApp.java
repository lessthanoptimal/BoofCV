/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.sfm.AccessPointTracks;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.abst.sfm.d2.PlToGrayMotion2D;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.sfm.d2.StitchingFromMotion2D;
import boofcv.factory.sfm.FactoryMotion2D;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.gui.DemonstrationBase;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ddogleg.struct.DogArray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent class for applications which estimate image motion based upon fit parameters to a model on extracted
 * point features. Only gray scale images are processed, but the output can be in gray scale or color.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class VideoStitchBaseApp<I extends ImageBase<I>, IT extends InvertibleTransform<IT>>
		extends DemonstrationBase implements ImageMotionInfoPanel.AlgorithmListener {
	// size of the image being stitched into
	int stitchWidth;
	int stitchHeight;

	// show a rectangle around the view be shown
	boolean showImageView;

	int borderTolerance = 30;

	// tracks feature in the video stream
	protected PointTracker<I> tracker;

	BufferedImage stitchOut;

	StitchingFromMotion2D<I, IT> alg;
	Quadrilateral_F64 corners;

	// number of times stitching has failed and it was reset
	int totalResets;

	// what video frame is being processed
	long frameID;

	private final static int maxIterations = 100;

	protected Motion2DPanel gui;
	protected ImageMotionInfoPanel infoPanel = new ImageMotionInfoPanel();

	// stabilization parameters
	protected int absoluteMinimumTracks;
	protected double respawnTrackFraction;
	protected double respawnCoverageFraction;
	protected double maxJumpFraction;
	protected double inlierThreshold;

	private static int maxFeatures = 350;

	private boolean algorithmChanged = false;

	//--------------------- BEGIN LOCK
	final Object trackLock = new Object();
	DogArray<Point2D_F64> allTracks = new DogArray<>(Point2D_F64::new);
	List<Point2D_F64> inliers = new ArrayList<>();
	//--------------------- END

	// TODO Specify tracker and motion model in info panel

	protected VideoStitchBaseApp( List<?> exampleInputs, Motion2DPanel gui, boolean color, Class imageType ) {
		super(true, true, exampleInputs,
				color ? ImageType.pl(3, imageType) : ImageType.single(imageType));

		this.gui = gui;
		gui.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e ) {
				streamPaused = !streamPaused;
			}
		});

		ConfigPointTracker config = infoPanel.configTracker;
		config.typeTracker = ConfigPointTracker.TrackerType.KLT;
		config.klt.maximumTracks.setFixed(maxFeatures);
		config.klt.templateRadius = 3;
		config.klt.pruneClose = true;
		config.klt.toleranceFB = 4;
		config.detDesc.detectPoint.general.maxFeatures = maxFeatures;
		config.detDesc.detectPoint.general.radius = 3;
		config.detDesc.detectPoint.general.threshold = 1;

		config.detDesc.detectFastHessian.initialSampleStep = 2;
		config.detDesc.detectFastHessian.maxFeaturesPerScale = 250;
		config.dda.maxInactiveTracks = 100;
	}

	protected void initializeGui() {
		infoPanel.listenerAlg = this;
		infoPanel.listenerVis = () -> {
			updateShowFlags();
			repaint();
		};
		infoPanel.initializeGui();
		add(infoPanel, BorderLayout.WEST);
		add(gui, BorderLayout.CENTER);

		updateShowFlags();
	}

	private void updateShowFlags() {
		gui.showInliers = infoPanel.showInliers;
		gui.showAll = infoPanel.showAll;
		gui.showImageView = infoPanel.showView;
	}

	protected void setStitchImageSize( int width, int height ) {
		this.stitchWidth = width;
		this.stitchHeight = height;

		stitchOut = new BufferedImage(stitchWidth, stitchHeight, BufferedImage.TYPE_INT_RGB);
	}

	@Override
	protected void configureVideo( int which, SimpleImageSequence sequence ) {
		super.configureVideo(which, sequence);
		sequence.setLoop(true);
	}

	protected void handleAlgorithmChange() {
		tracker = createTracker();
		alg = createAlgorithm(tracker);
	}

	protected PointTracker<I> createTracker() {
		return infoPanel.panelTrackers.createTracker(super.getImageType(0));
	}

	protected StitchingFromMotion2D<I, IT> createAlgorithm( PointTracker<I> tracker ) {

		ImageType<I> imageType = super.getImageType(0);

		IT fitModel = createFitModelStructure();

		if (imageType.getFamily() == ImageType.Family.PLANAR) {
			Class imageClass = imageType.getImageClass();

			ImageMotion2D<I, IT> motion = FactoryMotion2D.createMotion2D(maxIterations, inlierThreshold, 2, absoluteMinimumTracks,
					respawnTrackFraction, respawnCoverageFraction, false, tracker, fitModel);

			ImageMotion2D<I, IT> motion2DColor = new PlToGrayMotion2D(motion, imageClass);

			return FactoryMotion2D.createVideoStitch(maxJumpFraction, motion2DColor, imageType);
		} else {
			ImageMotion2D<I, IT> motion = FactoryMotion2D.createMotion2D(maxIterations, inlierThreshold, 2, absoluteMinimumTracks,
					respawnTrackFraction, respawnCoverageFraction, false, tracker, fitModel);

			return FactoryMotion2D.createVideoStitch(maxJumpFraction, motion, imageType);
		}
	}

	protected IT createFitModelStructure() {
		IT fitModel = switch (infoPanel.motionModels) {
			case 0 -> (IT)new Affine2D_F64();
			case 1 -> (IT)new Homography2D_F64();
			default -> throw new IllegalArgumentException("Unknown motion model");
		};
		return fitModel;
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {
		if (algorithmChanged) {
			algorithmChanged = false;
			handleAlgorithmChange();
		}

		if (alg == null)
			return;

		long time0 = System.nanoTime();
		if (infoPanel.resetRequested()) {
			totalResets = 0;
			alg.reset();
		} else if (!alg.process((I)input)) {
			alg.reset();
			totalResets++;
		}
		long time1 = System.nanoTime();

		this.frameID = frameID;

		updateGUI((I)input, buffered, (time1 - time0)*1e-6);
	}

	void updateGUI( I frame, BufferedImage imageGUI, final double timeMS ) {
		corners = alg.getImageCorners(frame.width, frame.height, null);
		ConvertBufferedImage.convertTo(alg.getStitchedImage(), stitchOut, true);

		if (checkLocation(corners)) {
			// the change will only be visible in the next update
			alg.setOriginToCurrent();
		}

		AccessPointTracks access = (AccessPointTracks)alg.getMotion();

		synchronized (trackLock) {
			allTracks.reset();
			inliers.clear();
			for (int i = 0; i < access.getTotalTracks(); i++) {
				access.getTrackPixel(i, allTracks.grow());
				if (access.isTrackNew(i))
					continue;
				if (access.isTrackInlier(i))
					inliers.add(allTracks.getTail());
			}
		}

		final int numInliers = inliers.size();
		final int numFeatures = allTracks.size();

		showImageView = infoPanel.isShowView();

		Homography2D_F64 H = alg.getWorldToCurr(null).invert(null);

		// NOTE: imageGUI and stitchOut could be modified while this is being called..
		//       should double buffer to be safe and avoid artifacts
		// Doing this outside of UI thread to avoid slowing the UI down, but more chance of artifacts
		gui.updateImages(imageGUI, stitchOut);

		// update GUI
		SwingUtilities.invokeLater(() -> {
			gui.setCorners(corners);
			gui.setCurrToWorld(H);
			synchronized (trackLock) {
				gui.setInliers(inliers);
				gui.setAllTracks(allTracks.toList());
			}
			infoPanel.setPeriodMS(timeMS);
			infoPanel.setFrameID(frameID);
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
	protected abstract boolean checkLocation( Quadrilateral_F64 corners );
}
