/*  Student information for assignment:
 *
 *  On <MY|OUR> honor, <NAME1> and <NAME2), this programming assignment is <MY|OUR> own work
 *  and <I|WE> have not provided this code to any other student.
 *
 *  Number of slip days used: 0
 *
 *  Student 1 (Student whose Canvas account is being used)
 *  UTEID:
 *  email address:
 *  Grader name:
 *
 *  Student 2
 *  UTEID:
 *  email address:
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class SimpleHuffProcessor implements IHuffProcessor {

    private IHuffViewer myViewer;
    private int[] freq;
    private TreeNode root;
    private String[] sequences;
    private int headerFormat;
    private int leaveCount;

    /**
     * Preprocess data so that compression is possible ---
     * count characters/create tree/store state so that
     * a subsequent call to compress will work. The InputStream
     * is <em>not</em> a BitInputStream, so wrap it int one as needed.
     *
     * @param in           is the stream which could be subsequently compressed
     * @param headerFormat a constant from IHuffProcessor that determines what kind of
     *                     header to use, standard count format, standard tree format, or
     *                     possibly some format added in the future.
     * @return number of bits saved by compression or some other measure
     * Note, to determine the number of
     * bits saved, the number of bits written includes
     * ALL bits that will be written including the
     * magic number, the header format number, the header to
     * reproduce the tree, AND the actual data.
     * @throws IOException if an error occurs while reading from the input file.
     */
    public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
        BitInputStream curInput = new BitInputStream(in);
        freq = new int[ALPH_SIZE + 1];
        freq[PSEUDO_EOF] = 1;
        int curBits = curInput.readBits(BITS_PER_WORD);
        int letterCount = 0;
        while (curBits != -1) {
            letterCount++;
            freq[curBits]++;
            curBits = curInput.readBits(BITS_PER_WORD);
        }

        int totalBits = letterCount * BITS_PER_WORD;
        root = constructTree();
        sequences = new String[ALPH_SIZE + 1];
        constructSequences("", root);
        int compressedBits = cntBits(headerFormat);
        return totalBits - compressedBits;
    }

    private int cntBits(int headerFormat) {
        int compressedBits = 0;
        for (int i = 0; i < freq.length; i++) {
            if (freq[i] != 0) {
                compressedBits += (freq[i] * sequences[i].length());
            }
        }
        for (int curFreq : freq) {
            if (curFreq != 0) {
                leaveCount++;
            }
        }
        if (headerFormat == STORE_COUNTS) {
            compressedBits += ALPH_SIZE * BITS_PER_INT;
        } else if (headerFormat == STORE_TREE) {
            int numNodes = leaveCount * 2 - 1;
            compressedBits += numNodes + BITS_PER_INT;
            int newNumBits = BITS_PER_WORD + 1;
            compressedBits += leaveCount * newNumBits;
        }
        //magic number and header format ints
        compressedBits += 2 * BITS_PER_INT;
        this.headerFormat = headerFormat;
        return compressedBits;
    }

    private void constructSequences(String curSeq, TreeNode curNode) {
        if (curNode != null) {
            if (curNode.isLeaf()) {
                sequences[curNode.getValue()] = curSeq;
            } else {
                constructSequences(curSeq + "0", curNode.getLeft());
                constructSequences(curSeq + "1", curNode.getRight());
            }
        }
    }

    private TreeNode constructTree() {
        PriorityQueue pq = new PriorityQueue();
        for (int i = 0; i < ALPH_SIZE + 1; i++) {
            if (freq[i] != 0) {
                TreeNode curNode = new TreeNode(i, freq[i]);
                pq.add(curNode);
            }
        }

        while (pq.size() > 1) {
            TreeNode firstNode = pq.remove();
            TreeNode secondNode = pq.remove();
            TreeNode curNode = new TreeNode(firstNode, PSEUDO_EOF + 1, secondNode);
            pq.add(curNode);
        }
        return pq.remove();
    }

    private static class PriorityQueue {
        private ArrayList<TreeNode> nums;

        public PriorityQueue() {
            nums = new ArrayList<>();
        }

        public void add(TreeNode curNode) {
            int j = nums.size() - 1;
            while (j >= 0 && nums.get(j).compareTo(curNode) <= 0) {
                j--;
            }
            nums.add(++j, curNode);
        }

        public TreeNode remove() {
            return nums.removeLast();
        }

        public int size() {
            return nums.size();
        }
    }

    /**
     * Compresses input to output, where the same InputStream has
     * previously been pre-processed via <code>preprocessCompress</code>
     * storing state used by this call.
     * <br> pre: <code>preprocessCompress</code> must be called before this method
     *
     * @param in    is the stream being compressed (NOT a BitInputStream)
     * @param out   is bound to a file/stream to which bits are written
     *              for the compressed file (not a BitOutputStream)
     * @param force if this is true create the output file even if it is larger than the input file.
     *              If this is false do not create the output file if it is larger than the input file.
     * @return the number of bits written.
     * @throws IOException if an error occurs while reading from the input file or
     *                     writing to the output file.
     */
    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
        BitInputStream curInput = new BitInputStream(in);
        BitOutputStream curOutput = new BitOutputStream(out);
        int totBitsWritten = 0;
        totBitsWritten += 2 * BITS_PER_INT;
        curOutput.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        curOutput.writeBits(BITS_PER_INT, headerFormat);
        if (headerFormat == STORE_COUNTS) {
            for (int i = 0; i < ALPH_SIZE; i++) {
                curOutput.writeBits(BITS_PER_INT, freq[i]);
            }
            totBitsWritten += ALPH_SIZE * BITS_PER_INT;
        } else if (headerFormat == STORE_TREE) {
            int totTreeBits = (2 * leaveCount - 1) + leaveCount * (BITS_PER_WORD + 1);
            curOutput.writeBits(BITS_PER_INT, totTreeBits);
            treeOutput(root, curOutput);
            totBitsWritten += BITS_PER_INT + totTreeBits;
        }
        int curWord = curInput.readBits(BITS_PER_WORD);
        while (curWord != -1) {
            totBitsWritten += sequences[curWord].length();
            for (int i = 0; i < sequences[curWord].length(); i++) {
                curOutput.writeBits(1, (sequences[curWord].charAt(i) == '1' ? 1 : 0));
            }
            curWord = curInput.readBits(BITS_PER_WORD);
        }
        for (int i = 0; i < sequences[PSEUDO_EOF].length(); i++) {
            curOutput.writeBits(1, (sequences[PSEUDO_EOF].charAt(i) == '1' ? 1 : 0));
        }
        totBitsWritten += sequences[PSEUDO_EOF].length();

        curOutput.close();
        return totBitsWritten;
    }


    private void treeOutput(TreeNode curNode, BitOutputStream curOutput) {
        if (!curNode.isLeaf()) {
            curOutput.writeBits(1, 0);
            treeOutput(curNode.getLeft(), curOutput);
            treeOutput(curNode.getRight(), curOutput);
        } else {
            curOutput.writeBits(1, 1);
            curOutput.writeBits(BITS_PER_WORD + 1, curNode.getValue());
        }
    }

    /**
     * Uncompress a previously compressed stream in, writing the
     * uncompressed bits/data to out.
     *
     * @param in  is the previously compressed data (not a BitInputStream)
     * @param out is the uncompressed file/stream
     * @return the number of bits written to the uncompressed file/stream
     * @throws IOException if an error occurs while reading from the input file or
     *                     writing to the output file.
     */
    public int uncompress(InputStream in, OutputStream out) throws IOException {
        BitInputStream curInput = new BitInputStream(in);
        BitOutputStream curOutput = new BitOutputStream(out);
        int curFormat = curInput.readBits(BITS_PER_INT);
        int totBitsWritten=0;


        curInput.close();
        curOutput.close();
        return totBitsWritten;
    }

    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    private void showString(String s) {
        if (myViewer != null) {
            myViewer.update(s);
        }
    }
}
