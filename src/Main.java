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

    public void put(String key, String value) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            // RandomAccessFile을 "rw"로 열면 내부적으로 해당 파일이 존재하지 않을 경우 새로 생성한다.
            raf.seek(raf.length());
            long offset = raf.getFilePointer();

            // 키 기록
            raf.writeInt(key.length());
            raf.writeUTF(key);

            // 값 기록
            raf.writeInt(value.length());
            raf.writeUTF(value);

            // 인덱스에 (키 -> 오프셋) 저장
            indexMap.put(key, offset);
        }
    }

    public String get(String key) throws IOException {
        Long offset = indexMap.get(key);
        if (offset == null) {
            // 인덱스에 없으면 null
            return null;
        }

        // 파일에 offset 위치로 가서 키 밸류 값을 읽는다
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(offset);
            int keySize = raf.readInt();
            String storedKey = raf.readUTF();
            int valueSize = raf.readInt();
            String value = raf.readUTF();
            return value;
        }
    }

    public void loadIndex() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            long fileLength = raf.length();
            long offset = 0;
            while (offset < fileLength) {
                raf.seek(offset);
                int keySize = raf.readInt();
                String key = raf.readUTF();
                int valueSize = raf.readInt();
                String value = raf.readUTF();

                indexMap.put(key, offset);

                // 다음 레코드 오프셋
                offset = raf.getFilePointer();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Main db = new Main("data.db");

        try {
            db.loadIndex();
        } catch (FileNotFoundException e) {
            System.out.println("data.db 파일을 찾을 수 없습니다.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        db.put("hello", "world");
        db.put("test", "1234");

        System.out.println(db.get("hello"));
        System.out.println(db.get("test"));
    }
}