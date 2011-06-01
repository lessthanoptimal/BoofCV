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

import java.util.List;


/**
 * A variation on {@link SimpleScoreRansac} that uses a fit score for the set of points instead of
 * the number of inlier points.
 *
 * @author Peter Abeles
 */
public class SimpleScoreRansac<T> extends SimpleRansacCommon<T> {

    // computes how good of a fit the points are to the model
    RansacFitScore<T> fitScorer;

    // how many points are drawn to generate the initial model
    protected int numInitialSample;
    // the error threshold for the initial sample to be accepted
    private double initialModelThresh;

    // the minimum number of points in the match set that are needed for it to be
    // accepted
    private int minMatchSetSize;
    // threshold to include points in the match set
    private double matchSetThreshold;

    private double bestFitError;

    // if the best fit error is better than this value it will stop
    // running through the loop and exit
    private double exitIterThreshold;

    /**
     * Creates a new instance of the ransac algorithm.
     *
     * @param randSeed The random seed used by the random number generator.
     * @param modelFitter Computes the model parameters given a set of points.
     * @param modelDistance Computes the difference between a point an a model.
     * @param maxIterations The maximum number of iterations the RANSAC algorithm will perform.
     * @param fitScorer Computes the goodness of fit for the points against the model
     * @param numInitialSample How many points will it initially draw to create a model.
     * @param initialModelThresh The error threshold used to accept the initial model.
     * @param minMatchSetSize The minimum number of points that need to match the initial model for it to be accepted.
     * @param matchSetThreshold The threshold used to accept the model generated from the match set
     * @param exitIterThreshold If the match set is better than this threshold it will stop running.
     */
    public SimpleScoreRansac( long randSeed ,
                              ModelFitter<T> modelFitter ,
                              DistanceFromModel<T> modelDistance ,
                              RansacFitScore<T> fitScorer ,
                              int maxIterations ,
                              int numInitialSample , double initialModelThresh,
                              int minMatchSetSize , double matchSetThreshold,
                              double exitIterThreshold )
    {
        super(modelFitter,modelDistance,randSeed,maxIterations);

        this.fitScorer = fitScorer;
        this.numInitialSample = numInitialSample;
        this.initialModelThresh = initialModelThresh;
        this.minMatchSetSize = minMatchSetSize;
        this.matchSetThreshold = matchSetThreshold;
        this.exitIterThreshold = exitIterThreshold;
    }

    /**
     * Constructor primarily used for debugging
     */
    protected SimpleScoreRansac(){}

    /**
     * Returns the error for the best fit parameters.
     */
    @Override
    public double getError() {
        return bestFitError;
    }

    /**
     * Runs the RANSAC algortihm on the provided data set.
     *
     *
     * @param dataSet The list of points that are to be processed.  This can be modified.
     * @param paramInitial An initial value that can be used by the optimization algorithm.
     * If null it will use all zeros.
     *
     * @return True if it sucessed in finding a good model, false otherwise.
     */
    @Override
    public boolean process( List<T> dataSet , double[] paramInitial ) {
        if( paramInitial == null ) {
            paramInitial = new double[ bestFitParam.length ];
        } else if( paramInitial.length != bestFitParam.length ) {
            throw new IllegalArgumentException("Parameter has an unexpected length");
        }

        bestFitError = Double.MAX_VALUE;
        bestFitPoints.clear();

        // see if it has the minimum number of points
        if( dataSet.size() < minMatchSetSize )
            return false;

        int i;
        for( i = 0; i < maxIterations; i++ ) {
            randomDraw(dataSet,numInitialSample,initialSample,rand);

            if( !modelFitter.fitModel(initialSample,paramInitial) )
                continue;

//            System.out.println("init error = "+optimizer.getFitError());

            // see if the fit to the initial set is good enough
            if( fitScorer.computeFitScore(initialSample,paramInitial,modelDistance) < initialModelThresh ) {

                // select points which match that fit
                if( !selectMatchSet(dataSet,matchSetThreshold,minMatchSetSize,paramInitial)) {
                    continue;
                }

                if( !modelFitter.fitModel(candidatePoints,paramInitial) )
                    continue;

                // see if the results are better than the previous results
                double score = fitScorer.computeFitScore(initialSample,paramInitial,modelDistance);
                if( score < bestFitError ) {
                    bestFitError = score;
                    System.arraycopy(paramInitial,0,bestFitParam,0,bestFitParam.length);
                    bestFitPoints.clear();
                    bestFitPoints.addAll(candidatePoints);

                    if( bestFitError < exitIterThreshold ) {
                        break;
                    }
                }
            }
        }

//        System.out.println("RANSAC num iterations "+i);

        return bestFitError != Double.MAX_VALUE;
    }
}
