package boofcv.struct.feature;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public class TestMatrixOfList {
	@Test
	public void reshape() {
		MatrixOfList a = new MatrixOfList(1,1);
		a.reshape(5,3);
		assertEquals(5,a.width);
		assertEquals(3,a.height);
		assertTrue(a.grid.length>=5*3);
		for(List l : a.grid )
			assertTrue(l!=null);
	}

	@Test
	public void reset() {
		MatrixOfList a = new MatrixOfList(2,3);
		for(List l : a.grid )
			l.add(new Integer(1));
		a.reset();
		for(List l : a.grid )
			assertEquals(0,l.size());
	}

	@Test
	public void getWidth_getHeight() {
		MatrixOfList a = new MatrixOfList(2,3);
		assertEquals(2,a.getWidth());
		assertEquals(3,a.getHeight());
	}

	@Test
	public void createSingleList() {
		MatrixOfList a = new MatrixOfList(2,3);
		a.reshape(3,4);
		for(List l : a.grid )
			l.add(new Integer(1));
		List<List> b = a.createSingleList();

		assertEquals(12,b.size());
	}

	@Test
	public void isInBounds() {
		MatrixOfList a = new MatrixOfList(2,3);
		assertFalse(a.isInBounds(-1,0));
		assertFalse(a.isInBounds(0,-1));
		assertFalse(a.isInBounds(2,0));
		assertFalse(a.isInBounds(0,3));

		assertTrue(a.isInBounds(0,0));
		assertTrue(a.isInBounds(1,2));
	}
}
