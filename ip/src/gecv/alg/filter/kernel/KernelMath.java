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

package gecv.alg.filter.kernel;

import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInteger;
import gecv.struct.image.ImageSInt32;


/**
 * @author Peter Abeles
 */
public class KernelMath {

	public static void fill( Kernel2D_F32 kernel , float value ) {
		int N = kernel.width*kernel.width;
		for( int i = 0; i < N; i++ ) {
			kernel.data[i] = value;
		}
	}

	public static void fill( Kernel2D_I32 kernel , int value ) {
		int N = kernel.width*kernel.width;
		for( int i = 0; i < N; i++ ) {
			kernel.data[i] = value;
		}
	}

	public static Kernel2D_F32 transpose( Kernel2D_F32 a ) {
		Kernel2D_F32 b = new Kernel2D_F32( a.width );

		for( int i = 0; i < a.width; i++ ) {
			for( int j = 0; j < a.width; j++ ) {
				b.set(j,i,a.get(i,j));
			}
		}
		return b;
	}

	public static Kernel2D_I32 transpose( Kernel2D_I32 a ) {
		Kernel2D_I32 b = new Kernel2D_I32( a.width );

		for( int i = 0; i < a.width; i++ ) {
			for( int j = 0; j < a.width; j++ ) {
				b.set(j,i,a.get(i,j));
			}
		}
		return b;
	}

	public static Kernel2D_F32 convolve( Kernel1D_F32 a , Kernel1D_F32 b ) {
		int w = a.width;

		Kernel2D_F32 ret = new Kernel2D_F32(w);

		int index = 0;
		for( int i = 0; i < w; i++ ) {
			for( int j = 0; j < w; j++ ) {
				ret.data[ index++ ] = a.data[i] * b.data[j];
			}
		}

		return ret;
	}

	public static Kernel2D_I32 convolve( Kernel1D_I32 a , Kernel1D_I32 b ) {
		int w = a.width;

		Kernel2D_I32 ret = new Kernel2D_I32(w);

		int index = 0;
		for( int i = 0; i < w; i++ ) {
			for( int j = 0; j < w; j++ ) {
				ret.data[ index++ ] = a.data[i] * b.data[j];
			}
		}

		return ret;
	}

	/**
	 * Normalizes the array such that it sums up to one.
	 *
	 * @param kernel The kernel being normalized.
	 */
	public static void normalizeSumToOne(Kernel1D_F32 kernel) {

		float[] data = kernel.data;
		float total = 0;
		for (int i = 0; i < data.length; i++) total += data[i];
		for (int i = 0; i < data.length; i++) data[i] /= total;
	}

	/**
	 * Normalizes the array such that it sums up to one.
	 *
	 * @param kernel The kernel being normalized.
	 */
	public static void normalizeSumToOne(Kernel2D_F32 kernel) {

		float[] data = kernel.data;
		float total = 0;
		for (int i = 0; i < data.length; i++) total += data[i];
		for (int i = 0; i < data.length; i++) data[i] /= total;
	}

	public static ImageFloat32 convertToImage( Kernel2D_F32 kernel ) {
		int w = kernel.getWidth();
		ImageFloat32 ret = new ImageFloat32(w,w);

		for( int i = 0; i < w; i++ ) {
			for( int j = 0; j < w; j++ ) {
				ret.set(j,i,kernel.get(j,i));
			}
		}

		return ret;
	}

	public static ImageSInt32 convertToImage( Kernel2D_I32 kernel ) {
		int w = kernel.getWidth();
		ImageSInt32 ret = new ImageSInt32(w,w);

		for( int i = 0; i < w; i++ ) {
			for( int j = 0; j < w; j++ ) {
				ret.set(j,i,kernel.get(j,i));
			}
		}

		return ret;
	}

	public static Kernel2D_F32 convertToKernel( ImageFloat32 image ) {
		int w = image.getWidth();
		Kernel2D_F32 ret = new Kernel2D_F32(w);

		for( int i = 0; i < w; i++ ) {
			for( int j = 0; j < w; j++ ) {
				ret.set(j,i,image.get(j,i));
			}
		}

		return ret;
	}

	public static Kernel2D_I32 convertToKernel( ImageInteger image ) {
		int w = image.getWidth();
		Kernel2D_I32 ret = new Kernel2D_I32(w);

		for( int i = 0; i < w; i++ ) {
			for( int j = 0; j < w; j++ ) {
				ret.set(j,i,image.get(j,i));
			}
		}

		return ret;
	}

	public static Kernel2D_I32 convert( Kernel2D_F32 original , float minFrac ) {
		Kernel2D_I32 ret = new Kernel2D_I32(original.width);
		convert( original.data,ret.data,original.width*original.width,minFrac);
		return ret;
	}

	public static Kernel1D_I32 convert( Kernel1D_F32 original , float minFrac ) {

		Kernel1D_I32 ret = new Kernel1D_I32(original.width);
		convert( original.data,ret.data,original.width,minFrac);

		return ret;
	}

	public static void convert( float input[] , int output[] , int size , float minFrac) {
		float max = maxAbs(input,size);
		float min = minAbs(input,size,max*minFrac);

		for( int i = 0; i < size; i++ ) {
			output[i] = (int)(input[i]/min);
		}
	}

	public static float maxAbs( float data[] , int size ) {
		float max = 0;
		for( int i = 0; i < size; i++ ) {
			float v = Math.abs(data[i]);

			if( v > max )
				max = v;
		}
		return max;
	}

	public static float minAbs( float data[] , int size , float minValue ) {
		float min = Float.MAX_VALUE;
		for( int i = 0; i < size; i++ ) {
			float v = Math.abs(data[i]);

			if( v < min && v >= minValue )
				min = v;
		}
		return min;
	}

	public static boolean isEquals( float expected[] , float found[] , int size , float tol ) {
		for( int i = 0; i < size; i++ ) {
			float norm = Math.max(Math.abs(expected[i]),Math.abs(found[i]));
			if( norm < 1.0 ) norm = 1.0f;
			float diff = Math.abs(expected[i] - found[i])/norm;
			if( diff > tol )
				return false;
		}
		return true;
	}

	public static boolean isEquals( int expected[] , int found[] , int size ) {
		for( int i = 0; i < size; i++ ) {
			float diff = Math.abs(expected[i] - found[i]);
			if( diff > 0 )
				return false;
		}
		return true;
	}
}
