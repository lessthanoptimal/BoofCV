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

package boofcv.alg.transform.fft;

import boofcv.abst.transform.fft.DiscreteFourierTransform;
import boofcv.struct.image.*;

import static boofcv.alg.transform.fft.DiscreteFourierTransformOps.createTransformF32;
import static boofcv.alg.transform.fft.DiscreteFourierTransformOps.createTransformF64;

/**
 * Implementation of functions in {@link DiscreteFourierTransformOps} which are image type agnostic
 *
 * @author Peter Abeles
 */
public class GDiscreteFourierTransformOps {
	/**
	 * Creates a {@link boofcv.abst.transform.fft.DiscreteFourierTransform} for the specified type.
	 *
	 * @see DiscreteFourierTransform
	 *
	 * @param type Image data type
	 * @return {@link boofcv.abst.transform.fft.DiscreteFourierTransform}
	 */
	public static <T extends GrayF, W extends ImageInterleaved>
	DiscreteFourierTransform<T,W> createTransform( ImageDataType type ) {
		switch( type ) {
			case F32: return (DiscreteFourierTransform)createTransformF32();
			case F64: return (DiscreteFourierTransform)createTransformF64();
		}
		throw new IllegalArgumentException("Unsupported image type "+type);
	}

	/**
	 * Moves the zero-frequency component into the image center (width/2,height/2).   This function can
	 * be called to undo the transform.
	 *
	 * @param transform the DFT which is to be shifted.
	 * @param forward If true then it does the shift in the forward direction.  If false then it undoes the transforms.
	 */
	public static void shiftZeroFrequency(ImageInterleaved transform, boolean forward ) {
		if( transform instanceof InterleavedF32 ) {
			DiscreteFourierTransformOps.shiftZeroFrequency((InterleavedF32) transform, forward);
		} else if( transform instanceof InterleavedF64 ) {
			DiscreteFourierTransformOps.shiftZeroFrequency((InterleavedF64)transform,forward);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}

	/**
	 * Computes the magnitude of the complex image:<br>
	 * magnitude = sqrt( real<sup>2</sup> + imaginary<sup>2</sup> )
	 * @param transform (Input)  Complex interleaved image
	 * @param magnitude (Output) Magnitude of image
	 */
	public static void magnitude( ImageInterleaved transform , GrayF magnitude ) {
		if( transform instanceof InterleavedF32 ) {
			DiscreteFourierTransformOps.magnitude((InterleavedF32) transform, (GrayF32) magnitude);
		} else if( transform instanceof InterleavedF64 ) {
			DiscreteFourierTransformOps.magnitude((InterleavedF64) transform, (GrayF64) magnitude);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}

	/**
	 * Computes the phase of the complex image:<br>
	 * phase = atan2( imaginary , real )
	 * @param transform (Input) Complex interleaved image
	 * @param phase (output) Phase of image
	 */
	public static void phase( ImageInterleaved transform , GrayF phase ) {
		if( transform instanceof InterleavedF32 ) {
			DiscreteFourierTransformOps.phase((InterleavedF32) transform, (GrayF32) phase);
		} else if( transform instanceof InterleavedF64 ) {
			DiscreteFourierTransformOps.phase((InterleavedF64) transform, (GrayF64) phase);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}

	/**
	 * Converts a regular image into a complex interleaved image with the imaginary component set to zero.
	 *
	 * @param real (Input) Regular image.
	 * @param complex (Output) Equivalent complex image.
	 */
	public static void realToComplex(GrayF real , ImageInterleaved complex ) {
		if( complex instanceof InterleavedF32 ) {
			DiscreteFourierTransformOps.realToComplex((GrayF32) real, (InterleavedF32) complex);
		} else if( complex instanceof InterleavedF64 ) {
			DiscreteFourierTransformOps.realToComplex((GrayF64) real, (InterleavedF64) complex);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}

	/**
	 * Performs element-wise complex multiplication between a real image and a complex image.
	 *
	 * @param realA (Input) Regular image
	 * @param complexB (Input) Complex image
	 * @param complexC (Output) Complex image
	 */
	public static void multiplyRealComplex( GrayF realA ,
											ImageInterleaved complexB , ImageInterleaved complexC ) {
		if( complexB instanceof InterleavedF32 ) {
			DiscreteFourierTransformOps.multiplyRealComplex((GrayF32) realA, (InterleavedF32) complexB, (InterleavedF32) complexC);
		} else if( complexB instanceof InterleavedF64 ) {
			DiscreteFourierTransformOps.multiplyRealComplex((GrayF64) realA, (InterleavedF64) complexB, (InterleavedF64) complexC);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}

	/**
	 * Performs element-wise complex multiplication between two complex images.
	 *
	 * @param complexA (Input) Complex image
	 * @param complexB (Input) Complex image
	 * @param complexC (Output) Complex image
	 */
	public static void multiplyComplex( ImageInterleaved complexA , ImageInterleaved complexB , ImageInterleaved complexC ) {
		if( complexB instanceof InterleavedF32 ) {
			DiscreteFourierTransformOps.multiplyComplex((InterleavedF32) complexA, (InterleavedF32) complexB, (InterleavedF32) complexC);
		} else if( complexB instanceof InterleavedF64 ) {
			DiscreteFourierTransformOps.multiplyComplex((InterleavedF64) complexA, (InterleavedF64) complexB, (InterleavedF64) complexC);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}
}
