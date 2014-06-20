/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.segmentation.*;
import boofcv.alg.segmentation.fh04.SegmentFelzenszwalbHuttenlocher04;
import boofcv.alg.segmentation.ms.SegmentMeanShift;
import boofcv.alg.segmentation.slic.SegmentSlic;
import boofcv.alg.segmentation.watershed.WatershedVincentSoille1991;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class FactoryImageSegmentation {

	public static <T extends ImageBase>ImageSuperpixels<T>
	meanShift( ConfigSegmentMeanShift config ,  ImageType<T> imageType )
	{
		if( config == null )
			config = new ConfigSegmentMeanShift();


		SegmentMeanShift<T> ms = FactorySegmentationAlg.meanShift(config,imageType);

		return new MeanShift_to_ImageSuperpixels<T>(ms,config.connectRule);
	}

	public static <T extends ImageBase>ImageSuperpixels<T>
	slic( ConfigSlic config , ImageType<T> imageType )
	{
		SegmentSlic<T> ms = FactorySegmentationAlg.slic(config, imageType);

		return new Slic_to_ImageSuperpixels<T>(ms);
	}

	public static <T extends ImageBase>ImageSuperpixels<T>
	fh04( ConfigFh04 config , ImageType<T> imageType )
	{
		if( config == null )
			config = new ConfigFh04();

		SegmentFelzenszwalbHuttenlocher04<T> fh = FactorySegmentationAlg.fh04(config, imageType);

		return new Fh04_to_ImageSuperpixels<T>(fh,config.connectRule);
	}

	/**
	 * Creates an instance of {@link WatershedVincentSoille1991}.  Watershed works better when initial seeds
	 * are provided.  In this adaptation of watershed to {@link boofcv.abst.segmentation.ImageSuperpixels} only the more basic algorithm
	 * is used where each local minima is a region, which causes over segmentation.  Watershed also only can process
	 * gray scale U8 images.  All other image types are converted into that format.
	 *
	 * @see WatershedVincentSoille1991
	 *
	 * @param config Configuration.  If null default is used.
	 * @param <T>
	 * @return
	 */
	public static <T extends ImageBase>ImageSuperpixels<T>
	watershed( ConfigWatershed config , ImageType<T> imageType )
	{
		if( config == null )
			config = new ConfigWatershed();

		WatershedVincentSoille1991 watershed = FactorySegmentationAlg.watershed(config.connectRule);

		Watershed_to_ImageSuperpixels ret = new Watershed_to_ImageSuperpixels<T>(watershed,config.minimumRegionSize,config.connectRule);
		ret.setImageType(imageType);
		return ret;
	}
}
