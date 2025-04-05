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

    public void vacuum() throws IOException {
        VacuumService vac = new VacuumService(filePath);
        vac.vacuum();

        // vacuum 후에는 indexMap 재구축
        indexMap.clear();
        loadIndex();
    }

    /**
     * put(key, value):
     * 파일 포맷: [deletedFlag(boolean), keySize(int) key(UTF), valueSize(int), value(UTF)]
     */
    public void put(String key, String value) throws IOException {
        checkOrInitFileHeader();

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
//            if (deletedFlag) {
//                return null;
//            }

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
        checkOrInitFileHeader();

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
        checkOrInitFileHeader();

        indexMap.clear();

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            // 헤더 읽는 부분. 이 부분을 offset에 반영해야 error가 발생하지 않음.
            int magicNumber = raf.readInt();
            int version = raf.readInt();

            long fileLength = raf.length();

            // 파일 포인터는 현재 헤더 끝에 있음. 이 지점을 offset으로 삼아야 에러가 발생하지 않음
            long offset = raf.getFilePointer();

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

                offset = recordEnd;
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

        db.delete("test");
        System.out.println(db.get("test")); // 이것도 null. indexMap에서 인덱스를 연결해제 해서 논리적으로 찾을 수 없게하고 vacuum 단계에서 실제 데이터 삭제
        db.vacuum();  // 별도 메서드

        System.out.println(db.get("hello")); // world
        System.out.println(db.get("원래 없던 키값")); // null
        System.out.println(db.get("test")); // null

    }

    /**
     * 파일 헤더를 검사하거나, 없으면 새로 파일을 생성한다.
     * magic number, version 등 체크
     */
    private void checkOrInitFileHeader() throws IOException {
        File file = new File(filePath);

        // 파일이 없을 때, 크기가 0일 때
        if (!file.exists() || file.length() == 0) {
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                // 새 파일 헤더 기록
                raf.writeInt(GlobalVariables.MAGIC_NUMBER.getValue());
                raf.writeInt(GlobalVariables.VERSION.getValue());

                /**
                 * 추후 확장 시 여기에 헤더 정보 추가적으로 작성(페이지 크기 ..)
                 */
            }
            return;
        }

        // 기존 파일이 있는 경우 헤더 검사
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            int magicNumber = raf.readInt();
            int version = raf.readInt();

            if (magicNumber != GlobalVariables.MAGIC_NUMBER.getValue()) {
                throw new IOException("잘못된 매직 넘버, 이 DB 파일이 아닙니다.");
            }
            if (version != GlobalVariables.VERSION.getValue()) {
                throw new IOException("파일 버전이 맞지 않습니다. version = " + version + ", expected = " + GlobalVariables.VERSION.getValue());
            }
        }
    }
}