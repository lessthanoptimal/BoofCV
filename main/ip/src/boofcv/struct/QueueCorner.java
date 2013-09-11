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

package boofcv.struct;

import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.FastQueue;


/**
 * A list that allows fast access to a queue of points that represents corners in an image.
 * All the points are predeclared and recycled.
 *
 * @author Peter Abeles
 */
public class QueueCorner extends FastQueue<Point2D_I16> {

	public QueueCorner(int max) {
		super(max,Point2D_I16.class,true);
	}

	public QueueCorner() {
		super(10,Point2D_I16.class,true);
	}

	public final void add(int x, int y) {
		grow().set((short)x,(short)y);
	}

	public final void add( Point2D_I16 pt ) {
		grow().set(pt.x, pt.y);
	}
}
