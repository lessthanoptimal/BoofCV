package boofcv.alg.sfm.robust;

import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.DecomposeEssential;
import boofcv.alg.geo.PositiveDepthConstraintCheck;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.GeoModelEstimator1;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Estimates the motion between two views up to a scale factor by computing an essential matrix,
 * decomposing it, and using the positive depth constraint to select the best candidate.  The returned
 * motion is the motion from the first camera frame into the second camera frame.
 *
 * @author Peter Abeles
 */
public class Se3FromEssentialGenerator implements ModelGenerator<Se3_F64,AssociatedPair> {

	// Estimates essential matrix from observations
	GeoModelEstimator1<DenseMatrix64F,AssociatedPair> computeEssential;
	// decomposes essential matrix to extract motion
	DecomposeEssential decomposeE = new DecomposeEssential();
	// used to select best hypothesis
	PositiveDepthConstraintCheck depthCheck;

	DenseMatrix64F E = new DenseMatrix64F(3,3);

	/**
	 * Specifies how the essential matrix is computed
	 *
	 * @param computeEssential Algorithm for computing the essential matrix
	 */
	public Se3FromEssentialGenerator(GeoModelEstimator1<DenseMatrix64F,AssociatedPair> computeEssential,
									 TriangulateTwoViewsCalibrated triangulate ) {
		this.computeEssential = computeEssential;
		this.depthCheck = new PositiveDepthConstraintCheck(triangulate);
	}

	@Override
	public Se3_F64 createModelInstance() {
		return new Se3_F64();
	}

	/**
	 * Computes the camera motion from the set of observations.   The motion is from the first
	 * into the second camera frame.
	 *
	 * @param dataSet Associated pairs in normalized camera coordinates.
	 * @param model The best pose according to the positive depth constraint.
	 */
	@Override
	public boolean generate(List<AssociatedPair> dataSet, Se3_F64 model ) {
		if( !computeEssential.process(dataSet,E) )
			return false;

		// extract the possible motions
		decomposeE.decompose(E);
		List<Se3_F64> candidates = decomposeE.getSolutions();

		// use positive depth constraint to select the best one
		Se3_F64 bestModel = null;
		int bestCount = -1;
		for( int i = 0; i < candidates.size(); i++ ) {
			Se3_F64 s = candidates.get(i);
			int count = 0;
			for( AssociatedPair p : dataSet ) {
				if( depthCheck.checkConstraint(p.keyLoc,p.currLoc,s)) {
					count++;
				}
			}

			if( count > bestCount ) {
				bestCount = count;
				bestModel = s;
			}
		}

		model.set(bestModel);
		return true;
	}

	@Override
	public int getMinimumPoints() {
		return computeEssential.getMinimumPoints();
	}
}
