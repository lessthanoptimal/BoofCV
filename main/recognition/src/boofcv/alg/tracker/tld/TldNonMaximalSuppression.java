package boofcv.alg.tracker.tld;

import boofcv.struct.FastQueue;
import boofcv.struct.GrowQueue_I32;
import boofcv.struct.ImageRectangle;

/**
 * Performs non-maximum suppression on high confidence detected regions.  A graph of connected regions is constructed.
 * Two regions are considered connected if their overlap is above a threshold.  A region is considered a local maximum
 * if it has a score higher than all its neighbors.  A weighted average is computed using all regions connected to
 * the local maximum.
 *
 * NOTE: This is a completely different non-maximum algorithm from what was described in the paper.  The algorithm
 * described in the paper only approximates non-maximum suppression.
 *
 * @author Peter Abeles
 */
public class TldNonMaximalSuppression {

	// cut off for connecting two nodes
	private double connectionThreshold;

	// connection graph
	private FastQueue<Connections> conn = new FastQueue<Connections>(Connections.class,true);

	// storage for intermediate results
	private ImageRectangle work = new ImageRectangle();

	/**
	 * Configures non-maximum suppression
	 *
	 * @param connectionThreshold Two regions are considered connected of their overlap is >= to this value.
	 *                               0 to 1.0. A value of 0.5 is recommended
	 */
	public TldNonMaximalSuppression(double connectionThreshold) {
		this.connectionThreshold = connectionThreshold;
	}

	/**
	 * Finds local maximums from the set of provided regions
	 *
	 * @param regions Set of high confidence regions for target
	 * @param output Output after non-maximum suppression
	 */
	public void process( FastQueue<TldRegion> regions , FastQueue<TldRegion> output ) {

		final int N = regions.size;

		conn.growArray(N);
		for( int i = 0; i < N; i++ ) {
			conn.data[i].reset();
		}

		// Create the graph of connected regions and mark which regions are local maximums
		for( int i = 0; i < N; i++ ) {
			TldRegion ra = regions.get(i);
			Connections ca = conn.data[i];

			// it is connected to itself
//			ca.indexes.add(i);

			for( int j = i+1; j < N; j++ ) {
				TldRegion rb = regions.get(j);
				Connections cb = conn.data[j];

				// see if they are connected
				double overlap = computeOverlap(ra.rect,rb.rect);
				if( overlap < connectionThreshold ) {
					continue;
				}

				// connect the two and check for strict maximums
				ca.maximum &= ra.confidence > rb.confidence;
				cb.maximum &= rb.confidence > ra.confidence;
				ra.connections++;
				rb.connections++;
			}
		}

		// Compute the output from local maximums.
		for( int i = 0; i < N; i++ ) {
			TldRegion ra = regions.get(i);
			Connections ca = conn.data[i];

			if( ca.maximum ) {
				TldRegion o = output.grow();
				o.connections = ra.connections;
				o.confidence = ra.confidence;
				o.rect.set(ra.rect);
//				computeAverage(regions,ca.indexes,o.rect);       // TODO this is changed
			} else if( ra.connections == 0 ) {
				System.out.println("Not a maximum but has zero connections?");
			}
		}
	}

	/**
	 * Computes the fractional area of intersection between the two regions.
	 *
	 * @return number from 0 to 1.  higher means more intersection
	 */
	public double computeOverlap( ImageRectangle a , ImageRectangle b ) {
		if( !a.intersection(b,work) )
			return 0;

		int areaI = work.area();

		int bottom = a.area() + b.area() - areaI;

		return areaI/ (double)bottom;
	}

	/**
	 * Weighted average of connected rectangles.
	 * @param regions List of all regions
	 * @param connections List of connected regions being considered
	 * @param output The average output
	 */
	public static void computeAverage( FastQueue<TldRegion> regions , GrowQueue_I32 connections , ImageRectangle output ) {

		double centerX=0,centerY=0;
		double width=0,height=0;

		double total = 0;

		for( int i = 0; i < connections.size; i++ ) {
			TldRegion r = regions.get( connections.get(i) );
			ImageRectangle a = r.rect;

			total += r.confidence;

			centerX += r.confidence *(a.x0+a.x1)/2.0;
			centerY += r.confidence *(a.y0+a.y1)/2.0;

			width += r.confidence *a.getWidth();
			height += r.confidence *a.getHeight();
		}

		centerX /= total;
		centerY /= total;
		width /= total;
		height /= total;

		output.x0 = (int)(centerX - width/2.0 + 0.5);
		output.y0 = (int)(centerY - height/2.0 + 0.5);
		output.x1 = output.x0 + (int)(width+0.5);
		output.y1 = output.y0 + (int)(height+0.5);
	}

	public FastQueue<Connections> getConnections() {
		return conn;
	}

	public static class Connections
	{
		boolean maximum;

		public void reset() {
			maximum = true;
		}
	}
}
