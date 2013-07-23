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
import boofcv.struct.FastQueue;
import boofcv.struct.GrowQueue_F64;
import boofcv.struct.GrowQueue_I32;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.shapes.RectangleCorner2D_F64;
import georegression.struct.shapes.RectangleCorner2D_I32;
import org.ddogleg.sorting.QuickSelectArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
public class TldTracker<T extends ImageSingleBand, D extends ImageSingleBand> {

	// specified configuration parameters for the tracker
	private TldConfig<T,D> config;

	// selected region for output
	private RectangleCorner2D_F64 targetRegion = new RectangleCorner2D_F64();
	private ImageRectangle targetRegion_I32 = new ImageRectangle();

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
	// Removes all but the best rectangles.
	private TldNonMaximalSuppression nonmax;

	// All rectangles which pass the detection tests
	private FastQueue<TldRegion> candidateDetections = new FastQueue<TldRegion>(TldRegion.class,true);
	// Rectangles after non-maximum suppression
	private FastQueue<TldRegion> detectedTargets = new FastQueue<TldRegion>(TldRegion.class,true);

	// did tracking totally fail and it needs to reacquire a track?
	private boolean reacquiring;

	// Is the region hypothesis valid and can be used for learning?
	private boolean valid;
	// Was the previous region hypothesis valid?
	private boolean previousValid;

	// Variables used when initializing
	private List<ImageRectangle> initPositive = new ArrayList<ImageRectangle>();
	private List<ImageRectangle> initNegative = new ArrayList<ImageRectangle>();

	// Storage for sorting of reslts
	private GrowQueue_F64 storageMetric = new GrowQueue_F64();
	private GrowQueue_I32 storageIndexes = new GrowQueue_I32();
	private List<ImageRectangle> storageRect = new ArrayList<ImageRectangle>();

	// regions which need to have their ferns updated
	private List<ImageRectangle> fernPositive = new ArrayList<ImageRectangle>();
	private List<ImageRectangle> fernNegative = new ArrayList<ImageRectangle>();

	// random number generator
	private Random rand;

