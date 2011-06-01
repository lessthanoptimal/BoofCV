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

package gecv.numerics.fitting.modelset.distance;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestFitByMedianStatistics {
    @Test
    public void metric_and_prune() {
        List<Double> inliers = new ArrayList<Double>();

        for( int i = 0; i < 200; i++ ) {
            inliers.add((double)i);
        }

        // randomize the inputs
        Collections.sort(inliers);

        FitByMedianStatistics<Double> fit = new FitByMedianStatistics<Double>(0.90);

        fit.init(new DistanceFromMeanModel(),inliers);

        fit.computeStatistics();

        assertEquals(100,fit.getErrorMetric(),1e-8);

        fit.prune();

        assertEquals(180,inliers.size());
    }

}
