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

package boofcv.alg.border;

import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Interface for creating a copy of an image with a border added to it. Functions are provided for
 * growing just a single row and column. For an entire image see {@link boofcv.alg.misc.ImageMiscOps}
 *
 * @author Peter Abeles
 */
public interface GrowBorder<T extends ImageBase<T>,PixelArray> {

	void setBorder( ImageBorder<T> border );

	void setImage( T image );

	void growRow( int y , int borderLower , int borderUpper, PixelArray output , int offset );

	void growCol( int x , int borderLower , int borderUpper, PixelArray output , int offset );

	ImageType<T> getImageType();
}
