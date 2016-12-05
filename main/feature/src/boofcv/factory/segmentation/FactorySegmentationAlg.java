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

package boofcv.factory.segmentation;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.segmentation.ComputeRegionMeanColor;
import boofcv.alg.segmentation.fh04.FhEdgeWeights;
import boofcv.alg.segmentation.fh04.SegmentFelzenszwalbHuttenlocher04;
import boofcv.alg.segmentation.fh04.impl.*;
import boofcv.alg.segmentation.ms.*;
import boofcv.alg.segmentation.slic.*;
import boofcv.alg.segmentation.watershed.WatershedVincentSoille1991;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Factory for low level segmentation algorithms.
 *
 * @author Peter Abeles
 */
public class FactorySegmentationAlg {

	/**
	 * Creates an instance of {@link boofcv.alg.segmentation.ComputeRegionMeanColor} for the specified image type.
	 *
	 * @param imageType image type
	 * @return ComputeRegionMeanColor
	 */
	public static <T extends ImageBase>
	ComputeRegionMeanColor<T> regionMeanColor(ImageType<T> imageType) {
		if( imageType.getFamily() == ImageType.Family.GRAY) {
			switch( imageType.getDataType() ) {
				case U8:
					return (ComputeRegionMeanColor)new ComputeRegionMeanColor.U8();
				case F32:
					return (ComputeRegionMeanColor)new ComputeRegionMeanColor.F32();
			}
		} else if( imageType.getFamily() == ImageType.Family.PLANAR) {
			int N = imageType.getNumBands();
			switch( imageType.getDataType() ) {
				case U8:
					return (ComputeRegionMeanColor)new ComputeRegionMeanColor.PL_U8(N);
				case F32:
					return (ComputeRegionMeanColor)new ComputeRegionMeanColor.PL_F32(N);
			}
		}

		throw new IllegalArgumentException("Unknown imageType");
	}

	/**
	 * Creates an instance of {@link boofcv.alg.segmentation.ms.SegmentMeanShift}.  Uniform distributions are used for spacial and color
	 * weights.
	 *
	 * @param config Specify configuration for mean-shift
	 * @param imageType Type of input image
	 * @return SegmentMeanShift
	 */
	public static<T extends ImageBase>
	SegmentMeanShift<T> meanShift( ConfigSegmentMeanShift config,  ImageType<T> imageType )
	{
		if( config == null )
			config = new ConfigSegmentMeanShift();

		int spacialRadius = config.spacialRadius;
		float colorRadius = config.colorRadius;

		int maxIterations = 20;
		float convergenceTol = 0.1f;

		SegmentMeanShiftSearch<T> search;

		if( imageType.getFamily() == ImageType.Family.GRAY) {
			InterpolatePixelS interp = FactoryInterpolation.bilinearPixelS(imageType.getImageClass(), BorderType.EXTENDED);
			search = new SegmentMeanShiftSearchGray(maxIterations,convergenceTol,interp,
					spacialRadius,spacialRadius,colorRadius,config.fast);
		} else {
			InterpolatePixelMB interp = FactoryInterpolation.createPixelMB(0,255,
					InterpolationType.BILINEAR, BorderType.EXTENDED,(ImageType)imageType);
			search = new SegmentMeanShiftSearchColor(maxIterations,convergenceTol,interp,
					spacialRadius,spacialRadius,colorRadius,config.fast,imageType);
		}

		ComputeRegionMeanColor<T> regionColor = regionMeanColor(imageType);
		MergeRegionMeanShift merge = new MergeRegionMeanShift(spacialRadius/2+1,Math.max(1,colorRadius/2));

		MergeSmallRegions<T> prune = config.minimumRegionSize >= 2 ?
				new MergeSmallRegions<>(config.minimumRegionSize, config.connectRule, regionColor) : null;

		return new SegmentMeanShift<>(search, merge, prune, config.connectRule);
	}

