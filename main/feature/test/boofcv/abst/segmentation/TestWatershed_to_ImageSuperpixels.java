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

package boofcv.abst.segmentation;

import boofcv.factory.segmentation.FactoryImageSegmentation;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class TestWatershed_to_ImageSuperpixels<T extends ImageBase> extends GeneralImageSuperpixelsChecks<T> {
	public TestWatershed_to_ImageSuperpixels() {
		super(ImageType.single(ImageUInt8.class),
				ImageType.single(ImageFloat32.class),
				ImageType.ms(3, ImageUInt8.class),
				ImageType.ms(3, ImageFloat32.class));
	}

	@Override
	public ImageSuperpixels<T> createAlg( ImageType<T> imageType ) {
		return FactoryImageSegmentation.watershed(null,null);
	}

}
