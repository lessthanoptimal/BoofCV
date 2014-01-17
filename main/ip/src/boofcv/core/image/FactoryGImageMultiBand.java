/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.MultiSpectral;

/**
 * @author Peter Abeles
 */
public class FactoryGImageMultiBand {

	public static GImageMultiBand wrap( ImageSingleBand image ) {
		return new GSingleToMB(FactoryGImageSingleBand.wrap(image));
	}

	public static GImageMultiBand wrap( MultiSpectral image ) {
		return new MS(image);
	}

	public static class MS implements GImageMultiBand {
		MultiSpectral image;

		public MS(MultiSpectral image) {
			this.image = image;
		}

		@Override
		public int getWidth() {return image.getWidth();}

		@Override
		public int getHeight() {return image.getHeight();}

		@Override
		public int getNumberOfBands() {return image.getNumBands();}

		@Override
		public void set(int x, int y, float[] value) {
			for( int i = 0; i < image.getNumBands(); i++ ) {
				ImageSingleBand band = image.getBand(i);
				GeneralizedImageOps.set(band,x,y,value[i]);
			}
		}

		@Override
		public void get(int x, int y, float[] value) {
			for( int i = 0; i < image.getNumBands(); i++ ) {
				ImageSingleBand band = image.getBand(i);
				value[i] = (float)GeneralizedImageOps.get(band,x,y);
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
		public <T extends ImageBase> T getImage() {
			return (T)sb.getImage();
		}
	}
}
