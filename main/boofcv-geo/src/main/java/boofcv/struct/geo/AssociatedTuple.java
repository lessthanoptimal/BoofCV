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

package boofcv.struct.geo;

import georegression.struct.point.Point2D_F64;

/**
 * Interface for arbitrary number of matched 2D features
 *
 * @author Peter Abeles
 */
public interface AssociatedTuple {
	double getX( int index );

	double getY( int index );

	Point2D_F64 get( int index );

	void set( int index, double x, double y );

	void set( int index, Point2D_F64 src );

	int size();

	void setTo( AssociatedTuple src );
}
