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
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a detection cascade for each region.  The cascade is composed of a variance test, Fern classifier, and finally
 * the template classifier.  The next test in the cascade is only considered if the previous passes.  Several changes
 * have been made in how the cascade operates compared to the original paper.  See code for comments.
 *
 * @author Peter Abeles
 */
public class TldDetection<T extends ImageGray> {

	// Detects rectangles: Removes candidates don't match the fern descriptors
	private TldFernClassifier<T> fern;
	// Detects rectangles: Removes candidates don't match NCC descriptors
	protected TldTemplateMatching<T> template;
	// Detects rectangles: Removes candidates which lack texture
	private TldVarianceFilter<T> variance;

	// Storage for results of the fern test on individual regions
	protected FastQueue<TldRegionFernInfo> fernInfo = new FastQueue<>(TldRegionFernInfo.class, true);

	protected TldParameters config;

	// Storage for sorting of results
	private GrowQueue_F64 storageMetric = new GrowQueue_F64();
	private GrowQueue_I32 storageIndexes = new GrowQueue_I32();
	private List<ImageRectangle> storageRect = new ArrayList<>();

	// storage for regions which pass the fern test
	protected List<ImageRectangle> fernRegions = new ArrayList<>();

	// list all regions which had the template test run on them
	protected FastQueue<TldRegion> candidateDetections = new FastQueue<>(TldRegion.class, true);
	// results from non-maximum suppression
	private FastQueue<TldRegion> localMaximums = new FastQueue<>(TldRegion.class, true);

	// list of regions which have almost the same confidence as the maximum
	private List<ImageRectangle> ambiguousRegions = new ArrayList<>();

	private TldHelperFunctions helper = new TldHelperFunctions();

	// the most likely region
	private TldRegion best;
	// is it ambiguous which region is the best?
	protected boolean ambiguous;
	// was it successful at selecting a single region?
	private boolean success;

	// Removes all but the best rectangles.
	private TldNonMaximalSuppression nonmax;

	public TldDetection(TldFernClassifier<T> fern, TldTemplateMatching<T> template, TldVarianceFilter<T> variance, TldParameters config) {
		this.fern = fern;
		this.template = template;
		this.variance = variance;
		this.config = config;

		nonmax = new TldNonMaximalSuppression(config.regionConnect);
	}

	protected TldDetection() {
	}

	/**
	 * Detects the object inside the image.  Eliminates candidate regions using a cascade of tests
	 */
	protected void detectionCascade( FastQueue<ImageRectangle> cascadeRegions ) {

		// initialize data structures
		success = false;
		ambiguous = false;
		best = null;
		candidateDetections.reset();
		localMaximums.reset();
		ambiguousRegions.clear();

		storageMetric.reset();
		storageIndexes.reset();
		storageRect.clear();
		fernRegions.clear();

		fernInfo.reset();

		int totalP = 0;
		int totalN = 0;

		// Run through all candidate regions, ignore ones without enough variance, compute
		// the fern for each one
		TldRegionFernInfo info = fernInfo.grow();
		for( int i = 0; i < cascadeRegions.size; i++ ) {
			ImageRectangle region = cascadeRegions.get(i);

			if( !variance.checkVariance(region)) {
				continue;
			}

			info.r = region;

			if( fern.lookupFernPN(info)) {
				totalP += info.sumP;
				totalN += info.sumN;
				info = fernInfo.grow();
			}
		}
		fernInfo.removeTail();

		// avoid overflow errors in the future by re-normalizing the Fern detector
		if( totalP > 0x0fffffff)
			fern.renormalizeP();
		if( totalN > 0x0fffffff)
			fern.renormalizeN();

		// Select the ferns with the highest likelihood
		selectBestRegionsFern(totalP, totalN);

		// From the remaining regions, score using the template algorithm
		computeTemplateConfidence();

		if( candidateDetections.size == 0 ) {
			return;
		}

		// use non-maximum suppression to reduce the number of candidates
		nonmax.process(candidateDetections, localMaximums);

		best = selectBest();
		if( best != null ) {
			ambiguous = checkAmbiguous(best);

			success = true;
		}
	}

	/**
	 * Computes the confidence for all the regions which pass the fern test
	 */
	protected void computeTemplateConfidence() {
		double max = 0;
		for( int i = 0; i < fernRegions.size(); i++ ) {
			ImageRectangle region = fernRegions.get(i);

			double confidence = template.computeConfidence(region);

			max = Math.max(max,confidence);

			if( confidence < config.confidenceThresholdUpper)
				continue;
			TldRegion r = candidateDetections.grow();
			r.connections = 0;
			r.rect.set(region);
			r.confidence = confidence;
		}
	}

	/**
	 * compute the probability that each region is the target conditional upon this image
	 * the sumP and sumN are needed for image conditional probability
	 *
	 * NOTE: This is a big change from the original paper
	 */
	protected void selectBestRegionsFern(double totalP, double totalN) {

		for( int i = 0; i < fernInfo.size; i++ ) {
			TldRegionFernInfo info = fernInfo.get(i);

			double probP = info.sumP/totalP;
			double probN = info.sumN/totalN;

			// only consider regions with a higher P likelihood
			if( probP > probN ) {
				// reward regions with a large difference between the P and N values
				storageMetric.add(-(probP-probN));
				storageRect.add( info.r );
			}
		}

		// Select the N regions with the highest fern probability
		if( config.maximumCascadeConsider < storageMetric.size ) {
			int N = Math.min(config.maximumCascadeConsider, storageMetric.size);
			storageIndexes.resize(storageMetric.size);

			QuickSelect.selectIndex(storageMetric.data, N - 1, storageMetric.size, storageIndexes.data);
			for( int i = 0; i < N; i++ ) {
				fernRegions.add(storageRect.get(storageIndexes.get(i)));
			}
		} else {
			fernRegions.addAll(storageRect);
		}
	}

	public TldRegion selectBest() {
		TldRegion best = null;
		double bestConfidence = 0;

		for( int i = 0; i < localMaximums.size; i++ ) {
			TldRegion r = localMaximums.get(i);

			if( r.confidence > bestConfidence ) {
				bestConfidence = r.confidence;
				best = r;
			}
		}

		return best;
	}

	private boolean checkAmbiguous( TldRegion best ) {
		double thresh = best.confidence*0.9;

		for( int i = 0; i < localMaximums.size; i++ ) {
			TldRegion r = localMaximums.get(i);

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

	public GrowQueue_F64 getStorageMetric() {
		return storageMetric;
	}

	public List<ImageRectangle> getStorageRect() {
		return storageRect;
	}

	public FastQueue<TldRegion> getCandidateDetections() {
		return candidateDetections;
	}

	public FastQueue<TldRegion> getLocalMaximums() {
		return localMaximums;
	}

	public List<ImageRectangle> getAmbiguousRegions() {
		return ambiguousRegions;
	}

	public FastQueue<TldRegionFernInfo> getFernInfo() {
		return fernInfo;
	}

	/**
	 * Rectangles selected by the fern classifier as candidates
	 * @return List of rectangles
	 */
	public List<ImageRectangle> getSelectedFernRectangles() {
		return fernRegions;
	}

	public boolean isSuccess() {
		return success;
	}
}
