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

package boofcv.struct.image;


/**
 * Used to create new images from its type alone
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryImage {

	public static <T extends ImageGray> T create(Class<T> type , int width , int height )
	{
		if( type == GrayU8.class) {
			return (T)new GrayU8(width,height);
		} else if( type == GrayS8.class) {
			return (T)new GrayS8(width,height);
		} else if( type == GrayU16.class) {
			return (T)new GrayU16(width,height);
		} else if( type == GrayS16.class) {
			return (T)new GrayS16(width,height);
		} else if( type == GrayS32.class) {
			return (T)new GrayS32(width,height);
		} else if( type == GrayF32.class) {
			return (T)new GrayF32(width,height);
		} else{
			throw new IllegalArgumentException("Unknown image type: "+type);
		}
	}
}
