/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.misc;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.metric.Intersection2D_F32;
import georegression.struct.shapes.RectangleCorner2D_F32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImplAverageResample {

	Random rand = new Random(234);
	int imageWidth = 30;
	int imageHeight = 40;

	@Test
	public void horizontal() {
		// align along existing pixels. Easy case.  Becomes a copy
		horizontal(2, 3, 6, 4, 3, 4, 6);
		//general case
		horizontal(2.2f, 3, 5.4f, 4, 3, 4, 6);
	}

	private void horizontal(float srcX0, int srcY0, float srcWidth, int srcHeight, int dstX0, int dstY0, int dstWidth) {
		ImageUInt8 src = new ImageUInt8(imageWidth,imageHeight);
		ImageFloat32 dst = new ImageFloat32(imageWidth+5,imageHeight-3);

		ImageMiscOps.fillUniform(src,rand,0,50);

		ImplAverageResample.horizontal(src, srcX0, srcY0, srcWidth, srcHeight, dst, dstX0, dstY0, dstWidth);

		for( int y = dstY0; y < dstY0+srcHeight; y++ ) {
			for( int x = dstX0; x < dstX0+dstWidth; x++ ) {
				float x0 = srcX0 + (x-dstX0)*srcWidth/dstWidth;
				float y0 = srcY0 + (y-dstY0);
				float x1 = x0 + srcWidth/dstWidth;
				float y1 = y0 + 1;

				float expected = compute(src,x0,y0,x1,y1);
				float found = dst.get(x,y);

				assertEquals(x+" "+y,expected,found,1e-4);
			}
		}
	}

	@Test
	public void vertical() {
		// align along existing pixels. Easy case.  Becomes a copy
		vertical(2, 3, 5, 6, 3, 4, 6);
		//general case
		vertical(2, 3.2f, 5, 5.7f, 3, 4, 6);
	}

	private void vertical(int srcX0, float srcY0, int srcWidth, float srcHeight, int dstX0, int dstY0, int dstHeight) {
		ImageFloat32 src = new ImageFloat32(imageWidth,imageHeight);
		ImageUInt8 dst = new ImageUInt8(imageWidth+5,imageHeight-3);

		ImageMiscOps.fillUniform(src,rand,0,50);

		ImplAverageResample.vertical(src, srcX0, srcY0, srcWidth, srcHeight, dst, dstX0, dstY0, dstHeight);

		for( int y = dstY0; y < dstY0+dstHeight; y++ ) {
			for( int x = dstX0; x < dstX0+srcWidth; x++ ) {
				float x0 = srcX0 + (x-dstX0);
				float y0 = srcY0 + (y-dstY0)*srcHeight/dstHeight;
				float x1 = x0 + 1;
				float y1 = y0 + srcHeight/dstHeight;

				int expected = (int)(compute(src,x0,y0,x1,y1)+0.5f);
				int found = dst.get(x,y);

				assertEquals(x+" "+y,expected,found);
			}
		}
	}

	public float compute( ImageSingleBand image , float x0 , float y0 , float x1, float y1 ) {
		float ret = 0;

		RectangleCorner2D_F32 rect = new RectangleCorner2D_F32(x0,y0,x1,y1);
		RectangleCorner2D_F32 pixel = new RectangleCorner2D_F32();
		RectangleCorner2D_F32 intersection = new RectangleCorner2D_F32();
		int gridX0 = (int)x0;
		int gridY0 = (int)y0;
		int gridX1 = (int)(x1+1);
		int gridY1 = (int)(y1+1);

		float totalArea = 0;

		for( int y = gridY0; y < gridY1; y++ ) {
			for( int x = gridX0 ; x < gridX1; x++ ) {
				pixel.set(x,y,x+1,y+1);

				if( !Intersection2D_F32.intersection(rect,pixel,intersection) )
					continue;
				float area = intersection.area();

				totalArea += area;
				ret += area*GeneralizedImageOps.get(image,x,y);
			}
		}

		return ret/totalArea;
	}

}
