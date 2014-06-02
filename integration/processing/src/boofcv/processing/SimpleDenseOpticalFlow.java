package boofcv.processing;

import boofcv.abst.flow.DenseOpticalFlow;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageBase;
import processing.core.PImage;

/**
 * Simplified version of {@link DenseOpticalFlow}
 *
 * @author Peter Abeles
 */
public class SimpleDenseOpticalFlow<T extends ImageBase> {
	DenseOpticalFlow<T> alg;

	ImageFlow flow = new ImageFlow(1,1);

	T source;
	T destination;

	public SimpleDenseOpticalFlow(DenseOpticalFlow<T> alg) {
		this.alg = alg;

		source = alg.getInputType().createImage(1,1);
		destination = alg.getInputType().createImage(1,1);
	}

	public void process( PImage source , PImage destination ) {
		this.source.reshape(source.width, source.height);
		this.destination.reshape(destination.width, destination.height);
		flow.reshape(source.width,source.height);

		ConvertProcessing.convertFromRGB(source,this.source);
		ConvertProcessing.convertFromRGB(destination,this.destination);

		alg.process(this.source,this.destination,flow);
	}

	public void process( SimpleImage<T> source , SimpleImage<T> destination ) {
		flow.reshape(source.image.width, source.image.height);
		alg.process(source.image,destination.image,flow);
	}

	public ImageFlow getFlow() {
		return flow;
	}

	public PImage visualizeFlow() {
		return VisualizeProcessing.denseFlow(flow);
	}

	public DenseOpticalFlow<T> getFlowAlgorithm() {
		return alg;
	}
}
