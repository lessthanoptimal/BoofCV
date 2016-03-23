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

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.PolynomialPixel;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.ImageType;

/**
 * <p>
 * Implementation of {@link PolynomialPixel}.
 * </p>
 * <p>
 * NOTE: This code was automatically generated using {@link GenerateImplPolynomialPixel}.
 * </p>
 * 
 * @author Peter Abeles
 */
public class ImplPolynomialPixel_I extends PolynomialPixel<GrayI> {

	public ImplPolynomialPixel_I(int maxDegree, float min, float max) {
		super(maxDegree, min, max);
	}

	@Override
	public float get(float x, float y) {
		if( x < 0 || y < 0 || x > image.width-1 || y > image.height-1 )
			return get_border(x,y);
		int width = image.getWidth();
		int height = image.getHeight();

		final int xt = (int) x;
		final int yt = (int) y;

		// offM makes sure even numbered M will bound the test point with samples
		int x0 = xt - M/2 + offM;
		int x1 = x0 + M;
		int y0 = yt - M/2 + offM;
		int y1 = y0 + M;

		if( x0 < 0 ) { x0 = 0;}
		if( x1 > width) {x1 = width;}

		if( y0 < 0 ) { y0 = 0;}
		if( y1 > height) {y1 = height;}

		final int horizM = x1-x0;
		final int vertM = y1-y0;

		interp1D.setInput(horiz,horizM);
		for( int i = 0; i < vertM; i++ ) {
			for( int j = 0; j < horizM; j++ ) {
				horiz[j] = image.get(j+x0,i+y0);
			}
			vert[i]=interp1D.process(x-x0,0,horizM-1);
		}
		interp1D.setInput(vert,vertM);

		float ret = interp1D.process(y-y0,0,vertM-1);

		// because it is fitting polynomials it can go above and below max values.
		if( ret > max ) {
			ret = max;
		} else if( ret < min ) {
			ret = min;
		}
		return ret;
	}

	@Override
	public float get_fast(float x, float y) {
		int xt = (int) x;
		int yt = (int) y;

		int x0 = xt - M/2 + offM;
		int y0 = yt - M/2 + offM;

		interp1D.setInput(horiz,horiz.length);
		for( int i = 0; i < M; i++ ) {
			for( int j = 0; j < M; j++ ) {
				horiz[j] = image.get(j+x0,i+y0);
			}
			vert[i]=interp1D.process(x-x0,0,M-1);
		}
		interp1D.setInput(vert,vert.length);

		float ret = interp1D.process(y-y0,0,M-1);

		// because it is fitting polynomials it can go above or below max or min values.
		if( ret > max ) {
			ret = max;
		} else if( ret < min ) {
			ret = min;
		}
		return ret;
	}

	public float get_border(float x, float y) {
		int xt = (int) Math.floor(x);
		int yt = (int) Math.floor(y);

		int x0 = xt - M/2 + offM;
		int y0 = yt - M/2 + offM;

		ImageBorder_S32 border = (ImageBorder_S32)this.border;

		interp1D.setInput(horiz,horiz.length);
		for( int i = 0; i < M; i++ ) {
			for( int j = 0; j < M; j++ ) {
				horiz[j] = border.get(j+x0,i+y0);
			}
			vert[i]=interp1D.process(x-x0,0,M-1);
		}
		interp1D.setInput(vert,vert.length);

		float ret = interp1D.process(y-y0,0,M-1);

		// because it is fitting polynomials it can go above or below max or min values.
		if( ret > max ) {
			ret = max;
		} else if( ret < min ) {
			ret = min;
		}
		return ret;
	}
	@Override
	public ImageType<GrayI> getImageType() {
		return ImageType.single(GrayI.class);
	}

}
