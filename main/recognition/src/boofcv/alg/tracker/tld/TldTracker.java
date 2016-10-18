/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.factory.tracker.FactoryTrackerAlg;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.shapes.Rectangle2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.Random;

/**
 * <p>
 * Main class for Tracking-Learning-Detection (TLD) [1] (a.k.a Predator) object tracker for video sequences.
 * TLD tracks an object which is specified by a user using a rectangle.  The description of the object is
 * dynamically updated using P and N constraints.
 * </p>
 *
 * <p>
 * To start tracking {@link #initialize(ImageGray, int, int, int, int)} must first be called
 * to specify the region being tracked.  Then each time a new image in the sequences arrives
 * {@link #track(ImageGray)} is called.  Be sure to check its return value to see if tracking
 * was successful or not.  If tracking fails one frame it can recover.  This is often the case where an object
 * becomes obscured and then visible again.
 * </p>
 *
 * <p>
 * NOTE: This implementation is based the description found in [1].  The spirit of the original algorithm is replicated,
 * but there are a several algorithmic changes.  The most significant modifications are as follow; 1) The KLT tracker
 * used to update the rectangle does not use NCC features to validate a track or the median based outlier removal.
 * Instead a robust model matching algorithm finds the best fit motion. 2) The non-maximum suppression algorithm has
 * been changed so that it computes a more accurate local maximum and only uses local rectangles to
 * compute the average response.  3) Fern selection is done by selecting the N best using a likelihood ratio
 * conditional on the current image.  4) Learning only happens when a track is considered strong.
 * See code for more details. Note, this is not a port of the OpenTLD project.
 * </p>
 * <p>
 * [1] Zdenek Kalal, "Tracking-Learning-Detection" University of Surrey, April 2011 Phd Thesis.
 * </p>
 * @author Peter Abeles
 */
public class TldTracker<T extends ImageGray, D extends ImageGray> {

	// specified configuration parameters for the tracker
	private TldParameters config;

	// selected region for output
	private Rectangle2D_F64 targetRegion = new Rectangle2D_F64();

	// region selected by KLT tracker
	// NOTE: The tracker updates a pointing point region.  Rounding to the closest integer rectangle introduces errors
	//       which can build up.
	private Rectangle2D_F64 trackerRegion = new Rectangle2D_F64();
	private ImageRectangle trackerRegion_I32 = new ImageRectangle();

	// Region used inside detection cascade
	private FastQueue<ImageRectangle> cascadeRegions = new FastQueue<>(ImageRectangle.class, true);

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
	// code for detection cascade
	private TldDetection<T> detection;

	// did tracking totally fail and it needs to reacquire a track?
	private boolean reacquiring;

	// Is the region hypothesis valid and can be used for learning?
	private boolean valid;

	// is the current track considered a strong match and learning can occur?
	private boolean strongMatch;
	// area of the previous track before it lost track
	private double previousTrackArea;

	private TldLearning<T> learning;

	// is learning on or off
	private boolean performLearning = true;

	/**
	 * Configures the TLD tracker
	 *
	 * @param config Configuration class which specifies the tracker's behavior
	 */
	public TldTracker( TldParameters config ,
					   InterpolatePixelS<T> interpolate , ImageGradient<T,D> gradient ,
					   Class<T> imageType , Class<D> derivType) {
		this.config = config;

		Random rand = new Random(config.randomSeed);

		PyramidKltTracker<T, D> tracker = FactoryTrackerAlg.kltPyramid(config.trackerConfig, imageType, derivType);

		tracking = new TldRegionTracker<>(config.trackerGridWidth, config.trackerFeatureRadius,
				config.maximumErrorFB, gradient, tracker, imageType, derivType);
		adjustRegion = new TldAdjustRegion(config.motionIterations);
		variance = new TldVarianceFilter<>(imageType);
		template = new TldTemplateMatching<>(interpolate);
		fern = new TldFernClassifier<>(
				rand, config.numFerns, config.fernSize, 20, 0.5f, interpolate);

		detection = new TldDetection<>(fern, template, variance, config);
		learning = new TldLearning<>(rand, config, template, variance, fern, detection);
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

		if( imagePyramid == null ||
				imagePyramid.getInputWidth() != image.width || imagePyramid.getInputHeight() != image.height ) {
			int minSize = (config.trackerFeatureRadius*2+1)*5;
			int scales[] = selectPyramidScale(image.width,image.height,minSize);
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

		learning.initialLearning(targetRegion, cascadeRegions);
		strongMatch = true;
		previousTrackArea = targetRegion.area();
	}


	/**
	 * Creates a list containing all the regions which need to be tested
	 */
	private void createCascadeRegion( int imageWidth , int imageHeight ) {

		cascadeRegions.reset();

		int rectWidth = (int)(targetRegion.getWidth()+0.5);
		int rectHeight = (int)(targetRegion.getHeight()+0.5);

		for( int scaleInt = -config.scaleSpread; scaleInt <= config.scaleSpread; scaleInt++ ) {
			// try several scales as specified in the paper
			double scale = Math.pow(1.2,scaleInt);

			// the actual rectangular region being tested at this scale
			int actualWidth = (int)(rectWidth*scale);
			int actualHeight = (int)(rectHeight*scale);

			// see if the region is too small or too large
			if( actualWidth < config.detectMinimumSide || actualHeight < config.detectMinimumSide )
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

				reacquiring = false;
				valid = false;
				// set it to the detected region
				ImageRectangle r = region.rect;
				targetRegion.set(r.x0, r.y0, r.x1, r.y1);
				// get tracking running again
				tracking.initialize(imagePyramid);

				checkNewTrackStrong(region.confidence);

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
					learning.updateLearning(targetRegion);
				}

			} else {
				reacquiring = true;
				success = false;
			}
		}

