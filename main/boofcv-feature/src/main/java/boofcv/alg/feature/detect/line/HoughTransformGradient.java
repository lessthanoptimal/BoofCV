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

package boofcv.alg.feature.detect.line;

import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.detect.peak.MeanShiftPeak;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.weights.WeightPixelGaussian_F32;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.struct.ListIntPoint2D;
import boofcv.struct.QueueCorner;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Base class for Hough transforms which use a pixel coordinate and the gradient to describe a line.
 * </p>
 *
 * <p>
 * [1] Section 9.3 of E.R. Davies, "Machine Vision Theory Algorithms Practicalities," 3rd Ed. 2005
 * </p>
 *
 * @author Peter Abeles
 */
public class HoughTransformGradient<D extends ImageGray<D>> {

	// extracts line from the transform
	NonMaxSuppression extractor;
	// stores returned lines
	DogArray<LineParametric2D_F32> linesAll = new DogArray<>(10, LineParametric2D_F32::new);
	// Lines after similar ones have been merged together
	List<LineParametric2D_F32> linesMerged = new ArrayList<>();

	// contains a set of counts for detected lines in each pixel
	// floating point image used because that's what FeatureExtractor's take as input
	GrayF32 transform = new GrayF32(1, 1);
	// found lines in transform space
	final QueueCorner foundLines = new QueueCorner(10);
	// list of points in the transform with non-zero values
	final ListIntPoint2D candidates = new ListIntPoint2D();
	// line intensities for later pruning
	DogArray_F32 foundIntensity = new DogArray_F32(10);

	// Refine lines using mean shift. If radius <= 0 it won't be used
	MeanShiftPeak<GrayF32> refine = new MeanShiftPeak<>(10, 0.001f,
			new WeightPixelGaussian_F32(), true, GrayF32.class, BorderType.ZERO);

	HoughTransformParameters parameters;

	// used to make the input image type generic
	GImageGray _derivX, _derivY;

	// post processing pruning
	ImageLinePruneMerge post = new ImageLinePruneMerge();

	// tuning parameters for merging
	double mergeAngle = Math.PI*0.05;
	double mergeDistance = 10;
	int maxLines = 0; // zero means no restrictions

	/**
	 * Specifies parameters of transform.
	 *
	 * @param extractor Extracts local maxima from transform space. A set of candidates is provided, but can be ignored.
	 */
	public HoughTransformGradient( NonMaxSuppression extractor,
								   HoughTransformParameters parameters,
								   Class<D> derivType ) {
		this.extractor = extractor;
		this.parameters = parameters;
		refine.setImage(transform);
		refine.setRadius(3);

		_derivX = FactoryGImageGray.create(derivType);
		_derivY = FactoryGImageGray.create(derivType);
	}

	/**
	 * Computes the Hough transform using the image gradient and a binary image which flags pixels as being edges or not.
	 *
	 * @param derivX (Input) Image derivative along x-axis.
	 * @param derivY (Input) Image derivative along y-axis.
	 * @param binary (Input) Non-zero pixels are considered to be line pixels.
	 */
	public <TD extends ImageGray<TD>> void transform( TD derivX, TD derivY, GrayU8 binary ) {
		InputSanityCheck.checkSameShape(derivX, derivY, binary);
		parameters.initialize(derivX.width, derivX.height, transform);
		ImageMiscOps.fill(transform, 0);

		_derivX.wrap(derivX);
		_derivY.wrap(derivY);
		transform(binary);

		extractLines();
		if (maxLines <= 0) {
			linesMerged.clear();
			linesMerged.addAll(linesAll.toList());
		} else {
			mergeLines(binary.width, binary.height);
		}
	}

	/**
	 * Searches for local maximas and converts into lines.
	 */
	protected void extractLines() {
		linesAll.reset();
		foundLines.reset();
		foundIntensity.reset();

		extractor.process(transform, null, candidates, null, foundLines);

		for (int i = 0; i < foundLines.size(); i++) {
			Point2D_I16 p = foundLines.get(i);

			if (parameters.isTransformValid(p.x, p.y)) {
				LineParametric2D_F32 l = linesAll.grow();
				l.p.setTo(p.x, p.y);
				refine.search(p.x, p.y);
				// check for divergence
				if (l.p.distance(refine.getPeakX(), refine.getPeakY()) < refine.getRadius()*2) {
					l.p.setTo(refine.getPeakX(), refine.getPeakY());
				}
				parameters.transformToLine(l.p.x, l.p.y, l);
				foundIntensity.push(transform.get(p.x, p.y));
			}
		}
	}

