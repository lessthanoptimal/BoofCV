package boofcv.alg.tracker.meanshift;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import org.ddogleg.util.UtilDouble;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestTrackerMeanShiftComaniciu2003 {

	Random rand = new Random(234);

	@Test
	public void track() {
		InterpolatePixelS interpSB = FactoryInterpolation.bilinearPixelS(ImageFloat32.class);
		InterpolatePixelMB interpolate = FactoryInterpolation.createPixelMB(interpSB);
		LocalWeightedHistogramRotRect calcHistogram = new LocalWeightedHistogramRotRect(30,3,10,3,255,interpolate);

		MultiSpectral<ImageFloat32> image = new MultiSpectral<ImageFloat32>(ImageFloat32.class,100,150,3);

		fail("finish");
	}

	@Test
	public void distanceHistogram() {
		LocalWeightedHistogramRotRect calcHist = new LocalWeightedHistogramRotRect(10,3,5,3,255,null);

		TrackerMeanShiftComaniciu2003 alg = new TrackerMeanShiftComaniciu2003(true,100,1e-4f,0.1f,calcHist);

		int sampleHistIndex[] = new int[ calcHist.getHistogram().length ];
		float histogram[] = new float[ calcHist.getHistogram().length ];

		// score for identical histograms
		for( int i = 0; i < histogram.length; i++ ) {
			sampleHistIndex[i] = i;
			histogram[i] = alg.keyHistogram[i] = rand.nextFloat();
		}
		UtilDouble.normalize(histogram);
		UtilDouble.normalize(alg.keyHistogram);

		double foundIdentical = alg.distanceHistogram(sampleHistIndex,histogram);
		assertEquals(0,foundIdentical,1e-3);

		// make the histograms very different
		for( int i = 0; i < histogram.length; i++ ) {
			histogram[i] = rand.nextFloat();
			alg.keyHistogram[i] = rand.nextFloat();
		}
		UtilDouble.normalize(histogram);
		UtilDouble.normalize(alg.keyHistogram);

		double foundDifferent = alg.distanceHistogram(sampleHistIndex,histogram);

		assertTrue( foundDifferent > 0.05 );
	}

}
