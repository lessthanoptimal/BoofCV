/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.misc;

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageFloat64;
import boofcv.struct.image.ImageInteger;
import boofcv.struct.image.ImageSingleBand;


/**
 * @author Peter Abeles
 */
public class BasicImageIO {

	public static void print(ImageSingleBand a) {

		if( a.getTypeInfo().isInteger() ) {
			print((ImageInteger)a);
		} else if( a instanceof  ImageFloat32 ) {
			print((ImageFloat32)a);
		} else {
			print((ImageFloat64)a);
		}
	}

	public static void print(ImageFloat64 a) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.printf("%6.2f ", a.get(x, y));
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print(ImageFloat32 a) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.printf("%6.2f ", a.get(x, y));
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print(ImageInteger a) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.printf("%4d ", a.get(x, y));
			}
			System.out.println();
		}
		System.out.println();
	}
}
