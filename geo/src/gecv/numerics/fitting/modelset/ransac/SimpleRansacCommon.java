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

package gecv.numerics.fitting.modelset.ransac;

import gecv.numerics.fitting.modelset.DistanceFromModel;
import gecv.numerics.fitting.modelset.ModelFitter;
import gecv.numerics.fitting.modelset.ModelMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


/**
 * <p>
 * This is a base class for standard implementations of RANSAC.  The following is a description of
 * RANSAC is from wikipedia:<br>
 * <br>
 * "RANSAC is an abbreviation for "RANdom SAmple Consensus". It is an iterative method to estimate
 * parameters of a mathematical model from a set of observed data which contains outliers. It is a
 * non-deterministic algorithm in the sense that it produces a reasonable result only with a certain
 * probability, with this probability increasing as more iterations are allowed. The algorithm was
 * first published by Fischler and Bolles in 1981."
 * </p>
 * <p>
 * Functions are provided by this class for efficiently selecting a random set of points and selecting
 * the match set after a model has been determined.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class SimpleRansacCommon<T> implements ModelMatcher<T> {
    // fits a model to the set of points its provided
    protected ModelFitter<T> modelFitter;
    // computes the distance a point is from the model
    protected DistanceFromModel<T> modelDistance;

    // used to randomly select points/samples
    protected Random rand;

        // list of points which are a candidate for the best fit set
    protected List<T> candidatePoints = new ArrayList<T>();

    // list of samples from the best fit model
    protected List<T> bestFitPoints = new ArrayList<T>();
    protected double bestFitParam[];

    // the maximum number of iterations it will perform
    protected int maxIterations;

    // the set of points which were initially sampled
    protected List<T> initialSample = new ArrayList<T>();

    public SimpleRansacCommon( ModelFitter<T> modelFitter ,
                               DistanceFromModel<T> modelDistance ,
                               long randSeed , int maxIterations  ) {
        this.modelFitter = modelFitter;
        this.modelDistance = modelDistance;

        rand = new Random(randSeed);
        this.maxIterations = maxIterations;
        bestFitParam = new double[ modelFitter.getParameterLength() ];
    }

    public SimpleRansacCommon() {
    }

    /**
     * Returns the set of points which matched this model.
     */
    public List<T> getMatchSet() {
        return bestFitPoints;
    }

    /**
     * Returns the set of parameters that it found.
     */
    public double[] getParameters() {
        return bestFitParam;
    }

        /**
     * Two different methods are used to select the initial set of points depending on
     * if the data set is much larger or smaller than the initial sample size
     */
    public static <T> void randomDraw( List<T> dataSet , int numSample ,
                                       List<T> initialSample , Random rand ) {
        initialSample.clear();

        if( dataSet.size() > numSample*10 ) {
            // randomly select points until a set is found
            while( initialSample.size() < numSample ) {
                T s = dataSet.get( rand.nextInt(dataSet.size()));

                if( !initialSample.contains(s) ) {
                    initialSample.add(s);
                }
            }
        } else {
            // shuffle the dataset
            Collections.shuffle(dataSet,rand);

            for( int i = 0; i < numSample; i++ ) {
                initialSample.add( dataSet.get(i) );
            }
        }
    }

    /**
     * Looks for points in the data set which closely match the current best
     * fit model in the optimizer.
     *
     * @param dataSet The points being considered
     * @return true if enough points were matched, false otherwise
     */
    @SuppressWarnings({"ForLoopReplaceableByForEach"})
    protected boolean selectMatchSet( List<T> dataSet , double threshold , int minSize ,
                                      double []param ) {
        candidatePoints.clear();
        modelDistance.setParameters(param);
        for( int i = 0; i < dataSet.size(); i++ ) {
            T point = dataSet.get(i);

            double distance = modelDistance.computeDistance(point);
            if( distance < threshold ) {
                candidatePoints.add(point);
            }
        }

        return candidatePoints.size() >= minSize;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }
}
