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

import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageGray;
import georegression.struct.shapes.Rectangle2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Uses information from the user and from the tracker to update the positive and negative target model for both
 * ferns and templates.
 *
 * @author Peter Abeles
 */
public class TldLearning<T extends ImageGray> {

	// Random number generator
	private Random rand;

	// Detects rectangles: Removes candidates don't match the fern descriptors
	private TldFernClassifier<T> fern;
	// Detects rectangles: Removes candidates don't match NCC descriptors
	private TldTemplateMatching<T> template;
	// Detects rectangles: Removes candidates which lack texture
	private TldVarianceFilter<T> variance;

	private TldDetection<T> detection;

	// Storage for sorting of results
	private GrowQueue_F64 storageMetric = new GrowQueue_F64();

	// regions which need to have their ferns updated
	private List<ImageRectangle> fernNegative = new ArrayList<>();

	private ImageRectangle targetRegion_I32 = new ImageRectangle();

	private TldHelperFunctions helper = new TldHelperFunctions();
	private TldParameters config;

	/**
	 * Creates and configures learning
	 */
	public TldLearning(Random rand, TldParameters config,
					   TldTemplateMatching<T> template, TldVarianceFilter<T> variance, TldFernClassifier<T> fern,
					   TldDetection<T> detection ) {
		this.rand = rand;
		this.config = config;
		this.template = template;
		this.variance = variance;
		this.fern = fern;
		this.detection = detection;
	}

	/**
	 * Select positive and negative examples based on the region the user's initially selected region.  The selected
	 * region is used as a positive example while all the other regions far away are used as negative examples.
	 *
	 * @param targetRegion user selected region
	 * @param cascadeRegions Set of regions used by the cascade detector
	 */
	public void initialLearning( Rectangle2D_F64 targetRegion ,
								 FastQueue<ImageRectangle> cascadeRegions ) {
		storageMetric.reset();
		fernNegative.clear();


		// learn the initial descriptor
		TldHelperFunctions.convertRegion(targetRegion, targetRegion_I32);

		// select the variance the first time using user selected region
		variance.selectThreshold(targetRegion_I32);
		// add positive examples
		template.addDescriptor(true, targetRegion_I32);
		fern.learnFernNoise(true, targetRegion_I32);

		// Find all the regions which can be used to learn a negative descriptor
		for( int i = 0; i < cascadeRegions.size; i++ ) {
			ImageRectangle r = cascadeRegions.get(i);

			// see if it passes the variance test
			if( !variance.checkVariance(r) )
				continue;

			// learn features far away from the target region
			double overlap = helper.computeOverlap(targetRegion_I32, r);
			if( overlap > config.overlapLower )
				continue;

			fernNegative.add(r);
		}

		// randomize which regions are used
//		Collections.shuffle(fernNegative,rand);
		int N = fernNegative.size();//Math.min(config.numNegativeFerns,fernNegative.size());

		for( int i = 0; i < N; i++ ) {
			fern.learnFern(false, fernNegative.get(i) );
		}

		// run detection algorithm and if there is an ambiguous solution mark it as not target
		detection.detectionCascade(cascadeRegions);

		learnAmbiguousNegative(targetRegion);
	}


	/**
	 * Updates learning using the latest tracking results.
	 * @param targetRegion Region selected by the fused tracking
	 */
	public void updateLearning( Rectangle2D_F64 targetRegion ) {

		storageMetric.reset();

		// learn the initial descriptor
		TldHelperFunctions.convertRegion(targetRegion, targetRegion_I32);

		template.addDescriptor(true, targetRegion_I32);
		fern.learnFernNoise(true, targetRegion_I32);

		// mark only a few of the far away regions as negative.  Marking all of them as negative is
		// computationally expensive
		FastQueue<TldRegionFernInfo> ferns = detection.getFernInfo();
		int N = Math.min(config.numNegativeFerns,ferns.size);
		for( int i = 0; i < N; i++ ) {
			int index = rand.nextInt(ferns.size);
			TldRegionFernInfo f = ferns.get(index);

			// no need to check variance here since the detector already did it

			// learn features far away from the target region
			double overlap = helper.computeOverlap(targetRegion_I32, f.r);
			if( overlap > config.overlapLower )
				continue;

			fern.learnFern(false, f.r );
		}

		learnAmbiguousNegative(targetRegion);
	}

	/**
	 * Mark regions which were local maximums and had high confidence as negative.  These regions were
	 * candidates for the tracker but were not selected
	 */
	protected void learnAmbiguousNegative(Rectangle2D_F64 targetRegion) {

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
					fern.learnFernNoise(false, r );
					template.addDescriptor(false,r);
				}
			}
		}
	}
}
