import java.util.ArrayList;

public class PriorityQueue {
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
