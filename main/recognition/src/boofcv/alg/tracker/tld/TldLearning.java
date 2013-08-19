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

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.struct.FastQueue;
import boofcv.struct.GrowQueue_F64;
import boofcv.struct.GrowQueue_I32;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.shapes.RectangleCorner2D_F64;
import georegression.transform.affine.AffinePointOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class TldLearning<T extends ImageSingleBand> {

	Random rand;

	int numSamplesTranslation = 5;
	int numSamplesScale = 5;


	// region selected by KLT tracker
	// NOTE: The tracker updates a pointing point region.  Rounding to the closest integer rectangle introduces errors
	//       which can build up.
	private RectangleCorner2D_F64 trackerRegion = new RectangleCorner2D_F64();
	private ImageRectangle trackerRegion_I32 = new ImageRectangle();

	// Detects rectangles: Removes candidates don't match the fern descriptors
	private TldFernClassifier<T> fern;
	// Detects rectangles: Removes candidates don't match NCC descriptors
	private TldTemplateMatching<T> template;
	// Detects rectangles: Removes candidates which lack texture
	private TldVarianceFilter<T> variance;

	private TldDetection<T> detection;

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

	// provides sub-pixel interpolation to improve quality at different scales
	private InterpolatePixel<T> interpolate;

	ImageRectangle targetRegion_I32 = new ImageRectangle();

	Affine2D_F32 affine = new Affine2D_F32();
	Point2D_F32 point = new Point2D_F32();

	FastQueue<ImageRectangle> cascadeRegions;

	TldHelperFunctions helper = new TldHelperFunctions();
	TldConfig config;  // TODO remove this and use a value instead
	RectangleCorner2D_F64 tempRect = new RectangleCorner2D_F64();

	public TldLearning(Random rand, int numSamplesTranslation, int numSamplesScale, TldConfig config,
					   TldTemplateMatching<T> template, TldVarianceFilter<T> variance, TldFernClassifier<T> fern,
					   TldDetection<T> detection ,
					   InterpolatePixel<T> interpolate ) {
		this.rand = rand;
		this.numSamplesTranslation = numSamplesTranslation;
		this.numSamplesScale = numSamplesScale;
		this.config = config;
		this.template = template;
		this.variance = variance;
		this.fern = fern;
		this.detection = detection;
		this.interpolate = interpolate;
	}

	/**
	 * Select positive and negative examples based on the region the user initially selected.  Only use regions
	 * with significant variance during this initial learning phase
	 */
	// TODO sample translations around the selected rect
	// TODO sample different scales around selected rectangle
	public void initialLearning( RectangleCorner2D_F64 targetRegion ,
								 FastQueue<ImageRectangle> cascadeRegions , boolean detectionHasRun ) {

		this.cascadeRegions = cascadeRegions;

		storageMetric.reset();
		fernNegative.clear();

		// learn the initial descriptor
		affine.reset();
		TldHelperFunctions.convertRegion(targetRegion, targetRegion_I32);
		if( !detectionHasRun )
			variance.selectThreshold(targetRegion_I32);
		template.addDescriptor(true, targetRegion_I32);
		fern.learnFernNoise(true, targetRegion_I32);
//		fern.learnFernNoise(true, targetRegion, affine);

		// learn a distorted region which has been translated and scaled
//		learnRegionDistorted(targetRegion,true);

		// Mark all other regions as negative ferns
		affine.reset();
		for( int i = 0; i < cascadeRegions.size; i++ ) {
			ImageRectangle r = cascadeRegions.get(i);

			// see if it passes the variance test
			if( !variance.checkVariance(r) )
				continue;

			// learn features far away from the target region
			double overlap = helper.computeOverlap(targetRegion_I32, r);
			if( overlap > config.overlapLower )
				continue;

			fern.learnFern(false, r );
		}

		// run detection algorithm and if there is an ambiguous solution mark it as not target
		if( !detectionHasRun )
			detection.detectionCascade(cascadeRegions);

		learnNegative(targetRegion);

		System.out.println("  LEARNING: template positive "+
				template.getTemplatePositive().size()+" negative "+template.getTemplateNegative().size());
	}

	public void learnRegionDistorted( RectangleCorner2D_F64 targetRegion , boolean positive ) {
		for( int iterTranY = 0; iterTranY < numSamplesTranslation; iterTranY++ ) {
			affine.ty = -0.05f + (iterTranY*0.1f/(numSamplesTranslation-1));
			for( int iterTranX = 0; iterTranX < numSamplesTranslation; iterTranX++ ) {
				affine.tx = -0.05f + (iterTranX*0.1f/(numSamplesTranslation-1));
				for( int iterScale = 0; iterScale < numSamplesScale; iterScale++ ) {
					affine.a11 = affine.a22 = 0.8f + (iterScale*0.4f/(numSamplesScale-1));

					if( !checkInBounds(targetRegion,affine))
						continue;

					fern.learnFernNoise(positive, targetRegion, affine);
					template.addDescriptor(positive, targetRegion , affine );
				}
			}
		}
	}

	public void learnNegative( RectangleCorner2D_F64 targetRegion) {

		TldHelperFunctions.convertRegion(targetRegion, targetRegion_I32);

		if( detection.isSuccess() ) {
			TldRegion best = detection.getBest();

			// see if it found the correct solution
			double overlap = helper.computeOverlap(best.rect,targetRegion_I32);
			if( overlap <= config.overlapLower ) {
				template.addDescriptor(false,best.rect);
//				fern.learnFernNoise(false, best.rect );
			}

			// mark all ambiguous regions as bad
			List<ImageRectangle> ambiguous = detection.getAmbiguousRegions();
			for( ImageRectangle r : ambiguous ) {
				overlap = helper.computeOverlap(r,targetRegion_I32);
				if( overlap <= config.overlapLower ) {
//					tempRect.x0 = r.x0;
//					tempRect.y0 = r.y0;
//					tempRect.x1 = r.x1;
//					tempRect.y1 = r.y1;
//
//					learnRegionDistorted(tempRect,false);
//					fern.learnFernNoise(false, r );
					template.addDescriptor(false,r);
				}
			}
		}
	}

	/**
	 * Makes sure the distorted rectangle is in the image's bounds
	 */
	private boolean checkInBounds( RectangleCorner2D_F64 r , Affine2D_F32 affine ) {

		float c_x = (float)(r.x0+r.x1)/2.0f;
		float c_y = (float)(r.y0+r.y1)/2.0f;
		float width = (float)r.getWidth();
		float height = (float)r.getHeight();

		if( !checkInBounds(c_x,c_y,width,height,-0.5f,-0.5f))
			return false;

		if( !checkInBounds(c_x,c_y,width,height, 0.5f,-0.5f))
			return false;

		if( !checkInBounds(c_x,c_y,width,height, 0.5f, 0.5f))
			return false;

		if( !checkInBounds(c_x,c_y,width,height,-0.5f, 0.5f))
			return false;

		return true;
	}

	private boolean checkInBounds( float c_x, float c_y , float width , float height , float x , float y )  {
		point.set(x,y);
		AffinePointOps.transform(affine,point,point);

		x = c_x + point.x*width;
		y = c_y + point.y*height;

		return interpolate.isInSafeBounds(x,y);
	}

	/**
	 * Performs P/N-Learning to update the target's description
	 */
//	protected void performLearning( RectangleCorner2D_F64 targetRegion ) {
//
//		fernPositive.clear();
//		fernNegative.clear();
//
//		TldHelperFunctions.convertRegion(targetRegion,targetRegion_I32);
//		TldHelperFunctions.convertRegion(trackerRegion,trackerRegion_I32);
//
////		System.out.println(" *** LEARNING ****");
//		for( int i = 0; i < candidateDetections.size; i++ ) {
//			TldRegion r = candidateDetections.get(i);
//
//			double overlap = nonmax.computeOverlap(r.rect, targetRegion_I32);
//
//			boolean fernTest = fern.performTest(r.rect);
//
//			if( overlap >= config.overlapUpper ) {
//				// mark regions which overlap the target as positive
//
//				// be more careful about updating positives.  Computing confidence is computationally expensive
//				if( !fernTest )
//					fern.learnFernNoise(true, r.rect);
////					fernPositive.add(r.rect);
//
//			} else if( overlap <= config.overlapLower ) {
//				// mark regions which do not overlap the target as negative
//
//				// an unknown fern is by default negative, by always incrementing negative ferns it makes
//				// it harder for one to turn into a false positive
//				if( fernTest )
//					fern.learnFernNoise(false, r.rect);
////					fernNegative.add(r.rect);
//
//				if( r.confidence > config.confidenceThresholdLower) {
//					// add a negative template if it had a high score
//					template.addDescriptor(false,r.rect);
//				}
//			}
//		}
//
//		// TODO rever this back to doing learning inside the main loop.  Can reduce number of templates/ferns added
//		// since they will only be added when neccisary.  near duplicates can work in otherwise
//
//		// update the fern models
////		for( int i = 0; i < fernPositive.size(); i++ ) {
////			fern.updateFerns(true,fernPositive.get(i));
////		}
////		for( int i = 0; i < fernNegative.size(); i++ ) {
////			fern.updateFerns(false,fernNegative.get(i));
////		}
//
//
//		// See if the track region dipped below the threshold
//		double confidenceTrack = template.computeConfidence(trackerRegion_I32);
//		if( confidenceTrack < config.confidenceThresholdUpper ) {
//			template.addDescriptor(true, trackerRegion_I32);
//		}
//		if( !fern.performTest(trackerRegion_I32))
//			fern.learnFernNoise(true, trackerRegion_I32);
//
////		System.out.println("  confidence trackRegion "+confidenceTrack);
////		System.out.println("  templates positive = "+template.getTemplatePositive().size()+" negative "+
////		template.getTemplateNegative().size());
//	}
}
