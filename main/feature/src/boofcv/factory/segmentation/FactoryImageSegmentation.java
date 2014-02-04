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

import boofcv.abst.segmentation.Fh04_to_ImageSegmentation;
import boofcv.abst.segmentation.ImageSegmentation;
import boofcv.abst.segmentation.MeanShift_to_ImageSegmentation;
import boofcv.abst.segmentation.Slic_to_ImageSegmentation;
import boofcv.alg.segmentation.fh04.SegmentFelzenszwalbHuttenlocher04;
import boofcv.alg.segmentation.ms.SegmentMeanShift;
import boofcv.alg.segmentation.slic.SegmentSlic;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class FactoryImageSegmentation {

	public static <T extends ImageBase>ImageSegmentation<T>
	meanShift( ConfigSegmentMeanShift config ,  ImageType<T> imageType )
	{
		if( config == null )
			config = new ConfigSegmentMeanShift();


		SegmentMeanShift<T> ms = FactorySegmentationAlg.meanShift(config,imageType);

		return new MeanShift_to_ImageSegmentation<T>(ms,config.connectRule);
	}

	public static <T extends ImageBase>ImageSegmentation<T>
	slic( ConfigSlic config , ImageType<T> imageType )
	{
		SegmentSlic<T> ms = FactorySegmentationAlg.slic(config, imageType);

		return new Slic_to_ImageSegmentation<T>(ms);
	}

	public static <T extends ImageBase>ImageSegmentation<T>
	fh04( ConfigFh04 config , ImageType<T> imageType )
	{
		if( config == null )
			config = new ConfigFh04();

		SegmentFelzenszwalbHuttenlocher04<T> fh = FactorySegmentationAlg.fh04(config, imageType);

		return new Fh04_to_ImageSegmentation<T>(fh,config.rule);
	}
}
