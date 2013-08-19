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

package boofcv.alg.tracker.tld;

import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.factory.tracker.FactoryTrackerAlg;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.FastQueue;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.shapes.RectangleCorner2D_F64;

import java.util.Random;

/**
 * <p>
 * Main class for Tracking-Learning-Detection (TLD) [1] (a.k.a Predator) object tracker for video sequences.
 * TLD tracks an object which is specified by a user using a rectangle.  The description of the object is
 * dynamically updated using P and N constraints.
 * </p>
 *
 * <p>
 * To start tracking {@link #initialize(boofcv.struct.image.ImageSingleBand, int, int, int, int)} must first be called
 * to specify the region being tracked.  Then each time a new image in the sequences arrives
 * {@link #track(boofcv.struct.image.ImageSingleBand)} is called.  Be sure to check its return value to see if tracking
 * was successful or not.  If tracking fails one frame it can recover.  This is often the case where an object
 * becomes obscured and then visible again.
 * </p>
 *
 * <p>
 * NOTE: This implementation is based the description found in [1].  The spirit of the original algorithm is replicated,
 * but there are a few algorithmic changes.  The most significant modifications are as follow; 1) The KLT tracker
 * used to update the rectangle does not use NCC features to validate a track or the median based outlier removal.
 * Instead a robust model matching algorithm finds the best fit motion. 2) The non-maximum suppression algorithm has
 * been changed so that it computes a more accurate local maximum and only uses local rectangles to
 * compute the average response.  See code for more details. Note, this is not a port of the OpenTLD project.
 * </p>
 * <p>
 * [1] Nebehay, G. "Robust Object Tracking Based on Tracking-Learning-Detection." Master's Thesis.
 * Faculty of Informatics, TU Vienna (2012).
 * </p>
 * @author Peter Abeles
 */
// TODO adjust detection variance threshold
//    if it has a track use previous estimate
//    if track is lost use initial value
// TODO for walking 2 and 3 need to handle tracking outside of image border
//      tracking can be adjusted to handle that case, but it needs to realize when the target is no longer
//      inside the view rectangle
public class TldTracker<T extends ImageSingleBand, D extends ImageSingleBand> {

	// specified configuration parameters for the tracker
	private TldConfig<T,D> config;

	// selected region for output
	private RectangleCorner2D_F64 targetRegion = new RectangleCorner2D_F64();

	// region selected by KLT tracker
	// NOTE: The tracker updates a pointing point region.  Rounding to the closest integer rectangle introduces errors
	//       which can build up.
	private RectangleCorner2D_F64 trackerRegion = new RectangleCorner2D_F64();
	private ImageRectangle trackerRegion_I32 = new ImageRectangle();

	// Region used inside detection cascade
	private FastQueue<ImageRectangle> cascadeRegions = new FastQueue<ImageRectangle>(ImageRectangle.class,true);

	// Image pyramid of input image
	private PyramidDiscrete<T> imagePyramid;

	// Tracks features inside the current region
	private TldRegionTracker<T,D> tracking;
	// Adjusts the region using track information
	private TldAdjustRegion adjustRegion;
	// Detects rectangles: Removes candidates which lack texture
	private TldVarianceFilter<T> variance;
	// Detects rectangles: Removes candidates don't match the fern descriptors
	private TldFernClassifier<T> fern;
	// Detects rectangles: Removes candidates don't match NCC descriptors
	private TldTemplateMatching<T> template;

	private TldDetection<T> detection;

	// did tracking totally fail and it needs to reacquire a track?
	private boolean reacquiring;

	// Is the region hypothesis valid and can be used for learning?
	private boolean valid;
	// Was the previous region hypothesis valid?
	private boolean previousValid;


	TldHelperFunctions helper = new TldHelperFunctions();

	TldLearning<T> learning;

	// random number generator
	private Random rand;

	// is learning on or off
	private boolean performLearning = true;

	LearningTask learningTask;

	/**
	 * Configures the TLD tracker
	 *
	 * @param config Configuration class which specifies the tracker's behavior
	 */
	public TldTracker( TldConfig<T,D> config ) {
		this.config = config;

		rand = new Random(config.randomSeed);

		PyramidKltTracker<T, D> tracker = FactoryTrackerAlg.kltPyramid(config.trackerConfig, config.imageType, config.derivType);

		tracking = new TldRegionTracker<T, D>(config.trackerGridWidth,config.trackerFeatureRadius,
				config.maximumErrorFB,config.gradient,tracker,config.imageType,config.derivType);
		adjustRegion = new TldAdjustRegion(config.motionIterations);
		variance = new TldVarianceFilter<T>(config.imageType);
		template = new TldTemplateMatching<T>(config.interpolate);
		fern = new TldFernClassifier<T>(
				rand,config.numFerns,config.fernSize,20,0.5f,config.interpolate);

		detection = new TldDetection<T>(fern,template,variance,config);
		learning = new TldLearning<T>(rand,5,5,config,template,variance,fern,detection,config.interpolate);
	}

