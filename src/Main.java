import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private final String filePath;
    private final Map<String, Long> indexMap = new HashMap<>();

    public Main(String filePath) {
        this.filePath = filePath;
    }

    /**
     * put(key, value):
     * 파일 포맷: [deletedFlag(boolean), keySize(int) key(UTF), valueSize(int), value(UTF)]
     */
    public void put(String key, String value) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            // RandomAccessFile을 "rw"로 열면 내부적으로 해당 파일이 존재하지 않을 경우 새로 생성한다.
            raf.seek(raf.length());
            long offset = raf.getFilePointer();

            // soft delete flag 설정
            raf.writeBoolean(false);

            // 키 기록
            raf.writeInt(key.length());
            raf.writeUTF(key);

            // 값 기록
            raf.writeInt(value.length());
            raf.writeUTF(value);

            // 인덱스에 (키 -> 오프셋) 저장
            indexMap.put(key, offset); // 파일에 쓴 데이터를 인덱싱하기 위한 부분. 이 부분이 없으면 랜덤 액세스 기능 불가능.
        }
    }

    /**
     * indexMap에서 오프셋을 찾은 뒤, 그 위치의 deletedFlag가 false인지 체크
     * false면 데이터를 읽고 true면 null 반환
     */
    public String get(String key) throws IOException {
        Long offset = indexMap.get(key);
        if (offset == null) {
            // 인덱스에 없으면 null
            return null;
        }

        // 파일에 offset 위치로 가서 키 밸류 값을 읽는다
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(offset);

            boolean deletedFlag = raf.readBoolean();
            if (deletedFlag) {
                return null;
            }

            int keySize = raf.readInt();
            String storedKey = raf.readUTF();
            int valueSize = raf.readInt();
            String value = raf.readUTF();

            return value;
        }
    }

    /*
    Delete.java 클래스에서 내부 table lock을 잡고 대상 row를 하나씩 찾아서 삭제를 하는 흐름으로 구현되어 있다.
    H2의 기본 모드는 'MVStore' 이고, delete하게 되면 H2에서 이전 버전 데이터를 남겨 두었다가 트랜잭션이 끝나면
    가비지콜렉션 과정에서 제거하거나, 특정 시점에 재압축 등을 실행할 수 있다.

    H2에서는 삭제 시 소프트 삭제 후 가비지 컬렉션과 재압축 과정을 통해 공간을 정리한다.
     */
    public void delete(String key) throws IOException {
        Long offset = indexMap.remove(key);
        if (offset == null) {
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            // 레코드 시작 위치로 이동
            raf.seek(offset);

            // deletedFlag = true로 설정
            raf.writeBoolean(true);
        }
    }

    public void loadIndex() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            long fileLength = raf.length();
            long offset = 0;

            while (offset < fileLength) {
                raf.seek(offset);

                boolean deleted = raf.readBoolean();

                int keySize = raf.readInt();
                String key = raf.readUTF();
                int valueSize = raf.readInt();
                String value = raf.readUTF();

                long recordEnd = raf.getFilePointer();

                if (!deleted) {
                    indexMap.put(key, offset);
                }
            }
        } catch (FileNotFoundException e) {
        }
    }

    public static void main(String[] args) throws IOException {
        File file = new File("data.db");
        if (file.exists()) {
            file.delete();
        }

        Main db = new Main("data.db");


        db.loadIndex();

        db.put("hello", "world");
        db.put("test", "1234");
        db.put("apple", "banana");

        System.out.println("Before Delete");
        System.out.println(db.get("hello"));
        System.out.println(db.get("test"));
        System.out.println(db.get("apple"));

        db.delete("test");

        System.out.println("After Delete");
        System.out.println(db.get("hello"));
        System.out.println(db.get("test")); // null이 출력된다.
        System.out.println(db.get("apple"));
        /*
         * 삭제되면 논리적으로 null이지만, 물리 파일에는 남아 있다가 나중에 GC로 없어지는 구조
         * 아직 트랜잭션 개념을 도입하지 않았으므로 소프트 삭제되는 물리적으로 파일에 레코드가 남아 있는 단계까지 구현함.
         * 완전히 삭제하려면 할 수 있지만 이 부분은 가비지콜렉터가 하는 로직이라 그냥 간단하게 null 반환하게만 학습용으로 처리.
         */
    }
}