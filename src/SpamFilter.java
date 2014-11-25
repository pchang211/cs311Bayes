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
	final static double DELTA = .0001;
	
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
		System.out.println("Classifying 1000 random messages...");
		
		SpamFilter spamFilter = new SpamFilter();
		spamFilter.split();
		
		Set featureSet = preprocess();
		spamFilter.train(featureSet);
		
		spamFilter.evaluate(featureSet);
		
		
//		System.out.println(spamFilter.priors.get(Label.SPAM));
//		for (String s : spamFilter.trainSet) {
//			System.out.println(s);
//		}
		
        System.out.println("all messages:");
//		readFiles(allfolder);
		System.out.println();
		
		System.out.println("spam messages:");
//		readFiles(spamfolder);
		System.out.println();
		
		System.out.println("ham messages:");
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
	public void train(Set featureSet)
	{
//		System.out.println("train set size: " + trainSet.size());
		for (String file : trainSet) {
			if (!file.contains(".DS_Store")) generateCounts(file, featureSet);
		}
		
//		for (FeatureLabelPair p : counts.keySet()) {
//			System.out.println(p.feature +", " + p.label + " => " + counts.get(p));
//		}
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
//			String path = "spamdata/testing/" + file;
			scan = new Scanner(new File(path));

			/* increment priors */
			if (priors.containsKey(label)) {
				priors.put(label, priors.get(label) + 1);
			}
			else {
				priors.put(label, 1.0);
			}
			
			String current;
			while(scan.hasNext()) {
				current = scan.next();
				if (featureSet.contains(current)) {
					FeatureLabelPair pair = new FeatureLabelPair(current, label);
					
					/* increment counts */
					if (counts.containsKey(pair)) {
						counts.put(pair, counts.get(pair)+1);
					}
					else {
						counts.put(pair, 1.0);
					}
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
		double prob = 1;
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
		
		for (String f : features) {
			FeatureLabelPair pair = new FeatureLabelPair(f, Label.SPAM);
			if (counts.containsKey(pair)) {
				prob *= counts.get(pair)/priors.get(Label.SPAM);
			}
		}
		prob *= priors.get(Label.SPAM)/TRAIN_SIZE;
		
		if (prob > DELTA) {
			System.out.println(file + ", prob: " + prob + ", " + Label.SPAM);
			return Label.SPAM;
		}
		System.out.println(file + ", prob: " + prob + ", " + Label.HAM);
		return Label.HAM;
	}

	public void evaluate(Set<String> featureSet) {
		double accuracy = 0;
		double spamCount = 0;
		for (String file : testSet) {
			Label label = getLabel(file);
			if (label == classify(file, featureSet)) accuracy++;
			if (label.equals(Label.SPAM)) {
				spamCount ++;
			}
			
		}
//		System.out.println("log DELTA = " + Math.log(DELTA));
		System.out.println("accuracy: " + accuracy/testSet.size());
		System.out.println("prior probability: " + (1 - spamCount/testSet.size()));
	}
	
}