	public static <T extends ImageBase>
	FhEdgeWeights<T> weightsFelzenszwalb04( ConnectRule rule , ImageType<T> imageType) {
		if( imageType.getFamily() == ImageType.Family.GRAY) {
			if( rule == ConnectRule.FOUR ) {
				switch( imageType.getDataType() ) {
					case U8:
						return (FhEdgeWeights)new FhEdgeWeights4_U8();
					case F32:
						return (FhEdgeWeights)new FhEdgeWeights4_F32();
				}
			} else if( rule == ConnectRule.EIGHT ) {
				switch( imageType.getDataType() ) {
					case U8:
						return (FhEdgeWeights)new FhEdgeWeights8_U8();
					case F32:
						return (FhEdgeWeights)new FhEdgeWeights8_F32();
				}
			}
		} else if( imageType.getFamily() == ImageType.Family.PLANAR) {
			int N = imageType.getNumBands();
			if( rule == ConnectRule.FOUR ) {
				switch( imageType.getDataType() ) {
					case U8:
						return (FhEdgeWeights)new FhEdgeWeights4_PLU8(N);
					case F32:
						return (FhEdgeWeights)new FhEdgeWeights4_PLF32(N);
				}
			} else if( rule == ConnectRule.EIGHT ) {
				switch( imageType.getDataType() ) {
					case U8:
						return (FhEdgeWeights)new FhEdgeWeights8_PLU8(N);
					case F32:
						return (FhEdgeWeights)new FhEdgeWeights8_PLF32(N);
				}
			}
		}

		throw new IllegalArgumentException("Unknown imageType or connect rule");
	}

	public static<T extends ImageBase>
	SegmentFelzenszwalbHuttenlocher04<T> fh04(ConfigFh04 config, ImageType<T> imageType)
	{

		if( config == null )
			config = new ConfigFh04();

		FhEdgeWeights<T> edgeWeights = weightsFelzenszwalb04(config.connectRule,imageType);

		SegmentFelzenszwalbHuttenlocher04<T> alg =
				new SegmentFelzenszwalbHuttenlocher04<>(config.K, config.minimumRegionSize, edgeWeights);

		if( config.approximateSortBins > 0 ) {
			alg.configureApproximateSort(config.approximateSortBins);
		}

		return alg;
	}

	public static<T extends ImageBase>
	SegmentSlic<T> slic( ConfigSlic config , ImageType<T> imageType )
	{
		if( config == null )
			throw new IllegalArgumentException("No default configuration since the number of segments must be specified.");

		if( imageType.getFamily() == ImageType.Family.GRAY) {
				switch( imageType.getDataType() ) {
					case U8:
						return (SegmentSlic)new SegmentSlic_U8(config.numberOfRegions,
								config.spacialWeight,config.totalIterations,config.connectRule);
					case F32:
						return (SegmentSlic)new SegmentSlic_F32(config.numberOfRegions,
								config.spacialWeight,config.totalIterations,config.connectRule);
				}
		} else if( imageType.getFamily() == ImageType.Family.PLANAR) {
			int N = imageType.getNumBands();
				switch( imageType.getDataType() ) {
					case U8:
						return (SegmentSlic)new SegmentSlic_PlU8(config.numberOfRegions,
								config.spacialWeight,config.totalIterations,config.connectRule,N);
					case F32:
						return (SegmentSlic)new SegmentSlic_PlF32(config.numberOfRegions,
								config.spacialWeight,config.totalIterations,config.connectRule,N);
				}
		}
		throw new IllegalArgumentException("Unknown imageType or connect rule");
	}

	public static WatershedVincentSoille1991 watershed( ConnectRule rule ) {
		if( rule == ConnectRule.FOUR )
			return new WatershedVincentSoille1991.Connect4();
		else if( rule == ConnectRule.EIGHT )
			return new WatershedVincentSoille1991.Connect8();
		else
			throw new IllegalArgumentException("Unknown connectivity rule");
	}
}
