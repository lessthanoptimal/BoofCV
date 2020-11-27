/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.census;

import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;

/**
 * Brute force implementation of Census Transform used for testing
 *
 * @author Peter Abeles
 */
public class CensusNaive {
	public static void region3x3( ImageGray input , GrayU8 output ) {
		GImageGray src = FactoryGImageGray.wrap(FactoryImageBorder.wrap(BorderType.EXTENDED,input));

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int census = 0;
				double center = src.get(x,y).doubleValue();
				int bit = 1;
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						if( i == 0 && j == 0 )
							continue;
						if( src.get(x+j,y+i).doubleValue() > center ) {
							census |= bit;
						}
						bit <<= 1;
					}
				}
				output.set(x,y,census);
			}
		}
	}

	public static void region5x5( ImageGray input , GrayS32 output ) {
		GImageGray src = FactoryGImageGray.wrap(FactoryImageBorder.wrap(BorderType.EXTENDED,input));

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int census = 0;
				double center = src.get(x,y).doubleValue();
				int bit = 1;
				for (int i = -2; i <= 2; i++) {
					for (int j = -2; j <= 2; j++) {
						if( i == 0 && j == 0 )
							continue;
						if( src.get(x+j,y+i).doubleValue() > center ) {
							census |= bit;
						}
						bit <<= 1;
					}
				}
				output.set(x,y,census);
			}
		}
	}

	public static void sample(ImageGray input , final DogArray<Point2D_I32> sample, GrayS64 output ) {
		output.reshape(input.width,input.height);
		GImageGray src = FactoryGImageGray.wrap(FactoryImageBorder.wrap(BorderType.EXTENDED,input));

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				long census = 0;
				int bit = 1;
				double center = src.get(x,y).doubleValue();
				for (int i = 0; i < sample.size; i++) {
					Point2D_I32 p = sample.get(i);
					if( src.get(x+p.x,y+p.y).doubleValue() > center ) {
						census |= bit;
					}
					bit <<= 1;
				}

				output.set(x,y,census);
			}
		}
	}

	public static void sample(ImageGray input , final DogArray<Point2D_I32> sample, InterleavedU16 output ) {
		int numBlocks = BoofMiscOps.bitsToWords(sample.size,16);
		output.reshape(input.width,input.height,numBlocks);
		GImageGray src = FactoryGImageGray.wrap(FactoryImageBorder.wrap(BorderType.EXTENDED,input));

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int census = 0;
				int bit = 1;
				double center = src.get(x,y).doubleValue();
				for (int i = 0; i < sample.size; i++) {
					Point2D_I32 p = sample.get(i);
					if( src.get(x+p.x,y+p.y).doubleValue() > center ) {
						census |= bit;
					}
					if( (i+1)%16 == 0 ) {
						bit = 1;
						output.setBand(x,y,i/16,census);
						census = 0;
					} else {
						bit <<= 1;
					}
				}

				if( bit != 1 ) {
					output.setBand(x,y,sample.size/16,census);
				}
			}
		}
	}
}
