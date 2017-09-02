/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.struct;

/**
 * Used to specify the minimum size of something in an image. Can be specified in pixels are fraction of image.
 * If both styles are specified then the smallest of the two should be used.
 *
 * @author Peter Abeles
 */
public class ConfigMinimumSize implements Configuration {
	/**
	 * If &ge; then this specifies the minimum size in pixels
	 */
	public int pixels=-1;
	/**
	 * If &ge; 0 the this specifies the size as a function of min(image width, image height)
	 */
	public double fraction=-1;

	public ConfigMinimumSize(int pixels, double fraction) {
		this.pixels = pixels;
		this.fraction = fraction;
	}

	public ConfigMinimumSize() {
	}

	public static ConfigMinimumSize byPixels( int pixels ) {
		return new ConfigMinimumSize(pixels,-1);
	}

	public static ConfigMinimumSize bySize( double fraction ) {
		return new ConfigMinimumSize(-1,fraction);
	}

	public double compute(int imageWidth , int imageHeight ) {

		double size = -1;
		if (fraction >= 0) {
			int m = Math.min(imageWidth, imageHeight);

			size = fraction*m;
		}
		if( pixels >= 0 ) {
			if( size >= 0 ) {
				size = Math.min(size,pixels);
			} else {
				size = pixels;
			}
		}
		return size;
	}

	public int computeI( int imageWidth , int imageHeight ) {
		double size = compute(imageWidth,imageHeight);
		if( size >= 0 )
			return (int)(size+0.5);
		else
			return -1;
	}

	@Override
	public void checkValidity() {}

	public ConfigMinimumSize copy() {
		return new ConfigMinimumSize(pixels,fraction);
	}

	@Override
	public String toString() {
		String out = "ConfigMinimumSize{";
		if( pixels >= 0)
				out += "pixels=" + pixels;
		if( fraction >= 0 )
				out +=", fraction=" + fraction;
		out += '}';
		return out;
	}
}
