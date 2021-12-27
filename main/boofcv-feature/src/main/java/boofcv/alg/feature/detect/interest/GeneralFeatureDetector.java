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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I16;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * <p>
 * Detects features which are local maximums and/or local minimums in the feature intensity image.
 * A list of pixels to exclude as candidates can be provided. Image derivatives need to be computed
 * externally and provided as needed. The passed in {@link GeneralFeatureIntensity} is used to determine if local
 * maximums or minimums should be detected.
 * </p>
 *
 * <p>
 * If a maximum number of features is specified then the N most intense features are returned. By default all
 * found features are returned. Set to a value &le; 0 to detect all features.
 * </p>
 *
 * @param <I> Input image type.
 * @param <D> Image derivative type.
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class GeneralFeatureDetector<I extends ImageGray<I>, D extends ImageGray<D>> {
	// list of feature locations found by the extractor
	protected @Getter QueueCorner maximums = new QueueCorner(10);
	protected @Getter QueueCorner minimums = new QueueCorner(10);

	/** Corners which should be excluded from detection */
	protected @Getter @Setter @Nullable QueueCorner exclude;

	// selects the features with the largest intensity
	protected @Getter FeatureSelectLimitIntensity<Point2D_I16> selectMax;
	protected FastArray<Point2D_I16> selected = new FastArray<>(Point2D_I16.class);
	/**
	 * The maximum number of features that can be detected. If both maximums and minimums can be detected the the limit
	 * per set will be half this. If not a positive value then there is no limit.
	 */
	protected @Getter @Setter int featureLimit;

	/** extracts corners from the intensity image */
	protected @Nullable @Getter NonMaxSuppression extractorMin;
	protected @Nullable @Getter NonMaxSuppression extractorMax;
	// Two extracts are needed because of the requirement to exclude specified corners.
	// Otherwise a single extractor could be used

	// computes the feature intensity image
	protected GeneralFeatureIntensity<I, D> intensity;

	// Storage for initial set of found features
	protected QueueCorner found = new QueueCorner(10);

	/**
	 * Specifies which algorithms to use and configures the detector.
	 *
	 * @param intensity Computes how much like the feature the region around each pixel is.
	 * @param extractorMin Extracts the corners from intensity image
	 */
	public GeneralFeatureDetector( GeneralFeatureIntensity<I, D> intensity,
								   @Nullable NonMaxSuppression extractorMin,
								   @Nullable NonMaxSuppression extractorMax,
								   FeatureSelectLimitIntensity<Point2D_I16> selectMax ) {
		if (extractorMin == null && intensity.localMinimums())
			throw new IllegalArgumentException("Must provide a minimum extractor");
		if (extractorMax == null && intensity.localMaximums())
			throw new IllegalArgumentException("Must provide a maximum extractor");
		if (extractorMin != null && !extractorMin.canDetectMinimums())
			throw new IllegalArgumentException("The minimum extractor doesn't detect minimums");
		if (extractorMax != null && !extractorMax.canDetectMaximums())
			throw new IllegalArgumentException("The maximum extractor doesn't detect maximums");

		if (extractorMin != null && extractorMin.getUsesCandidates() && !intensity.hasCandidates())
			throw new IllegalArgumentException("The extractor requires candidate features, which the intensity does not provide.");
		if (extractorMax != null && extractorMax.getUsesCandidates() && !intensity.hasCandidates())
			throw new IllegalArgumentException("The extractor requires candidate features, which the intensity does not provide.");

		this.intensity = intensity;
		this.extractorMin = extractorMin;
		this.extractorMax = extractorMax;
		this.selectMax = selectMax;

		// sanity check ignore borders and increase the size of the extractor's ignore border
		// if its ignore border is too small then false positive are highly likely
		if (intensity.localMinimums()) {
			if (intensity.getIgnoreBorder() > Objects.requireNonNull(extractorMin).getIgnoreBorder())
				extractorMin.setIgnoreBorder(intensity.getIgnoreBorder());
		}
		if (intensity.localMaximums()) {
			if (intensity.getIgnoreBorder() > Objects.requireNonNull(extractorMax).getIgnoreBorder())
				extractorMax.setIgnoreBorder(intensity.getIgnoreBorder());
		}
	}

	protected GeneralFeatureDetector() {}

	/**
	 * Computes point features from image gradients.
	 *
	 * @param image Original image.
	 * @param derivX image derivative in along the x-axis. Only needed if {@link #getRequiresGradient()} is true.
	 * @param derivY image derivative in along the y-axis. Only needed if {@link #getRequiresGradient()} is true.
	 * @param derivXX Second derivative. Only needed if {@link #getRequiresHessian()} is true.
	 * @param derivXY Second derivative. Only needed if {@link #getRequiresHessian()} is true.
	 * @param derivYY Second derivative. Only needed if {@link #getRequiresHessian()} is true.
	 */
	public void process( I image,
						 @Nullable D derivX, @Nullable D derivY,
						 @Nullable D derivXX, @Nullable D derivYY, @Nullable D derivXY ) {
		minimums.reset();
		maximums.reset();

		intensity.process(image, derivX, derivY, derivXX, derivYY, derivXY);
		GrayF32 intensityImage = intensity.getIntensity();

		// If there is a limit on the number of detections split it evenly between maximums and minimums
		int limitPerSetMin = -1;
		int limitPerSetMax = -1;

		if (featureLimit > 0) {
			if (intensity.localMaximums() && intensity.localMinimums()) {
				limitPerSetMin = featureLimit/2;
				limitPerSetMax = featureLimit - limitPerSetMin;
			} else if (intensity.localMinimums()) {
				limitPerSetMin = featureLimit;
			} else if (intensity.localMaximums()) {
				limitPerSetMax = featureLimit;
			}
		}

		// Detect local minimums and maximums separately while excluding points in the exclude list
		if (intensity.localMinimums()) {
			Objects.requireNonNull(extractorMin);
			markExcludedPixels(intensityImage, -Float.MAX_VALUE);
			if (intensity.hasCandidates()) {
				extractorMin.process(intensityImage, intensity.getCandidatesMin(), null, found, null);
			} else {
				extractorMin.process(intensityImage, null, null, found, null);
			}
			resolveSelectAmbiguity(intensityImage, exclude, found, minimums, limitPerSetMin, false);
		}

		if (intensity.localMaximums()) {
			Objects.requireNonNull(extractorMax);
			markExcludedPixels(intensityImage, Float.MAX_VALUE);
			if (intensity.hasCandidates()) {
				extractorMax.process(intensityImage, null, intensity.getCandidatesMax(), null, found);
			} else {
				extractorMax.process(intensityImage, null, null, null, found);
			}
			resolveSelectAmbiguity(intensityImage, exclude, found, maximums, limitPerSetMax, true);
		}
	}

	private void markExcludedPixels( GrayF32 intensityImage, float value ) {
		if (exclude == null)
			return;

		for (int i = 0; i < exclude.size; i++) {
			Point2D_I16 p = exclude.get(i);
			intensityImage.unsafe_set(p.x, p.y, value);
		}
	}

	/**
	 * More features were detected than requested. Need to select a subset of them
	 */
	private void resolveSelectAmbiguity( GrayF32 intensity, @Nullable QueueCorner excluded,
										 QueueCorner found, QueueCorner output, int numSelect, boolean positive ) {
		output.reset();
		if (numSelect > 0) {
			selectMax.select(intensity, -1, -1, positive, excluded, found, numSelect, selected);
			output.appendAll(selected);
		} else {
			output.appendAll(found);
		}
	}

	/**
	 * If the image gradient is required for calculations.
	 *
	 * @return true if the image gradient is required.
	 */
	public boolean getRequiresGradient() {
		return intensity.getRequiresGradient();
	}

	/**
	 * If the image Hessian is required for calculations.
	 *
	 * @return true if the image Hessian is required.
	 */
	public boolean getRequiresHessian() {
		return intensity.getRequiresHessian();
	}

	public GrayF32 getIntensity() {
		return intensity.getIntensity();
	}

	/**
	 * Changes feature extraction threshold.
	 *
	 * @param threshold The new feature extraction threshold.
	 */
	public void setThreshold( float threshold ) {
		if (extractorMin != null)
			extractorMin.setThresholdMinimum(-threshold);
		if (extractorMax != null)
			extractorMax.setThresholdMaximum(threshold);
	}

	/**
	 * Returns the current feature extraction threshold.
	 *
	 * @return feature extraction threshold.
	 */
	public float getThreshold() {
		return extractorMin != null ? -extractorMin.getThresholdMinimum() :
				Objects.requireNonNull(extractorMax).getThresholdMaximum();
	}

	/**
	 * true if it will detector local minimums
	 */
	public boolean isDetectMinimums() {
		return intensity.localMinimums();
	}

	/**
	 * true if it will detector local maximums
	 */
	public boolean isDetectMaximums() {
		return intensity.localMaximums();
	}

	/**
	 * Species the search radius for the feature
	 *
	 * @param radius Radius in pixels
	 */
	public void setSearchRadius( int radius ) {
		if (extractorMin != null)
			extractorMin.setSearchRadius(radius);
		if (extractorMax != null)
			extractorMax.setSearchRadius(radius);
	}

	public int getSearchRadius() {
		return extractorMin != null ? extractorMin.getSearchRadius() :
				Objects.requireNonNull(extractorMax).getSearchRadius();
	}

	public @Nullable Class<I> getImageType() {
		return intensity.getImageType();
	}

	public @Nullable Class<D> getDerivType() {
		return intensity.getDerivType();
	}
}
