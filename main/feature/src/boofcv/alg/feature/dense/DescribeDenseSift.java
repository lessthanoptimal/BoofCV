/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.GImageSingleBand;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.metric.UtilAngle;
import org.ddogleg.struct.FastQueue;

/**
 * @author Peter Abeles
 */
public class DescribeDenseSift<D extends ImageSingleBand> {

	double periodRows;
	double periodColumns;

	// width of a subregion, in samples
	int widthSubregion;
	// width of the outer grid, in sub-regions
	int widthGrid;

	// number of bins in the orientation histogram
	int numHistogramBins;
	float histogramBinWidth;

	float gaussianWeight[];
	GImageSingleBand imageDerivX,imageDerivY;

	FastQueue<TupleDesc_F64> descriptors = new FastQueue<TupleDesc_F64>(TupleDesc_F64.class,true);

	public DescribeDenseSift(int widthSubregion, int widthGrid, int numHistogramBins, double periodRows, double periodColumns) {
		this.widthSubregion = widthSubregion;
		this.widthGrid = widthGrid;
		this.numHistogramBins = numHistogramBins;
		this.periodRows = periodRows;
		this.periodColumns = periodColumns;

		final int DOF = (widthGrid*widthGrid)*numHistogramBins;

		descriptors = new FastQueue<TupleDesc_F64>(TupleDesc_F64.class,true) {
			@Override
			protected TupleDesc_F64 createInstance() {
				return new TupleDesc_F64(DOF);
			}
		};
	}

	public void process(D derivX , D derivY ) {
		InputSanityCheck.checkSameShape(derivX,derivY);
		if( derivX.stride != derivY.stride || derivX.startIndex != derivY.startIndex )
			throw new IllegalArgumentException("stride and start index must be the same");

		int width = widthSubregion*widthGrid;
		int radius = width/2;

		int X0 = radius,X1 = derivX.width-radius;
		int Y0 = radius,Y1 = derivX.height-radius;

		int numX = (int)((X1-X0)/periodColumns);
		int numY = (int)((Y1-Y0)/periodRows);

		imageDerivX.wrap(derivX);
		imageDerivY.wrap(derivY);

		descriptors.reset();

		for (int i = 0; i < numY; i++) {
			int y = (Y1-Y0)*i/(numY-1) + Y0;

			for (int j = 0; j < numX; j++) {
				int x = (X1-X0)*i/(numX-1) + X0;

				TupleDesc_F64 desc = descriptors.grow();

				computeDescriptor(x,y,desc);
			}
		}

	}

	private void computeDescriptor( int cx , int cy , TupleDesc_F64 desc ) {

		desc.fill(0);

		int widthPixels = widthSubregion*widthGrid;
		int radius = widthPixels/2;

		ImageFloat32 image = null;

		for (int i = 0; i < widthPixels; i++) {
			int pixelIndex = (cy-radius+i)*image.stride + (cx-radius);

			float subY = i/(float)widthSubregion;

			for (int j = 0; j < widthPixels; j++, pixelIndex++ ) {
				float subX = j/(float)widthSubregion;

				float spacialDX = imageDerivX.getF(pixelIndex);
				float spacialDY = imageDerivY.getF(pixelIndex);

				double angle = UtilAngle.domain2PI(Math.atan2(spacialDX,spacialDY));

				float weightGaussian = gaussianWeight[i*widthPixels+j];
				float weightGradient = (float)Math.sqrt(spacialDX*spacialDX + spacialDY*spacialDY);

				// trilinear interpolation intro descriptor
				trilinearInterpolation(weightGaussian*weightGradient,subX,subY,angle,desc);
			}
		}
	}

	void trilinearInterpolation( float weight , float sampleX , float sampleY , double angle , TupleDesc_F64 descriptor )
	{
		for (int i = 0; i < widthGrid; i++) {
			double weightGridY = 1.0 - Math.abs(sampleY-i);
			if( weightGridY <= 0) continue;
			for (int j = 0; j < widthGrid; j++) {
				double weightGridX = 1.0 - Math.abs(sampleX-j);
				if( weightGridX <= 0 ) continue;
				for (int k = 0; k < numHistogramBins; k++) {
					double angleBin = k*histogramBinWidth;
					double weightHistogram = 1.0 - UtilAngle.dist(angle,angleBin)/histogramBinWidth;
					if( weightHistogram <= 0 ) continue;

					int descriptorIndex = (i*widthGrid + j)*numHistogramBins + k;
					descriptor.value[descriptorIndex] += weight*weightGridX*weightGridY*weightHistogram;
				}
			}
		}
	}
}
