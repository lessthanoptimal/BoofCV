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

package gecv.core.image;

import gecv.alg.InputSanityCheck;
import gecv.struct.image.*;

/**
 * Functions for converting between different image types. Numerical values do not change or are closely approximated
 * in these functions.  
 *
 * @author Peter Abeles
 */
public class ConvertImage {

	/**
	 * <p>
	 * Converts an {@link gecv.struct.image.ImageUInt8} into a {@link ImageFloat32}.  If the {@link gecv.struct.image.ImageUInt8}
	 * is treated as a signed image or not is specified.
	 * </p>
	 * <p/>
	 * <p>
	 * If signed:<br>
	 * dst(x,y) = (float)src(x,y)<br>
	 * If unsigned:<br>
	 * dst(x,y) = (float)( src(x,y) & 0xFF )
	 * </p>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageFloat32 convert(ImageUInt8 src, ImageFloat32 dst ) {
		if (dst == null) {
			dst = new ImageFloat32(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		if (src.isSubimage() || dst.isSubimage()) {

			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.getIndex(0, y);
				int indexDst = dst.getIndex(0, y);

				for (int x = 0; x < src.width; x++) {
					dst.data[indexDst++] = src.data[indexSrc++] & 0xFF;
				}
			}

		} else {
			final int N = src.width * src.height;

			for (int i = 0; i < N; i++) {
				dst.data[i] = src.data[i] & 0xFF;
			}
		}

		return dst;
	}

	/**
	 * <p>
	 * Converts an {@link gecv.struct.image.ImageUInt8} into a {@link gecv.struct.image.ImageSInt16}.
	 * </p>
	 * <p/>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageSInt16 convert(ImageUInt8 src, ImageSInt16 dst ) {
		if (dst == null) {
			dst = new ImageSInt16(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		convert(src,(ImageInt16)dst);

		return dst;
	}

/**
	 * <p>
	 * Converts an {@link gecv.struct.image.ImageUInt8} into a {@link gecv.struct.image.ImageUInt16}.
	 * </p>
	 * <p/>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageUInt16 convert(ImageUInt8 src, ImageUInt16 dst ) {
		if (dst == null) {
			dst = new ImageUInt16(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		convert(src,(ImageInt16)dst);

		return dst;
	}

	/**
	 * Common class for converting unsigned 8-bit into 16-bit image.
	 */
	private static void convert(ImageUInt8 src, ImageInt16 dst ) {
		if (src.isSubimage() || dst.isSubimage()) {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.getIndex(0, y);
				int indexDst = dst.getIndex(0, y);

				for (int x = 0; x < src.width; x++) {
					dst.data[indexDst++] = (short)(src.data[indexSrc++] & 0xFF);
				}
			}
		} else {
			final int N = src.width * src.height;

			for (int i = 0; i < N; i++) {
				dst.data[i] = (short)(src.data[i] & 0xFF);
			}
		}
	}

	/**
	 * <p>
	 * Converts an {@link gecv.struct.image.ImageSInt16} into a {@link ImageFloat32}.
	 * </p>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageFloat32 convert(ImageSInt16 src, ImageFloat32 dst ) {
		if (dst == null) {
			dst = new ImageFloat32(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		if (src.isSubimage() || dst.isSubimage()) {

			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.getIndex(0, y);
				int indexDst = dst.getIndex(0, y);

				for (int x = 0; x < src.width; x++) {
					dst.data[indexDst++] = src.data[indexSrc++];
				}
			}

		} else {
			final int N = src.width * src.height;

			for (int i = 0; i < N; i++) {
				dst.data[i] = src.data[i];
			}
		}

		return dst;
	}

/**
	 * <p>
	 * Converts an {@link gecv.struct.image.ImageUInt16} into a {@link ImageFloat32}.
	 * </p>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageFloat32 convert(ImageUInt16 src, ImageFloat32 dst ) {
		if (dst == null) {
			dst = new ImageFloat32(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		if (src.isSubimage() || dst.isSubimage()) {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.getIndex(0, y);
				int indexDst = dst.getIndex(0, y);

				for (int x = 0; x < src.width; x++) {
					dst.data[indexDst++] = src.data[indexSrc++] & 0xFFFF;
				}
			}
		} else {
			final int N = src.width * src.height;

			for (int i = 0; i < N; i++) {
				dst.data[i] = src.data[i] & 0xFFFF;
			}
		}

		return dst;
	}

	/**
	 * <p>
	 * Converts an {@link gecv.struct.image.ImageSInt16} into a {@link gecv.struct.image.ImageUInt8}.
	 * </p>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageUInt8 convert(ImageSInt16 src, ImageUInt8 dst ) {
		if (dst == null) {
			dst = new ImageUInt8(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		if (src.isSubimage() || dst.isSubimage()) {

			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.getIndex(0, y);
				int indexDst = dst.getIndex(0, y);

				for (int x = 0; x < src.width; x++) {
					dst.data[indexDst++] = (byte)src.data[indexSrc++];
				}
			}

		} else {
			final int N = src.width * src.height;

			for (int i = 0; i < N; i++) {
				dst.data[i] = (byte)src.data[i];
			}
		}

		return dst;
	}

	/**
	 * <p>
	 * Converts an {@link gecv.struct.image.ImageUInt16} into a {@link gecv.struct.image.ImageUInt8}.
	 * </p>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageUInt8 convert(ImageUInt16 src, ImageUInt8 dst ) {
		if (dst == null) {
			dst = new ImageUInt8(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		if (src.isSubimage() || dst.isSubimage()) {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.getIndex(0, y);
				int indexDst = dst.getIndex(0, y);

				for (int x = 0; x < src.width; x++) {
					dst.data[indexDst++] = (byte)(src.data[indexSrc++] & 0xFFFF);
				}
			}

		} else {
			final int N = src.width * src.height;

			for (int i = 0; i < N; i++) {
				dst.data[i] = (byte)(src.data[i] & 0xFFFF);
			}
		}

		return dst;
	}

	/**
	 * <p>
	 * Converts {@link gecv.struct.image.ImageFloat32} into {@link gecv.struct.image.ImageUInt8}.  No additional
	 * scaling is done, just a straight conversion.
	 * </p>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageUInt8 convert(ImageFloat32 src, ImageUInt8 dst) {
		if (dst == null) {
			dst = new ImageUInt8(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		convert(src,(ImageInt8)dst);

		return dst;
	}

	/**
	 * <p>
	 * Converts {@link gecv.struct.image.ImageFloat32} into {@link gecv.struct.image.ImageSInt8}.  No additional
	 * scaling is done, just a straight conversion.
	 * </p>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageSInt8 convert(ImageFloat32 src, ImageSInt8 dst) {
		if (dst == null) {
			dst = new ImageSInt8(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		convert(src,(ImageInt8)dst);

		return dst;
	}

	/**
	 * Generic class to convert F32 into any I8 image.
	 */
	private static void convert(ImageFloat32 src, ImageInt8 dst) {

		if (src.isSubimage() || dst.isSubimage()) {

			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.getIndex(0, y);
				int indexDst = dst.getIndex(0, y);

				for (int x = 0; x < src.width; x++) {
					dst.data[indexDst++] = (byte) src.data[indexSrc++];
				}
			}

		} else {
			final int N = src.width * src.height;

			for (int i = 0; i < N; i++) {
				dst.data[i] = (byte) src.data[i];
			}
		}
	}

	/**
	 * <p>
	 * Converts {@link gecv.struct.image.ImageFloat32} into {@link gecv.struct.image.ImageSInt16}.  No additional
	 * scaling is done, just a straight conversion.
	 * </p>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageSInt16 convert(ImageFloat32 src, ImageSInt16 dst) {
		if (dst == null) {
			dst = new ImageSInt16(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		convert(src,(ImageInt16)dst);

		return dst;
	}

	/**
	 * <p>
	 * Converts {@link gecv.struct.image.ImageFloat32} into {@link gecv.struct.image.ImageUInt16}.  No additional
	 * scaling is done, just a straight conversion.
	 * </p>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageUInt16 convert(ImageFloat32 src, ImageUInt16 dst) {
		if (dst == null) {
			dst = new ImageUInt16(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		convert(src,(ImageInt16)dst);

		return dst;
	}

	/**
	 * Internal function used to convert F32 into any I16 image
	 */
	private static void convert(ImageFloat32 src, ImageInt16 dst) {

		if (src.isSubimage() || dst.isSubimage()) {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.getIndex(0, y);
				int indexDst = dst.getIndex(0, y);

				for (int x = 0; x < src.width; x++) {
					dst.data[indexDst++] = (short) src.data[indexSrc++];
				}
			}

		} else {
			final int N = src.width * src.height;

			for (int i = 0; i < N; i++) {
				dst.data[i] = (short) src.data[i];
			}
		}
	}

	/**
	 * <p>
	 * Converts {@link gecv.struct.image.ImageFloat32} into {@link gecv.struct.image.ImageFloat64}.  No additional
	 * scaling is done, just a straight conversion.
	 * </p>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageFloat64 convert(ImageFloat32 src, ImageFloat64 dst) {
		if (dst == null) {
			dst = new ImageFloat64(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		for (int y = 0; y < src.height; y++) {
			int indexSrc = src.getIndex(0, y);
			int indexDst = dst.getIndex(0, y);

			for (int x = 0; x < src.width; x++) {
				dst.data[indexDst++] = src.data[indexSrc++];
			}
		}

		return dst;
	}

	/**
	 * <p>
	 * Converts {@link gecv.struct.image.ImageFloat64} into {@link gecv.struct.image.ImageFloat32}.  No additional
	 * scaling is done, just a straight conversion.
	 * </p>
	 *
	 * @param src	Input image which is being converted.
	 * @param dst	The output image.  If null a new image is created.
	 */
	public static ImageFloat32 convert(ImageFloat64 src, ImageFloat32 dst) {
		if (dst == null) {
			dst = new ImageFloat32(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		for (int y = 0; y < src.height; y++) {
			int indexSrc = src.getIndex(0, y);
			int indexDst = dst.getIndex(0, y);

			for (int x = 0; x < src.width; x++) {
				dst.data[indexDst++] = (float)src.data[indexSrc++];
			}
		}

		return dst;
	}
}
