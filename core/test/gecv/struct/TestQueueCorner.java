package gecv.struct;

import org.junit.Test;
import pja.geometry.struct.point.Point2D_I16;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestQueueCorner {

    /**
     * Tests add() and get()
     */
    @Test
    public void add_get() {
        QueueCorner queue = new QueueCorner(20);

        assertEquals(0,queue.size());
        queue.add(1,2);
        assertEquals(1,queue.size());
        Point2D_I16 pt = queue.get(0);
        assertEquals(1,pt.getX());
        assertEquals(2,pt.getY());
    }

    @Test
    public void getMaxSize() {
        QueueCorner queue = new QueueCorner(20);

        assertEquals(0,queue.size());
        assertEquals(20,queue.getMaxSize());
    }


    @Test
    public void reset() {
        QueueCorner queue = new QueueCorner(20);

        assertEquals(0,queue.size());
        queue.add(1,2);
        Point2D_I16 p = queue.get(0);
        assertEquals(1,queue.size());
        queue.reset();
        assertEquals(0,queue.size());
        queue.add(1,2);
        assertTrue(p==queue.get(0));
    }
}
