/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.struct;

import pja.geometry.struct.point.Point2D_I16;


/**
 * A list that allows fast access to a queue of points that represents corners in an image.
 * All the points are predeclared and recycled.
 *
 * @author Peter Abeles
 */
public class QueueCorner {

	public Point2D_I16[] points;
	public int num;


	public QueueCorner(int max) {
		points = new Point2D_I16[max];
		num = 0;

		for (int i = 0; i < points.length; i++) {
			points[i] = new Point2D_I16();
		}
	}

	public void reset() {
		num = 0;
	}

	public final void add(int x, int y) {
		if (num >= points.length) {
			resize(num * 2);
		}

		points[num++].set((short) x, (short) y);
	}

	public Point2D_I16 get(int index) {
		if (index >= num) throw new IllegalArgumentException("Out of range.");

		return points[index];
	}

	public int size() {
		return num;
	}

	public int getMaxSize() {
		return points.length;
	}

	/**
	 * Increases the size of the list while maintaining the values of existing elements
	 */
	public void resize(int newSize) {
		Point2D_I16[] adj = new Point2D_I16[newSize];

		int m = Math.min(newSize, points.length);

		System.arraycopy(points, 0, adj, 0, points.length);
		for (int i = m; i < newSize; i++) {
			adj[i] = new Point2D_I16();
		}

		points = adj;
		this.num = m;
	}

	public boolean isFull() {
		return points.length == num;
	}
}
