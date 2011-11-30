/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.deriv.SparseImageGradient;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageBase;

/**
 * <p>
 * Modified SURF descriptor which attempts to smooth out edge conditions.  Based upon MU-SURF described in
 * [1] it computes features in over lapping sub-regions and has a separate set of weights for the large grid and
 * for sub-regions.  Due to these improvements it will in general produce better results than {@link DescribePointSurf}
 * at the cost of additional computations.
 * </p>

 * <p>
 * [1] M. Agrawal, K. Konolige, and M. Blas, "CenSurE: Center Surround Extremas for Realtime Feature Detection and
 * Matching,"  Computer Vision – ECCV 2008
 * </p>
 *
 * @author Peter Abeles
 */
public class DescribePointSurfMod<II extends ImageBase> extends DescribePointSurf<II> {

	// how many sample points sub-regions overlap.
	private int overLap;

	// used to weigh feature computation
	private Kernel2D_F64 weightGrid;
	private Kernel2D_F64 weightSub;

	/**
	 * Creates a SURF descriptor of arbitrary dimension by changing how the local region is sampled.
	 *
	 * @param widthLargeGrid Number of sub-regions wide the large grid is.  Typically 4.
	 * @param widthSubRegion Number of sample points wide a sub-region is.  Typically 5.
	 * @param widthSample The size of a sample point. Typically 2.
	 * @param overLap Number of sample points sub-regions overlap, Typically 2.
	 * @param sigmaLargeGrid Sigma used to weight points in the large grid. Typically 2.5
	 * @param sigmaSubRegion Sigma used to weight points in the sub-region grid. Typically 2.5
	 * @param useHaar If true the Haar wavelet will be used (what was used in [1]), false means an image gradient
	 * approximation will be used.  True is recommended.
	 */
	public DescribePointSurfMod(int widthLargeGrid, int widthSubRegion,
								int widthSample, int overLap ,
								double sigmaLargeGrid , double sigmaSubRegion ,
								boolean useHaar) {
		super(widthLargeGrid, widthSubRegion, widthSample, 1, useHaar);

		this.overLap = overLap;

		weightGrid = FactoryKernelGaussian.gaussianWidth(sigmaLargeGrid, widthLargeGrid);
		weightSub = FactoryKernelGaussian.gaussianWidth(sigmaSubRegion, widthSubRegion + 2 * overLap);

		double div = weightGrid.get(weightGrid.getRadius(),weightGrid.getRadius());
		for( int i = 0; i < weightGrid.data.length; i++ )
			weightGrid.data[i] /= div;

		div = weightSub.get(weightSub.getRadius(),weightSub.getRadius());
		for( int i = 0; i < weightSub.data.length; i++ )
			weightSub.data[i] /= div;
	}

	/**
	 * Create a SURF-64 descriptor.  See [1] for details.
	 */
	public DescribePointSurfMod() {
		this(4,5,2,2, 2.5 , 2.5 , false );
	}

	/**
	 * <p>
	 * Computes the SURF descriptor for the specified interest point.  If the feature
	 * goes outside of the image border (including convolution kernels) then null is returned.
	 * </p>
	 *
	 * @param x Location of interest point.
	 * @param y Location of interest point.
	 * @param scale Scale of the interest point. Null is returned if the feature goes outside the image border.
	 * @param angle The angle the feature is pointing at in radians.
	 * @param ret storage for the feature. Must have 64 values. If null a new feature will be declared internally.
	 * @return The SURF interest point or null if the feature region goes outside the image.
	 */
	@Override
	public SurfFeature describe( double x , double y ,
								 double scale , double angle ,
								 SurfFeature ret ) {
		// some functions require an integer pixel coordinate, rounding is more accurate
		int xInt = (int)Math.round(x);
		int yInt = (int)Math.round(y);

		// By assuming that the entire feature is inside the image faster algorithms can be used
		// the results are also of dubious value when interacting with the image border.
		boolean isInBounds =
				SurfDescribeOps.isInside(ii,xInt,yInt,(widthLargeGrid*widthSubRegion)/2+overLap,widthSample,scale,angle);

		// declare the feature if needed
		if( ret == null )
			ret = new SurfFeature(featureDOF);
		else if( ret.value.length != featureDOF )
			throw new IllegalArgumentException("Provided feature must have "+featureDOF+" values");

		// extract descriptor
		SparseImageGradient<II,?> gradient = SurfDescribeOps.createGradient(isInBounds, useHaar, widthSample, scale, (Class<II>) ii.getClass());
		gradient.setImage(ii);

		SurfDescribeOps.featuresMod(x, y, angle, scale, weightGrid, weightSub,
				widthLargeGrid, widthSubRegion, overLap , gradient, ret.value);

		// normalize feature vector to have an Euclidean length of 1
		// adds light invariance
		SurfDescribeOps.normalizeFeatures(ret.value);

		// Laplacian's sign
		ret.laplacianPositive = computeLaplaceSign(xInt,yInt, scale);

		return ret;
	}

	@Override
	public int getRadius() {
		return (widthLargeGrid*widthSubRegion+widthSample)/2 + overLap;
	}
}
