/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.describe;

import boofcv.alg.interpolate.impl.ImplBilinearPixel_F32;
import boofcv.struct.deriv.GradientValue_F64;
import boofcv.struct.deriv.SparseImageGradient;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

/**
 * TODO write
 *
 * @author Peter Abeles
 */
public class DescribePointSurf2<II extends ImageSingleBand> extends DescribePointSurf<II> {

	ImageFloat32 derivX = new ImageFloat32(1,1);
	ImageFloat32 derivY = new ImageFloat32(1,1);

	ImplBilinearPixel_F32 interpX = new ImplBilinearPixel_F32();
	ImplBilinearPixel_F32 interpY = new ImplBilinearPixel_F32();

	public DescribePointSurf2(int widthLargeGrid, int widthSubRegion, int widthSample,
							  double weightSigma , boolean useHaar) {
		super(widthLargeGrid, widthSubRegion, widthSample, weightSigma, useHaar);
	}

	/**
	 * Create a SURF-64 descriptor.  See [1] for details.
	 */
	public DescribePointSurf2() {
		this(4,5,2, 5.5 , true);
	}

	/**
	 * <p>
	 * Computes the SURF descriptor for the specified interest point.  If the feature
	 * goes outside of the image border (including convolution kernels) then null is returned.
	 * </p>
	 *
	 * @param c_x Location of interest point.
	 * @param c_y Location of interest point.
	 * @param scale Scale of the interest point. Null is returned if the feature goes outside the image border.
	 * @param angle The angle the feature is pointing at in radians.
	 * @param ret storage for the feature. Must have 64 values. If null a new feature will be declared internally.
	 * @return The SURF interest point or null if the feature region goes outside the image.
	 */
	@Override
	public SurfFeature describe( double c_x , double c_y ,
								 double scale , double angle ,
								 SurfFeature ret )
	{
		double c = Math.cos(angle);
		double s = Math.sin(angle);
		int sampleWidth = (int)Math.ceil(SurfDescribeOps.rotatedWidth(widthLargeGrid*widthSubRegion,c,s));
		int regionRadius = sampleWidth/2 + sampleWidth%2;
		sampleWidth = regionRadius*2+1;

		// todo keep as float?
		int tl_x = (int)(c_x-regionRadius*scale);
		int tl_y = (int)(c_y-regionRadius*scale);

		// By assuming that the entire feature is inside the image faster algorithms can be used
		// the results are also of dubious value when interacting with the image border.
//		boolean isInBounds = SurfDescribeOps.isInside(ii.width,ii.height,tl_x,tl_y,subRegionWidth,widthSample*scale);

		// declare the feature if needed
		if( ret == null )
			ret = new SurfFeature(featureDOF);
		else if( ret.value.length != featureDOF )
			throw new IllegalArgumentException("Provided feature must have "+featureDOF+" values");

		// extract descriptor
		SparseImageGradient gradient = SurfDescribeOps.createGradient(false, useHaar, widthSample, scale, ii.getClass());
		gradient.setImage(ii);

		// extra sample for interpolation
		sampleWidth += 1;
		derivX.reshape(sampleWidth,sampleWidth);
		derivY.reshape(sampleWidth,sampleWidth);
		interpX.setImage(derivX);
		interpY.setImage(derivY);

		for( int y = 0; y < sampleWidth; y++ ) {
			int pixelY = (int)(tl_y + y*scale + 0.5);
			for( int x = 0; x < sampleWidth; x++ ) {
				int pixelX = (int)(tl_x + x*scale + 0.5);

				GradientValue_F64 g = (GradientValue_F64)gradient.compute(pixelX,pixelY);
				
				derivX.set(x,y,(float)g.x);
				derivY.set(x,y,(float)g.y);
			}
		}

		computeDescriptor((float) ((c_x - tl_x) / scale), c, s, ret.value);

		// normalize feature vector to have an Euclidean length of 1
		// adds light invariance
		SurfDescribeOps.normalizeFeatures(ret.value);

		// Laplacian's sign
		ret.laplacianPositive = computeLaplaceSign((int)Math.round(c_x),(int)Math.round(c_y), scale);

		return ret;
	}

	protected void computeDescriptor(float center, double c, double s, double features[]) {
		// image center, taking in account the extra sample for interpolation
		int regionIndex = 0;

		int width = widthLargeGrid*widthSubRegion;
		int centerGrid = width/2;

		for( int rY = 0; rY < width; rY += widthSubRegion ) {
			for( int rX = 0; rX < width; rX += widthSubRegion ) {
				double sum_dx = 0, sum_dy=0, sum_adx=0, sum_ady=0;

				// compute and sum up the response  inside the sub-region
				for( int i = 0; i < widthSubRegion; i++ ) {
					float regionY = rY + i-centerGrid;
					for( int j = 0; j < widthSubRegion; j++ ) {
						float w = (float)weight.get(rX + j, rY + i);

						float regionX = rX + j-centerGrid;

						// rotate the pixel along the feature's direction
						float pixelX = (float)(center + c*regionX - s*regionY);
						float pixelY = (float)(center + s*regionX + c*regionY);

						float dx = w*interpX.get(pixelX,pixelY);
						float dy = w*interpY.get(pixelX,pixelY);

						// align the gradient along image patch
						// note the transform is transposed
						double pdx =  c*dx + s*dy;
						double pdy = -s*dx + c*dy;

						sum_dx += pdx;
						sum_adx += Math.abs(pdx);
						sum_dy += pdy;
						sum_ady += Math.abs(pdy);
					}
				}
				features[regionIndex++] = sum_dx;
				features[regionIndex++] = sum_adx;
				features[regionIndex++] = sum_dy;
				features[regionIndex++] = sum_ady;
			}
		}
	}
}
