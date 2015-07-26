/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.core.image.border.ImageBorder;
import boofcv.core.image.border.ImageBorder_IL_F32;
import boofcv.struct.image.*;

/**
 * @author Peter Abeles
 */
public class FactoryGImageMultiBand {

	public static GImageMultiBand wrap( ImageBase image ) {
		if( image instanceof ImageSingleBand )
			return wrap((ImageSingleBand)image);
		else if( image instanceof MultiSpectral )
			return wrap((MultiSpectral)image);
		else if( image instanceof ImageInterleaved )
			return wrap((ImageInterleaved)image);
		throw new RuntimeException("Unknown image type");
	}

	public static GImageMultiBand wrap( ImageSingleBand image ) {
		return new GSingleToMB(FactoryGImageSingleBand.wrap(image));
	}

	public static GImageMultiBand create( ImageType imageType ) {
		if( imageType.getFamily() == ImageType.Family.MULTI_SPECTRAL ) {
			return new MS();
		} else if( imageType.getFamily() == ImageType.Family.INTERLEAVED ) {
			switch( imageType.getDataType() ) {
				case U8:
					return new IL_U8();
				case S8:
					return new IL_S8();
				case F32:
					return new IL_F32();
				default:
					throw new IllegalArgumentException("Need to support more data types");
			}
		} else {
			throw new RuntimeException("Add support for more families");
		}
	}

	public static GImageMultiBand wrap( MultiSpectral image ) {
		return new MS(image);
	}

	public static GImageMultiBand wrap( ImageInterleaved image ) {
		switch( image.getDataType() ) {
			case U8:
				return new IL_U8((InterleavedU8)image);
			case S8:
				return new IL_S8((InterleavedS8)image);
			case F32:
				return new IL_F32((InterleavedF32)image);
			default:
				throw new IllegalArgumentException("Need to support more data types");
		}
	}

	public static class MS implements GImageMultiBand {
		MultiSpectral image;
		GImageSingleBand bandWrappers[];

		public MS(MultiSpectral image) {
			wrap(image);
		}

		public MS() {
		}

		@Override
		public void wrap(ImageBase image) {
			if( this.image == null ) {
				this.image = (MultiSpectral) image;

				bandWrappers = new GImageSingleBand[this.image.getNumBands()];
				for (int i = 0; i < bandWrappers.length; i++) {
					bandWrappers[i] = FactoryGImageSingleBand.wrap(this.image.getBand(i));
				}
			} else {
				this.image = (MultiSpectral) image;
				for (int i = 0; i < bandWrappers.length; i++) {
					bandWrappers[i].wrap(this.image.getBand(i));
				}
			}
		}

		@Override
		public int getWidth() {return image.getWidth();}

		@Override
		public int getHeight() {return image.getHeight();}

		@Override
		public int getNumberOfBands() {return image.getNumBands();}

		@Override
		public void set(int x, int y, float[] value) {
			int index = this.image.getIndex(x,y);
			setF(index,value);
		}

		@Override
		public void get(int x, int y, float[] value) {
			int index = this.image.getIndex(x,y);
			getF(index, value);
		}

		@Override
		public void setF(int index, float[] value) {
			for( int i = 0; i < image.getNumBands(); i++ ) {
				bandWrappers[i].set(index, value[i]);
			}
		}

		@Override
		public void getF(int index, float[] value) {
			for( int i = 0; i < image.getNumBands(); i++ ) {
				value[i] = bandWrappers[i].getF(index);
			}
		}

		@Override
		public float getF(int index) {
			throw new RuntimeException("Not supported for MultiSpectral images.  Would be slow.");
		}

		@Override
		public <T extends ImageBase> T getImage() {
			return (T)image;
		}
	}

	public static class GSingleToMB implements GImageMultiBand {

		GImageSingleBand sb;

		public GSingleToMB(GImageSingleBand sb) {
			this.sb = sb;
		}

		@Override
		public void wrap(ImageBase image) {
			if( this.sb == null ) {
				this.sb = FactoryGImageSingleBand.wrap((ImageSingleBand)image);
			} else {
				this.sb.wrap((ImageSingleBand)image);
			}
		}

		@Override
		public int getWidth() {return sb.getWidth();}

		@Override
		public int getHeight() {return sb.getHeight();}

		@Override
		public int getNumberOfBands() {return 1;}

		@Override
		public void set(int x, int y, float[] value) {
			sb.set(x,y,value[0]);
		}

		@Override
		public void get(int x, int y, float[] value) {
			value[0]=sb.getF(y*sb.getWidth()+x);
		}

		@Override
		public void setF(int index, float[] value) {
			sb.set(index,value[0]);
		}

		@Override
		public void getF(int index, float[] value) {
			value[0] = sb.getF(index);
		}

