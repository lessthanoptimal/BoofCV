package boofcv.alg.sfm.robust;

import boofcv.numerics.fitting.modelset.ModelGenerator;
import boofcv.struct.geo.GeoModelEstimator1;

import java.util.List;

/**
 * Wrapper class for converting {@link GeoModelEstimator1} into {@link ModelGenerator}.
 *
 * @author Peter Abeles
 */
public abstract class EstimatorToGenerator<Model,Point> implements ModelGenerator<Model,Point> {

	GeoModelEstimator1<Model,Point> alg;

	public EstimatorToGenerator(GeoModelEstimator1<Model, Point> alg ) {
		this.alg = alg;
	}

	@Override
	public boolean generate(List<Point> dataSet, Model out) {
		return alg.process(dataSet,out);
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinimumPoints();
	}
}
