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

import gecv.numerics.fitting.modelset.DistanceFromModel;

import java.util.List;


/**
 * Computes the mean error and prunes points based on the number of standard deviations they are away.
 *
 * @author Peter Abeles
 */
public class FitByMeanStatistics<T> implements StatisticalFit<T> {

    private DistanceFromModel<T> modelError;
    private List<T> inliers;

    // the number of standard deviations away that points are pruned
    private double pruneThreshold;

    // the mean error
    private double meanError;
    // the standard deviation of the error
    private double stdError;

    /**
     *
     * @param pruneThreshold Number of standard deviations away that points will be pruned.
     */
    public FitByMeanStatistics( double pruneThreshold ) {
        this.pruneThreshold = pruneThreshold;
    }

    @Override
    public void init( DistanceFromModel<T> modelError , List<T> inliers ) {
        this.modelError = modelError;
        this.inliers = inliers;
    }

    @Override
    public void computeStatistics() {
        computeMean();
        computeStandardDeviation();
    }

    @Override
    public void prune() {
        double thresh = stdError* pruneThreshold;

        for( int j = inliers.size()-1; j >= 0; j-- ){
            T pt = inliers.get(j);

            // only prune points which are less accurate than the mean
            if( modelError.computeDistance(pt)-meanError > thresh ) {
                inliers.remove(j);
            }
        }
    }

    @Override
    public double getErrorMetric() {
        return meanError;
    }

    /**
     * Computes the mean and standard deviation of the points from the model
     */
    private void computeMean() {
        meanError = 0;

        int size = inliers.size();
        for( int i = 0; i < size; i++ ) {
            T pt = inliers.get(i);

            meanError += modelError.computeDistance(pt);
        }

        meanError /= size;

    }

    private void computeStandardDeviation() {
        stdError = 0;
        int size = inliers.size();
        for( int i = 0; i < size; i++ ) {
            T pt = inliers.get(i);

            double e = modelError.computeDistance(pt) - meanError;
            stdError += e*e;
        }

        stdError = Math.sqrt(stdError/size);
    }
}
