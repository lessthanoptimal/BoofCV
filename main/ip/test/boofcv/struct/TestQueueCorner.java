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
import org.junit.Test;

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