	// is learning on or off
	private boolean performLearning = true;

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
				rand,config.numFerns,config.fernSize,config.interpolate);
		nonmax = new TldNonMaximalSuppression(config.regionConnect);
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

		initialLearning();
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
	 * Select positive and negative examples based on the region the user initially selected.  Only use regions
	 * with significant variance during this initial learning phase
	 */
	private void initialLearning() {

		initPositive.clear();
		initNegative.clear();
		storageMetric.reset();
		storageIndexes.reset();

		// set the user selected region as a positive example
		convertRegion(targetRegion,targetRegion_I32);
		fern.updateFerns(true, targetRegion_I32);
		template.addDescriptor(true, targetRegion_I32);
		variance.selectThreshold(targetRegion_I32);

		// Create a list of regions which are close to and far away from the selected region and have texture
		for( int i = 0; i < cascadeRegions.size; i++ ) {
			ImageRectangle region = cascadeRegions.get(i);

			if( !variance.checkVariance(region))
				continue;

			double overlap = nonmax.computeOverlap(region, targetRegion_I32);

			if( overlap < config.overlapLower ) {
				initNegative.add(region);
			} else if( overlap > config.overlapUpper ) {
				initPositive.add(region);
				storageMetric.add(overlap);
			}
		}

		// select the most similar examples to the selected target and train on those
		int N = Math.min(initPositive.size(),config.initLearnPositive);
		storageIndexes.resize(storageMetric.getSize());
		QuickSelectArray.selectIndex(storageMetric.data, N, storageMetric.getSize(), storageIndexes.data);

		for( int i = 0; i < N; i++ ) {
			ImageRectangle r = initPositive.get(storageIndexes.get(i));
//			if( !fern.performTest(r) )
				fern.updateFerns(true,r);
//			double confidence = template.computeConfidence(r);  // TODO twice here too
//			if( confidence < config.confidenceThresholdLower )
//				template.addDescriptor(true,r);
		}

		// train using the first N negative examples
		N = Math.min(initNegative.size(),config.initLearnNegative);
		Collections.shuffle(initNegative,rand);
		for( int i = 0; i < N; i++ ) {
			ImageRectangle r = initNegative.get(i);
			fern.updateFerns(false, r);

			double confidence = template.computeConfidence(r);
			if( confidence >= config.confidenceThresholdUpper ) {
				template.addDescriptor(false,r);
			}
		}

//		System.out.println("Initial Learning");
//		System.out.println("  NCC positive = "+template.getTemplatePositive().size()+" negative "+template.getTemplateNegative().size());
//		System.out.println("  variance threshold "+variance.getThreshold());
	}

	/**
	 * Updates track region.
	 *
	 * @param image Next image in the sequence.
	 * @return true if the object could be found and false if not
	 */
	public boolean track( T image ) {

//		System.out.println("----------------------- TRACKING ---------------------------");

		boolean success = true;
		valid = false;

		imagePyramid.process(image);
		template.setImage(image);
		variance.setImage(image);
		fern.setImage(image);

		if( reacquiring ) {
			// It can reinitialize if there is a single detection
			if( detectionCascade() && detectedTargets.size == 1 ) {
				TldRegion region = detectedTargets.get(0);
//				System.out.println("Track reacquired: confidence = "+region.confidence+"  num regions "+detectedTargets.size);
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
			detectionCascade();

			// update the previous track region using the tracker
			trackerRegion.set(targetRegion);
			boolean trackingWorked = tracking.process(imagePyramid, trackerRegion);
			trackingWorked &= adjustRegion.process(tracking.getPairs(), trackerRegion);
//			if( trackingWorked ) {
//				convertRegion(trackerRegion,trackerRegion_I32);
//				trackingWorked &= variance.checkVariance(trackerRegion_I32);
//			}

			if( hypothesisFusion( trackingWorked ) ) {
				// if it found a hypothesis and it is valid for learning, then learn
				if( valid && performLearning )
					performLearning();
			} else {
				reacquiring = true;
				success = false;
			}
		}

		previousValid = valid;

		return success;
	}

	/**
	 * Detects the object inside the image.  Eliminates candidate regions using a cascade of tests
	 */
	protected boolean detectionCascade() {

		candidateDetections.reset();
		detectedTargets.reset();

		storageMetric.reset();
		storageIndexes.reset();
		storageRect.clear();
		initPositive.clear();

		// go through all detection regions
		for( int i = 0; i < cascadeRegions.size; i++ ) {
			ImageRectangle region = cascadeRegions.get(i);

			if( !variance.checkVariance(region)) {
				continue;
			}

			if( !fern.performTest(region))
				continue;

			storageMetric.add(fern.getProbability());
			storageRect.add(region);
		}

		if( storageMetric.size == 0 )
			return false;

		// Give preference towards regions with a higher probability
		if( config.maximumCascadeConsider < storageMetric.size ) {
			int N = Math.min(config.maximumCascadeConsider, storageMetric.size);
			storageIndexes.resize(storageMetric.size);

			QuickSelectArray.selectIndex(storageMetric.data,N-1, storageMetric.size, storageIndexes.data);
			for( int i = 0; i < N; i++ ) {
				initPositive.add( storageRect.get( storageIndexes.get(i)));
			}
		} else {
			initPositive.addAll(storageRect);
		}

		for( int i = 0; i < initPositive.size(); i++ ) {
			ImageRectangle region = initPositive.get(i);

			double confidence = template.computeConfidence(region);
			if( confidence < config.confidenceThresholdUpper)
				continue;

			TldRegion r = candidateDetections.grow();
			r.connections = 0;
			r.rect.set(region);
			r.confidence = confidence;
		}

//		System.out.println("considered regions = "+initPositive.size());
//		System.out.println("Candidate regions  = "+candidateDetections.size);

		// use non-maximum suppression to reduce the number of candidates
		nonmax.process(candidateDetections, detectedTargets);

//		System.out.println("Detected targets   = "+detectedTargets.size);

		return detectedTargets.size > 0;
	}

	/**
	 * Combines hypotheses from tracking and detection.
	 *
	 * @param trackingWorked If the sequential tracker updated the track region successfully or not
	 * @return true a hypothesis was found, false if it failed to find a hypothesis
	 */
	protected boolean hypothesisFusion( boolean trackingWorked ) {

		valid = false;

		boolean uniqueDetection = detectedTargets.size == 1;
		TldRegion detectedRegion = uniqueDetection ? detectedTargets.get(0) : null;

		double confidenceTarget = 0;

		if( trackingWorked ) {

			// get the scores from tracking and detection
			double scoreTrack = template.computeConfidence(trackerRegion_I32);
			double scoreDetected = 0;
			double overlap = 0;

			if( detectedRegion != null ) {
				scoreDetected = detectedRegion.confidence;
				overlap = nonmax.computeOverlap(trackerRegion_I32, detectedRegion.rect);
			}

//			System.out.println("FUSION: score track "+scoreTrack+" detection "+scoreDetected);

			if( detectedRegion != null && scoreDetected > scoreTrack && overlap < config.overlapUpper ) {
//				System.out.println("FUSION: using detection region");
				// if there is a unique detection and it has higher confidence than the
				// track region, use the detected region
				convertRegion(detectedRegion.rect, targetRegion);
				confidenceTarget = detectedRegion.confidence;
			} else {
//				System.out.println("FUSION: using track region");
				// Otherwise use the tracker region
				targetRegion.set(trackerRegion);
				confidenceTarget = scoreTrack;

				// see if the most likely detected region overlaps the track region
				if( scoreTrack >= config.confidenceThresholdUpper)  {
					valid = true;
				} else if( previousValid && scoreTrack >= config.confidenceThresholdLower) {
					valid = true;
				}
			}
		} else if( detectedTargets.size == 1 ) {
//			System.out.println("FUSION: tracker failed, using detection: total detections "+detectedTargets.size);
			// just go with the best detected region
			detectedRegion = detectedTargets.get(0);
			convertRegion(detectedRegion.rect, targetRegion);
			confidenceTarget = detectedRegion.confidence;
		} else {
//			System.out.println("FUSION: failed");
			return false;
		}
//		System.out.println("FUSION: valid = "+valid);

		return confidenceTarget >= config.confidenceAccept;
	}


//	private TldRegion selectBestTarget() {
//		double bestScore = 0;
//		TldRegion best = null;
//		for( int i = 0; i < detectedTargets.size; i++ ) {
//			TldRegion r = detectedTargets.get(i);
//			if( r.confidence > bestScore ) {
//				bestScore = r.confidence;
//				best = r;
//			}
//		}
//
//		return best;
//	}

	/**
	 * Performs P/N-Learning to update the target's description
	 */
	protected void performLearning() {

		fernPositive.clear();
		fernNegative.clear();

		convertRegion(targetRegion,targetRegion_I32);
		convertRegion(trackerRegion,trackerRegion_I32);

//		System.out.println(" *** LEARNING ****");
		for( int i = 0; i < candidateDetections.size; i++ ) {
			TldRegion r = candidateDetections.get(i);

			double overlap = nonmax.computeOverlap(r.rect, targetRegion_I32);

			boolean fernTest = fern.performTest(r.rect);

			if( overlap >= config.overlapUpper ) {
				// mark regions which overlap the target as positive

				// be more careful about updating positives.  Computing confidence is computationally expensive
				if( !fernTest )
					fern.updateFerns(true,r.rect);
//					fernPositive.add(r.rect);

			} else if( overlap <= config.overlapLower ) {
				// mark regions which do not overlap the target as negative

				// an unknown fern is by default negative, by always incrementing negative ferns it makes
				// it harder for one to turn into a false positive
				if( fernTest )
					fern.updateFerns(false, r.rect);
//					fernNegative.add(r.rect);

				if( r.confidence > config.confidenceThresholdLower) {
					// add a negative template if it had a high score
					template.addDescriptor(false,r.rect);
				}
			}
		}

		// TODO rever this back to doing learning inside the main loop.  Can reduce number of templates/ferns added
		// since they will only be added when neccisary.  near duplicates can work in otherwise

		// update the fern models
//		for( int i = 0; i < fernPositive.size(); i++ ) {
//			fern.updateFerns(true,fernPositive.get(i));
//		}
//		for( int i = 0; i < fernNegative.size(); i++ ) {
//			fern.updateFerns(false,fernNegative.get(i));
//		}


		// See if the track region dipped below the threshold
		double confidenceTrack = template.computeConfidence(trackerRegion_I32);
		if( confidenceTrack < config.confidenceThresholdUpper ) {
			template.addDescriptor(true, trackerRegion_I32);
		}
		if( !fern.performTest(trackerRegion_I32))
			fern.updateFerns(true,trackerRegion_I32);

//		System.out.println("  confidence trackRegion "+confidenceTrack);
//		System.out.println("  templates positive = "+template.getTemplatePositive().size()+" negative "+
//		template.getTemplateNegative().size());
	}

	/**
	 * Selects the scale for the image pyramid based on image size and feature size
	 * @return scales for image pyramid
	 */
	public static int[] selectPyramidScale( int imageWidth , int imageHeight, int featureRadius ) {
		int w = Math.max(imageWidth,imageHeight);
		int minSize = (featureRadius*2+1)*10;

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

	public static void convertRegion(RectangleCorner2D_F64 input, RectangleCorner2D_I32 output) {
		output.x0 = (int)(input.x0+0.5);
		output.x1 = (int)(input.x1+0.5);
		output.y0 = (int)(input.y0+0.5);
		output.y1 = (int)(input.y1+0.5);
	}

	public static void convertRegion(RectangleCorner2D_I32 input, RectangleCorner2D_F64 output) {
		output.x0 = input.x0;
		output.x1 = input.x1;
		output.y0 = input.y0;
		output.y1 = input.y1;
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

	public FastQueue<TldRegion> getCandidateDetections() {
		return candidateDetections;
	}

	public FastQueue<TldRegion> getDetectedTargets() {
		return detectedTargets;
	}

	public RectangleCorner2D_F64 getTrackerRegion() {
		return trackerRegion;
	}

	public TldConfig<T, D> getConfig() {
		return config;
	}

	public TldNonMaximalSuppression getNonmax() {
		return nonmax;
	}
}
