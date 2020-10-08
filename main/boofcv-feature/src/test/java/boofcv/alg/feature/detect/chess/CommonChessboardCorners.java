/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import boofcv.alg.distort.impl.DistortSupport;
import boofcv.struct.distort.PixelTransform;
import boofcv.testing.BoofStandardJUnit;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F32;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
abstract class CommonChessboardCorners extends BoofStandardJUnit {
	protected int p = 40;
	protected int w = 20;
	protected int rows = 4;
	protected int cols = 5;
	protected double angle;

	@BeforeEach
	void setup() {
		p = 40;
		w = 20;
		angle = 0;
	}

	List<ChessboardCorner> createExpected(int rows , int cols , int width , int height ) {
		List<ChessboardCorner> list = new ArrayList<>();

		PixelTransform<Point2D_F32> inputToOutput = DistortSupport.transformRotate(width/2,height/2,
				width/2,height/2,(float)-angle);

		Point2D_F32 tmp = new Point2D_F32();

		for (int row = 1; row < rows; row++) {
			for( int col = 1; col < cols; col++ ) {
				ChessboardCorner c = new ChessboardCorner();
				inputToOutput.compute(p+col*w,p+row*w,tmp);
				c.x = tmp.x;
				c.y = tmp.y;
				c.orientation = ((row%2)+(col%2))%2 == 0 ? Math.PI/4 : -Math.PI/4;
				// take in account the image being rotated
				c.orientation = UtilAngle.boundHalf(c.orientation+angle);

				list.add(c);
			}
		}

		return list;
	}
}
