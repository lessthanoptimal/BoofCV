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

package boofcv.abst.tracker;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Polygon2D_I32;

import java.util.Random;

/**
 * Tests for trackers which track using texture inside a single band image
 *
 * @author Peter Abeles
 */
public abstract class TextureGrayTrackerObjectRectangleTests extends GenericTrackerObjectRectangleTests<GrayU8> {

	public TextureGrayTrackerObjectRectangleTests() {
		super(new ImageType<GrayU8>(ImageType.Family.GRAY, ImageDataType.U8,1));

		input = new GrayU8(width,height);
	}

	@Override
	protected void render( double scale , double tranX , double tranY ) {
		Random rand = new Random(234);

		for( int i = 0; i < 500; i++ ) {

			int x = (int)(scale*rand.nextInt(width-10)) + (int)tranX;
			int y = (int)(scale*rand.nextInt(height-10)) + (int)tranY;
			int w = (int)(scale*rand.nextInt(100)+20);
			int h = (int)(scale*rand.nextInt(100)+20);

			Polygon2D_I32 p = new Polygon2D_I32(4);
			p.vertexes.data[0].set(x,y);
			p.vertexes.data[1].set(x+w,y);
			p.vertexes.data[2].set(x+w,y+h);
			p.vertexes.data[3].set(x,y+h);

			convexFill(p, input,rand.nextInt(255));
		}
	}

	public static void convexFill(Polygon2D_I32 poly , GrayU8 image , double value ) {
		int minX = Integer.MAX_VALUE; int maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE; int maxY = Integer.MIN_VALUE;

		for( int i = 0; i < poly.size(); i++ ) {
			Point2D_I32 p = poly.vertexes.data[i];
			if( p.y < minY ) {
				minY = p.y;
			} else if( p.y > maxY ) {
				maxY = p.y;
			}
			if( p.x < minX ) {
				minX = p.x;
			} else if( p.x > maxX ) {
				maxX = p.x;
			}
		}
		ImageRectangle bounds = new ImageRectangle(minX,minY,maxX,maxY);
		BoofMiscOps.boundRectangleInside(image, bounds);

		Point2D_F64 p = new Point2D_F64();
		Polygon2D_F64 poly64 = new Polygon2D_F64(4);
		for( int i = 0; i < 4; i++ )
			poly64.vertexes.data[i].set( poly.vertexes.data[i].x , poly.vertexes.data[i].y );

		for( int y = bounds.y0; y < bounds.y1; y++ ) {
			p.y = y;
			for( int x = bounds.x0; x < bounds.x1; x++ ) {
				p.x = x;

				if( Intersection2D_F64.containConvex(poly64, p)) {
					GeneralizedImageOps.set(image, x, y, value);
				}
			}
		}
	}
}
