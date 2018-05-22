/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.android;

import android.graphics.Bitmap;
import android.media.Image;
import boofcv.alg.color.ColorFormat;
import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
import boofcv.struct.image.ImageBase;


/**
 * Generalized operations related to compute different image derivatives.
 *
 * @author Peter Abeles
 */
public class BOverrideConvertAndroid extends BOverrideClass {

	static {
		BOverrideManager.register(BOverrideConvertAndroid.class);
	}

	public static YuvToBoof_420888 yuv420ToBoof;
	public static BitmapToBoof bitmapToBoof;
	public static BoofToBitmap boofToBitmap;

	public interface YuvToBoof_420888<T extends ImageBase<T>> {
		void yuvToBoof420(Image input, ColorFormat color, ImageBase output, byte[] work );
	}

	public interface BitmapToBoof<T extends ImageBase<T>> {
		void bitmapToBoof(Bitmap input, ImageBase output, byte[] work );
	}

	public interface BoofToBitmap<T extends ImageBase<T>> {
		void boofToBitmap(ColorFormat color, ImageBase input, Bitmap output, byte[] work );
	}

	public static boolean invokeYuv420ToBoof(Image input, ColorFormat color, ImageBase output, byte[] work) {
		boolean processed = false;
		if( BOverrideConvertAndroid.yuv420ToBoof != null ) {
			try {
				BOverrideConvertAndroid.yuv420ToBoof.yuvToBoof420(input,color,output,work);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}


	public static boolean invokeBitmapToBoof(Bitmap input, ImageBase output, byte[] work) {
		boolean processed = false;
		if( BOverrideConvertAndroid.bitmapToBoof != null ) {
			try {
				BOverrideConvertAndroid.bitmapToBoof.bitmapToBoof(input,output,work);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}

	public static boolean invokeBoofToBitmap(ColorFormat color, ImageBase input, Bitmap output, byte[] work) {
		boolean processed = false;
		if( BOverrideConvertAndroid.boofToBitmap != null ) {
			try {
				BOverrideConvertAndroid.boofToBitmap.boofToBitmap(color,input,output,work);
				processed = true;
			} catch( RuntimeException ignore ) {}
		}
		return processed;
	}
}
