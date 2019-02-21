package boofcv.alg.misc.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;
import boofcv.struct.image.*;
import org.junit.jupiter.api.Test;
import org.omg.SendingContext.RunTime;

import java.util.Random;

import static boofcv.alg.misc.impl.ImplImageBandMath.average;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class TestImplImageBandMath_MT {
	final private int width = 80;
	final private int height = 90;
	final private int numBands = 11;
	final private int firstBand = 0;
	final private int lastBand = numBands -1;
	Class[] types = new Class[]{GrayU8.class, GrayU16.class, GrayS16.class, GrayS32.class, GrayS64.class, GrayF32.class, GrayF64.class};


	Random rand = new Random(234);

	@Test
	void implement() {
		for (Class type : types) {
			testType(type);
		}
	}


	void testType(Class type){
		Planar input = new Planar(type, width, height,numBands);
		ImageGray output = GeneralizedImageOps.createSingleBand(type, width, height);
		ImageGray outputMT = GeneralizedImageOps.createSingleBand(type, width, height);
		ImageGray avg = GeneralizedImageOps.createSingleBand(type, width, height);

		if( output.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
		}
		GImageGray[] testImages = new GImageGray[lastBand - firstBand + 1];
		for (int i = firstBand; i <= lastBand; i++) {
			testImages[i] = FactoryGImageGray.wrap(input.getBand(i));
		}
		if (type == GrayU8.class) {
			ImplImageBandMath.average(input, (GrayU8) output, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayU8) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.median(input, (GrayU8) output, firstBand, lastBand);
			ImplImageBandMath_MT.median(input, (GrayU8) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.minimum(input, (GrayU8) output, firstBand, lastBand);
			ImplImageBandMath_MT.minimum(input, (GrayU8) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.maximum(input, (GrayU8) output, firstBand, lastBand);
			ImplImageBandMath_MT.maximum(input, (GrayU8) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.average(input, (GrayU8) avg, firstBand, lastBand);
			ImplImageBandMath.stdDev(input, (GrayU8) output, (GrayU8) avg, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayU8) avg, firstBand, lastBand);
			ImplImageBandMath_MT.stdDev(input, (GrayU8) outputMT, (GrayU8) avg, firstBand, lastBand);
			checkResult(output, outputMT);

		} else if (type == GrayU16.class) {
			ImplImageBandMath.average(input, (GrayU16) output, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayU16) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.median(input, (GrayU16) output, firstBand, lastBand);
			ImplImageBandMath_MT.median(input, (GrayU16) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.minimum(input, (GrayU16) output, firstBand, lastBand);
			ImplImageBandMath_MT.minimum(input, (GrayU16) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.maximum(input, (GrayU16) output, firstBand, lastBand);
			ImplImageBandMath_MT.maximum(input, (GrayU16) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.average(input, (GrayU16) avg, firstBand, lastBand);
			ImplImageBandMath.stdDev(input, (GrayU16) output, (GrayU16) avg, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayU16) avg, firstBand, lastBand);
			ImplImageBandMath_MT.stdDev(input, (GrayU16) outputMT, (GrayU16) avg, firstBand, lastBand);
			checkResult(output, outputMT);

		} else if (type == GrayS16.class) {
			ImplImageBandMath.average(input, (GrayS16) output, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayS16) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.median(input, (GrayS16) output, firstBand, lastBand);
			ImplImageBandMath_MT.median(input, (GrayS16) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.minimum(input, (GrayS16) output, firstBand, lastBand);
			ImplImageBandMath_MT.minimum(input, (GrayS16) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.maximum(input, (GrayS16) output, firstBand, lastBand);
			ImplImageBandMath_MT.maximum(input, (GrayS16) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.average(input, (GrayS16) avg, firstBand, lastBand);
			ImplImageBandMath.stdDev(input, (GrayS16) output, (GrayS16) avg, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayS16) avg, firstBand, lastBand);
			ImplImageBandMath_MT.stdDev(input, (GrayS16) outputMT, (GrayS16) avg, firstBand, lastBand);
			checkResult(output, outputMT);

		} else if (type == GrayS32.class) {
			ImplImageBandMath.average(input, (GrayS32) output, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayS32) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.median(input, (GrayS32) output, firstBand, lastBand);
			ImplImageBandMath_MT.median(input, (GrayS32) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.minimum(input, (GrayS32) output, firstBand, lastBand);
			ImplImageBandMath_MT.minimum(input, (GrayS32) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.maximum(input, (GrayS32) output, firstBand, lastBand);
			ImplImageBandMath_MT.maximum(input, (GrayS32) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.average(input, (GrayS32) avg, firstBand, lastBand);
			ImplImageBandMath.stdDev(input, (GrayS32) output, (GrayS32) avg, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayS32) avg, firstBand, lastBand);
			ImplImageBandMath_MT.stdDev(input, (GrayS32) outputMT, (GrayS32) avg, firstBand, lastBand);
			checkResult(output, outputMT);

		} else if (type == GrayS64.class) {
			ImplImageBandMath.average(input, (GrayS64) output, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayS64) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.median(input, (GrayS64) output, firstBand, lastBand);
			ImplImageBandMath_MT.median(input, (GrayS64) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.minimum(input, (GrayS64) output, firstBand, lastBand);
			ImplImageBandMath_MT.minimum(input, (GrayS64) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.maximum(input, (GrayS64) output, firstBand, lastBand);
			ImplImageBandMath_MT.maximum(input, (GrayS64) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.average(input, (GrayS64) avg, firstBand, lastBand);
			ImplImageBandMath.stdDev(input, (GrayS64) output, (GrayS64) avg, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayS64) avg, firstBand, lastBand);
			ImplImageBandMath_MT.stdDev(input, (GrayS64) outputMT, (GrayS64) avg, firstBand, lastBand);
			checkResult(output, outputMT);

		} else if (type == GrayF32.class) {
			ImplImageBandMath.average(input, (GrayF32) output, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayF32) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.median(input, (GrayF32) output, firstBand, lastBand);
			ImplImageBandMath_MT.median(input, (GrayF32) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.minimum(input, (GrayF32) output, firstBand, lastBand);
			ImplImageBandMath_MT.minimum(input, (GrayF32) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.maximum(input, (GrayF32) output, firstBand, lastBand);
			ImplImageBandMath_MT.maximum(input, (GrayF32) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.average(input, (GrayF32) avg, firstBand, lastBand);
			ImplImageBandMath.stdDev(input, (GrayF32) output, (GrayF32) avg, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayF32) avg, firstBand, lastBand);
			ImplImageBandMath_MT.stdDev(input, (GrayF32) outputMT, (GrayF32) avg, firstBand, lastBand);
			checkResult(output, outputMT);

		} else if (type == GrayF64.class) {
			ImplImageBandMath.average(input, (GrayF64) output, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayF64) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.median(input, (GrayF64) output, firstBand, lastBand);
			ImplImageBandMath_MT.median(input, (GrayF64) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.minimum(input, (GrayF64) output, firstBand, lastBand);
			ImplImageBandMath_MT.minimum(input, (GrayF64) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.maximum(input, (GrayF64) output, firstBand, lastBand);
			ImplImageBandMath_MT.maximum(input, (GrayF64) outputMT, firstBand, lastBand);
			checkResult(output, outputMT);

			ImplImageBandMath.average(input, (GrayF64) avg, firstBand, lastBand);
			ImplImageBandMath.stdDev(input, (GrayF64) output, (GrayF64) avg, firstBand, lastBand);
			ImplImageBandMath_MT.average(input, (GrayF64) avg, firstBand, lastBand);
			ImplImageBandMath_MT.stdDev(input, (GrayF64) outputMT, (GrayF64) avg, firstBand, lastBand);
			checkResult(output, outputMT);
		} else {
			throw new RuntimeException("Failed to implement testing of class: " + type);
		}

	}

	private void checkResult(ImageGray output, ImageGray outputMT) {
		ImageType imageType = output.getImageType();
		GImageGray gOutput = FactoryGImageGray.wrap(output);
		GImageGray gOutputMT = FactoryGImageGray.wrap(outputMT);
		int nrPixels = gOutput.getHeight() * gOutput.getWidth();
		for (int i = 0; i < nrPixels; i++) {
			assertEquals(gOutput.getF(i), gOutput.getF(i), 1e-6);
		}

	}
}

