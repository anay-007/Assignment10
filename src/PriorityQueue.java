import java.util.ArrayList;

public class PriorityQueue {
    private ArrayList<TreeNode> nums;

    /*
    pre: None
    post: Makes a new priority queue
     */
    public PriorityQueue() {
        nums = new ArrayList<>();
    }

    /*
    pre: curNode != null
    post: Adds a new node to the PQ
     */
    public void add(TreeNode curNode) {
        if (curNode == null) {
            throw new IllegalArgumentException("curNode can't be null");
        }
        int j = nums.size() - 1;
        while (j >= 0 && nums.get(j).compareTo(curNode) <= 0) {
            j--;
        }
        nums.add(++j, curNode);
    }

    /*
    pre: size() > 0
    post: Removes and returns the last node w/ the smallest freq
     */
    public TreeNode remove() {
        if (size() <= 0) {
            throw new IllegalArgumentException("size can't be <= 0");
        }
        return nums.remove(nums.size() - 1);
    }

    /*
    pre: None
    post: Returns the PQ's size
     */
    public int size() {
        return nums.size();
    }
}
