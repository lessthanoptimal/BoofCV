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

import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;


/**
 * Creates an image pyramid given an image type.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryImagePyramid {

	public static <T extends ImageBase>
	ImagePyramid<T> create( Class<T> type ,
							int width , int height ,
							boolean saveOriginalReference )
	{
		if( type == ImageUInt8.class ) {
			return (ImagePyramid<T>)new ImagePyramid_I8(width,height,saveOriginalReference);
		} else if( type == ImageFloat32.class ) {
			return (ImagePyramid<T>)new ImagePyramid_F32(width,height,saveOriginalReference);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}
}
