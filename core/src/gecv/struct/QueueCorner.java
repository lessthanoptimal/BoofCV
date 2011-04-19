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

}
