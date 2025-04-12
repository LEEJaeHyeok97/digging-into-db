class BPlusTreeTest {

    public static void main(String[] args) {
        BPlusTree bPlusTree = new BPlusTree();

        // insert
        bPlusTree.insert(10, "hello");
        bPlusTree.insert(20, "world");
        bPlusTree.insert(5, "java");
        bPlusTree.insert(15, "b+tree");
        bPlusTree.insert(30, "db");


        // 검색
        System.out.println("search 10 : " + bPlusTree.search(10));
        System.out.println("search 10 : " + bPlusTree.search(20));
        System.out.println("search 10 : " + bPlusTree.search(15));

    }
}