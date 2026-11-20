/*  Student information for assignment:
 *
 *  On OUR honor, Anay and Sid, this programming assignment is OUR own work
 *  and WE have not provided this code to any other student.
 *
 *  Number of slip days used: 0
 *
 *  Student 1 (Student whose Canvas account is being used)
 *  UTEID: aa227842
 *  email address: aa227842@eid.utexas.edu
 *  Grader name: Parker
 *
 *  Student 2
 *  UTEID: st42435
 *  email address: st42435@my.utexas.edu
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class SimpleHuffProcessor implements IHuffProcessor {

    private IHuffViewer myViewer;
    private int[] freq;
    private TreeNode root;
    private String[] sequences;
    private int headerFormat;
    private int leaveCount;
    private static final int HEADER_INTS = 2;

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
        root = constructTree(freq);
        sequences = new String[ALPH_SIZE + 1];
        constructSequences("", root);
        return totalBits - cntBits(headerFormat);
    }

    /*
    pre: headerFormat == STORE_COUNTS or STORE_TREE
    post: Returns the total number of bits in the compressed file
     */
    private int cntBits(int headerFormat) throws IOException {
        int compressedBits = 0;
        for (int i = 0; i < freq.length; i++) {
            if (freq[i] != 0) {
                compressedBits += (freq[i] * sequences[i].length());
            }
        }
        leaveCount=0;
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
        } else {
            throw new IOException("Invalid headerFormat type");
        }
        // magic number and header format ints
        compressedBits += 2 * BITS_PER_INT;
        this.headerFormat = headerFormat;
        return compressedBits;
    }

    /*
    pre: sequences != null, curSeq != null;
    post: sequences is filled by every leaf node in curNode's subtree's bit values
     */
    private void constructSequences(String curSeq, TreeNode curNode) {
        if (sequences == null || curSeq == null) {
            throw new IllegalArgumentException("Sequences and curSeq can't be null");
        }
        if (curNode != null) {
            if (curNode.isLeaf()) {
                sequences[curNode.getValue()] = curSeq;
            } else {
                constructSequences(curSeq + "0", curNode.getLeft());
                constructSequences(curSeq + "1", curNode.getRight());
            }
        }
    }

    /*
    pre: freq != null, freq.length >= ALPH_SIZE + 1
    post: Returns a root of a new tree constructed from the frequency array
     */
    private TreeNode constructTree(int[] freq) {
        if (freq == null || freq.length < ALPH_SIZE + 1) {
            throw new IllegalArgumentException("Freq is not formatted properly");
        }
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
     *              If this is false do not create the output file if it is 
     *              larger than the input file.
     * @return the number of bits written.
     * @throws IOException if an error occurs while reading from the input file or
     *                     writing to the output file.
     */
    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
        BitInputStream curInput = new BitInputStream(in);
        BitOutputStream curOutput = new BitOutputStream(out);
        int curWord = curInput.readBits(BITS_PER_WORD);
        int totBitsWritten=writeBits(curOutput);
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
    
    /*
    pre: curOutput != null
    post: Returns num of bits written to out
     */
    private int writeBits(BitOutputStream curOutput){
        if (curOutput == null) {
            throw new IllegalArgumentException("curOutput can't be null");
        }
        int totBitsWritten = HEADER_INTS * BITS_PER_INT;
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
        return totBitsWritten;
    }

    /*
    pre: curNode != null, curOutput != null
    post: Writes the tree in preorder in reference to curNode
     */
    private void treeOutput(TreeNode curNode, BitOutputStream curOutput) {
        if (curNode == null || curOutput == null) {
            throw new IllegalArgumentException("curNode and curOutput cannot be null");
        }
        if (!curNode.isLeaf()) {
            curOutput.writeBits(1, 0);
            treeOutput(curNode.getLeft(), curOutput);
            treeOutput(curNode.getRight(), curOutput);
        } else {
            curOutput.writeBits(1, 1);
            curOutput.writeBits(BITS_PER_WORD + 1, curNode.getValue());
        }
    }

    /*
    pre: curInput != null
    post: Returns the root of a tree constructed by reading in the input bits
     */
    private TreeNode reverseTreeOutput(BitInputStream curInput) throws IOException {
        if (curInput == null) {
            throw new IllegalArgumentException("curInput cannot be null");
        }
        int bit = curInput.readBits(1);
        if (bit == 0) {
            TreeNode left = reverseTreeOutput(curInput);
            TreeNode right = reverseTreeOutput(curInput);
            return new TreeNode(left, PSEUDO_EOF + 1, right);
        } else if (bit == 1) {
            return new TreeNode(curInput.readBits(BITS_PER_WORD + 1), 0);
        } else {
            throw new IOException("Input stream out of bits");
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
        int supposedMagicNum = curInput.readBits(BITS_PER_INT);
        if (supposedMagicNum != MAGIC_NUMBER) {
            throw new IOException("Unrecognized magic number value");
        }
        int headerType = curInput.readBits(BITS_PER_INT);
        TreeNode tempRoot;
        if (headerType == STORE_COUNTS) {
            int[] tempFreq = new int[ALPH_SIZE + 1];
            for (int i = 0; i < ALPH_SIZE; i++) {
                tempFreq[i] = curInput.readBits(BITS_PER_INT);
            }
            tempFreq[PSEUDO_EOF] = 1;
            tempRoot = constructTree(tempFreq);
        } else if (headerType == STORE_TREE) {
            curInput.readBits(BITS_PER_INT);
            tempRoot = reverseTreeOutput(curInput);
        } else {
            throw new IOException("Unrecognized header type");
        }
        int totBitsWritten = uncompressTraversal(tempRoot, curInput, curOutput);
        curInput.close();
        curOutput.close();
        return totBitsWritten;
    }

    /*
    pre: root != null, in != null, out != null
    post: Returns the number of bits written to out
     */
    private int uncompressTraversal(TreeNode root, BitInputStream in, BitOutputStream out) 
            throws IOException {
        if (root == null || in == null || out == null) {
            throw new IllegalArgumentException("root, in, and out cannot be null");
        }
        int res = 0;
        TreeNode curr = root;
        while (curr.getValue() != PSEUDO_EOF) {
            int bit = in.readBits(1);
            if (bit == -1) {
                throw new IOException("Input stream ran out of bits");
            }
            curr = (bit == 0) ? curr.getLeft() : curr.getRight();
            if (curr.isLeaf()) {
                if (curr.getValue() != PSEUDO_EOF) {
                    out.writeBits(BITS_PER_WORD, curr.getValue());
                    res += BITS_PER_WORD;
                    curr = root;
                }
            }
        }
        return res;
    }

    /*
    pre: None
    post: myViewer is set to viewer
     */
    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    /*
    pre: None
    post: Displays the string S
     */
    private void showString(String s) {
        if (myViewer != null) {
            myViewer.update(s);
        }
    }
}
