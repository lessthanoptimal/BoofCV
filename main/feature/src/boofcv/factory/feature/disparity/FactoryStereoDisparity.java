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

package boofcv.factory.feature.disparity;

import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.disparity.WrapDisparitySadRect;
import boofcv.abst.feature.disparity.WrapDisparitySparseSadRect;
import boofcv.alg.feature.disparity.DisparityScoreRowFormat;
import boofcv.alg.feature.disparity.DisparitySelect;
import boofcv.alg.feature.disparity.DisparitySparseScoreSadRect;
import boofcv.alg.feature.disparity.DisparitySparseSelect;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import static boofcv.factory.feature.disparity.FactoryStereoDisparityAlgs.*;

/**
 * <p>
 * Creates high level interfaces for computing the disparity between two rectified stereo images.
 * Algorithms which select the best disparity for each region independent of all the others are
 * referred to as Winner Takes All (WTA) in the literature.  Dense algorithms compute the disparity for the
 * whole image while sparse algorithms do it in a per pixel basis as requested.
 * </p>
 *
 * <p>
 * Typically disparity calculations with regions will produce less erratic results, but their precision will
 * be decreased.  This is especially evident along the border of objects.  Computing a wider range of disparities
 * can better results, but is very computationally expensive.
 * </p>
 *
 * <p>
 * Dense vs Sparse.  Here dense refers to computing the disparity across the whole image at once.  Sparse refers
 * to computing the disparity for a single pixel at a time as requested by the user,
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class FactoryStereoDisparity {

	/**
	 * <p>
	 * Crates algorithms for computing dense disparity images up to pixel level accuracy.
	 * </p>
	 *
	 * <p>
	 * NOTE: For RECT_FIVE the size of the sub-regions it uses is what is specified.
	 * </p>
	 *
	 * @param minDisparity Minimum disparity that it will check. Must be &ge; 0 and &lt; maxDisparity
	 * @param maxDisparity Maximum disparity that it will calculate. Must be &gt; 0
	 * @param regionRadiusX Radius of the rectangular region along x-axis.
	 * @param regionRadiusY Radius of the rectangular region along y-axis.
	 * @param maxPerPixelError Maximum allowed error in a region per pixel.  Set to &lt; 0 to disable.
	 * @param validateRtoL Tolerance for how difference the left to right associated values can be.  Try 6
	 * @param texture Tolerance for how similar optimal region is to other region.  Closer to zero is more tolerant.
	 *                Try 0.1
	 * @param imageType Type of input image.
	 * @return Rectangular region based WTA disparity.algorithm.
	 */
	public static <T extends ImageGray> StereoDisparity<T,GrayU8>
	regionWta( DisparityAlgorithms whichAlg ,
			   int minDisparity , int maxDisparity,
			   int regionRadiusX, int regionRadiusY ,
			   double maxPerPixelError ,
			   int validateRtoL ,
			   double texture ,
			   Class<T> imageType ) {

		double maxError = (regionRadiusX*2+1)*(regionRadiusY*2+1)*maxPerPixelError;

		// 3 regions are used not just one in this case
		if( whichAlg == DisparityAlgorithms.RECT_FIVE )
			maxError *= 3;

		DisparitySelect select;
		if( imageType == GrayU8.class || imageType == GrayS16.class ) {
			select = selectDisparity_S32((int) maxError, validateRtoL, texture);
		} else if( imageType == GrayF32.class ) {
			select = selectDisparity_F32((int) maxError, validateRtoL, texture);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}

		DisparityScoreRowFormat<T,GrayU8> alg = null;

		switch( whichAlg ) {
			case RECT:
				if( imageType == GrayU8.class ) {
					alg = FactoryStereoDisparityAlgs.scoreDisparitySadRect_U8(minDisparity,
							maxDisparity,regionRadiusX,regionRadiusY,select);
				} else if( imageType == GrayS16.class ) {
					alg = FactoryStereoDisparityAlgs.scoreDisparitySadRect_S16(minDisparity,
							maxDisparity, regionRadiusX, regionRadiusY, select);
				} else if( imageType == GrayF32.class ) {
					alg = FactoryStereoDisparityAlgs.scoreDisparitySadRect_F32(minDisparity,
							maxDisparity, regionRadiusX, regionRadiusY, select);
				}
				break;

			case RECT_FIVE:
				if( imageType == GrayU8.class ) {
					alg = FactoryStereoDisparityAlgs.scoreDisparitySadRectFive_U8(minDisparity,
							maxDisparity,regionRadiusX,regionRadiusY,select);
				} else if( imageType == GrayS16.class ) {
					alg = FactoryStereoDisparityAlgs.scoreDisparitySadRectFive_S16(minDisparity,
							maxDisparity, regionRadiusX, regionRadiusY, select);
				} else if( imageType == GrayF32.class ) {
					alg = FactoryStereoDisparityAlgs.scoreDisparitySadRectFive_F32(minDisparity,
							maxDisparity, regionRadiusX, regionRadiusY, select);
				}
				break;

			default:
				throw new IllegalArgumentException("Unknown algorithms "+whichAlg);

		}
		if( alg == null)
			throw new RuntimeException("Image type not supported: "+imageType.getSimpleName() );

		return new WrapDisparitySadRect<>(alg);
	}

	/**
	 * <p>
	 * Returns an algorithm for computing a dense disparity images with sub-pixel disparity accuracy.
	 * </p>
	 *
	 * <p>
	 * NOTE: For RECT_FIVE the size of the sub-regions it uses is what is specified.
	 * </p>
	 *
	 * @param minDisparity Minimum disparity that it will check. Must be &ge; 0 and &lt; maxDisparity
	 * @param maxDisparity Maximum disparity that it will calculate. Must be &gt; 0
	 * @param regionRadiusX Radius of the rectangular region along x-axis. Try 3.
	 * @param regionRadiusY Radius of the rectangular region along y-axis. Try 3.
	 * @param maxPerPixelError Maximum allowed error in a region per pixel.  Set to &lt; 0 to disable.
	 * @param validateRtoL Tolerance for how difference the left to right associated values can be.  Try 6
	 * @param texture Tolerance for how similar optimal region is to other region.  Disable with a value &le; 0.
	 *                Closer to zero is more tolerant. Try 0.1
	 * @param imageType Type of input image.
	 * @return Rectangular region based WTA disparity.algorithm.
	 */
	public static <T extends ImageGray> StereoDisparity<T,GrayF32>
	regionSubpixelWta( DisparityAlgorithms whichAlg ,
					   int minDisparity , int maxDisparity,
					   int regionRadiusX, int regionRadiusY ,
					   double maxPerPixelError ,
					   int validateRtoL ,
					   double texture ,
					   Class<T> imageType ) {

		double maxError = (regionRadiusX*2+1)*(regionRadiusY*2+1)*maxPerPixelError;

		// 3 regions are used not just one in this case
		if( whichAlg == DisparityAlgorithms.RECT_FIVE )
			maxError *= 3;

		DisparitySelect select;
		if( imageType == GrayU8.class || imageType == GrayS16.class ) {
			select = selectDisparitySubpixel_S32((int) maxError, validateRtoL, texture);
		} else if( imageType == GrayF32.class ) {
			select = selectDisparitySubpixel_F32((int) maxError, validateRtoL, texture);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}

		DisparityScoreRowFormat<T,GrayF32> alg = null;

		switch( whichAlg ) {
			case RECT:
				if( imageType == GrayU8.class ) {
					alg = FactoryStereoDisparityAlgs.scoreDisparitySadRect_U8(minDisparity,
							maxDisparity,regionRadiusX,regionRadiusY,select);
				} else if( imageType == GrayS16.class ) {
					alg = FactoryStereoDisparityAlgs.scoreDisparitySadRect_S16(minDisparity,
							maxDisparity, regionRadiusX, regionRadiusY, select);
				} else if( imageType == GrayF32.class ) {
					alg = FactoryStereoDisparityAlgs.scoreDisparitySadRect_F32(minDisparity,
							maxDisparity, regionRadiusX, regionRadiusY, select);
				}
				break;

			case RECT_FIVE:
				if( imageType == GrayU8.class ) {
					alg = FactoryStereoDisparityAlgs.scoreDisparitySadRectFive_U8(minDisparity,
							maxDisparity,regionRadiusX,regionRadiusY,select);
				} else if( imageType == GrayS16.class ) {
					alg = FactoryStereoDisparityAlgs.scoreDisparitySadRectFive_S16(minDisparity,
							maxDisparity, regionRadiusX, regionRadiusY, select);
				} else if( imageType == GrayF32.class ) {
					alg = FactoryStereoDisparityAlgs.scoreDisparitySadRectFive_F32(minDisparity,
							maxDisparity, regionRadiusX, regionRadiusY, select);
				}
				break;

			default:
				throw new IllegalArgumentException("Unknown algorithms "+whichAlg);

		}
		if( alg == null)
			throw new RuntimeException("Image type not supported: "+imageType.getSimpleName() );

		return new WrapDisparitySadRect<>(alg);
	}

	/**
	 * WTA algorithms that computes disparity on a sparse per-pixel basis as requested..
	 *
	 * @param minDisparity Minimum disparity that it will check. Must be &ge; 0 and &lt; maxDisparity
	 * @param maxDisparity Maximum disparity that it will calculate. Must be &gt; 0
	 * @param regionRadiusX Radius of the rectangular region along x-axis.
	 * @param regionRadiusY Radius of the rectangular region along y-axis.
	 * @param maxPerPixelError Maximum allowed error in a region per pixel.  Set to &lt; 0 to disable.
	 * @param texture Tolerance for how similar optimal region is to other region.  Closer to zero is more tolerant.
	 *                Try 0.1
	 * @param subpixelInterpolation true to turn on sub-pixel interpolation
	 * @param imageType Type of input image.
	 * @param <T> Image type
	 * @return Sparse disparity algorithm
	 */
	public static <T extends ImageGray> StereoDisparitySparse<T>
	regionSparseWta( int minDisparity , int maxDisparity,
					 int regionRadiusX, int regionRadiusY ,
					 double maxPerPixelError ,
					 double texture ,
					 boolean subpixelInterpolation ,
					 Class<T> imageType ) {

		double maxError = (regionRadiusX*2+1)*(regionRadiusY*2+1)*maxPerPixelError;

		if( imageType == GrayU8.class ) {
			DisparitySparseSelect<int[]> select;
			if( subpixelInterpolation)
				select = selectDisparitySparseSubpixel_S32((int) maxError, texture);
			else
				select = selectDisparitySparse_S32((int) maxError, texture);

			DisparitySparseScoreSadRect<int[],GrayU8>
					score = scoreDisparitySparseSadRect_U8(minDisparity,maxDisparity, regionRadiusX, regionRadiusY);

			return new WrapDisparitySparseSadRect(score,select);
		} else if( imageType == GrayF32.class ) {
			DisparitySparseSelect<float[]> select;
			if( subpixelInterpolation )
				select = selectDisparitySparseSubpixel_F32((int) maxError, texture);
			else
				select = selectDisparitySparse_F32((int) maxError, texture);

			DisparitySparseScoreSadRect<float[],GrayF32>
					score = scoreDisparitySparseSadRect_F32(minDisparity,maxDisparity, regionRadiusX, regionRadiusY);

			return new WrapDisparitySparseSadRect(score,select);
		} else
			throw new RuntimeException("Image type not supported: "+imageType.getSimpleName() );
	}
}
