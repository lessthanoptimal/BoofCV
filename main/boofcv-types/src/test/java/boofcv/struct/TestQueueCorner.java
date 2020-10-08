/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I16;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;


/**
 * @author Peter Abeles
 */
class TestQueueCorner extends BoofStandardJUnit {

    /**
     * Tests add() and get()
     */
    @Test
    void add_get() {
        QueueCorner queue = new QueueCorner(20);

        assertEquals(0,queue.size());
        queue.append(1,2);
        assertEquals(1,queue.size());
        Point2D_I16 pt = queue.get(0);
        assertEquals(1,pt.getX());
        assertEquals(2,pt.getY());
    }

    @Test
    void getMaxSize() {
        QueueCorner queue = new QueueCorner(20);

        assertEquals(0,queue.size());
        assertEquals(20,queue.getMaxSize());
    }

    @Test
    void appendAll() {
        QueueCorner queue = new QueueCorner(20);
        QueueCorner copy = new QueueCorner(1);

        copy.appendAll(queue);
        assertEquals(0,copy.size);

        queue.append(1,1);
        queue.append(2,3);
        copy.appendAll(queue);
        assertEquals(2,copy.size);
        assertEquals(queue.get(0), copy.get(0));
        assertEquals(queue.get(1), copy.get(1));
    }

    @Test
    void reset() {
        QueueCorner queue = new QueueCorner(20);

        assertEquals(0,queue.size());
        queue.append(1,2);
        Point2D_I16 p = queue.get(0);
        assertEquals(1,queue.size());
        queue.reset();
        assertEquals(0,queue.size());
        queue.append(1,2);
        assertSame(p, queue.get(0));
    }
}
