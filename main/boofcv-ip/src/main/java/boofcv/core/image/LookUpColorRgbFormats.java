/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.*;

/**
 * Specific implementations of {@link LookUpColorRgb}
 *
 * @author Peter Abeles
 */
public class LookUpColorRgbFormats {
	@SuppressWarnings({"NullAway.Init"})
	public static class PL_U8 implements LookUpColorRgb<Planar<GrayU8>> {
		Planar<GrayU8> image;

		@Override public void setImage( Planar<GrayU8> image ) {
			BoofMiscOps.checkTrue(image.getNumBands() >= 3);
			this.image = image;
		}

		@Override public int lookupRgb( int x, int y ) {
			return image.get24u8(x, y);
		}

		@Override public ImageType<Planar<GrayU8>> getImageType() {return ImageType.PL_U8;}
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class PL_F32 implements LookUpColorRgb<Planar<GrayF32>> {
		Planar<GrayF32> image;

		@Override public void setImage( Planar<GrayF32> image ) {
			BoofMiscOps.checkTrue(image.getNumBands() >= 3);
			this.image = image;
		}

		@Override public int lookupRgb( int x, int y ) {
			int index = image.getIndex(x, y);
			int red = (int)image.bands[0].data[index];
			int green = (int)image.bands[1].data[index];
			int blue = (int)image.bands[2].data[index];
			return (red << 16) | (green << 8) | blue;
		}

		@Override public ImageType<Planar<GrayF32>> getImageType() {return ImageType.PL_F32;}
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class IL_U8 implements LookUpColorRgb<InterleavedU8> {
		InterleavedU8 image;

		@Override public void setImage( InterleavedU8 image ) {
			BoofMiscOps.checkTrue(image.getNumBands() >= 3);
			this.image = image;
		}

		@Override public int lookupRgb( int x, int y ) {
			return image.get24(x, y);
		}

		@Override public ImageType<InterleavedU8> getImageType() {return ImageType.IL_U8;}
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class IL_F32 implements LookUpColorRgb<InterleavedF32> {
		InterleavedF32 image;

		@Override public void setImage( InterleavedF32 image ) {
			BoofMiscOps.checkTrue(image.getNumBands() >= 3);
			this.image = image;
		}

		@Override public int lookupRgb( int x, int y ) {
			int index = image.getIndex(x, y, 0);
			int red = (int)image.data[index];
			int green = (int)image.data[index + 1];
			int blue = (int)image.data[index + 2];
			return (red << 16) | (green << 8) | blue;
		}

		@Override public ImageType<InterleavedF32> getImageType() {return ImageType.IL_F32;}
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class SB_U8 implements LookUpColorRgb<GrayU8> {
		GrayU8 image;

		@Override public void setImage( GrayU8 image ) {
			this.image = image;
		}

		@Override public int lookupRgb( int x, int y ) {
			int v = image.unsafe_get(x, y);
			return (v << 16) | (v << 8) | v;
		}

		@Override public ImageType<GrayU8> getImageType() {return ImageType.SB_U8;}
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class SB_F32 implements LookUpColorRgb<GrayF32> {
		GrayF32 image;

		@Override public void setImage( GrayF32 image ) {
			this.image = image;
		}

		@Override public int lookupRgb( int x, int y ) {
			int v = (int)image.unsafe_get(x, y);
			return (v << 16) | (v << 8) | v;
		}

		@Override public ImageType<GrayF32> getImageType() {return ImageType.SB_F32;}
	}
}
