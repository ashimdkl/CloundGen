import java.io.Serializable;
import java.util.Comparator;

import components.map.Map;
import components.map.Map1L;
import components.set.Set;
import components.set.Set1L;
import components.simplereader.SimpleReader;
import components.simplereader.SimpleReader1L;
import components.simplewriter.SimpleWriter;
import components.simplewriter.SimpleWriter1L;
import components.sortingmachine.SortingMachine;
import components.sortingmachine.SortingMachine1L;

/**
 * generate a "tag cloud" from a text file, output into html.
 *
 *
 * @author Szcheng Chen, Ashim Dhakal
 */
public final class CloudGenerator {

    /**
     * string containing characters considered as word separators.
     */
    private static final String SEPARATORS = " \t, \n\r,.<>/?;:\"'{}[]_-+=~`!@#$%^&*()|";

    /**
     * maximum font size to be used in the tag cloud.
     */
    private static final int FMAX = 48;

    /**
     * minimum font size to be used in the tag cloud. represents the font size
     * for the least frequently occurring words that are still included in the
     * cloud.
     */
    private static final int FMIN = 11;;

    /**
     * private constructor to prevent instantiation.
     */
    private CloudGenerator() {
    }

    /**
     * processes the input text to count the frequency of each word.
     *
     * @param input
     *            the {SimpleReader} object for reading the input text.
     * @return A {Map} from word strings to their frequencies.
     */
    private static Map<String, Integer> processText(SimpleReader input) {

        // make a new map for the terms, and a new set storing the separator.
        Map<String, Integer> terms = new Map1L<>();
        Set<Character> separator = new Set1L<>();

        // store the separators into this set component.
        for (char ch : SEPARATORS.toCharArray()) {
            if (!separator.contains(ch)) {
                separator.add(ch);
            }
        }

        // the input can't be empty, check
        while (!input.atEOS()) {

            String temporaryLine = input.nextLine();
            int position = 0;

            // we need to go from the beg of the line to the end [x_____y]
            while (position < temporaryLine.length()) {

                /*
                 * we can call our nextElement function to see if its a
                 * separator, and if it is, we can just move on.
                 */
                String element = nextElement(temporaryLine, position,
                        separator);

                position = position + element.length();

                // check if the first character of the element is not a separator

                if (!separator.contains(element.charAt(0))) {

                    /*
                     * convert the element to lower case to standardize the word
                     * so we can use it for counting
                     */
                    String word = element.toLowerCase();

                    // check if the word is already in the map (terms)
                    if (terms.hasKey(word)) {
                        // if the word exists, get its current count
                        int currentCount = terms.value(word);
                        // increment the word's count by 1
                        terms.replaceValue(word, currentCount + 1);
                    } else {
                        // if the word does not exist in the map, add it.
                        terms.add(word, 1);
                    }
                }

            }
        }
        return terms;
    }

    /**
     * Extracts the next word or separator string from the given position in
     * text.
     *
     * @param text
     *            the text to scan.
     * @param start
     *            the starting index.
     * @param separators
     *            the set of characters considered as separators.
     * @return The next word or separator string.
     */
    private static String nextElement(String text, int start,
            Set<Character> separators) {
        int tempPositionVal = start;
        String valueToReturn;

        // check if the start position is after end of the text
        if (tempPositionVal >= text.length()) {
            valueToReturn = "";
        } else {

            // create a StringBuilder to hold hold characters of next element
            StringBuilder element = new StringBuilder();

            // check to see if the character at the start index is a separator
            boolean isSeparator = separators
                    .contains(text.charAt(tempPositionVal));

            int i = tempPositionVal;

            while (i < text.length()) {
                // fetch the current character once per loop iteration
                char currentChar = text.charAt(i);

                /*
                 * break the loop if the current character's separator status
                 * doesn't match the initial character's
                 */

                if (separators.contains(currentChar) != isSeparator) {
                    break;
                }

                // append the current character to 'element'
                element.append(currentChar);

                // move to the next character
                i++;
            }

            // return the accumulated string in 'element'
            valueToReturn = element.toString();
        }
        return valueToReturn;
    }

