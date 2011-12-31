/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.calibgrid;

import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestExtractOrderedTargetPoints {

	@Test
	public void process_positive() {
		fail("implement");
	}

	@Test
	public void process_negative() {
		fail("implement");
	}

	@Test
	public void selectFarthest() {
		fail("implement");
	}

	@Test
	public void orderCorners() {
		fail("implement");
	}

	@Test
	public void orderedBlobsIntoPoints() {
		fail("implement");
	}


	public static SquareBlob createBlob( int x0 , int y0 , int r )
	{
		return createBlob(x0-r,y0+r,x0+r,y0+r,x0+r,y0-r,x0-r,y0-r);
	}

	public static SquareBlob createBlob( int x0 , int y0 , int x1 , int y1 ,
										  int x2 , int y2 , int x3 , int y3 )
	{
		List<Point2D_I32> corners = new ArrayList<Point2D_I32>();
		corners.add( new Point2D_I32(x0,y0));
		corners.add( new Point2D_I32(x1,y1));
		corners.add( new Point2D_I32(x2,y2));
		corners.add( new Point2D_I32(x3,y3));

		return new SquareBlob(corners,corners);
	}
}
