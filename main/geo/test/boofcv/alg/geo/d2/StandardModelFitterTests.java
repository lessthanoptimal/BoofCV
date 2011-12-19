package boofcv.alg.geo.d2;


import boofcv.numerics.fitting.modelset.ModelFitter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Basic tests for model fitters
 *
 * @author Peter Abeles
 */
public abstract class StandardModelFitterTests<Model, Point> {

	int dof;

	protected Random rand = new Random(234);

	protected StandardModelFitterTests(int dof) {
		this.dof = dof;
	}

	/**
	 * Creates a new model fitter\
	 */
	public abstract ModelFitter<Model, Point> createAlg();

	/**
	 * Creates a random model
	 */
	public abstract Model createRandomModel();

	/**
	 * Creates a random point that fits the provided model
	 */
	public abstract Point createRandomPointFromModel( Model model );

	/**
	 * Checks to see of the dat set are described by the model correctly
	 */
	public abstract boolean doPointsFitModel( Model model , List<Point> dataSet );

	@Test
	public void checkMinPoints() {
		ModelFitter<Model, Point> fitter = createAlg();
		assertEquals(dof,fitter.getMinimumPoints());
	}

	/**
	 * Give it points which have been transform by the true affine model.  See
	 * if the transform is correctly estimated
	 */
	@Test
	public void simpleTest() {

		Model model = createRandomModel();

		List<Point> dataSet = new ArrayList<Point>();

		// give it perfect observations
		for( int i = 0; i < 10; i++ ) {
			Point p = createRandomPointFromModel(model);
			dataSet.add(p);
		}

		ModelFitter<Model, Point> fitter = createAlg();

		Model found = createRandomModel();
		fitter.fitModel(dataSet,null,found);

		// test the found transform by seeing if it recomputes the current points
		assertTrue(doPointsFitModel(found,dataSet));
	}

}
