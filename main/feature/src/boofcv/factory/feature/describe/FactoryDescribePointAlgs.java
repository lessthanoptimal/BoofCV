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

package boofcv.factory.feature.describe;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.describe.*;
import boofcv.alg.feature.describe.brief.BriefDefinition_I32;
import boofcv.alg.feature.describe.impl.*;
import boofcv.alg.filter.kernel.SteerableKernel;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.factory.filter.kernel.FactorySteerable;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;


/**
 * Creates algorithms for describing point features.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryDescribePointAlgs {

	public static <T extends ImageSingleBand>
	DescribePointSurf<T> surf(Class<T> imageType) {
		return new DescribePointSurf<T>();
	}

	public static <T extends ImageSingleBand>
	DescribePointSurf<T> msurf(Class<T> imageType) {
		return new DescribePointSurfMod<T>();
	}

	public static <T extends ImageSingleBand>
	DescribePointBrief<T> brief(BriefDefinition_I32 definition, BlurFilter<T> filterBlur ) {
		Class<T> imageType = filterBlur.getInputType();

		if( imageType == ImageFloat32.class ) {
			return (DescribePointBrief<T> )new ImplDescribePointBrief_F32(definition,(BlurFilter<ImageFloat32>)filterBlur);
		} else if( imageType == ImageUInt8.class ) {
			return (DescribePointBrief<T> )new ImplDescribePointBrief_U8(definition,(BlurFilter<ImageUInt8>)filterBlur);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+imageType.getSimpleName());
		}
	}

	// todo remove filterBlur for all BRIEF change to radius,sigma,type
	public static <T extends ImageSingleBand>
	DescribePointBriefSO<T> briefso(BriefDefinition_I32 definition, BlurFilter<T> filterBlur) {
		Class<T> imageType = filterBlur.getInputType();

		InterpolatePixel<T> interp = FactoryInterpolation.bilinearPixel(imageType);

		return new DescribePointBriefSO<T>(definition,filterBlur,interp);
	}

	public static <T extends ImageSingleBand, K extends Kernel2D>
	DescribePointGaussian12<T,K> steerableGaussian12( int radius , Class<T> imageType )
	{
		return new DescribePointGaussian12<T,K>(radius,imageType);
	}


	// todo comment
	public static <T extends ImageSingleBand, K extends Kernel2D>
	DescribePointSteerable2D<T,K> steerableGaussian( boolean normalized ,
													 double sigma ,
													 int radius ,
													 Class<T> imageType )
	{
		if( sigma <= 0 )
			sigma = FactoryKernelGaussian.sigmaForRadius(radius,4);
		else if ( radius <= 0 )
			radius = FactoryKernelGaussian.radiusForSigma(sigma,4);

		SteerableKernel<K>[] kernels = (SteerableKernel<K>[])new SteerableKernel[14];

		Class<K> kernelType = (Class) FactoryKernel.getKernelType(imageType,2);
		int index = 0;
		for( int N = 1; N <= 4; N++ ) {
			for( int i = 0; i <= N; i++ ) {
				int orderX = N-i;

				kernels[index++] = FactorySteerable.gaussian(kernelType,orderX,i, sigma, radius);
			}
		}

		return new DescribePointSteerable2D<T,K>(kernels,normalized,imageType);
	}

	public static <T extends ImageSingleBand, D extends TupleDesc>
	DescribePointPixelRegion<T,D> pixelRegion( int regionWidth , int regionHeight , Class<T> imageType )
	{
		if( imageType == ImageFloat32.class ) {
			return (DescribePointPixelRegion<T,D>)new ImplDescribePointPixelRegion_F32(regionWidth,regionHeight);
		} else if( imageType == ImageUInt8.class ) {
			return (DescribePointPixelRegion<T,D>)new ImplDescribePointPixelRegion_U8(regionWidth,regionHeight);
		} else {
			throw new IllegalArgumentException("Unsupported image type");
		}
	}

	public static <T extends ImageSingleBand>
	DescribePointPixelRegionNCC<T> pixelRegionNCC( int regionWidth , int regionHeight , Class<T> imageType )
	{
		if( imageType == ImageFloat32.class ) {
			return (DescribePointPixelRegionNCC<T>)new ImplDescribePointPixelRegionNCC_F32(regionWidth,regionHeight);
		} else if( imageType == ImageUInt8.class ) {
			return (DescribePointPixelRegionNCC<T>)new ImplDescribePointPixelRegionNCC_U8(regionWidth,regionHeight);
		} else {
			throw new IllegalArgumentException("Unsupported image type");
		}
	}
}
