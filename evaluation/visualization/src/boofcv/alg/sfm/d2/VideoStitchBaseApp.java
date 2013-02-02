/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.abst.sfm.d2.WrapImageMotionPtkSmartRespawn;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.sfm.robust.DistanceAffine2DSq;
import boofcv.alg.sfm.robust.DistanceHomographySq;
import boofcv.alg.sfm.robust.GenerateAffine2D;
import boofcv.alg.sfm.robust.GenerateHomographyLinear;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.VisualizeApp;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homo.Homography2D_F64;
import georegression.transform.ConvertTransform_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Parent class for applications which estimate image motion based upon fit parameters to a model on extracted
 * point features.  Only gray scale images are processed, but the output can be in gray scale or color.
 *
 * @author Peter Abeles
 */
public abstract class VideoStitchBaseApp<I extends ImageSingleBand, IT extends InvertibleTransform>
		extends VideoProcessAppBase<I> implements VisualizeApp
{
	int inputWidth,inputHeight;

	int stitchWidth = 1000;
	int stitchHeight = 600;

	boolean showImageView;

	int borderTolerance = 30;

	// data type which is being fit
	IT fitModel;

	// tracks feature in the video stream
	protected PointTracker<I> tracker;
	// finds the best fit model parameters to describe feature motion
	protected ModelMatcher<IT,AssociatedPair> modelMatcher;
	// batch refinement algorithm
	protected ModelFitter<IT,AssociatedPair> modelRefiner;

	BufferedImage stitchOut;

	StitchingFromMotion2D alg;

	StitchingFromMotion2D.Corners corners;

	protected Affine2D_F64 initialTransform;

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

	public VideoStitchBaseApp(int numAlgFamilies,
							  Class<I> imageType,
							  Motion2DPanel gui) {
		super(numAlgFamilies, imageType);

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

		createModelMatcher(maxIterations, 4);
		InterpolatePixel<I> interp = FactoryInterpolation.createPixel(0, 255, TypeInterpolate.BILINEAR, imageType);

		IT worldToInit;
		if( initialTransform == null )
			worldToInit = (IT)fitModel.createInstance();
		else
			worldToInit = (IT) ConvertTransform_F64.convert(initialTransform, fitModel.createInstance());

		ImageMotionPointTrackerKey<I,IT> motionAlg =
				new ImageMotionPointTrackerKey<I, IT>(tracker,modelMatcher,modelRefiner,
						(IT)fitModel.createInstance(),pruneThreshold);

		ImageMotionPtkSmartRespawn<I,IT> motionAlg2 =
				new ImageMotionPtkSmartRespawn<I, IT>(motionAlg,
						absoluteMinimumTracks,respawnTrackFraction,respawnCoverageFraction );


		ImageMotion2D<I,IT> motion = new WrapImageMotionPtkSmartRespawn<I,IT>(motionAlg2);

		ImageDistort<I> distorter = FactoryDistort.distort(interp, null, imageType);

		StitchingTransform transform;

		if( worldToInit instanceof Affine2D_F64 ) {
			transform = FactoryStitchingTransform.createAffine_F64();
		} else {
			transform = FactoryStitchingTransform.createHomography_F64();
		}

		return new StitchingFromMotion2D<I, IT>(
				motion,distorter,transform,maxJumpFraction,worldToInit, stitchWidth, stitchHeight);
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return alg != null;
	}

	/**
	 * Create a {@link org.ddogleg.fitting.modelset.ModelMatcher} for the type of model it is fitting to
	 *
	 * @param maxIterations Maximum number of iterations in RANSAC
	 * @param thresholdFit Inlier fit threshold
	 */
	protected void createModelMatcher( int maxIterations , double thresholdFit ) {

		ModelGenerator fitter;
		DistanceFromModel distance;

		if( fitModel instanceof Homography2D_F64 ) {
			GenerateHomographyLinear mf = new GenerateHomographyLinear(true);
			fitter = mf;
			modelRefiner = (ModelFitter)mf;
			distance = new DistanceHomographySq();
		} else if( fitModel instanceof Affine2D_F64 ) {
			GenerateAffine2D mf = new GenerateAffine2D();
			fitter = mf;
			distance = new DistanceAffine2DSq();
			modelRefiner = (ModelFitter)mf;
		} else {
			throw new RuntimeException("Unknown model type");
		}

		modelMatcher = new Ransac(123123,fitter,distance,maxIterations,thresholdFit);

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

		init(inputWidth,inputHeight);

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
		ConvertBufferedImage.convertTo(alg.getStitchedImage(), stitchOut);

		if( checkLocation(corners) ) {
			// the change will only be visible in the next update
			alg.setOriginToCurrent();
		}

		final int numAssociated = modelMatcher.getMatchSet().size();
		final int numFeatures = tracker.getActiveTracks(null).size();

		showImageView = infoPanel.getShowView();

		gui.setImages(imageGUI, stitchOut);
		gui.setShowImageView(infoPanel.getShowView());
		gui.setCorners(corners);

		// toggle on and off showing the active tracks
		if( infoPanel.getShowInliers())
			gui.setInliers(modelMatcher.getMatchSet());
		else
			gui.setInliers(null);
		if( infoPanel.getShowAll())
			gui.setAllTracks(tracker.getActiveTracks(null));
		else
			gui.setAllTracks(null);

		Homography2D_F64 H = alg.getWorldToCurr(null).invert(null);
		gui.setCurrToWorld(H);

		// update GUI
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// update GUI
				infoPanel.setFPS(fps);
				infoPanel.setNumInliers(numAssociated);
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
		if( sequence == null || modelMatcher == null )
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
		totalResets = 0;

		startWorkerThread();
	}

	protected abstract void init( int inputWidth , int inputHeight );

	/**
	 * Checks the location of the stitched region and decides if its location should be reset
	 */
	protected abstract boolean checkLocation( StitchingFromMotion2D.Corners corners );
}