		if( strongMatch ) {
			previousTrackArea = targetRegion.area();
		}


		return success;
	}

	private void checkNewTrackStrong( double confidence ) {
		// see if there is very high confidence of a match
		strongMatch = confidence > config.confidenceThresholdStrong;
		// otherwise see if it's the expected shape
		if( !strongMatch ) {
			double similarity = Math.abs( (targetRegion.area() - previousTrackArea)/previousTrackArea);
			strongMatch = similarity <= config.thresholdSimilarArea;
		}
	}

	/**
	 * Combines hypotheses from tracking and detection.
	 *
	 * @param trackingWorked If the sequential tracker updated the track region successfully or not
	 * @return true a hypothesis was found, false if it failed to find a hypothesis
	 */
	protected boolean hypothesisFusion( boolean trackingWorked , boolean detectionWorked ) {

		valid = false;

		boolean uniqueDetection = detectionWorked && !detection.isAmbiguous();
		TldRegion detectedRegion = detection.getBest();

		double confidenceTarget;

		if( trackingWorked ) {

			// get the scores from tracking and detection
			double scoreTrack = template.computeConfidence(trackerRegion_I32);
			double scoreDetected = 0;

			if( uniqueDetection ) {
				scoreDetected = detectedRegion.confidence;
			}

			double adjustment = strongMatch ? 0.07 : 0.02;

			if( uniqueDetection && scoreDetected > scoreTrack + adjustment ) {
				// if there is a unique detection and it has higher confidence than the
				// track region, use the detected region
				TldHelperFunctions.convertRegion(detectedRegion.rect, targetRegion);
				confidenceTarget = detectedRegion.confidence;

				// if it's far away from the current track, re-evaluate if it's a strongMatch
				checkNewTrackStrong(scoreDetected);

			} else {
				// Otherwise use the tracker region
				targetRegion.set(trackerRegion);
				confidenceTarget = scoreTrack;

				strongMatch |= confidenceTarget > config.confidenceThresholdStrong;

				// see if the most likely detected region overlaps the track region
				if( strongMatch && confidenceTarget >= config.confidenceThresholdLower )  {
					valid = true;
				}
			}
		} else if( uniqueDetection ) {
			// just go with the best detected region
			detectedRegion = detection.getBest();
			TldHelperFunctions.convertRegion(detectedRegion.rect, targetRegion);
			confidenceTarget = detectedRegion.confidence;
			strongMatch = confidenceTarget > config.confidenceThresholdStrong;
		} else {
			return false;
		}

		return confidenceTarget >= config.confidenceAccept;
	}

	/**
	 * Selects the scale for the image pyramid based on image size and feature size
	 * @return scales for image pyramid
	 */
	public static int[] selectPyramidScale( int imageWidth , int imageHeight, int minSize ) {
		int w = Math.max(imageWidth,imageHeight);

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
	public Rectangle2D_F64 getTargetRegion() {
		return targetRegion;
	}

	public Rectangle2D_F64 getTrackerRegion() {
		return trackerRegion;
	}

	public TldParameters getConfig() {
		return config;
	}

	public TldDetection<T> getDetection() {
		return detection;
	}
}
