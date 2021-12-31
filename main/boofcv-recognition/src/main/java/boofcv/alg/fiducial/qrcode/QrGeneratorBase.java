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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.drawing.FiducialRenderEngine;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Base class for rendering QR and Micro QR
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class QrGeneratorBase {
	/** How wide the square which encloses the marker is */
	public @Setter @Getter double markerWidth = 1.0;

	// used to toggle rendering of data
	protected boolean renderData = true;

	// derived constants
	protected double moduleWidth;
	protected int numModules;

	// data mask
	protected List<Point2D_I32> bitLocations;

	/** Used to render the marker */
	@Setter protected FiducialRenderEngine render;

	public QrGeneratorBase( double markerWidth ) {
		this.markerWidth = markerWidth;
	}

	public QrGeneratorBase() {}

	protected void positionPattern( double x, double y, Polygon2D_F64 where ) {
		// draw the outside square
		render.square(x, y, moduleWidth*7, moduleWidth);

		// draw the inside square
		render.square(x + moduleWidth*2, y + moduleWidth*2, moduleWidth*3);

		where.get(0).setTo(x, y);
		where.get(1).setTo(x + moduleWidth*7, y);
		where.get(2).setTo(x + moduleWidth*7, y + moduleWidth*7);
		where.get(3).setTo(x, y + moduleWidth*7);
	}

	protected void timingPattern( double x, double y, double slopeX, double slopeY, int length ) {
		for (int i = 1; i < length; i += 2) {
			render.square(x + i*slopeX, y + i*slopeY, moduleWidth);
		}
	}

	protected void square( int row, int col ) {
		render.square(col*moduleWidth, row*moduleWidth, moduleWidth);
	}
}
