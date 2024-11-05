/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package phase1;

import gr.uoc.csd.hy463.NXMLFileReader;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Set;
import mitos.stemmer.Stemmer;
import java.util.TreeMap;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.*;
import java.util.*;

public class Phase1 {

    // Define a class to represent documents
    static class Document {
        String id;
        String filePath;
        double vectorLength; 

        public Document(String id, String filePath, double vectorLength) {
            this.id = id;
            this.filePath = filePath;
            this.vectorLength = vectorLength;
        }
    }

public static void main(String[] args) {
    // Scan for the name of the folder to be read
    Scanner scanner = new Scanner(System.in);    
    System.out.print("Enter folder name: ");   
    String userInput = scanner.nextLine();
    
    File folder = new File(userInput);
    // Create a map to hold word counts across all files
    Map<String, Map<String, Map<String, Integer>>> wordCounts = new HashMap<>();
    // Create a set to store unique words
    Set<String> uniqueWords = new HashSet<>();
    // Create a map to store word locations
    Map<String, Map<String, List<Integer>>> wordLocations = new HashMap<>();
    // Create a list to store documents
    List<Document> documents = new ArrayList<>();
    // Read files and process them
    readFiles(folder, wordCounts, wordLocations, uniqueWords, documents);
    // Write documents information to DocumentsFile.txt
    writeDocumentsToFile(documents);
    
    
}