	/**
	 * Starts tracking the rectangular region.
	 *
	 * @param image First image in the sequence.
	 * @param x0 Top-left corner of rectangle. x-axis
	 * @param y0 Top-left corner of rectangle. y-axis
	 * @param x1 Bottom-right corner of rectangle. x-axis
	 * @param y1 Bottom-right corner of rectangle. y-axis
	 */
	public void initialize( T image , int x0 , int y0 , int x1 , int y1 ) {

		if( imagePyramid == null ) {
			int scales[] = selectPyramidScale(image.width,image.height,config.trackerFeatureRadius);
			imagePyramid = FactoryPyramid.discreteGaussian(scales,-1,1,true,(Class<T>)image.getClass());
		}
		imagePyramid.process(image);

		reacquiring = false;

		targetRegion.set(x0, y0, x1, y1);
		createCascadeRegion(image.width,image.height);

		template.reset();
		fern.reset();

		tracking.initialize(imagePyramid);
		variance.setImage(image);
		template.setImage(image);
		fern.setImage(image);
		adjustRegion.init(image.width,image.height);

		previousValid = false;

		learning.initialLearning(targetRegion, cascadeRegions,false);
	}


	/**
	 * Creates a list containing all the regions which need to be tested
	 */
	private void createCascadeRegion( int imageWidth , int imageHeight ) {

		cascadeRegions.reset();

		int rectWidth = (int)(targetRegion.getWidth()+0.5);
		int rectHeight = (int)(targetRegion.getHeight()+0.5);

		for( int scaleInt = -10; scaleInt <= 10; scaleInt++ ) {
			// try several scales as specified in the paper
			double scale = Math.pow(1.2,scaleInt);

			// the actual rectangular region being tested at this scale
			int actualWidth = (int)(rectWidth*scale);
			int actualHeight = (int)(rectHeight*scale);

			// see if the region is too small or too large
			if( actualWidth < 25 || actualHeight < 25 )
				continue;

			if( actualWidth >= imageWidth || actualHeight >= imageHeight )
				continue;

			// step size at this scale
			int stepWidth = (int)(rectWidth*scale*0.1);
			int stepHeight = (int)(rectHeight*scale*0.1);

			if( stepWidth < 1 ) stepWidth = 1;
			if( stepHeight < 1 ) stepHeight = 1;

			// maximum allowed values
			int maxX = imageWidth-actualWidth;
			int maxY = imageHeight-actualHeight;

			// start at (1,1).  Otherwise a more complex algorithm needs to be used for integral images
			for( int y0 = 1; y0 < maxY; y0 += stepHeight ) {
				for( int x0 = 1; x0 < maxX; x0 += stepWidth) {
					ImageRectangle r = cascadeRegions.grow();

					r.x0 = x0;
					r.y0 = y0;
					r.x1 = x0 + actualWidth;
					r.y1 = y0 + actualHeight;
				}
			}
		}
	}

	/**
	 * Updates track region.
	 *
	 * @param image Next image in the sequence.
	 * @return true if the object could be found and false if not
	 */
	public boolean track( T image ) {

		System.out.println("----------------------- TRACKING ---------------------------");

		boolean success = true;
		valid = false;

		imagePyramid.process(image);
		template.setImage(image);
		variance.setImage(image);
		fern.setImage(image);

		if( reacquiring ) {
			// It can reinitialize if there is a single detection
			detection.detectionCascade(cascadeRegions);
			if( detection.isSuccess() && !detection.isAmbiguous() ) {
				TldRegion region = detection.getBest();
				System.out.println("Track reacquired: confidence = "+region.confidence);
				reacquiring = false;
				valid = false;
				// set it to the detected region
				ImageRectangle r = region.rect;
				targetRegion.set(r.x0, r.y0, r.x1, r.y1);
				// get tracking running again
				tracking.initialize(imagePyramid);
			} else {
				success = false;
			}
		} else {
			detection.detectionCascade(cascadeRegions);

			// update the previous track region using the tracker
			trackerRegion.set(targetRegion);
			boolean trackingWorked = tracking.process(imagePyramid, trackerRegion);
			trackingWorked &= adjustRegion.process(tracking.getPairs(), trackerRegion);
			TldHelperFunctions.convertRegion(trackerRegion, trackerRegion_I32);

			if( hypothesisFusion( trackingWorked , detection.isSuccess() ) ) {
				// if it found a hypothesis and it is valid for learning, then learn
				if( valid && performLearning ) {
					System.out.println("  learning type "+learningTask);
					switch( learningTask ) {
						case LEARN_POSITIVE:
							learning.initialLearning(targetRegion,cascadeRegions,true);
							break;

						case LEARN_NEGATIVE:
							learning.learnNegative(targetRegion);
							break;
					}
				}
			} else {
				reacquiring = true;
				success = false;
			}
		}

		previousValid = valid;

		return success;
	}

