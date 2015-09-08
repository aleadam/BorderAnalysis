/** 
 * Alejandro Adam - September 8th, 2015
 * 
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.SaveDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.io.FilenameUtils;

public class BorderAnalysis implements PlugInFilter {
	private static int peakWidth = 20;
	private static double stringency = 3.0D;
	private static double edgeRatio = 5.0D;
	private String name;
	private ImagePlus origImp;
	private boolean getPeaks;
	private boolean detectEdges;
	private boolean detectThreshold;

	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}
		if (arg.equals("peaks")) {
			this.getPeaks = true;
			this.detectEdges = false;
			this.detectThreshold = false;
		} else if (arg.equals("edges")) {
			this.getPeaks = false;
			this.detectEdges = true;
			this.detectThreshold = false;
		} else if (arg.equals("threshold")) {
			this.getPeaks = false;
			this.detectEdges = false;
			this.detectThreshold = true;
		} else {
			return DONE;
		}
		this.origImp = imp;
		return NO_UNDO + NO_CHANGES + DOES_8G + DOES_16 + DOES_32;
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Peak detection settings:");
		gd.addNumericField("Peak width (2-100): ", peakWidth, 0);
		gd.addNumericField("Stringency (1-10): ", stringency, 2);
		if ((this.detectEdges) || (this.detectThreshold))
			gd.addNumericField("Edge ratio (1-10): ", edgeRatio, 2);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		peakWidth = (int) gd.getNextNumber();
		stringency = gd.getNextNumber();
		if ((this.detectEdges) || (this.detectThreshold))
			edgeRatio = gd.getNextNumber();

		if (peakWidth < 2)
			peakWidth = 2;
		if (peakWidth > 100)
			peakWidth = 100;
		if (stringency < 1.0D)
			stringency = 1.0D;
		if (stringency > 10.0D)
			stringency = 10.0D;
		if (edgeRatio < 1.0D)
			stringency = 1.0D;
		if (edgeRatio > 10.0D)
			stringency = 10.0D;

		this.name = this.origImp.getTitle();

		ProfilesData pd = new ProfilesData(ip, this.name, peakWidth, stringency);
		if (this.detectEdges) {
			EdgeDetector ed = new EdgeDetector(ip, this.name);
			ed.detect(pd.getPeakMedian(), pd.getAverageBelow(6), edgeRatio);
		} else if (this.detectThreshold) {
			ThresholdDetector td = new ThresholdDetector(ip, this.name);
			td.detect(pd.getPeakMedian(), pd.getAverageBelow(6), edgeRatio);
		} else if (this.getPeaks) {
			save(pd);
		}
	}

	public void save(ProfilesData pd) {
		try {
			SaveDialog sd = new SaveDialog("Save data as...", "",
					FilenameUtils.getBaseName(this.name), ".csv");
			String saveName = sd.getDirectory() + sd.getFileName();
			File file = new File(saveName);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter writer = new BufferedWriter(fw);

			writer.write("ROI raw data values");
			writer.newLine();
			for (int i = 0; i < pd.allPixelValues.length; i++) {
				writer.write("ROI " + i + ",");
				for (int j = 0; j < pd.allPixelValues[i].length; j++) {
					writer.write(pd.allPixelValues[i][j] + ",");
				}
				writer.newLine();
			}
			writer.newLine();
			writer.newLine();
			writer.write("Background (70% percentile): ," + pd.getTenth(6));
			writer.newLine();
			writer.write("Background (Average intensity below 70% percentile): ," + pd.getAverageBelow(6));
			writer.newLine();
			writer.write("Average peak intensity: ," + pd.getPeakAverage());
			writer.newLine();
			writer.write("Peak Standard deviation: ," + pd.getStDev());
			writer.newLine();
			writer.write("Total number of peaks: ," + pd.getPeakCount());
			writer.newLine();
			writer.write("Peak median: ," + pd.getPeakMedian());
			writer.newLine();
			writer.write("Peak range: ," + pd.getMinPeak() + " - " + pd.getMaxPeak());

			writer.newLine();
			writer.write("Peak width setting: ," + peakWidth);
			writer.newLine();
			writer.write("Stringency: ," + stringency);

			writer.newLine();
			writer.newLine();

			writer.write("Peaks");
			writer.newLine();
			for (int i = 0; i < pd.peakVal.length; i++) {
				writer.write("ROI " + i + ",");
				writer.newLine();
				writer.write("Pixel position: ,");
				for (int j = 0; j < pd.peakPos[i].length; j++) {
					if (i < pd.peakVal.length / 2) {
						writer.write("(" + pd.lines[i].x1 + ":" + pd.peakPos[i][j] + "),");
					} else {
						writer.write("(" + pd.peakPos[i][j] + ":" + pd.lines[i].y1 + "),");
					}
				}
				writer.newLine();
				writer.write("Pixel value: ,");
				for (int j = 0; j < pd.peakPos[i].length; j++) {
					writer.write(pd.peakVal[i][j] + ",");
				}
				writer.newLine();
			}
			writer.newLine();
			writer.newLine();
			writer.write("Peaks in a single column (for copy/paste)");
			writer.newLine();
			for (int i = 0; i < pd.peakVal.length; i++) {
				for (int j = 0; j < pd.peakVal[i].length; j++) {
					writer.write("" + pd.peakVal[i][j]);
					writer.newLine();
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			IJ.showMessage("Error saving file!",
					"Please check that the file is not in use and try running the plugin again.");
		}
	}

	void showAbout() {
		IJ.showMessage(
				"About MultipleProfilePeaks...",
				"It takes a single image and creates 20 evenly-spaced line ROI\n+"
						+ "(10 vertical lines, 10 horizontal lines), measures the intensity\n"
						+ "profile in each and finds the peak location and maxima,\n"
						+ "saving the data in a csv file for further analysis.");
	}
}