   public static void readFiles(File folder, Map<String, Map<String, Map<String, Integer>>> wordCounts,
                                 Map<String, Map<String, List<Integer>>> wordLocations, Set<String> uniqueWords,
                                 List<Document> documents) {
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                readFiles(fileEntry, wordCounts, wordLocations, uniqueWords, documents);
            } else {
                try {
                    // Process each file and update word counts, unique words, and documents
                    Document doc = processFile(fileEntry, wordCounts, wordLocations, uniqueWords, documents);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Print the total number of unique words
        System.out.println("Total unique words: " + uniqueWords.size());

        // Write the word counts to VocabularyFile.txt and PostingFile.txt
        writeVocabularyToFile(wordCounts, wordLocations);
    }

   public static Document processFile(File file, Map<String, Map<String, Map<String, Integer>>> wordCounts,
                                   Map<String, Map<String, List<Integer>>> wordLocations, Set<String> uniqueWords,
                                   List<Document> documents) throws IOException {
    // Load English stopwords
    Set<String> stopwordsEn = loadStopwords("resources/stopwordsEn.txt");
    // Load Greek stopwords
    Set<String> stopwordsGr = loadStopwords("resources/stopwordsGr.txt");

    // Initialize NXMLFileReader
    NXMLFileReader xmlFile = new NXMLFileReader(file);

    // Extract text from NXMLFileReader
    String body = xmlFile.getBody();
    String title = xmlFile.getTitle();
    String abstr = xmlFile.getAbstr();
    String journal = xmlFile.getJournal();
    String publisher = xmlFile.getPublisher();
    ArrayList<String> authors = xmlFile.getAuthors();
    HashSet<String> categories =xmlFile.getCategories();
    // Create strings with contents of nxml file
    String filetext = body+" "+title+" "+abstr+" "+journal+" "+publisher;
    String authortext = " ";
    String categoriestext = " ";
    for (int i = 0; i < authors.size(); i++) {
            String element = authors.get(i);
            filetext = filetext +" "+ element;
            authortext = authortext +" "+ element;
    }
    for (String element : categories) {
            filetext = filetext +" "+ element;
            categoriestext = categoriestext +" "+ element;
    }
    
    String filePath = file.getAbsolutePath();
    String id = file.getName();
    // Process text and update word counts
    countWords(body, id, "Body", wordCounts, wordLocations, stopwordsEn, stopwordsGr, uniqueWords);
    countWords(title, id, "Title", wordCounts, wordLocations, stopwordsEn, stopwordsGr, uniqueWords);
    countWords(abstr, id, "Abstr", wordCounts, wordLocations, stopwordsEn, stopwordsGr, uniqueWords);
    countWords(journal, id, "Journal", wordCounts, wordLocations, stopwordsEn, stopwordsGr, uniqueWords);
    countWords(publisher, id, "Publisher", wordCounts, wordLocations, stopwordsEn, stopwordsGr, uniqueWords);
    countWords(authortext, id, "Authors", wordCounts, wordLocations, stopwordsEn, stopwordsGr, uniqueWords);
    countWords(categoriestext, id, "Categories", wordCounts, wordLocations, stopwordsEn, stopwordsGr, uniqueWords);


    // Create and return Document object
    Document doc = new Document(id, filePath, calculateVectorLength(filetext, stopwordsEn, stopwordsGr, uniqueWords));
    documents.add(doc);
    return doc;
}


    public static void countWords(String text, String filename, String tag, Map<String, Map<String, Map<String, Integer>>> wordCounts,
                                  Map<String, Map<String, List<Integer>>> wordLocations,
                                  Set<String> stopwordsEn, Set<String> stopwordsGr, Set<String> uniqueWords) {
        if (text != null) {
            // Split text into words using only letters
            Pattern pattern = Pattern.compile("\\b[a-zA-Z]+\\b");
            Matcher matcher = pattern.matcher(text.toLowerCase());
            int position = 0;
            while (matcher.find()) {
                String word = matcher.group();
                // Apply stemming to normalize the word
                String stemmedWord = stemWord(word);
                // Skip stopwords
                if (!stopwordsEn.contains(stemmedWord) && !stopwordsGr.contains(stemmedWord)) {
                    // Update word counts with tag and filename information
                    wordCounts.computeIfAbsent(stemmedWord, k -> new HashMap<>())
                            .computeIfAbsent(filename, k -> new HashMap<>())
                            .merge(tag, 1, Integer::sum);
                    // Update word locations
                    wordLocations.computeIfAbsent(stemmedWord, k -> new HashMap<>())
                            .computeIfAbsent(filename, k -> new ArrayList<>())
                            .add(position);
                    // Add word to unique words set
                    uniqueWords.add(stemmedWord);
                }
                position = matcher.end();
            }
        }
    }

public static void writeVocabularyToFile(Map<String, Map<String, Map<String, Integer>>> wordCounts,
                                         Map<String, Map<String, List<Integer>>> wordLocations) {
    // Create a TreeMap to store word counts in ascending order
    TreeMap<String, Map<String, Map<String, Integer>>> sortedWordCounts = new TreeMap<>(wordCounts);
    String namefile="";
    int numofwords=0;
    // Create the CollectionIndex folder if it doesn't exist
    File collectionIndexFolder = new File("CollectionIndex");
    if (!collectionIndexFolder.exists()) {
        collectionIndexFolder.mkdir();
    }

    // Create the VocabularyFile.txt within the CollectionIndex folder
    File vocabularyFile = new File(collectionIndexFolder, "VocabularyFile.txt");

    // Create the PostingFile.txt within the CollectionIndex folder
    File postingFile = new File(collectionIndexFolder, "PostingFile.txt");

    try (BufferedWriter vocabularyWriter = new BufferedWriter(new FileWriter(vocabularyFile));
         BufferedWriter postingWriter = new BufferedWriter(new FileWriter(postingFile))) {
        // Write the word counts information into the VocabularyFile.txt and PostingFile.txt
        for (String word : sortedWordCounts.keySet()) {
            vocabularyWriter.write("Word: " + word + "\n");
            postingWriter.write("Word: " + word + "\n");
            Map<String, Map<String, Integer>> fileCounts = sortedWordCounts.get(word);
            int totalTermFrequency = 0; 
            for (String filename : fileCounts.keySet()) {
                if(!namefile.equals(filename)){
                    namefile = filename;
                    numofwords = countMostFrequentWord("0",namefile);
                }
                vocabularyWriter.write("\tFilename: " + filename + "\n");
                postingWriter.write("\tFilename: " + filename + "\n");
                Map<String, Integer> tagCounts = fileCounts.get(filename);
                int termFrequency = tagCounts.values().stream().mapToInt(Integer::intValue).sum(); 
                totalTermFrequency += termFrequency;
                for (String tag : tagCounts.keySet()) {
                    int count = tagCounts.get(tag);
                    if (count > 0) {
                        vocabularyWriter.write("\t\tTag: " + tag + ", Count: " + count + "\n");
                        postingWriter.write("\t\tTag: " + tag + ", Count: " + count + "\n");
                    }
                }
                List<Integer> locations = wordLocations.get(word).get(filename);
                postingWriter.write("\t\tLocations: " + locations.toString() + "\n");
                            // Write total term frequency for the word
                double tf = totalTermFrequency/(double)numofwords;
                postingWriter.write("\t\tTotal Term Frequency: " + tf + "\n");
            }

        }
        System.out.println("VocabularyFile.txt and PostingFile.txt have been created successfully." );
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    public static int countMostFrequentWord(String folderPath, String filename) {
        Map<String, Integer> wordFrequencyMap = new HashMap<>();

        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().equals(filename)) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        Pattern pattern = Pattern.compile("\\b[a-zA-Z]+\\b"); // Word boundary regex pattern
                        String line;
                        while ((line = reader.readLine()) != null) {
                            Matcher matcher = pattern.matcher(line);
                            while (matcher.find()) {
                                String word = matcher.group();
                                // Update the word frequency map
                                wordFrequencyMap.put(word, wordFrequencyMap.getOrDefault(word, 0) + 1);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break; // Exit loop once the file is found and processed
                }
            }
        } else {
            System.out.println("Folder is empty or does not exist.");
        }

        // Find the word with the maximum frequency
        int maxFrequency = 0;
        String mostFrequentWord = null;
        for (Map.Entry<String, Integer> entry : wordFrequencyMap.entrySet()) {
            if (entry.getValue() > maxFrequency) {
                maxFrequency = entry.getValue();
                mostFrequentWord = entry.getKey();
            }
        }

        // Return the number of times the most frequent word appeared
        return maxFrequency;
    }
    
    public static Set<String> loadStopwords(String filename) {
        Set<String> stopwords = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stopwords.add(line.trim().toLowerCase());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stopwords;
    }

    private static String stemWord(String word) {
        Stemmer.Initialize();
        return Stemmer.Stem(word);
    }
    
     public static void writeDocumentsToFile(List<Document> documents) {
        // Create the CollectionIndex folder if it doesn't exist
        File collectionIndexFolder = new File("CollectionIndex");
        if (!collectionIndexFolder.exists()) {
            collectionIndexFolder.mkdir();
        }

        // Create the DocumentsFile.txt within the CollectionIndex folder
        File documentsFile = new File(collectionIndexFolder, "DocumentsFile.txt");

        try (PrintWriter writer = new PrintWriter(new FileWriter(documentsFile))) {
            // Write document information to the file
            for (Document doc : documents) {
                writer.println("ID: " + doc.id);
                writer.println("File Path: " + doc.filePath);
                writer.println("Vector Length: " + doc.vectorLength);
                writer.println();
            }
            System.out.println("DocumentsFile.txt has been created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }   
     
        public static double calculateVectorLength(String text, Set<String> stopwordsEn, Set<String> stopwordsGr, Set<String> uniqueWords) {
        // Split text into words using only letters
        Pattern pattern = Pattern.compile("\\b[a-zA-Z]+\\b");
        Matcher matcher = pattern.matcher(text.toLowerCase());
        double sumOfSquares = 0.0;
        while (matcher.find()) {
            String word = matcher.group();
            // Apply stemming to normalize the word
            String stemmedWord = stemWord(word);
            // Skip stopwords
            if (!stopwordsEn.contains(stemmedWord) && !stopwordsGr.contains(stemmedWord)) {
                // Increment sum of squares for non-stopwords
                sumOfSquares += 1; // Assuming each word has a weight of 1 for simplicity
                // Add word to unique words set
                uniqueWords.add(stemmedWord);
            }
        }
        // Return square root of sum of squares
        return Math.sqrt(sumOfSquares);
    }
}