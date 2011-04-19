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
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;

/**
 * Functions for converting between different image types.
 *
 * @author Peter Abeles
 */
public class ConvertImage {

	/**
	 * <p>
	 * Converts an {@link ImageInt8} into a {@link ImageFloat32}.  If the {@link ImageInt8}
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
	 * @param signed
	 */
	public static ImageFloat32 convert(ImageInt8 src, ImageFloat32 dst, boolean signed) {
		if (dst == null) {
			dst = new ImageFloat32(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		if (src.isSubimage() || dst.isSubimage()) {

			if (signed) {
				for (int y = 0; y < src.height; y++) {
					int indexSrc = src.getIndex(0, y);
					int indexDst = dst.getIndex(0, y);

					for (int x = 0; x < src.width; x++) {
						dst.data[indexDst++] = src.data[indexSrc++];
					}
				}
			} else {
				for (int y = 0; y < src.height; y++) {
					int indexSrc = src.getIndex(0, y);
					int indexDst = dst.getIndex(0, y);

					for (int x = 0; x < src.width; x++) {
						dst.data[indexDst++] = src.data[indexSrc++] & 0xFF;
					}
				}
			}

		} else {
			final int N = src.width * src.height;

			if (signed) {
				for (int i = 0; i < N; i++) {
					dst.data[i] = src.data[i];
				}
			} else {
				for (int i = 0; i < N; i++) {
					dst.data[i] = src.data[i] & 0xFF;
				}
			}
		}

		return dst;
	}

	/**
	 * <p>
	 * Converts an {@link ImageInt16} into a {@link ImageFloat32}.  If the {@link ImageInt16}
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
	 * @param signed
	 */
	public static ImageFloat32 convert(ImageInt16 src, ImageFloat32 dst, boolean signed) {
		if (dst == null) {
			dst = new ImageFloat32(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

		if (src.isSubimage() || dst.isSubimage()) {

			if (signed) {
				for (int y = 0; y < src.height; y++) {
					int indexSrc = src.getIndex(0, y);
					int indexDst = dst.getIndex(0, y);

					for (int x = 0; x < src.width; x++) {
						dst.data[indexDst++] = src.data[indexSrc++];
					}
				}
			} else {
				for (int y = 0; y < src.height; y++) {
					int indexSrc = src.getIndex(0, y);
					int indexDst = dst.getIndex(0, y);

					for (int x = 0; x < src.width; x++) {
						dst.data[indexDst++] = src.data[indexSrc++] & 0xFFFF;
					}
				}
			}

		} else {
			final int N = src.width * src.height;

			if (signed) {
				for (int i = 0; i < N; i++) {
					dst.data[i] = src.data[i];
				}
			} else {
				for (int i = 0; i < N; i++) {
					dst.data[i] = src.data[i] & 0xFFFF;
				}
			}
		}

		return dst;
	}

	/**
	 * <p>
	 * Converts {@link gecv.struct.image.ImageFloat32} into {@link ImageInt8}.  No additional
	 * scaling is done, just a straight conversion.
	 * </p>
	 * <p/>
	 * <p>
	 * dst(x,y) = (byte)src(x,y)
	 * </p>
	 *
	 * @param src
	 * @param dst
	 */
	public static ImageInt8 convert(ImageFloat32 src, ImageInt8 dst) {
		if (dst == null) {
			dst = new ImageInt8(src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}

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

		return dst;
	}

}
