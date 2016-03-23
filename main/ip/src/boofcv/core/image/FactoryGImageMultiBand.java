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

package boofcv.core.image;

import boofcv.core.image.border.ImageBorder;
import boofcv.core.image.border.ImageBorder_IL_F32;
import boofcv.core.image.border.ImageBorder_IL_F64;
import boofcv.core.image.border.ImageBorder_IL_S32;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.*;

/**
 * @author Peter Abeles
 */
public class FactoryGImageMultiBand {

	public static GImageMultiBand wrap( ImageBase image ) {
		if( image instanceof ImageGray)
			return wrap((ImageGray)image);
		else if( image instanceof Planar)
			return wrap((Planar)image);
		else if( image instanceof ImageInterleaved )
			return wrap((ImageInterleaved)image);
		throw new RuntimeException("Unknown image type");
	}

	public static GImageMultiBand wrap( ImageGray image ) {
		return new GSingleToMB(FactoryGImageGray.wrap(image));
	}

	public static GImageMultiBand create( ImageType imageType ) {
		if( imageType.getFamily() == ImageType.Family.PLANAR) {
			return new PL();
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

	public static GImageMultiBand wrap( Planar image ) {
		return new PL(image);
	}

	public static GImageMultiBand wrap( ImageInterleaved image ) {
		switch( image.getDataType() ) {
			case U8:
				return new IL_U8((InterleavedU8)image);
			case S8:
				return new IL_S8((InterleavedS8)image);
			case U16:
				return new IL_U16((InterleavedU16)image);
			case S16:
				return new IL_S16((InterleavedS16)image);
			case S32:
				return new IL_S32((InterleavedS32)image);
			case S64:
				return new IL_S64((InterleavedS64)image);
			case F32:
				return new IL_F32((InterleavedF32)image);
			case F64:
				return new IL_F64((InterleavedF64)image);
			default:
				throw new IllegalArgumentException("Need to support more data types: "+image.getDataType());
		}
	}

	public static class PL implements GImageMultiBand {
		Planar image;
		GImageGray bandWrappers[];

		public PL(Planar image) {
			wrap(image);
		}

		public PL() {
		}

		@Override
		public void wrap(ImageBase image) {
			if( this.image == null ) {
				this.image = (Planar) image;

				bandWrappers = new GImageGray[this.image.getNumBands()];
				for (int i = 0; i < bandWrappers.length; i++) {
					bandWrappers[i] = FactoryGImageGray.wrap(this.image.getBand(i));
				}
			} else {
				this.image = (Planar) image;
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
			throw new RuntimeException("Not supported for Planar images.  Would be slow.");
		}

		@Override
		public <T extends ImageBase> T getImage() {
			return (T)image;
		}
	}

	public static class GSingleToMB implements GImageMultiBand {

		GImageGray sb;

		public GSingleToMB(GImageGray sb) {
			this.sb = sb;
		}

		@Override
		public void wrap(ImageBase image) {
			if( this.sb == null ) {
				this.sb = FactoryGImageGray.wrap((ImageGray)image);
			} else {
				this.sb.wrap((ImageGray)image);
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

	public static class IL_U16 extends IL<InterleavedU16> {
		public IL_U16(InterleavedU16 image) {
			wrap(image);
		}

		public IL_U16() {}

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
				image.data[index++] = (short)value[i];
			}
		}

		@Override
		public void getF(int index, float[] value) {
			for( int i = 0; i < image.getNumBands(); i++ ) {
				value[i] = image.data[index++] & 0xFFFF;
			}
		}

		@Override
		public float getF(int index) {
			return image.data[index] & 0xFF;
		}
	}

	public static class IL_S16 extends IL<InterleavedS16> {
		public IL_S16(InterleavedS16 image) {
			wrap(image);
		}

		public IL_S16() {}

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
				image.data[index++] = (short)value[i];
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

	public static class IL_S32 extends IL<InterleavedS32> {
		public IL_S32(InterleavedS32 image) {
			wrap(image);
		}

		public IL_S32() {}

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
				image.data[index++] = (int)value[i];
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

	public static class IL_S64 extends IL<InterleavedS64> {
		public IL_S64(InterleavedS64 image) {
			wrap(image);
		}

		public IL_S64() {}

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
				image.data[index++] = (long)value[i];
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

	public static class IL_F64 extends IL<InterleavedF64> {
		public IL_F64(InterleavedF64 image) {
			wrap(image);
		}

		public IL_F64() {}

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
				value[i] = (float)image.data[index++];
			}
		}

		@Override
		public float getF(int index) {
			return (float)image.data[index];
		}
	}

	public static GImageMultiBand wrap( ImageBorder image ) {
		if( image instanceof ImageBorder_IL_S32) {
			return new Border_IL_S32((ImageBorder_IL_S32) image);
		} else if( image instanceof ImageBorder_IL_F32) {
			return new Border_IL_F32((ImageBorder_IL_F32) image);
		} else if( image instanceof ImageBorder_IL_F64) {
			return new Border_IL_F64((ImageBorder_IL_F64) image);
		} else {
			throw new IllegalArgumentException("Not supported yet?");
		}
	}

	public static class Border_IL_S32 extends GMultiBorder<ImageBorder_IL_S32> {

		public Border_IL_S32(ImageBorder_IL_S32 image) {
			super(image);
		}

		@Override
		public int getNumberOfBands() {
			return ((ImageMultiBand)image.getImage()).getNumBands();
		}

		@Override
		public void set(int x, int y, float[] value) {
			int value_d[] = BoofMiscOps.convertArray(value,(int[])null);
			image.set(x,y,value_d);
		}

		@Override
		public void get(int x, int y, float[] value) {
			int value_d[] = new int[value.length];
			image.get(x,y,value_d);
			BoofMiscOps.convertArray(value_d,value);
		}
	}

	public static class Border_IL_F32 extends GMultiBorder<ImageBorder_IL_F32> {

		public Border_IL_F32(ImageBorder_IL_F32 image) {
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

	public static class Border_IL_F64 extends GMultiBorder<ImageBorder_IL_F64> {

		public Border_IL_F64(ImageBorder_IL_F64 image) {
			super(image);
		}

		@Override
		public int getNumberOfBands() {
			return ((ImageMultiBand)image.getImage()).getNumBands();
		}

		@Override
		public void set(int x, int y, float[] value) {
			double value_d[] = BoofMiscOps.convertArray(value,(double[])null);
			image.set(x,y,value_d);
		}

		@Override
		public void get(int x, int y, float[] value) {
			double value_d[] = new double[value.length];
			image.get(x,y,value_d);
			BoofMiscOps.convertArray(value_d,value);
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
