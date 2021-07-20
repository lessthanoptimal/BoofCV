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

package boofcv.alg.drawing;

import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;

/**
 * Interface for rendering fiducials to different document types. All units are in
 * document specific units.
 *
 * @author Peter Abeles
 */
public abstract class FiducialRenderEngine {
	/**
	 * Init needs to be called for each new fiducial.
	 */
	public abstract void init();

	/**
	 * Sets the gray scale value of the shape. 0.0 = black. 1.0 = white.
	 */
	public abstract void setGray( double value );

	public void square(double x0 , double y0 , double width ) {
		rectangle(x0,y0,x0+width,y0+width);
	}

	public abstract void circle(double cx, double cy , double radius );

	public abstract void square(double x0, double y0, double width0, double thickness);

	public abstract void rectangle( double x0 , double y0 , double x1 , double y1 );

	public abstract void draw(GrayU8 image , double x0 , double y0 , double x1 , double y1);

	public abstract void inputToDocument(double x , double y , Point2D_F64 document );

	// TODO add polyline with line thickness? How to manually do this?
}