	/**
	 * Combines hypotheses from tracking and detection.
	 *
	 * @param trackingWorked If the sequential tracker updated the track region successfully or not
	 * @return true a hypothesis was found, false if it failed to find a hypothesis
	 */
	protected boolean hypothesisFusion( boolean trackingWorked , boolean detectionWorked ) {

		System.out.println(" FUSION: tracking "+trackingWorked+"  detection "+detectionWorked);

		valid = false;

		boolean uniqueDetection = detectionWorked && !detection.isAmbiguous();
		TldRegion detectedRegion = detection.getBest();

		double confidenceTarget = 0;

		if( trackingWorked ) {

			// get the scores from tracking and detection
			double scoreTrack = template.computeConfidence(trackerRegion_I32);
			double scoreDetected = 0;
			double overlap = 0;

			if( uniqueDetection ) {
				scoreDetected = detectedRegion.confidence;
				overlap = helper.computeOverlap(trackerRegion_I32, detectedRegion.rect);
			}

			System.out.println("FUSION: score track "+scoreTrack+" detection "+scoreDetected);

			if( uniqueDetection && scoreDetected > scoreTrack ) {
				System.out.println("FUSION: using detection region");
				// if there is a unique detection and it has higher confidence than the
				// track region, use the detected region
				TldHelperFunctions.convertRegion(detectedRegion.rect, targetRegion);
				confidenceTarget = detectedRegion.confidence;
			} else {
				System.out.println("FUSION: using track region");
				// Otherwise use the tracker region
				targetRegion.set(trackerRegion);
				confidenceTarget = scoreTrack;

				// see if the most likely detected region overlaps the track region
				if( scoreTrack >= config.confidenceThresholdLower )  {
//					if( detectionWorked && !uniqueDetection) {
//						learningTask = LearningTask.LEARN_NEGATIVE;
//					} else { //if( !detectionWorked ){
						learningTask = LearningTask.LEARN_POSITIVE;
//					} else {
//						learningTask = LearningTask.NOTHING;
//					}
					valid = true;
//				} else if( previousValid && scoreTrack >= config.confidenceThresholdLower) {
//					learningTask = LearningTask.LEARN_POSITIVE;
//					valid = true;
//				} else {
//					learningTask = LearningTask.NOTHING;
//					valid = true;
				}
			}
		} else if( uniqueDetection ) {
			System.out.println("FUSION: tracker failed, using detection ");
			// just go with the best detected region
			detectedRegion = detection.getBest();
			TldHelperFunctions.convertRegion(detectedRegion.rect, targetRegion);
			confidenceTarget = detectedRegion.confidence;
		} else {
			System.out.println("FUSION: failed");
			return false;
		}
		System.out.println("FUSION: valid = "+valid+" confidence "+confidenceTarget);

		return confidenceTarget >= config.confidenceAccept;
	}

	/**
	 * Selects the scale for the image pyramid based on image size and feature size
	 * @return scales for image pyramid
	 */
	public static int[] selectPyramidScale( int imageWidth , int imageHeight, int featureRadius ) {
		int w = Math.max(imageWidth,imageHeight);
		int minSize = (featureRadius*2+1)*5;

		int maxScale = w/minSize;
		int n = 1;
		int scale = 1;
		while( scale*2 < maxScale ) {
			n++;
			scale *= 2;
		}

		int ret[] = new int[n];
		scale = 1;
		for( int i = 0; i < n; i++ ) {
			ret[i] = scale;
			scale *= 2;
		}

		return ret;
	}

	private static enum LearningTask {
		NOTHING,
		LEARN_POSITIVE,
		LEARN_NEGATIVE
	}

	public boolean isPerformLearning() {
		return performLearning;
	}

	public void setPerformLearning(boolean performLearning) {
		this.performLearning = performLearning;
	}

	public TldTemplateMatching<T> getTemplateMatching() {
		return template;
	}

	/**
	 * Returns the estimated location of the target in the current image
	 * @return Location of the target
	 */
	public RectangleCorner2D_F64 getTargetRegion() {
		return targetRegion;
	}

	public RectangleCorner2D_F64 getTrackerRegion() {
		return trackerRegion;
	}

	public TldConfig<T, D> getConfig() {
		return config;
	}

	public TldDetection<T> getDetection() {
		return detection;
	}
}