    /**
     * Comparator class for sorting map entries by their value in descending
     * order.
     */
    private static class IntegerDescendingComparator
            implements Comparator<Map.Pair<String, Integer>>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(Map.Pair<String, Integer> o1,
                Map.Pair<String, Integer> o2) {
            return o2.value().compareTo(o1.value());
        }
    }

    /**
     * Comparator class for sorting map entries by their key alphabetically.
     */
    private static class StringAlphabeticalComparator
            implements Comparator<Map.Pair<String, Integer>>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(Map.Pair<String, Integer> o1,
                Map.Pair<String, Integer> o2) {
            return o1.key().compareTo(o2.key());
        }
    }

    /**
     * Sorts the occurrences map by their values (frequencies) in descending
     * order.
     *
     * @param occurrences
     *            the map containing word occurrences to sort.
     * @return A {SortingMachine} sorted by word count in descending order.
     */
    private static SortingMachine<Map.Pair<String, Integer>> numSorting(
            Map<String, Integer> occurrences) {
        IntegerDescendingComparator comparator = new IntegerDescendingComparator();
        SortingMachine<Map.Pair<String, Integer>> sm = new SortingMachine1L<>(
                comparator);
        Map<String, Integer> temp = occurrences.newInstance();

        while (occurrences.size() > 0) {
            Map.Pair<String, Integer> pair = occurrences.removeAny();
            sm.add(pair);
            temp.add(pair.key(), pair.value());
        }

        occurrences.transferFrom(temp);
        sm.changeToExtractionMode();

        return sm;
    }

    /**
     * Converts a SortingMachine sorted by numerical order into one sorted
     * alphabetically.
     *
     * @param sortedByCount
     *            the {SortingMachine} sorted by numerical order.
     * @param number
     *            the number of terms to be showed in final result.
     * @return A {SortingMachine} sorted alphabetically.
     */
    private static SortingMachine<Map.Pair<String, Integer>> alphaSorting(
            SortingMachine<Map.Pair<String, Integer>> sortedByCount,
            int number) {
        StringAlphabeticalComparator comparator = new StringAlphabeticalComparator();
        SortingMachine<Map.Pair<String, Integer>> sm = new SortingMachine1L<>(
                comparator);

        for (int i = 0; i < number; i++) {
            Map.Pair<String, Integer> pair = sortedByCount.removeFirst();
            sm.add(pair);
        }

        sm.changeToExtractionMode();
        return sm;
    }

    /**
     * Generates the output HTML file displaying the word cloud.
     *
     * @param sortedTerms
     *            the {@link SortingMachine} containing sorted terms to display.
     * @param number
     *            the number of words to include in the output.
     * @param out
     *            the {@link SimpleWriter} for outputting the HTML file.
     * @param inputFile
     *            the name of the input file to reference in the output.
     */
    private static void generateOutput(
            SortingMachine<Map.Pair<String, Integer>> sortedTerms, int number,
            SimpleWriter out, String inputFile) {

        out.print("<html>\r\n" + "<head>\r\n" + "<title>Top " + number
                + " words in " + inputFile + "</title>\r\n"
                + "<link href=\"http://web.cse.ohio-state.edu/software/2231/"
                + "web-sw2/assignments/projects/tag-cloud-generator/data/tagcloud.css\""
                + " rel=\"stylesheet\" type=\"text/css\">\r\n"
                + "<link href=\"tagcloud.css\" rel=\"stylesheet\" type=\"text/css\">\r\n"
                + "</head>\r\n" + "<body>\r\n" + "<h2>Top " + number
                + " words in " + inputFile + "</h2>\r\n" + "<hr>\r\n"
                + "<div class=\"cdiv\">\r\n" + "<p class=\"cbox\">");

        // Calculate font size scaling factors
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (Map.Pair<String, Integer> term : sortedTerms) {
            int count = term.value();
            if (count > max) {
                max = count;
            }
            if (count < min) {
                min = count;
            }
        }
        double scale = (double) (FMAX - FMIN) / (max - min);

        // Generate tag cloud content
        while (sortedTerms.size() > 0) {
            Map.Pair<String, Integer> term = sortedTerms.removeFirst();
            int fontSize = (int) ((term.value() - min) * scale) + FMIN;
            out.println("<span style=\"cursor:default\" class=\"f" + fontSize
                    + "\" title=\"count: " + term.value() + "\">" + term.key()
                    + "</span> ");
        }

        // Close HTML document
        out.println("</p>\r\n" + "</div>\r\n" + "</body>\r\n" + "</html>");
    }

    /**
     * Main method to run the cloud generator program.
     *
     * @param args
     *            command line arguments (unused).
     */
    public static void main(String[] args) {
        SimpleWriter out = new SimpleWriter1L();
        SimpleReader in = new SimpleReader1L();

        out.print("write name of your input file: ");
        String nameIn = in.nextLine();
        SimpleReader input = new SimpleReader1L(nameIn);

        out.print("write name of your output file: ");
        String nameOut = in.nextLine();

        SimpleWriter output = new SimpleWriter1L(nameOut);

        out.print("how many words do you want to display?: ");
        String integer = in.nextLine();
        int number = Integer.parseInt(integer);

        if (!input.atEOS()) {
            Map<String, Integer> words = processText(input);
            SortingMachine<Map.Pair<String, Integer>> sortedByCount = numSorting(
                    words);
            SortingMachine<Map.Pair<String, Integer>> sortedAlphabetically = alphaSorting(
                    sortedByCount, number);
            generateOutput(sortedAlphabetically, number, output, nameIn);

        } else {
            out.println("non-valid file or empty file");
        }

        input.close();
        output.close();
        out.close();
        in.close();
    }
}
