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

package boofcv.alg.feature.detect.line;

import boofcv.struct.image.GrayF32;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;

/**
 * Parameterizes a line to a coordinate for the Hough transform. Many of these functions need to be thread safe
 *
 * @author Peter Abeles
 */
public interface HoughTransformParameters {
	void initialize( int width, int height, GrayF32 transform );

	boolean isTransformValid( int x, int y );

	void lineToCoordinate( LineParametric2D_F32 line, Point2D_F64 coordinate );

	void transformToLine( float x, float y, LineParametric2D_F32 line );

	void parameterize( int x, int y, GrayF32 transform );

	void parameterize( int x, int y, float derivX, float derivY, Point2D_F32 parameter );
}
