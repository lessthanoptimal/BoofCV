package boofcv.alg.feature.line;

import boofcv.alg.feature.detect.line.ConnectLinesGrid;
import boofcv.struct.feature.MatrixOfList;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestConnectLinesGrid {

	/**
	 * Very basic check which sees if lines are being connected in the same region
	 */
	@Test
	public void connectInSameRegion() {
		MatrixOfList<LineSegment2D_F32> grid = new MatrixOfList<LineSegment2D_F32>(1,1);

		grid.get(0,0).add(new LineSegment2D_F32(0,0,2,3));
		grid.get(0,0).add(new LineSegment2D_F32(2,3,4,6));

		ConnectLinesGrid app = new ConnectLinesGrid(0.1,1,1);
		app.process(grid);

		List<LineSegment2D_F32> list = grid.createSingleList();

		assertEquals(1,list.size());
		LineSegment2D_F32 l = list.get(0);
		assertEquals(0,l.a.x,1e-8);
		assertEquals(0,l.a.y,1e-8);
		assertEquals(4,l.b.x,1e-8);
		assertEquals(6,l.b.y,1e-8);
	}

	/**
	 * Very basic check to see if lines are connected between regions.
	 */
	@Test
	public void connectToNeighborRegion() {
		// check all the neighbors around 1,1 and see if they get connected
		checkConnectNeighbor(0,0);
		checkConnectNeighbor(1,0);
		checkConnectNeighbor(2,0);
		checkConnectNeighbor(2,1);
		checkConnectNeighbor(2,2);
		checkConnectNeighbor(1,2);
		checkConnectNeighbor(0,2);
		checkConnectNeighbor(0,1);
	}

	@Test
	public void checkAngleTolerance() {
		MatrixOfList<LineSegment2D_F32> grid = new MatrixOfList<LineSegment2D_F32>(1,1);

		grid.get(0,0).add(new LineSegment2D_F32(0,0,2,0));
		grid.get(0,0).add(new LineSegment2D_F32(2,0,4,4));

		// have the tolerance be too tight
		ConnectLinesGrid app = new ConnectLinesGrid(0.1,1,1);
		app.process(grid);
		assertEquals(2,grid.createSingleList().size());

		// now make the tolerance broader
		app = new ConnectLinesGrid(Math.PI,1,1);
		app.process(grid);
		assertEquals(1,grid.createSingleList().size());

	}

	@Test
	public void checkTangentTolerance() {
		MatrixOfList<LineSegment2D_F32> grid = new MatrixOfList<LineSegment2D_F32>(1,1);

		grid.get(0,0).add(new LineSegment2D_F32(0,0,2,0));
		grid.get(0,0).add(new LineSegment2D_F32(2,1,4,1));

		// have the tolerance be too tight
		ConnectLinesGrid app = new ConnectLinesGrid(2,0.1,2);
		app.process(grid);
		assertEquals(2,grid.createSingleList().size());

		// now make the tolerance broader
		app = new ConnectLinesGrid(2,1.1,2);
		app.process(grid);
		assertEquals(1,grid.createSingleList().size());
	}

	@Test
	public void checkParallelTolerance() {
		MatrixOfList<LineSegment2D_F32> grid = new MatrixOfList<LineSegment2D_F32>(1,1);

		grid.get(0,0).add(new LineSegment2D_F32(0,0,2,0));
		grid.get(0,0).add(new LineSegment2D_F32(3,0,5,0));

		// have the tolerance be too tight
		ConnectLinesGrid app = new ConnectLinesGrid(2,2,0.1);
		app.process(grid);
		assertEquals(2,grid.createSingleList().size());

		// now make the tolerance broader
		app = new ConnectLinesGrid(2,2,1.1);
		app.process(grid);
		assertEquals(1,grid.createSingleList().size());
	}

	private void checkConnectNeighbor( int x , int y ) {
		MatrixOfList<LineSegment2D_F32> grid = new MatrixOfList<LineSegment2D_F32>(3,3);

		grid.get(1,1).add(new LineSegment2D_F32(0,0,2,3));
		grid.get(x,y).add(new LineSegment2D_F32(2,3,4,6));

		ConnectLinesGrid app = new ConnectLinesGrid(0.1,1,1);
		app.process(grid);

		List<LineSegment2D_F32> list = grid.createSingleList();

		assertEquals(1,list.size());
		LineSegment2D_F32 l = list.get(0);
		if( l.a.x == 4 ) {
			Point2D_F32 temp = l.a;
			l.a = l.b;
			l.b = temp;
		}
		assertEquals(0,l.a.x,1e-8);
		assertEquals(0,l.a.y,1e-8);
		assertEquals(4,l.b.x,1e-8);
		assertEquals(6,l.b.y,1e-8);
	}
}
