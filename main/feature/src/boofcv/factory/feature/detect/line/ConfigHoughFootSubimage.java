package boofcv.factory.feature.detect.line;

import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.abst.feature.detect.line.DetectLineHoughFootSubimage}.
 *
 * @author Peter Abeles
 */
public class ConfigHoughFootSubimage implements Configuration {

	/**
	 * Lines in transform space must be a local max in a region with this radius. Try 5;
	 */
	public int localMaxRadius = 5;
	/**
	 * Minimum number of counts/votes inside the transformed image. Try 5.
	 */
	public int minCounts = 5;
	/**
	 * Lines which are this close to the origin of the transformed image are ignored.  Try 5.
	 */
	public int minDistanceFromOrigin = 5;
	/**
	 * Threshold for classifying pixels as edge or not.  Try 30.
	 */
	public float thresholdEdge = 30;
	/**
	 * Maximum number of lines to return. If <= 0 it will return them all.
	 */
	public int maxLines = 0;
	/**
	 * Number of sub-images in horizontal direction Try 2
	 */
	public int totalHorizontalDivisions = 2;
	/**
	 * Number of sub images in vertical direction.  Try 2
	 */
	public int totalVerticalDivisions = 2;

	public ConfigHoughFootSubimage() {
	}

	public ConfigHoughFootSubimage(int maxLines) {
		this.maxLines = maxLines;
	}

	public ConfigHoughFootSubimage(int localMaxRadius, int minCounts, int minDistanceFromOrigin,
								   float thresholdEdge, int maxLines, int totalHorizontalDivisions,
								   int totalVerticalDivisions) {
		this.localMaxRadius = localMaxRadius;
		this.minCounts = minCounts;
		this.minDistanceFromOrigin = minDistanceFromOrigin;
		this.thresholdEdge = thresholdEdge;
		this.maxLines = maxLines;
		this.totalHorizontalDivisions = totalHorizontalDivisions;
		this.totalVerticalDivisions = totalVerticalDivisions;
	}

	@Override
	public void checkValidity() {

	}
}
