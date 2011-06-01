package gecv.numerics.fitting.modelset.distance;

import gecv.numerics.fitting.modelset.DistanceFromModel;
import gecv.numerics.fitting.modelset.GenericModelSetTests;
import gecv.numerics.fitting.modelset.ModelFitter;
import gecv.numerics.fitting.modelset.ModelMatcher;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestStatisticalDistanceModelMatcher extends GenericModelSetTests {
    @Test
    public void standardTests() {
        configure(0.9,0.1, false);
        performSimpleModelFit();
        runMultipleTimes();
    }

    @Override
    public ModelMatcher<Double> createModelMatcher(DistanceFromModel<Double> distance,
                                                   ModelFitter<Double> fitter,
                                                   int minPoints, double fitThreshold) {
        return new StatisticalDistanceModelMatcher<Double>(5,0,0,10000,minPoints,
                StatisticalDistance.PERCENTILE,
                0.95,fitter,distance);
    }
}
