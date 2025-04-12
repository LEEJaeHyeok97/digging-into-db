import java.util.ArrayList;
import java.util.List;

/**
 * 간단한 BTree (메모리 버전으로 구현)
 * 각 노드 최대 자식은 10개로 구현
 * 실제 DB와 달리 메모리 상에서만 유지
 */
public class BPlusTree {
    private static final int ORDER = 10;
    private Node root;


    public BPlusTree() {
        root = new LeafNode();
    }

    /**
     * 검색
     */
    public String search(int key) {
        return root.search(key);
    }

    /**
     * 삽입
     */
    public void insert(int key, String value) {
        root.insert(key, value);
    }

    abstract static class Node {
        // B+Tree에서 내부 노드는 검색용 키만, 리프 노드는 키-값 페어를 저장
        // 공통적으로 '검색'과 '삽입' 메서드를 갖는다.
        abstract String search(int key);
        abstract SplitResult insert(int key, String value);
    }

    // 내부 노드 구현
    static class InternalNode extends Node {
        List<Integer> keys = new ArrayList<>();
        List<Node> children = new ArrayList<>();

        @Override
        String search(int key) {
            int i = 0;
            while (i < keys.size() && key >= keys.get(i)) {
                i++;
            }
            return children.get(i).search(key);
        }

        @Override
        SplitResult insert(int key, String value) {
            int i = 0;
            while (i < keys.size() && key >= keys.get(i)) {
                i++;
            }

            SplitResult splitResult = children.get(i).insert(key, value);

            if (splitResult != null) {
                keys.add(i, splitResult.key);
                children.set(i, splitResult.left);
                children.add(i + 1, splitResult.right);

                if (keys.size() >= ORDER) {
                    return splitInternal();
                }
            }
            return null;
        }

        private SplitResult splitInternal() {
            // 절반으로 분할
            int mid = keys.size() / 2;
            int upKey = keys.get(mid);

            InternalNode rightNode = new InternalNode();

            for (int i = mid+1; i < keys.size(); i++) {
                rightNode.keys.add(keys.get(i));
            }
            for (int i = mid+1; i < children.size(); i++) {
                rightNode.children.add(children.get(i));
            }

            // this(왼쪽)에서 mid 이후 데이터 제거
            while (keys.size() > mid) {
                keys.remove(keys.size()-1);
            }
            while (children.size() > mid+1) {
                children.remove(children.size()-1);
            }

            // SplitResult 반환
            SplitResult split = new SplitResult();
            split.key = upKey;
            split.left = this;
            split.right = rightNode;
            return split;
        }
    }


    // 리프 노드 구현
    static class LeafNode extends Node {
        List<Integer> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();


        @Override
        String search(int key) {
            for (int i = 0; i < keys.size(); i++) {
                if (keys.get(i) == key) {
                    return values.get(i);
                }
            }
            return null;
        }

        @Override
        SplitResult insert(int key, String value) {
            int i = 0;
            while (i < keys.size() && keys.get(i) < key) {
                i++;
            }
            // 동일 키 처리(학습용으로 단순 overwrite)
            if (i < keys.size() && keys.get(i) == key) {
                values.set(i, value);
                return null;
            } else {
                keys.add(i, key);
                values.add(i, value);
            }

            // 노드가 초과하면 split
            if (keys.size() >= ORDER) {
                return splitLeaf();
            }
            return null; // split 없음
        }

        private SplitResult splitLeaf() {
            int mid = keys.size() / 2;
            int upKey = keys.get(mid);

            LeafNode rightNode = new LeafNode();
            for (int i = mid; i < keys.size(); i++) {
                rightNode.keys.add(keys.get(i));
                rightNode.values.add(values.get(i));
            }

            // leftNode(this): mid 전까지 남기기
            while (keys.size() > mid) {
                keys.remove(keys.size()-1);
                values.remove(values.size()-1);
            }

            SplitResult split = new SplitResult();
            split.key = upKey; // 리프 split 시, 보통 리프의 첫 키를 올림
            split.left = this;
            split.right = rightNode;
            return split;
        }
    }

    static class SplitResult {
        int key;
        Node left;
        Node right;
    }


}
