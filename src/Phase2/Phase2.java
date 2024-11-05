/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Phase2;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import mitos.stemmer.Stemmer;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import gr.uoc.csd.hy463.Topic; 
import gr.uoc.csd.hy463.TopicsReader; 
import java.util.ArrayList; 
/**
 *
 * @author Nick
 */
public class Phase2 {
        
    public static void main(String[] args) throws Exception {
        Set<String> stopwordsEn = loadStopwords("resources/stopwordsEn.txt");
        Scanner scanner = new Scanner(System.in); 
        System.out.print("For asking a custom question type 1 .\nFor running a search based on provided topics type 2\n ");   
        String choice = scanner.nextLine();
        String[] words;
        String userInput;
        String word ; 
        int numoftopic;
        int flag = 1;
        if(choice.compareTo("1")==0 ){
            System.out.print("Enter question: ");   
            userInput = scanner.nextLine();
            words= userInput.split("[\\s\\t\\n]+");
        }
        else{
            ArrayList<Topic> topics = TopicsReader.readTopics("C:\\Users\\Nick\\Desktop\\463\\Project\\Phase1\\topics.xml"); 
            System.out.print("Type the number of the topic\n");   
            userInput = scanner.nextLine();
            numoftopic = Integer.parseInt(userInput);
            if(numoftopic <= 10){
                word = "diagnosis";
            }
            else if(numoftopic>10 && numoftopic <= 20){
                word = "test";
            }
            else {
                word = "treatment";
            }
            while(flag!=0){
                System.out.print("Type description or summary\n");   
                userInput = scanner.nextLine();
                if(userInput.compareTo("summary")==0){
                    word =word + " " + topics.get(numoftopic).getSummary();
                    flag = 0 ; 
                }
                else if(userInput.compareTo("description")==0){
                    word =word + " " + topics.get(numoftopic).getDescription();
                    flag = 0 ;
                }
            }
            words= word.split("[\\s\\t\\n]+");
        }
       
        String[] filteredWordsEn = filterStopwords(words, stopwordsEn);
        
        for (int i = 0; i < filteredWordsEn.length; i++) {
            filteredWordsEn[i] = stemWord(filteredWordsEn[i]);
        }
        Map<String, Map<String, Double>>wordfilenames =  findWordsInFile(filteredWordsEn);
       

        
        Map<String, Double> idfMap = calculateIDF(wordfilenames, 54);
        

        
        Map<String, Map<String, Double>> tfidfMap = calculateTFIDF(wordfilenames, idfMap);


        
        Map<String, Double> queryvector=calculateQTFIDF(filteredWordsEn,idfMap);
        Map<String, Double> fileCosineSimilarityMap = new HashMap<>();
        Set<String> uniqueFiles =  uniqueStringsInInnerMap(tfidfMap);
        for (String file : uniqueFiles){
            Map<String, Double> Docvector = findMatchingStrings(file,tfidfMap);
            double[] queryValues = findQueryVectorValues(Docvector,queryvector);
            double[] docValues = findDocVectorValues(Docvector);
            double cosSim = calculateCosineSimilarity(queryValues,docValues);
            fileCosineSimilarityMap.put(file, cosSim);          
        }
        
        List<Map.Entry<String, Double>> list = new LinkedList<>(fileCosineSimilarityMap.entrySet());

        // Sort the list based on the values
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o2.getValue().compareTo(o1.getValue()); // Descending order
            }
        });

        // Create a new LinkedHashMap to preserve the insertion order
        Map<String, Double> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        System.out.println();
        System.out.println("Relevant Documents presented in descending order :");
        System.out.println();
       
        for (Map.Entry<String, Double> entry : sortedMap.entrySet()) {
            String file = entry.getKey();
            Double cosSim = entry.getValue();
            String filepath = findFilePathByID(file);
            System.out.println("For document " + file + " cosine similarity calculated is : " + cosSim + " and the file's path is : " + filepath);
        }
        
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
    
    public static String[] filterStopwords(String[] words, Set<String> stopwords) {
        String[] filteredWords = new String[words.length];
        int index = 0;
        
        // Iterate over the array of words
        for (String word : words) {
            // If the word is not a stopword, add it to the filtered array
            if (!stopwords.contains(word)) {
                filteredWords[index++] = word;
            }
        }
        
        // Resize the array to remove any null entries
        return Arrays.copyOf(filteredWords, index);
    }

    private static String stemWord(String word) {
        Stemmer.Initialize();
        return Stemmer.Stem(word);
    }
    
   public static Map<String, Map<String, Double>> findWordsInFile(String[] words) {
        Map<String, Map<String, Double>> wordToFileMap = new HashMap<>();
        Path filePath = Paths.get("CollectionIndex", "PostingFile.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toString()))) {
            String line;
            String currentWord = null;
            Map<String, Double> currentFilenamesAndFrequencies = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Word: ")) {
                    // If a new word is encountered, add the previous word and filenames (if applicable) to the map
                    if (currentWord != null && containsIgnoreCase(words, currentWord)) {
                        wordToFileMap.put(currentWord, new HashMap<>(currentFilenamesAndFrequencies));
                    }
                    // Extract the word from the line
                    currentWord = line.substring(6).trim();
                    currentFilenamesAndFrequencies = new HashMap<>();
                } else if (line.startsWith("\tFilename: ")) {
                    // Extract the filename from the line
                    String filename = line.substring(11).trim();
                    double frequency = 0.0;
                    // Continue reading lines until we find the line with the frequency
                    while ((line = reader.readLine()) != null && !line.startsWith("\t\tTotal Term Frequency: ")) {
                        // Continue reading lines until we find the line with the frequency
                    }
                    // Extract the term frequency from the line
                    frequency = Double.parseDouble(line.substring(24).trim());
                    // Add the filename and its frequency to the map
                    currentFilenamesAndFrequencies.put(filename, frequency);
                }
            }
            // Add the last word and filenames (if applicable) to the map
            if (currentWord != null && containsIgnoreCase(words, currentWord)) {
                wordToFileMap.put(currentWord, new HashMap<>(currentFilenamesAndFrequencies));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return wordToFileMap;
    }

    public static boolean containsIgnoreCase(String[] array, String key) {
        for (String element : array) {
            if (element.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }
    
    public static Map<String, Double> calculateIDF(Map<String, Map<String, Double>> wordfilenames, int totalDocuments) {
        Map<String, Double> idfMap = new HashMap<>();

        for (String word : wordfilenames.keySet()) {
            int numDocumentsContainingTerm = wordfilenames.get(word).size();
            double idf = Math.log((double) totalDocuments / (double) numDocumentsContainingTerm);
            idfMap.put(word, idf);
        }

        return idfMap;
    }
    
    public static Map<String, Map<String, Double>> calculateTFIDF(Map<String, Map<String, Double>> wordfilenames, Map<String, Double> idfMap) {
        Map<String, Map<String, Double>> tfidfMap = new HashMap<>();

        for (String word : wordfilenames.keySet()) {
            Map<String, Double> documentTFIDFMap = new HashMap<>();
            Map<String, Double> filenameTFMap = wordfilenames.get(word);
            for (String filename : filenameTFMap.keySet()) {
                double tf = filenameTFMap.get(filename);
                double idf = idfMap.get(word);
                double tfidf = tf * idf;
                documentTFIDFMap.put(filename, tfidf);
            }
            tfidfMap.put(word, documentTFIDFMap);
        }

        return tfidfMap;
    }
    
    public static Set<String> uniqueStringsInInnerMap(Map<String, Map<String, Double>> tfidfMap) {
        Set<String> uniqueStrings = new HashSet<>();

        for (Map<String, Double> innerMap : tfidfMap.values()) {
            uniqueStrings.addAll(innerMap.keySet());
        }

        return uniqueStrings;
    }
    
        public static Map<String, Double> findMatchingStrings(String uniqueWord, Map<String, Map<String, Double>> tfidfMap) {
        Map<String, Double> matchingStrings = new HashMap<>();

        for (Map.Entry<String, Map<String, Double>> entry : tfidfMap.entrySet()) {
            String outerKey = entry.getKey();
            Map<String, Double> innerMap = entry.getValue();
            Double value = innerMap.get(uniqueWord);
            if (value != null) {
                matchingStrings.put(outerKey, value);
            }
        }

        return matchingStrings;
    }
        
        
    public static Map<String, Double> calculateQTFIDF(String[] filteredWordsEn, Map<String, Double> idfMap) {
        Map<String, Double> tfidfValues = new HashMap<>();
        int totalWords = filteredWordsEn.length;

        // Count frequency of unique words
        Map<String, Integer> wordFrequency = new HashMap<>();
        for (String word : filteredWordsEn) {
            wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
        }

        // Calculate TF-IDF for each word
        for (String word : wordFrequency.keySet()) {
            int wordCount = wordFrequency.get(word);
            double tf = (double) wordCount / totalWords;
            double idf = idfMap.getOrDefault(word, 0.0);
            double tfidf = tf * idf;
            tfidfValues.put(word, tfidf);
        }

        return tfidfValues;
    }
    
    public static double[] findQueryVectorValues(Map<String, Double> docVector, Map<String, Double> queryVector) {
        List<Double> queryValues = new ArrayList<>();

        for (Map.Entry<String, Double> entry : docVector.entrySet()) {
            String word = entry.getKey();
            Double queryValue = queryVector.getOrDefault(word, 0.0);
            queryValues.add(queryValue);
        }

        double[] queryValuesArray = new double[queryValues.size()];
        for (int i = 0; i < queryValues.size(); i++) {
            queryValuesArray[i] = queryValues.get(i);
        }

        return queryValuesArray;
    }
    
    
    public static String findFilePathByID(String searchString) {
        String filePath = null;
        String fileName = "CollectionIndex/DocumentsFile.txt"; // Provide the path to your file here

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("ID: ")) {
                    String id = line.substring(4).trim();
                    if (id.equals(searchString)) {
                        // Found matching ID, now read the next line for file path
                        line = br.readLine();
                        if (line != null && line.startsWith("File Path: ")) {
                            filePath = line.substring(11).trim();
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filePath;
    }
    
    
    public static double[] findDocVectorValues(Map<String, Double> map) {
        double[] array = new double[map.size()];
        int i = 0;
        for (double value : map.values()) {
            array[i++] = value;
        }
        return array;
    }
    
    
    public static double calculateDotProduct(double[] vector1, double[] vector2) {
        double dotProduct = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
        }
        return dotProduct;
    }

    public static double calculateMagnitude(double[] vector) {
        double magnitude = 0.0;
        for (double component : vector) {
            magnitude += Math.pow(component, 2);
        }
        return Math.sqrt(magnitude);
    }
    
        public static double calculateCosineSimilarity(double[] queryVector, double[] documentVector) {
        // Ensure vectors have the same dimensions
        if (queryVector.length != documentVector.length) {
            return 0.0; // Return zero similarity for vectors with different dimensions
        }
        
        double dotProduct = calculateDotProduct(queryVector, documentVector);
        double queryMagnitude = calculateMagnitude(queryVector);
        double documentMagnitude = calculateMagnitude(documentVector);

        if (queryMagnitude == 0 || documentMagnitude == 0) {
            return 0.0; 
        }
        
        double Magnitude = Math.sqrt(queryMagnitude * documentMagnitude);

        double cosineSimilarity = dotProduct / Magnitude;

        return cosineSimilarity;
    }
}
