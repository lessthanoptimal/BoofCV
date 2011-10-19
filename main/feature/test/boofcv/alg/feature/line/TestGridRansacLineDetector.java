package boofcv.alg.feature.line;

import boofcv.alg.feature.detect.line.GridRansacLineDetector;
import boofcv.alg.feature.detect.line.gridline.Edgel;
import boofcv.alg.feature.detect.line.gridline.GridLineModelDistance;
import boofcv.alg.feature.detect.line.gridline.GridLineModelFitter;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
import boofcv.struct.feature.MatrixOfList;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.line.LinePolar2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestGridRansacLineDetector {

	int width = 40;
	int height = 30;

	@Test
	public void checkObvious() {
		for(int size = 11; size <= 19; size += 2 )
			checkObvious(size);
	}

	/**
	 * Give it a single straight line and see if it can detect it.  Allow the region size to be changed to check
	 * for issues related to that
	 * @param regionSize
	 */
	protected void checkObvious( int regionSize ) {
//		System.out.println("regionSize = "+regionSize);
		int where = 25;
		ImageUInt8 image = new ImageUInt8(width,height);
		ImageFloat32 derivX = new ImageFloat32(width,height);
		ImageFloat32 derivY = new ImageFloat32(width,height);

		for( int i = 0; i < height; i++ ) {
			image.set(where,i,1);
			derivX.set(where,i,20);
		}

		GridLineModelDistance distance = new GridLineModelDistance(0.9f);
		GridLineModelFitter fitter = new GridLineModelFitter(0.9f);

		ModelMatcher<LinePolar2D_F32, Edgel> matcher =
				new SimpleInlierRansac<LinePolar2D_F32,Edgel>(123123,fitter,distance,25,2,2*regionSize/3,1000,1);
		GridRansacLineDetector alg = new GridRansacLineDetector(regionSize,5,matcher);

		alg.process(derivX,derivY,image);

		MatrixOfList<LineSegment2D_F32> lines = alg.getFoundLines();

		assertEquals(width/regionSize,lines.getWidth());
		assertEquals(height/regionSize,lines.getHeight());

		int gridCol = where/regionSize;
		for( int i = 0; i < lines.height; i++ ) {
			List<LineSegment2D_F32> l = lines.get(gridCol,i);

			assertTrue(l.size()==1);
			LineSegment2D_F32 a = l.get(0);
			assertTrue(Math.abs(a.slopeY())>1);
			assertTrue(Math.abs(a.slopeX())<0.01);
		}
	}

}
