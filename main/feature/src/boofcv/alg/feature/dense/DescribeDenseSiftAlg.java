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

package boofcv.alg.feature.dense;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.describe.DescribeSiftCommon;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.ImageGray;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

/**
 * <p>Computes {@link DescribePointSift SIFT} features in a regular grid across an entire image at a single
 * scale and orientation.   This is more computationally efficient than the more generic {@link DescribePointSift}
 * algorithm because it makes strong assumptions.  If given the same center point, an orientation of 0, and
 * sigmaToPixels is 1, they should produce the same descriptor.</p>
 *
 * <p>Sampling is done in regular increments in a grid pattern.  The example sampling points are computed such that
 * entire area sampled starts and ends at the most extreme possible pixels.  This most likely will require that
 * the sampling period be adjusted.  Multiple descriptors can overlap the same area, so pixel orientation and
 * magnitude is just computed once and saved.</p>
 *
 * @author Peter Abeles
 */
public class DescribeDenseSiftAlg<D extends ImageGray> extends DescribeSiftCommon {

	// sampling period along the image's rows an columns
	double periodRows;
	double periodColumns;

	// wrapper around gradient images so that multiple types are supported
	GImageGray imageDerivX,imageDerivY;

	// storage for descriptors
	FastQueue<TupleDesc_F64> descriptors;

	// storage for precomputed angle
	GrayF64 savedAngle = new GrayF64(1,1);
	GrayF32 savedMagnitude = new GrayF32(1,1);

	// saved location of where in the image it sampled
	FastQueue<Point2D_I32> sampleLocations = new FastQueue<>(Point2D_I32.class, true);

	/**
	 * Specifies SIFT descriptor structure and sampling frequency.
	 * @param widthSubregion Width of sub-region in samples.  Try 4
	 * @param widthGrid Width of grid in subregions.  Try 4.
	 * @param numHistogramBins Number of bins in histogram.  Try 8
	 * @param weightingSigmaFraction Sigma for Gaussian weighting function is set to this value * region width.  Try 0.5
	 * @param maxDescriptorElementValue Helps with non-affine changes in lighting. See paper.  Try 0.2
	 * @param periodColumns Number of pixels between samples along x-axis
	 * @param periodRows  Number of pixels between samples along y-axis
	 * @param derivType Type of input derivative image
	 */
	public DescribeDenseSiftAlg(int widthSubregion, int widthGrid, int numHistogramBins,
								double weightingSigmaFraction , double maxDescriptorElementValue,
								double periodColumns, double periodRows , Class<D> derivType ) {
		super(widthSubregion,widthGrid,numHistogramBins,weightingSigmaFraction,maxDescriptorElementValue);
		this.periodRows = periodRows;
		this.periodColumns = periodColumns;

		final int DOF = getDescriptorLength();

		imageDerivX = FactoryGImageGray.create(derivType);
		imageDerivY = FactoryGImageGray.create(derivType);

		descriptors = new FastQueue<TupleDesc_F64>(TupleDesc_F64.class,true) {
			@Override
			protected TupleDesc_F64 createInstance() {
				return new TupleDesc_F64(DOF);
			}
		};
	}

	/**
	 * Sets the gradient and precomputes pixel orientation and magnitude
	 *
	 * @param derivX image derivative x-axis
	 * @param derivY image derivative y-axis
	 */
	public void setImageGradient(D derivX , D derivY ) {
		InputSanityCheck.checkSameShape(derivX,derivY);
		if( derivX.stride != derivY.stride || derivX.startIndex != derivY.startIndex )
			throw new IllegalArgumentException("stride and start index must be the same");

		savedAngle.reshape(derivX.width,derivX.height);
		savedMagnitude.reshape(derivX.width,derivX.height);

		imageDerivX.wrap(derivX);
		imageDerivY.wrap(derivY);

		precomputeAngles(derivX);
	}

	/**
	 * Computes SIFT descriptors across the entire image
	 */
	public void process() {

		int width = widthSubregion*widthGrid;
		int radius = width/2;

		int X0 = radius,X1 = savedAngle.width-radius;
		int Y0 = radius,Y1 = savedAngle.height-radius;

		int numX = (int)((X1-X0)/periodColumns);
		int numY = (int)((Y1-Y0)/periodRows);

		descriptors.reset();
		sampleLocations.reset();

		for (int i = 0; i < numY; i++) {
			int y = (Y1-Y0)*i/(numY-1) + Y0;

			for (int j = 0; j < numX; j++) {
				int x = (X1-X0)*j/(numX-1) + X0;

				TupleDesc_F64 desc = descriptors.grow();

				computeDescriptor(x,y,desc);
				sampleLocations.grow().set(x,y);
			}
		}
	}

	/**
	 * Computes the angle of each pixel and its gradient magnitude
	 */
	void precomputeAngles(D image) {
		int savecIndex = 0;
		for (int y = 0; y < image.height; y++) {
			int pixelIndex = y*image.stride + image.startIndex;

			for (int x = 0; x < image.width; x++, pixelIndex++, savecIndex++ ) {
				float spacialDX = imageDerivX.getF(pixelIndex);
				float spacialDY = imageDerivY.getF(pixelIndex);

				savedAngle.data[savecIndex] = UtilAngle.domain2PI(Math.atan2(spacialDY,spacialDX));
				savedMagnitude.data[savecIndex] = (float)Math.sqrt(spacialDX*spacialDX + spacialDY*spacialDY);
			}
		}
	}

	/**
	 * Computes the descriptor centered at the specified coordinate
	 * @param cx center of region x-axis
	 * @param cy center of region y-axis
	 * @param desc The descriptor
	 */
	public void computeDescriptor( int cx , int cy , TupleDesc_F64 desc  ) {

		desc.fill(0);

		int widthPixels = widthSubregion*widthGrid;
		int radius = widthPixels/2;

		for (int i = 0; i < widthPixels; i++) {
			int angleIndex = (cy-radius+i)*savedAngle.width + (cx-radius);

			float subY = i/(float)widthSubregion;

			for (int j = 0; j < widthPixels; j++, angleIndex++ ) {
				float subX = j/(float)widthSubregion;

				double angle = savedAngle.data[angleIndex];

				float weightGaussian = gaussianWeight[i*widthPixels+j];
				float weightGradient = savedMagnitude.data[angleIndex];

				// trilinear interpolation intro descriptor
				trilinearInterpolation(weightGaussian*weightGradient,subX,subY,angle,desc);
			}
		}

		normalizeDescriptor(desc,maxDescriptorElementValue);
	}

	public double getPeriodRows() {
		return periodRows;
	}

	public void setPeriodRows(double periodRows) {
		this.periodRows = periodRows;
	}

	public double getPeriodColumns() {
		return periodColumns;
	}

	public void setPeriodColumns(double periodColumns) {
		this.periodColumns = periodColumns;
	}

	public FastQueue<TupleDesc_F64> getDescriptors() {
		return descriptors;
	}

	/**
	 * Returns where in the image it sampled the features
	 */
	public FastQueue<Point2D_I32> getLocations() {
		return sampleLocations;
	}

	public Class<D> getDerivType () {
		return (Class)(imageDerivX.getImageType());
	}
}
