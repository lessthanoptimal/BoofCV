/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.aztec;

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.drawing.FiducialRenderEngine;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.Polygon2D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;

/**
 * Generates an image of an Aztec Code marker as specified in ISO/IEC 24778:2008(E)
 *
 * @author Peter Abeles
 */
public class AztecGenerator {
	/** How wide the square which encloses the marker is */
	public @Getter double markerWidth = 1.0;

	// Derived constants
	protected int lengthInSquares; // length of a side on the marker in units of squares
	protected double squareWidth; // length of a square in document units
	protected int orientationSquareCount; // squares in orientation region

	/** Used to render the marker */
	@Getter protected FiducialRenderEngine render;

	/** Convenience function for rendering images */
	public static GrayU8 renderImage( int pixelPerSquare, int border, AztecCode marker ) {
		int numSquares = marker.getMarkerSquareCount();
		var render = new FiducialImageEngine();
		render.configure(pixelPerSquare*border, numSquares*pixelPerSquare);
		new AztecGenerator().setMarkerWidth(numSquares*pixelPerSquare).setRender(render).render(marker);
		return render.getGray();
	}

	public AztecGenerator render( AztecCode marker ) {
		lengthInSquares = marker.getMarkerSquareCount();
		squareWidth = markerWidth/lengthInSquares;

		render.init();

		// Render the orientation and locator patterns
		orientationSquareCount = marker.getLocatorSquareCount() + 4;
		//noinspection IntegerDivisionInFloatingPointContext
		double orientationLoc = ((lengthInSquares - orientationSquareCount)/2)*squareWidth;
		orientationPattern(orientationLoc, orientationLoc, orientationSquareCount);
		locatorPattern(orientationLoc + 2*squareWidth, orientationLoc + 2*squareWidth,
				marker.getLocatorRingCount(), marker.locatorRings);

		// Render the reference grid
		if (marker.structure == AztecCode.Structure.FULL && marker.dataLayers > 4) {
			int center = lengthInSquares/2;
			int odd = ((lengthInSquares - orientationSquareCount)/2)%2;
			for (int location = 0; center + location < lengthInSquares; location += 16) {
				referenceGridLine(odd, center - location, 1, 0, lengthInSquares);
				referenceGridLine(odd, center + location, 1, 0, lengthInSquares);

				referenceGridLine(center - location, odd, 0, 1, lengthInSquares);
				referenceGridLine(center + location, odd, 0, 1, lengthInSquares);
			}
		}

		// TODO Render the mode message

		// TODO Render data layers

		return this;
	}

	/**
	 * Renders the locator pattern given it's top-left corner of the outermost ring. This does NOT include
	 * the orientation pattern.
	 */
	protected void locatorPattern( double tl_x, double tl_y, int ringCount, DogArray<Polygon2D_F64> ringLocations ) {
		ringLocations.resetResize(ringCount);
		for (int ring = ringCount; ring > 0; ring--) {
			// number of squares wide the ring is
			int modules = (ring - 1)*4 + 1;
			// Number of document units wide the ring is
			double width = squareWidth*modules;

			// Draw the ring
			render.square(tl_x, tl_y, width, squareWidth);

			// Save the ring's outer contour
			Polygon2D_F64 where = ringLocations.get(ring - 1);
			where.get(0).setTo(tl_x, tl_y);
			where.get(1).setTo(tl_x + width, tl_y);
			where.get(2).setTo(tl_x + width, tl_y + width);
			where.get(3).setTo(tl_x, tl_y + width);

			tl_x += 2*squareWidth;
			tl_y += 2*squareWidth;
		}
	}

	protected void orientationPattern( double tl_x, double tl_y, int moduleCount ) {
		// shorthand
		final double sw = squareWidth;
		final double rw = moduleCount*sw;

		// render the big square ring
		render.square(tl_x, tl_y, rw, sw);

		// top-left
		render.square(tl_x - sw, tl_y, sw);
		render.rectangleWH(tl_x - sw, tl_y - sw, 2*sw, sw);

		// top-right
		render.rectangleWH(tl_x + rw, tl_y - sw, sw, 2*sw);

		// bottom-right
		render.square(tl_x + rw, tl_y + rw - sw, sw);
	}

	protected void referenceGridLine( int tl_x, int tl_y, int dx, int dy, int count ) {
		int forbidden0 = (lengthInSquares - orientationSquareCount)/2 - 1;
		int forbidden1 = forbidden0 + orientationSquareCount + 2;

		for (int i = 0; i < count; i += 2) {
			int squareX = tl_x + i*dx;
			int squareY = tl_y + i*dy;

			// Don't draw inside the locator and orientation patterns
			if (squareX >= forbidden0 && squareX < forbidden1 && squareY >= forbidden0 && squareY < forbidden1)
				continue;

			double x = squareX*squareWidth;
			double y = squareY*squareWidth;

			render.square(x, y, squareWidth, squareWidth);
		}
	}

	public AztecGenerator setMarkerWidth( double width ) {
		this.markerWidth = width;
		return this;
	}

	public AztecGenerator setRender( FiducialRenderEngine render ) {
		this.render = render;
		return this;
	}
}

