/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.disparity.block.DisparitySelect;
import boofcv.alg.feature.disparity.block.DisparitySparseScoreSadRect;
import boofcv.alg.feature.disparity.block.DisparitySparseSelect;
import boofcv.alg.feature.disparity.block.score.DisparitySparseScoreBM_SAD_F32;
import boofcv.alg.feature.disparity.block.score.DisparitySparseScoreBM_SAD_U8;
import boofcv.alg.feature.disparity.block.select.*;
import boofcv.alg.feature.disparity.sgm.SgmDisparitySelector;
import boofcv.alg.feature.disparity.sgm.SgmStereoDisparityHmi;
import boofcv.alg.feature.disparity.sgm.StereoMutualInformation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import javax.annotation.Nullable;

/**
 * Algorithms related to computing the disparity between two rectified stereo images.
 *
 * @author Peter Abeles
 */
public class FactoryStereoDisparityAlgs {

	/**
	 * Creates SGM stereo using HMI.
	 */
	public static SgmStereoDisparityHmi createSgmHmi( @Nullable ConfigureDisparitySGM config ) {
		if( config == null )
			config = new ConfigureDisparitySGM();

		int maxError = config.maxError < 0 ? Integer.MAX_VALUE : config.maxError;

		StereoMutualInformation stereoMI = new StereoMutualInformation();
		stereoMI.configureSmoothing(config.smoothingRadius);
		stereoMI.configureHistogram(config.totalGrayLevels);
		SgmDisparitySelector selector = new SgmDisparitySelector();
		selector.setRightToLeftTolerance(config.validateRtoL);
		selector.setMaxError(maxError);
		selector.setTextureThreshold(config.texture);
		SgmStereoDisparityHmi sgm = new SgmStereoDisparityHmi(config.pyramidLayers,stereoMI,selector);
		sgm.setDisparityMin(config.minDisparity);
		sgm.setDisparityRange(config.rangeDisparity);
		sgm.getAggregation().setPathsConsidered(config.paths);
		sgm.getAggregation().setPenalty1(config.penaltySmallChange);
		sgm.getAggregation().setPenalty2(config.penaltyLargeChange);

		return sgm;
	}

	public static DisparitySelect<int[],GrayU8> selectDisparity_S32(int maxError , int tolR2L , double texture) {
		if( maxError < 0 && tolR2L < 0  & texture <= 0 )
			return new SelectErrorBasicWta_S32_U8();
		else
			return new SelectErrorWithChecksWta_S32_U8(maxError,tolR2L,texture);
	}

	public static DisparitySelect<float[],GrayU8> selectDisparity_F32(int maxError , int tolR2L , double texture) {
		if( maxError < 0 && tolR2L < 0  & texture <= 0 )
			return new SelectErrorBasicWta_F32_U8();
		else
			return new SelectErrorWithChecksWta_F32_U8(maxError,tolR2L,texture);
	}

	public static <D extends ImageGray<D>> DisparitySelect<float[],D> selectCorrelation_F32(int tolR2L , double texture, boolean subpixel) {
		if( !subpixel &&  tolR2L < 0 && texture <= 0 )
			return (DisparitySelect)new SelectCorrelationWta_F32_U8();
		else if( !subpixel )
			return (DisparitySelect)new SelectCorrelationChecksWta_F32_U8(tolR2L,texture);
		else
			return (DisparitySelect)new SelectCorrelationSubpixel.F32_F32(tolR2L,texture);
	}

	public static DisparitySelect<int[],GrayF32>
	selectDisparitySubpixel_S32( int maxError , int tolR2L , double texture) {
		return new SelectErrorSubpixel.S32_F32(maxError,tolR2L,texture);
	}

	public static DisparitySelect<float[],GrayF32>
	selectDisparitySubpixel_F32( int maxError , int tolR2L , double texture) {
		return new SelectErrorSubpixel.F32_F32(maxError,tolR2L,texture);
	}

	public static DisparitySparseSelect<int[]>
	selectDisparitySparse_S32( int maxError , double texture) {
		if( maxError < 0 && texture <= 0 )
			return new SelectSparseErrorBasicWta_S32();
		else
			return new SelectSparseErrorWithChecksWta_S32(maxError,texture);
	}

	public static DisparitySparseSelect<float[]>
	selectDisparitySparse_F32( int maxError , double texture) {
		if( maxError < 0 && texture <= 0 )
			return new SelectSparseErrorBasicWta_F32();
		else
			return new SelectSparseErrorWithChecksWta_F32(maxError,texture);
	}

	public static DisparitySparseSelect<int[]>
	selectDisparitySparseSubpixel_S32( int maxError , double texture) {
		return new SelectSparseErrorSubpixel.S32(maxError,texture);
	}

	public static DisparitySparseSelect<float[]>
	selectDisparitySparseSubpixel_F32( int maxError , double texture) {
		return new SelectSparseErrorSubpixel.F32(maxError,texture);
	}

	public static DisparitySparseScoreSadRect<int[],GrayU8>
	scoreDisparitySparseSadRect_U8( int minDisparity , int maxDisparity,
									int regionRadiusX, int regionRadiusY )
	{
		return new DisparitySparseScoreBM_SAD_U8(minDisparity,
				maxDisparity,regionRadiusX,regionRadiusY);
	}

	public static DisparitySparseScoreSadRect<float[],GrayF32>
	scoreDisparitySparseSadRect_F32( int minDisparity, int maxDisparity,
									 int regionRadiusX, int regionRadiusY )
	{
		return new DisparitySparseScoreBM_SAD_F32(minDisparity,
				maxDisparity,regionRadiusX,regionRadiusY);
	}

}
