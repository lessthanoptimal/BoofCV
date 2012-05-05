/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.disparity;

import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.disparity.WrapDisparitySadRect;
import boofcv.abst.feature.disparity.WrapDisparitySparseSadRect;
import boofcv.alg.feature.disparity.DisparityScoreSadRect;
import boofcv.alg.feature.disparity.DisparitySelect;
import boofcv.alg.feature.disparity.DisparitySparseScoreSadRect;
import boofcv.alg.feature.disparity.DisparitySparseSelect;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

import static boofcv.factory.feature.disparity.FactoryStereoDisparityAlgs.*;

/**
 * Creates high level interfaces for computing the disparity between two rectified stereo images.
 * Algorithms which select the best disparity for each region independent of all the others are
 * referred to as Winner Takes All (WTA) in the literature.  Dense algorithms compute the disparity for the
 * whole image while sparse algorithms do it in a per pixel basis as requested.
 *
 * Typically disparity calculations with regions will produce less erratic results, but their precision will
 * be decreased.  This is especially evident along the border of objects.  Computing a wider range of disparities
 * can better results, but is very computationally expensive.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class FactoryStereoDisparity {

	/**
	 * <p>
	 * Computes disparity by finding the disparity with the smallest error using a single rectangular
	 * region. Optionally additional validation can be performed to remove some false positives.
	 * </p>
	 *
	 * <p>
	 * For more detailed information on validation parameters see {@link boofcv.alg.feature.disparity.SelectRectStandard}.
	 * </p>
	 *
	 * @param maxDisparity
	 * @param regionRadiusX Region's radius along x-axis.
	 * @param regionRadiusY
	 * @param maxPerPixelError
	 * @param validateRtoL
	 * @param texture
	 * @param imageType
	 * @param <T>
	 * @return
	 */
	public static <T extends ImageSingleBand> StereoDisparity<T,ImageUInt8>
	regionWta( int minDisparity , int maxDisparity,
			   int regionRadiusX, int regionRadiusY ,
			   double maxPerPixelError ,
			   int validateRtoL ,
			   double texture ,
			   Class<T> imageType ) {

		double maxError = (regionRadiusX*2+1)*(regionRadiusY*2+1)*maxPerPixelError;

		DisparityScoreSadRect<T,ImageUInt8> alg;

		if( imageType == ImageUInt8.class ) {
			DisparitySelect<int[],ImageUInt8> select =
					selectDisparity_S32((int) maxError, validateRtoL, texture);
			alg = (DisparityScoreSadRect)FactoryStereoDisparityAlgs.scoreDisparitySadRect_U8(minDisparity,
					maxDisparity,regionRadiusX,regionRadiusY,select);
		} else if( imageType == ImageFloat32.class ) {
			DisparitySelect<float[],ImageUInt8> select =
					selectDisparity_F32((int) maxError, validateRtoL, texture);
			alg = (DisparityScoreSadRect)FactoryStereoDisparityAlgs.scoreDisparitySadRect_F32(minDisparity,
					maxDisparity, regionRadiusX, regionRadiusY, select);
		} else
			throw new RuntimeException("Image type not supported: "+imageType.getSimpleName() );

		return new WrapDisparitySadRect<T,ImageUInt8>(alg);
	}

	public static <T extends ImageSingleBand> StereoDisparity<T,ImageFloat32>
	regionSubpixelWta( int minDisparity , int maxDisparity,
					   int regionRadiusX, int regionRadiusY ,
					   double maxPerPixelError ,
					   int validateRtoL ,
					   double texture ,
					   Class<T> imageType ) {

		double maxError = (regionRadiusX*2+1)*(regionRadiusY*2+1)*maxPerPixelError;

		DisparityScoreSadRect<T,ImageFloat32> alg;

		if( imageType == ImageUInt8.class ) {
			DisparitySelect<int[],ImageFloat32> select =
					selectDisparitySubpixel_S32((int) maxError, validateRtoL, texture);
			alg = (DisparityScoreSadRect)FactoryStereoDisparityAlgs.scoreDisparitySadRect_U8(minDisparity,
					maxDisparity,regionRadiusX,regionRadiusY,select);
		} else if( imageType == ImageFloat32.class ) {
			DisparitySelect<float[],ImageFloat32> select =
					selectDisparitySubpixel_F32((int) maxError, validateRtoL, texture);
			alg = (DisparityScoreSadRect)FactoryStereoDisparityAlgs.scoreDisparitySadRect_F32(minDisparity,
					maxDisparity, regionRadiusX, regionRadiusY, select);
		} else
			throw new RuntimeException("Image type not supported: "+imageType.getSimpleName() );

		return new WrapDisparitySadRect<T,ImageFloat32>(alg);
	}

	public static <T extends ImageSingleBand> StereoDisparitySparse<T>
	regionSparseWta( int minDisparity , int maxDisparity,
					 int regionRadiusX, int regionRadiusY ,
					 double maxPerPixelError ,
					 double texture ,
					 boolean subpixelInterpolation ,
					 Class<T> imageType ) {

		double maxError = (regionRadiusX*2+1)*(regionRadiusY*2+1)*maxPerPixelError;

		if( imageType == ImageUInt8.class ) {
			DisparitySparseSelect<int[]> select;
			if( subpixelInterpolation)
				select = selectDisparitySparseSubpixel_S32((int) maxError, texture);
			else
				select = selectDisparitySparse_S32((int) maxError, texture);

			DisparitySparseScoreSadRect<int[],ImageUInt8>
					score = scoreDisparitySparseSadRect_U8(minDisparity,maxDisparity, regionRadiusX, regionRadiusY);

			return new WrapDisparitySparseSadRect(score,select);
		} else if( imageType == ImageFloat32.class ) {
			DisparitySparseSelect<float[]> select;
			if( subpixelInterpolation )
				select = selectDisparitySparseSubpixel_F32((int) maxError, texture);
			else
				select = selectDisparitySparse_F32((int) maxError, texture);

			DisparitySparseScoreSadRect<float[],ImageFloat32>
					score = scoreDisparitySparseSadRect_F32(minDisparity,maxDisparity, regionRadiusX, regionRadiusY);

			return new WrapDisparitySparseSadRect(score,select);
		} else
			throw new RuntimeException("Image type not supported: "+imageType.getSimpleName() );
	}
}
