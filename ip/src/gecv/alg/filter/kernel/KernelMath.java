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

import gecv.struct.convolve.*;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt32;


/**
 * @author Peter Abeles
 */
public class KernelMath {

	public static void setTo( Kernel2D_F32 kernel , float value ) {
		int N = kernel.width*kernel.width;
		for( int i = 0; i < N; i++ ) {
			kernel.data[i] = value;
		}
	}

	public static Kernel2D transpose( Kernel2D a ) {
		if( a instanceof Kernel2D_F32)
			return transpose((Kernel2D_F32)a);
		else
			return transpose((Kernel2D_I32)a);

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

	public static void normalizeByLargestMagnitude(Kernel2D_F32 kernel) {

		float norm = 0;

		int N = kernel.width*kernel.width;
		for( int i = 0; i < N; i++ ) {
			float v = Math.abs(kernel.data[i]);
			if( v > norm )
				norm = v;
		}

		for( int i = 0; i < N; i++ ) {
			kernel.data[i] /= norm;
		}
	}

	public static void normalizeByLargestMagnitude(Kernel1D_F32 kernel) {

		float norm = 0;

		int N = kernel.width;
		for( int i = 0; i < N; i++ ) {
			float v = Math.abs(kernel.data[i]);
			if( v > norm )
				norm = v;
		}

		for( int i = 0; i < N; i++ ) {
			kernel.data[i] /= norm;
		}
	}

	public static void normalizeEnergy( Kernel2D_F32 kernel ) {
		double total = 0;

		int N = kernel.width*kernel.width;
		for( int i = 0; i < N; i++ ) {
			float v = kernel.data[i];
			total += v*v;
		}
		float norm = (float)Math.sqrt(total);
		for( int i = 0; i < N; i++ ) {
			kernel.data[i] /= norm;
		}
	}

	public static void normalizeEnergy( Kernel1D_F32 kernel ) {
		double total = 0;

		int N = kernel.width;
		for( int i = 0; i < N; i++ ) {
			float v = kernel.data[i];
			total += v*v;
		}
		float norm = (float)Math.sqrt(total);
		for( int i = 0; i < N; i++ ) {
			kernel.data[i] /= norm;
		}
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

	public static Kernel2D_I32 convertToKernel( ImageSInt32 image ) {
		int w = image.getWidth();
		Kernel2D_I32 ret = new Kernel2D_I32(w);

		for( int i = 0; i < w; i++ ) {
			for( int j = 0; j < w; j++ ) {
				ret.set(j,i,image.get(j,i));
			}
		}

		return ret;
	}

	public static Kernel2D_I32 convert( Kernel2D_F32 original ) {
		float minAbs = Float.POSITIVE_INFINITY;

		int N = original.width*original.width;
		for( int i = 0; i < N; i++ ) {
			float v = Math.abs(original.data[i]);
			if( v < minAbs )
				minAbs = v;
		}

		Kernel2D_I32 ret = new Kernel2D_I32(original.width);
		for( int i = 0; i < N; i++ ) {
			ret.data[i] = (int)Math.round(original.data[i]/minAbs);
		}
		return ret;
	}

	public static Kernel1D_I32 convert( Kernel1D_F32 original ) {
		float minAbs = Float.POSITIVE_INFINITY;

		int N = original.width;
		for( int i = 0; i < N; i++ ) {
			float v = Math.abs(original.data[i]);
			if( v < minAbs && v > 0 )
				minAbs = v;
		}

		Kernel1D_I32 ret = new Kernel1D_I32(original.width);
		for( int i = 0; i < N; i++ ) {
			ret.data[i] = Math.round(original.data[i]/minAbs);
		}
		return ret;
	}
}
