/** 
 * Alejandro Adam - September 8th, 2015
 * 
 */

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Line;
import ij.process.ImageProcessor;
import java.util.Arrays;

public class ProfilesData {
	public ImagePlus imp;
	private String name;
	public int width;
	public int height;
	private double[] tenths;
	private double peakAverage;
	private int count;
	public double[][] allPixelValues;
	public double[][] peakVal;
	public int[][] peakPos;
	public Line[] lines;

	public ProfilesData(ImageProcessor ip, String name, int peakWidth,
			double stringency) {
		this.name = name;
		this.width = ip.getWidth();
		this.height = ip.getHeight();
		ImageStack stack = new ImageStack(this.width, this.height);
		stack.addSlice("1", ip);
		this.imp = new ImagePlus(name, stack);
		this.lines = generateRoi();

		this.allPixelValues = new double[this.lines.length][];
		for (int i = 0; i < this.lines.length; i++) {
			this.imp.setRoi(this.lines[i]);
			double[] val = this.lines[i].getPixels();
			this.allPixelValues[i] = val;
		}
		this.tenths = getTenths(this.allPixelValues);
		populatePeaks(this.allPixelValues, peakWidth, stringency);
		for (int i = 0; i < this.peakVal.length; i++) {
			for (int j = 0; j < this.peakVal[i].length; j++) {
				this.peakAverage += this.peakVal[i][j];
				this.count += 1;
			}
		}
		this.peakAverage /= this.count;
	}

	public double getPeakAverage() {
		return this.peakAverage;
	}

	public double getPeakCount() {
		return this.count;
	}

	public double getPeakMedian() {
		int size = 0;
		for (int i = 0; i < this.peakVal.length; i++) {
			size += this.peakVal[i].length;
		}
		double[] data = new double[size];
		int c = 0;
		for (int i = 0; i < this.peakVal.length; i++) {
			for (int j = 0; j < this.peakVal[i].length; j++) {
				data[(c++)] = this.peakVal[i][j];
			}
		}
		Arrays.sort(data);
		if (data.length % 2 == 0) {
			return (data[(data.length / 2 - 1)] + data[(data.length / 2)]) / 2.0D;
		}
		return data[(data.length / 2)];
	}

	private double getVariance() {
		double var = 0.0D;
		for (int i = 0; i < this.peakVal.length; i++) {
			for (int j = 0; j < this.peakVal[i].length; j++) {
				var = var + (this.peakVal[i][j] - this.peakAverage) * (this.peakVal[i][j] - this.peakAverage);
			}
		}
		return var / (this.count - 1);
	}

	public double getStDev() {
		if (this.count < 2) {
			return 0.0D;
		}
		return Math.sqrt(getVariance());
	}

	public double getMaxPeak() {
		Double max = Double.valueOf(0.0D);
		for (int i = 0; i < this.peakVal.length; i++) {
			for (int j = 0; j < this.peakVal[i].length; j++) {
				max = Double.valueOf(max.doubleValue() > this.peakVal[i][j] ? max.doubleValue() : this.peakVal[i][j]);
			}
		}
		return max.doubleValue();
	}

	public double getMinPeak() {
		Double min = Double.valueOf(Double.MAX_VALUE);
		for (int i = 0; i < this.peakVal.length; i++) {
			for (int j = 0; j < this.peakVal[i].length; j++) {
				min = Double.valueOf(min.doubleValue() < this.peakVal[i][j] ? min.doubleValue() : this.peakVal[i][j]);
			}
		}
		return min.doubleValue();
	}

	public String getName() {
		return this.name;
	}

	public double getTenth(int tenth) {
		return this.tenths[tenth];
	}

	public double getAverageBelow(int tenth) {
		int size = 0;
		double avg = 0.0D;
		int count = 0;
		for (int i = 0; i < this.allPixelValues.length; i++) {
			size += this.allPixelValues[i].length;
		}
		double[] data = new double[size];
		int c = 0;
		for (int i = 0; i < this.allPixelValues.length; i++) {
			for (int j = 0; j < this.allPixelValues[i].length; j++) {
				data[(c++)] = this.allPixelValues[i][j];
			}
		}
		Arrays.sort(data);
		for (int i = 0; i < data.length * tenth / 10; i++) {
			avg += data[i];
			count++;
		}
		if (count == 0) {
			return 0.0D;
		}
		return avg / count;
	}

	private Line[] generateRoi() {
		Line[] lines = new Line[20];
		for (int i = 0; i < 10; i++) {
			lines[i] = new Line((0.5D + i) * this.width / 10.0D, 0.0D, (0.5D + i) * this.width / 10.0D, this.height);
		}
		for (int j = 0; j < 10; j++) {
			lines[(10 + j)] = new Line(0.0D, (0.5D + j) * this.height / 10.0D, this.width, (0.5D + j) * this.height / 10.0D);
		}
		return lines;
	}

	private double[] getTenths(double[][] values) {
		int size = 0;
		double[] tenths = new double[10];
		for (int i = 0; i < values.length; i++) {
			size += values[i].length;
		}
		double[] data = new double[size];
		int c = 0;
		for (int i = 0; i < values.length; i++) {
			for (int j = 0; j < values[i].length; j++) {
				data[(c++)] = values[i][j];
			}
		}
		Arrays.sort(data);
		for (int i = 0; i < 10; i++) {
			tenths[i] = data[(data.length * (i + 1) / 10 - 1)];
		}
		return tenths;
	}

	private void populatePeaks(double[][] values, int peakWidth,
			double stringency) {
		PeakDetector[] pd = new PeakDetector[values.length];
		this.peakPos = new int[values.length][];
		this.peakVal = new double[values.length][];
		for (int i = 0; i < values.length; i++) {
			pd[i] = new PeakDetector(values[i]);
			this.peakPos[i] = pd[i].process(peakWidth, stringency);
			this.peakVal[i] = new double[this.peakPos[i].length];
			for (int j = 0; j < this.peakPos[i].length; j++) {
				this.peakVal[i][j] = values[i][this.peakPos[i][j]];
			}
		}
	}
}
