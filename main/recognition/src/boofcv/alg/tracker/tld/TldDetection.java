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

import boofcv.struct.FastQueue;
import boofcv.struct.GrowQueue_F64;
import boofcv.struct.GrowQueue_I32;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageSingleBand;
import org.ddogleg.sorting.QuickSelectArray;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class TldDetection<T extends ImageSingleBand> {

	// Detects rectangles: Removes candidates don't match the fern descriptors
	private TldFernClassifier<T> fern;
	// Detects rectangles: Removes candidates don't match NCC descriptors
	private TldTemplateMatching<T> template;
	// Detects rectangles: Removes candidates which lack texture
	private TldVarianceFilter<T> variance;


	FastQueue<TldRegionFernInfo> fernInfo = new FastQueue<TldRegionFernInfo>(TldRegionFernInfo.class,true);

	TldConfig config;

	// Storage for sorting of reslts
	private GrowQueue_F64 storageMetric = new GrowQueue_F64();
	private GrowQueue_I32 storageIndexes = new GrowQueue_I32();
	private List<ImageRectangle> storageRect = new ArrayList<ImageRectangle>();

	private List<ImageRectangle> initPositive = new ArrayList<ImageRectangle>();

	FastQueue<TldRegion> candidateDetections = new FastQueue<TldRegion>(TldRegion.class,true);
	FastQueue<TldRegion> detectedTargets = new FastQueue<TldRegion>(TldRegion.class,true);

	private List<ImageRectangle> ambiguousRegions = new ArrayList<ImageRectangle>();

	TldHelperFunctions helper = new TldHelperFunctions();

	TldRegion best;
	boolean ambiguous;
	boolean success;

	// Removes all but the best rectangles.
	private TldNonMaximalSuppression nonmax;

	public TldDetection(TldFernClassifier<T> fern, TldTemplateMatching<T> template, TldVarianceFilter<T> variance, TldConfig config) {
		this.fern = fern;
		this.template = template;
		this.variance = variance;
		this.config = config;

		nonmax = new TldNonMaximalSuppression(config.regionConnect);
	}

	/**
	 * Detects the object inside the image.  Eliminates candidate regions using a cascade of tests
	 */
	protected void detectionCascade( FastQueue<ImageRectangle> cascadeRegions ) {

		success = false;
		best = null;
		candidateDetections.reset();
		detectedTargets.reset();
		ambiguousRegions.clear();

		storageMetric.reset();
		storageIndexes.reset();
		storageRect.clear();
		initPositive.clear();

		fernInfo.reset();

		int totalP = 0;
		int totalN = 0;

		// go through all detection regions
		for( int i = 0; i < cascadeRegions.size; i++ ) {
			ImageRectangle region = cascadeRegions.get(i);

			if( !variance.checkVariance(region)) {
				continue;
			}

			TldRegionFernInfo info = fernInfo.grow();
			info.reset();
			info.r = region;

			if( !fern.lookupFernPN(info))
				fernInfo.removeTail();

			totalP += info.sumP;
			totalN += info.sumN;
		}

		double ftotalP = totalP;
		double ftotalN = totalN;

		// compute the probability that each region is the target conditional upon this image
		// the sumP and sumN are needed for image conditional probability
		for( int i = 0; i < fernInfo.size; i++ ) {
			TldRegionFernInfo info = fernInfo.get(i);

			double probP = info.sumP/ftotalP;
			double probN = info.sumN/ftotalN;

			if( probP > probN ) {
				storageMetric.add(-probP);
				storageRect.add( info.r );
			}
		}

		if( storageMetric.size == 0 ) {
			System.out.println("  DETECTION: All regions failed fern test");
			return;
		}

		// Give preference towards regions with a higher Fern probability
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

		double maxConfidence = -1;
		for( int i = 0; i < initPositive.size(); i++ ) {
			ImageRectangle region = initPositive.get(i);

			double confidence = template.computeConfidence(region);
			maxConfidence = Math.max(confidence,maxConfidence);

			if( confidence < config.confidenceThresholdUpper)
				continue;
			TldRegion r = candidateDetections.grow();
			r.connections = 0;
			r.rect.set(region);
			r.confidence = confidence;

		}

		if( candidateDetections.size == 0 ) {
			System.out.println("DETECTION: No strong candidates: ferns "+initPositive.size());
			System.out.println("           max confidence "+maxConfidence);
			return;
		}

		System.out.println("DETECTION: pass fern regions     = "+initPositive.size());
		System.out.println("DETECTION: pass template regions = "+candidateDetections.size);

		// use non-maximum suppression to reduce the number of candidates
		nonmax.process(candidateDetections, detectedTargets);

		System.out.println("DETECTION: maximum regions       = " + detectedTargets.size);

		best = selectBest();
		ambiguous = checkAmbiguous(best);

		System.out.println("DETECTION: ambiguous = "+ambiguous+" best confidence = "+best.confidence);
		success = true;
		return;
	}

	public TldRegion selectBest() {
		TldRegion best = null;
		double bestConfidence = 0;

		for( int i = 0; i < detectedTargets.size; i++ ) {
			TldRegion r = detectedTargets.get(i);

			if( r.confidence > bestConfidence ) {
				bestConfidence = r.confidence;
				best = r;
			}
		}

		return best;
	}

	private boolean checkAmbiguous( TldRegion best ) {
		double thresh = best.confidence*0.9;

		for( int i = 0; i < detectedTargets.size; i++ ) {
			TldRegion r = detectedTargets.get(i);

			if( r.confidence >= thresh ) {
				double overlap = helper.computeOverlap(r.rect,best.rect);

				if( overlap <= config.overlapLower )  {
					ambiguousRegions.add(r.rect);
				}
			}
		}

		return !ambiguousRegions.isEmpty();
	}

	public TldRegion getBest() {
		return best;
	}

	public boolean isAmbiguous() {
		return ambiguous;
	}

	public TldNonMaximalSuppression getNonmax() {
		return nonmax;
	}

	public FastQueue<TldRegion> getCandidateDetections() {
		return candidateDetections;
	}

	public FastQueue<TldRegion> getDetectedTargets() {
		return detectedTargets;
	}

	public List<ImageRectangle> getAmbiguousRegions() {
		return ambiguousRegions;
	}

	public boolean isSuccess() {
		return success;
	}
}