	protected void mergeLines( int width, int height ) {
		post.reset();
		for (int i = 0; i < linesAll.size(); i++) {
			post.add(linesAll.get(i), foundIntensity.get(i));
		}

		// NOTE: angular accuracy is a function of range from sub image center. This pruning
		// function uses a constant value for range accuracy. A custom algorithm should really
		// be used here.
		post.pruneSimilar((float)mergeAngle, (float)mergeDistance, width, height);
		post.pruneNBest(maxLines);

		post.createList(linesMerged);
	}

	/**
	 * Takes the detected point along the line and its gradient and converts it into transform space.
	 *
	 * @param x point in image.
	 * @param y point in image.
	 * @param derivX gradient of point.
	 * @param derivY gradient of point.
	 */
	final protected void parameterize( final ListIntPoint2D candidates, final int x, final int y, float derivX, float derivY ) {
		Point2D_F32 parameter = new Point2D_F32();
		parameters.parameterize(x, y, derivX, derivY, parameter);

		// finds the foot a line normal equation and put the point into image coordinate
		int x0 = (int)parameter.x;
		int y0 = (int)parameter.y;

		// weights for bilinear interpolate type weightings
		float wx = parameter.x - x0;
		float wy = parameter.y - y0;

		// make a soft decision and spread counts across neighbors
		addParameters(candidates, x0, y0, (1f - wx)*(1f - wy));
		addParameters(candidates, x0 + 1, y0, wx*(1f - wy));
		addParameters(candidates, x0, y0 + 1, (1f - wx)*wy);
		addParameters(candidates, x0 + 1, y0 + 1, wx*wy);
	}

	final protected void addParameters( ListIntPoint2D candidates, int x, int y, float amount ) {
		if (transform.isInBounds(x, y)) {
			int index = transform.startIndex + y*transform.stride + x;
			// keep track of candidate pixels so that a sparse search can be done
			// to detect lines
			if (transform.data[index] == 0)
				candidates.add(x, y);
			transform.data[index] += amount;
		}
	}

	/**
	 * Returns the Hough transform image.
	 *
	 * @return Transform image.
	 */
	public GrayF32 getTransform() {
		return transform;
	}

	public DogArray<LineParametric2D_F32> getLinesAll() {
		return linesAll;
	}

	/**
	 * Returns the intensity/edge count for each returned line. Useful when doing
	 * post processing pruning.
	 *
	 * @return Array containing line intensities.
	 */
	public float[] getFoundIntensity() {
		return foundIntensity.data;
	}

	void transform( GrayU8 binary ) {
		candidates.configure(transform.width, transform.height);

		// apply the transform to the entire image
		for (int y = 0; y < binary.height; y++) {
			int start = binary.startIndex + y*binary.stride;
			int end = start + binary.width;

			for (int index = start; index < end; index++) {
				if (binary.data[index] != 0) {
					int x = index - start;
					parameterize(candidates, x, y, _derivX.unsafe_getF(x, y), _derivY.unsafe_getF(x, y));
				}
			}
		}
	}

	public void setRefineRadius( int radius ) {
		refine.setRadius(radius);
	}

	public int getRefineRadius() {
		return refine.getRadius();
	}

	public double getMergeAngle() {
		return mergeAngle;
	}

	public void setMergeAngle( double mergeAngle ) {
		this.mergeAngle = mergeAngle;
	}

	public double getMergeDistance() {
		return mergeDistance;
	}

	public void setMergeDistance( double mergeDistance ) {
		this.mergeDistance = mergeDistance;
	}

	public int getMaxLines() {
		return maxLines;
	}

	public void setMaxLines( int maxLines ) {
		this.maxLines = maxLines;
	}

	/**
	 * Lines after merging/pruning has occurred
	 */
	public List<LineParametric2D_F32> getLinesMerged() {
		return linesMerged;
	}

	public NonMaxSuppression getExtractor() {
		return extractor;
	}

	public MeanShiftPeak<GrayF32> getRefine() {
		return refine;
	}

	public HoughTransformParameters getParameters() {
		return parameters;
	}
}
