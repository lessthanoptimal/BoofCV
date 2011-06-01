/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.struct.pyramid;

import gecv.core.image.inst.FactoryImageGenerator;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;


/**
 * Makes creating common image pyramid types less verbose.
 *
 * @author Peter Abeles
 */
public class ImagePyramidFactory {

	@SuppressWarnings({"unchecked"})
	public static <T extends ImageBase> ImagePyramid<T> create( int width , int height ,
																boolean saveOriginalReference ,
																Class<T> type )
	{
		if( type == ImageFloat32.class ) {
			return (ImagePyramid<T>)create_F32(width,height,saveOriginalReference);
		} else if( type == ImageUInt8.class ) {
			return (ImagePyramid<T>)create_U8(width,height,saveOriginalReference);
		} else {
			throw new IllegalArgumentException("Add image type");
		}
	}

	public static ImagePyramid<ImageFloat32> create_F32( int bottomWidth, int bottomHeight,
												  boolean saveOriginalReference )
	{
		return new ImagePyramid<ImageFloat32>(bottomWidth,bottomHeight,saveOriginalReference,
				FactoryImageGenerator.create(ImageFloat32.class));
	}

	public static ImagePyramid<ImageUInt8> create_U8( int bottomWidth, int bottomHeight,
												  boolean saveOriginalReference )
	{
		return new ImagePyramid<ImageUInt8>(bottomWidth,bottomHeight,saveOriginalReference,
				FactoryImageGenerator.create(ImageUInt8.class));
	}
}
