package boofcv.alg.geo.d2;

import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.ModelFitter;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homo.HomographyPointOps;

import java.util.List;


/**
 * @author Peter Abeles
 */
public class TestModelFitterLinearHomography extends StandardModelFitterTests<Homography2D_F64,AssociatedPair> {

	public TestModelFitterLinearHomography() {
		super(4);
	}

	@Override
	public ModelFitter<Homography2D_F64, AssociatedPair> createAlg() {
		return new ModelFitterLinearHomography();
	}

	@Override
	public Homography2D_F64 createRandomModel() {
		Homography2D_F64 model = new Homography2D_F64();
		model.a11 = rand.nextDouble();
		model.a12 = rand.nextDouble();
		model.a13 = rand.nextDouble();
		model.a21 = rand.nextDouble();
		model.a22 = rand.nextDouble();
		model.a23 = rand.nextDouble();
		model.a31 = rand.nextDouble();
		model.a32 = rand.nextDouble();
		model.a33 = rand.nextDouble();

		return model;
	}

	@Override
	public AssociatedPair createRandomPointFromModel(Homography2D_F64 transform) {
		AssociatedPair ret = new AssociatedPair();
		ret.keyLoc.x = rand.nextDouble()*10;
		ret.keyLoc.y = rand.nextDouble()*10;

		HomographyPointOps.transform(transform, ret.keyLoc, ret.currLoc);

		return ret;
	}

	@Override
	public boolean doPointsFitModel(Homography2D_F64 transform, List<AssociatedPair> dataSet) {
		Point2D_F64 expected = new Point2D_F64();

		for( AssociatedPair p : dataSet ) {
			HomographyPointOps.transform(transform,p.keyLoc,expected);

			if( expected.distance(p.currLoc) > 0.01 )
				return false;
		}

		return true;
	}
}
