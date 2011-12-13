package boofcv.numerics.optimization;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestLevenbergMarquardt {

	@Test
	public void simpleLinearCase() {
		LineResidual residual = new LineResidual();
		LineGradient gradient = new LineGradient();

		LevenbergMarquardt<double[],double[]> alg =
				new LevenbergMarquardt<double[],double[]>(2,residual,gradient);

		List<double[]> state = createState(-2,-1,0,1,2);
		List<double[]> obs = createLinearObservations(1, 2, state);
		assertTrue(alg.process(new double[]{0, 0}, obs,state));

		System.out.println("initial = "+alg.getInitialCost());
		System.out.println("error = "+alg.getFinalCost());

		double[] found = alg.getModelParameters();

		assertEquals(1,found[0],1e-6);
		assertEquals(2,found[1],1e-6);
	}

	@Test
	public void simpleNonLinear() {
		NonlinearResidual residual = new NonlinearResidual();
		NonlinearGradient gradient = new NonlinearGradient();

		LevenbergMarquardt<double[],double[]> alg =
				new LevenbergMarquardt<double[],double[]>(2,residual,gradient);

		List<double[]> state = createState(-2,-1,0,1,2);
		List<double []> obs = createNonLinearObservations(1,2,state);
		assertTrue(alg.process(new double[]{0, 0}, obs,state));

//		System.out.println("initial = "+alg.getInitialCost());
//		System.out.println("error = "+alg.getFinalCost());

		double[] found = alg.getModelParameters();

		assertEquals(1,found[0],1e-6);
		assertEquals(2,found[1],1e-6);
	}

	private List<double[]> createState( double ...t )
	{
		List<double[]> ret = new ArrayList<double[]>();

		for( double a : t ) {
			double[] m = new double[]{a};
			ret.add(m);
		}

		return ret;
	}

	private List<double[]> createLinearObservations( double a , double x0 , List<double[]> state )
	{
		List<double[]> ret = new ArrayList<double[]>();

		for( double[] s : state ) {
			double tval = s[0];
			double []d = new double[1];
			d[0] = tval*a+x0;
			ret.add(d);
		}

		return ret;
	}

	private List<double[]> createNonLinearObservations( double a , double b , List<double[]> state )
	{
		List<double[]> ret = new ArrayList<double[]>();

		for( double []s : state ) {
			double tval = s[0];
			double []d = new double[1];
			d[0] = a*Math.exp(tval*b);
			ret.add(d);
		}

		return ret;
	}

	private static class LineResidual implements OptimizationResidual<double[],double[]>
	{
		double a,b;

		@Override
		public void setModel(double[] model) {
			a = model[0];
			b = model[1];
		}

		@Override
		public int getNumberOfFunctions() {
			return 1;
		}

		@Override
		public int getModelSize() {
			return 2;
		}

		@Override
		public boolean estimate(double[] state, double[] estimated) {
			estimated[0] = state[0]*a + b;
			return true;
		}

		@Override
		public boolean computeResiduals( double[] obs , double[] state, double[] residuals) {

			estimate(state,residuals);

			residuals[0] = obs[0] - residuals[0];

			return true;
		}
	}

	private static class LineGradient implements OptimizationDerivative<double[]>
	{

		@Override
		public void setModel(double[] model) {}

		@Override
		public boolean computeDerivative( double[] state , double[][] residuals) {

			residuals[0][0] = state[0];
			residuals[0][1] = 1;

			return true;
		}
	}

	public static class NonlinearResidual implements OptimizationResidual<double[],double[]>
	{
		double a,b;

		@Override
		public void setModel(double[] model) {
			a = model[0];
			b = model[1];
		}

		@Override
		public int getModelSize() {
			return 2;
		}

		@Override
		public int getNumberOfFunctions() {
			return 1;
		}

		@Override
		public boolean estimate(double[] state, double[] estimated) {
			estimated[0] = a*Math.exp(state[0]*b);
			return true;
		}

		@Override
		public boolean computeResiduals( double[] obs , double[] state , double[] residuals) {

			estimate(state,residuals);

			residuals[0] = obs[0] - residuals[0];

			return true;
		}
	}

	public static class NonlinearGradient implements OptimizationDerivative<double[]>
	{
		double[] model;

		@Override
		public void setModel(double[] model) {
			this.model = model;
		}


		@Override
		public boolean computeDerivative( double[] state , double[][] gradient) {

			double a = model[0];
			double b = model[1];

			double t = state[0];

			gradient[0][0] = Math.exp(b*t);
			gradient[0][1] = a*t*Math.exp(b*t);

			return true;
		}
	}
}