		@Override
		public float getF(int index) {
			return sb.getF(index);
		}

		@Override
		public <T extends ImageBase> T getImage() {
			return (T)sb.getImage();
		}
	}

	public static abstract class IL<T extends ImageInterleaved<T>> implements GImageMultiBand {
		T image;

		@Override
		public void wrap(ImageBase image) {
			this.image = (T) image;
		}

		@Override
		public int getWidth() {return image.getWidth();}

		@Override
		public int getHeight() {return image.getHeight();}

		@Override
		public int getNumberOfBands() {return image.getNumBands();}

		@Override
		public <T extends ImageBase> T getImage() {
			return (T)image;
		}
	}

	public static class IL_U8 extends IL<InterleavedU8> {
		public IL_U8(InterleavedU8 image) {
			wrap(image);
		}

		public IL_U8() {}

		@Override
		public void set(int x, int y, float[] value) {
			setF(image.getIndex(x,y),value);
		}

		@Override
		public void get(int x, int y, float[] value) {
			getF(image.getIndex(x,y), value);
		}

		@Override
		public void setF(int index, float[] value) {
			for( int i = 0; i < image.getNumBands(); i++ ) {
				image.data[index++] = (byte)value[i];
			}
		}

		@Override
		public void getF(int index, float[] value) {
			for( int i = 0; i < image.getNumBands(); i++ ) {
				value[i] = image.data[index++] & 0xFF;
			}
		}

		@Override
		public float getF(int index) {
			return image.data[index] & 0xFF;
		}
	}

	public static class IL_S8 extends IL<InterleavedS8> {
		public IL_S8(InterleavedS8 image) {
			wrap(image);
		}

		public IL_S8() {}

		@Override
		public void set(int x, int y, float[] value) {
			setF(image.getIndex(x,y),value);
		}

		@Override
		public void get(int x, int y, float[] value) {
			getF(image.getIndex(x,y), value);
		}

		@Override
		public void setF(int index, float[] value) {
			for( int i = 0; i < image.getNumBands(); i++ ) {
				image.data[index++] = (byte)value[i];
			}
		}

		@Override
		public void getF(int index, float[] value) {
			for( int i = 0; i < image.getNumBands(); i++ ) {
				value[i] = image.data[index++];
			}
		}

		@Override
		public float getF(int index) {
			return image.data[index];
		}
	}

	public static class IL_F32 extends IL<InterleavedF32> {
		public IL_F32(InterleavedF32 image) {
			wrap(image);
		}

		public IL_F32() {}

		@Override
		public void set(int x, int y, float[] value) {
			setF(image.getIndex(x,y),value);
		}

		@Override
		public void get(int x, int y, float[] value) {
			getF(image.getIndex(x,y), value);
		}

		@Override
		public void setF(int index, float[] value) {
			for( int i = 0; i < image.getNumBands(); i++ ) {
				image.data[index++] = value[i];
			}
		}

		@Override
		public void getF(int index, float[] value) {
			for( int i = 0; i < image.getNumBands(); i++ ) {
				value[i] = image.data[index++];
			}
		}

		@Override
		public float getF(int index) {
			return image.data[index];
		}
	}

	public static GImageMultiBand wrap( ImageBorder image ) {
		if( image instanceof ImageBorder_IL_F32) {
			return new Border_ILF32((ImageBorder_IL_F32) image);
		} else {
			throw new IllegalArgumentException("Not supported yet?");
		}
	}

	public static class Border_ILF32 extends GMultiBorder<ImageBorder_IL_F32> {

		public Border_ILF32(ImageBorder_IL_F32 image) {
			super(image);
		}

		@Override
		public int getNumberOfBands() {
			return ((ImageMultiBand)image.getImage()).getNumBands();
		}

		@Override
		public void set(int x, int y, float[] value) {
			image.set(x,y,value);
		}

		@Override
		public void get(int x, int y, float[] value) {
			image.get(x,y,value);
		}
	}

	public static abstract class GMultiBorder<T extends ImageBorder> implements GImageMultiBand {

		protected T image;

		public GMultiBorder(T image) {
			this.image = image;
		}

		@Override
		public void wrap(ImageBase image) {
			this.image.setImage(image);
		}

		@Override
		public int getWidth() {
			return image.getImage().getWidth();
		}

		@Override
		public int getHeight() {
			return image.getImage().getHeight();
		}

		@Override
		public ImageMultiBand getImage() {
			return (ImageMultiBand)image.getImage();
		}

		public void setF(int index, float[] value) {throw new RuntimeException("Not supported");}

		@Override
		public void getF(int index, float[] value) {throw new RuntimeException("Not supported");}

		@Override
		public float getF(int index) {throw new RuntimeException("Not supported");}
	}
}
