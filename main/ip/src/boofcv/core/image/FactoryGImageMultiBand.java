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

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;

/**
 * @author Peter Abeles
 */
public class FactoryGImageMultiBand {

	public static GImageMultiBand wrap( ImageSingleBand image ) {
		return new GSingleToMB(FactoryGImageSingleBand.wrap(image));
	}

	public static GImageMultiBand create( ImageType imageType ) {
		if( imageType.getFamily() == ImageType.Family.MULTI_SPECTRAL ) {
			return new MS();
		} else {
			throw new RuntimeException("Add support for more families");
		}
	}

	public static GImageMultiBand wrap( MultiSpectral image ) {
		return new MS(image);
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
		public <T extends ImageBase> T getImage() {
			return (T)sb.getImage();
		}
	}
}
