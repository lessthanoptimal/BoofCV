package boofcv.alg.sfm.robust;

import boofcv.struct.geo.GeoModelRefine;
import org.ddogleg.fitting.modelset.ModelFitter;

import java.util.List;

/**
 * Wrapper around {@link GeoModelRefine} for {@link ModelFitter}
 *
 * @author Peter Abeles
 */
public abstract class GeoModelRefineToModelFitter<Model,Point> implements ModelFitter<Model,Point> {

	GeoModelRefine<Model,Point> alg;

	protected GeoModelRefineToModelFitter(GeoModelRefine<Model, Point> alg) {
		this.alg = alg;
	}

	@Override
	public boolean fitModel(List<Point> dataSet, Model initial, Model found) {
		return alg.process(initial,dataSet,found);
	}
}
