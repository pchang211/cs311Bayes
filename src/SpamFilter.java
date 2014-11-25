import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class SpamFilter {

	final static File allfolder = new File("spamdata/");
	final static File spamfolder = new File("spamdata/spam");
	final static File hamfolder = new File("spamdata/ham");
	
	
	final static int TRAIN_SIZE = 3000;
	final static double DELTA = .000001;
	final static double LAMBDA = .000001;
	
	
	private double delta ;// = .0001;
	private double lambda; // = .003;
	
	private Set<String> trainSet;
	private Set<String> testSet;
	
	private HashMap<Label, Double> priors;
	private HashMap<FeatureLabelPair, Double> counts;
	
	public static void main(String[] args) throws IOException
	{		
		
		Scanner scan = new Scanner(System.in);
//		BufferedWriter out = new BufferedWriter(new FileWriter("output.txt")); 
		
//		System.out.println("How many messages to classify?");
		
//		int nummsgs = Integer.parseInt(scan.nextLine());
//		System.out.println("Classifying 1000 random messages...");
		
		SpamFilter spamFilter = new SpamFilter();
		spamFilter.split();
		
		Set<String> featureSet = preprocess();
		spamFilter.train(featureSet);
		
//		double maxAccuracy = 0;
//		double maxLambda = -1;
//		double maxDelta = -1;
//		for (double delta = .000000001; delta < .00001; delta+= .0000001) {
		spamFilter.evaluate(featureSet, DELTA, LAMBDA);
//			double lambda = .003;
//			for (double lambda = .000001; lambda < .001; lambda += .00005) {
//				double accuracy = spamFilter.evaluate(featureSet, delta, lambda);
//				if (accuracy > maxAccuracy) {
//					maxAccuracy = accuracy;
//					maxDelta = delta;
//					maxLambda = lambda;
//				}
//			}
		
//		System.out.println("MAX DELTA: " + maxDelta + ", MAX LAMBDA: " + maxLambda + ", MAX ACCURACY: " + maxAccuracy);
		
//        System.out.println("all messages:");
//		readFiles(allfolder);
//		System.out.println();
		
//		System.out.println("spam messages:");
//		readFiles(spamfolder);
//		System.out.println();
		
//		System.out.println("ham messages:");
//		readFiles(hamfolder);
		
		
		
//		out.close();
		
	}
	
	public SpamFilter() {
		trainSet = new HashSet<String>();
//		trainSet.add("file1.spam.txt");
//		trainSet.add("file2.ham.txt");
		testSet = new HashSet<String>();
		
		priors = new HashMap<Label, Double>();
		counts = new HashMap<FeatureLabelPair, Double>();
	}

	/*
	 * Takes as parameter the name of a folder and returns a list of filenames (Strings) 
	 * in the folder.
	 */
	public static ArrayList<String> readFiles(File folder){
		
		//List to store filenames in folder
		ArrayList<String> filelist = new ArrayList<String>();
		
		//call to recursive method that reads all filenames in folder
		listFilesForFolder(folder, filelist );

		return filelist;
	}
	
	
	/*
	 * Recursive method that takes as parameter a folder and an empty ArrayList and
	 * fills the list with the names of all the files in the given foler.
	 */
	public static void listFilesForFolder(File folder, ArrayList<String> files) {
		
		for (File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) 
	            listFilesForFolder(fileEntry, files);
	        else {
	        	String filename = fileEntry.getName();
	        	files.add(filename);
//	            System.out.println(filename);
	        }
	    }
	}

	/**
	 * Splits data into train and test sets
	 */
	public void split() {
		Random rand = new Random();

		ArrayList<String> examples = readFiles(allfolder);
		for (int i = 0; i<TRAIN_SIZE; i++) {
			trainSet.add(examples.remove(rand.nextInt(examples.size()-1)));
		}
		
		// add the rest to our test set
		for (String s : examples) {
			testSet.add(s);
		}
	}
	
	/*
	 * TO DO
	 * Preprocessor: Reads email messages to fill a table of features.
	 * You may modify the method header (return type, parameters) as you see fit.
	 */
	public static Set<String> preprocess() throws IOException
	{
		Scanner scanner = new Scanner(new File("features.txt"));
		HashSet<String> featureSet = new HashSet<String>();
		String current;
		while (scanner.hasNext()) {
			current = scanner.next();
			featureSet.add(current);
		}
		return featureSet;
	}
	
	private enum Label {
		SPAM, HAM
	}
	
	private static Label getLabel(String filename) {
		if (filename.contains("spam.txt")) {
			return Label.SPAM;
		}
		else return Label.HAM;
	}
	
	/**
	 * Private class to store a feature and label for calculating counts
	 *
	 */
	private class FeatureLabelPair {
		public String feature;
		public Label label;
		
		public FeatureLabelPair(String feature, Label label) {
			this.feature = feature;
			this.label = label;
		}
		
		@Override
		public int hashCode() {
			return this.label.hashCode() + 7*this.feature.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != getClass()) return false;
			FeatureLabelPair pair = (FeatureLabelPair) obj;
			return (this.feature.equals(pair.feature) && this.label.equals(pair.label));
		}
	}
	
	/*
	 * TO DO
	 * Trainer: Reads email messages and computes probabilities for the Bayesian formula.
	 * You may modify the method header (return type, parameters) as you see fit.
	 */
	public void train(Set<String> featureSet)
	{
		initializeCounts(featureSet);
		for (String file : trainSet) {
			if (!file.contains(".DS_Store")) generateCounts(file, featureSet);
		}	
	}

	/**
	 * Initializes all label and feature/label counts to 0
	 * @param featureSet
	 */
	private void initializeCounts(Set<String> featureSet) {
		priors.put(Label.SPAM, 0.0);
		priors.put(Label.HAM, 0.0);
		for (String f : featureSet) {
			counts.put(new FeatureLabelPair(f, Label.SPAM), 0.0);
			counts.put(new FeatureLabelPair(f, Label.HAM), 0.0);
		}
	}
	
	private String getPath(String file) {
		Label label = getLabel(file);
		String path = (label==Label.SPAM) ? spamfolder+"/"+file : hamfolder+"/"+file;
		return path;
	}
	
	private void generateCounts(String file, Set<String> featureSet) {
		Scanner scan;
		try {
			Label label = getLabel(file);
			String path = getPath(file);
			scan = new Scanner(new File(path));

			/* increment priors */
			priors.put(label, priors.get(label) + 1);
			
			String current;
			while(scan.hasNext()) {
				current = scan.next();
				if (featureSet.contains(current)) {
					FeatureLabelPair pair = new FeatureLabelPair(current, label);
					/* increment counts */
					counts.put(pair, counts.get(pair)+1);
				}
			}
			scan.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	/*
	 * TO DO
	 * Classifier: Reads and classifies an email message as either spam or ham.
	 * You may modify the method header (return type, parameters) as you see fit.
	 */
	public Label classify(String file, Set<String> featureSet)
	{
		double prob = 0;
		HashSet<String> features;
		try {
			Scanner scan = new Scanner(new File(getPath(file)));
			features = new HashSet<String>();
			String word;
			while (scan.hasNext()) {
				word = scan.next();
				if (featureSet.contains(word)) features.add(word);
			}
			scan.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		int numFeatures = featureSet.size();
		for (String f : features) {
			FeatureLabelPair spamPair = new FeatureLabelPair(f, Label.SPAM);
			prob += Math.log(
					(counts.get(spamPair) + lambda)/
					(priors.get(Label.SPAM) + numFeatures*lambda)
					);
		}
		prob += Math.log(priors.get(Label.SPAM)/TRAIN_SIZE);
		
		if (prob > Math.log(delta)) return Label.SPAM;
		return Label.HAM;
	}
	
	public static int labelToInt(Label label) {
		if (label.equals(Label.SPAM)) return 1;
		else return 0;
	}
	
	public double evaluate(Set<String> featureSet, double delta, double lambda) {
		this.delta = delta;
		this.lambda = lambda;
		
		double correct = 0;
		double falsePos = 0;
		double falseNeg = 0;
		for (String file : testSet) {
			Label label = getLabel(file);
			Label prediction = classify(file, featureSet);
			System.out.println(file + " " + labelToInt(prediction));
			
			if (label.equals(Label.HAM)) {
				if (prediction.equals(Label.SPAM)) falsePos++;
				else correct++;
			}
			// the example is spam
			else {
				if (prediction.equals(Label.HAM)) falseNeg++; 
				else correct++;
			}			
		}
		
		System.out.println("delta: " + delta + ", lambda: " + lambda + ", overall accuracy: " + correct/testSet.size());
		System.out.println("false positives: " + falsePos/testSet.size());
		System.out.println("false negatives: " + falseNeg/testSet.size());
		
		return correct/testSet.size();
	}
	
}
