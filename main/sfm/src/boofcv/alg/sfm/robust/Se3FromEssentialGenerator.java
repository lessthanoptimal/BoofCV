package boofcv.alg.sfm.robust;

import boofcv.abst.geo.EpipolarMatrixEstimator;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.DecomposeEssential;
import boofcv.numerics.fitting.modelset.HypothesisList;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Estimates the motion between two views up to a scale factor by computing an essential matrix,
 * decomposing it, and using the positive depth constraint to select the best candidate.
 *
 * @author Peter Abeles
 */
public class Se3FromEssentialGenerator implements ModelGenerator<Se3_F64,AssociatedPair> {

	// Estimates essential matrix from observations
	EpipolarMatrixEstimator computeEssential;
	// decomposes essential matrix to extract motion
	DecomposeEssential decomposeE = new DecomposeEssential();
	// used to select the best candidate motion
	TriangulateTwoViewsCalibrated triangulate;

	// triangulated point in 3D
	Point3D_F64 found = new Point3D_F64();

	/**
	 * Specifies how the essential matrix is computed
	 *
	 * @param computeEssential Algorithm for computing the essential matrix
	 */
	public Se3FromEssentialGenerator(EpipolarMatrixEstimator computeEssential,
									 TriangulateTwoViewsCalibrated triangulate ) {
		this.computeEssential = computeEssential;
		this.triangulate = triangulate;
	}

	@Override
	public Se3_F64 createModelInstance() {
		return new Se3_F64();
	}

	/**
	 * Computes the camera motion from the set of observations.
	 *
	 * @param dataSet Associated pairs in normalized camera coordinates.
	 * @param models The best pose according to the positive depth constraint.
	 */
	@Override
	public void generate(List<AssociatedPair> dataSet, HypothesisList<Se3_F64> models) {
		if( !computeEssential.process(dataSet) )
			return;

		DenseMatrix64F E = computeEssential.getEpipolarMatrix();

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
				triangulate.triangulate(p.keyLoc,p.currLoc,s,found);
				if( found.z > 0 ) {
					count++;
				}
			}

			if( count > bestCount ) {
				bestCount = count;
				bestModel = s;
			}
		}

		models.pop().set(bestModel);
	}

	@Override
	public int getMinimumPoints() {
		return computeEssential.getMinimumPoints();
	}
}
