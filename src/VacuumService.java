import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * VACUUM 기능 구현
 * 1. 임시 파일(data_temp.db)를 만들고, 헤더를 기록
 * 2. 원본 파일(data.db)을 열어서, 헤더는 건너뛴 뒤 레코드들을 순회.
 * 3. deletedFlag가 false인 레코드만 임시 파일에 다시 쓰기.
 * 4. 작업이 끝나면 원본 파일을 삭제하거나 이름을 변경. 임시 파일 이름을 data.db로 rename
 * 5. indexMap을 다시 로드(loadIndex())해서 새 파일 기준으로 인덱스를 재구축.
 */
public class VacuumService {

    private final String originalFilePath;

    public VacuumService(String originalFilePath) {
        this.originalFilePath = originalFilePath;
    }

    public void vacuum() throws IOException {
        File originalFile = new File(originalFilePath);
        if (!originalFile.exists() || originalFile.length() == 0) {
            // 파일이 비어 있거나 없으면 압축할 필요가 없다.
            return;
        }

        File tempFile = new File("data_temp.db");
        if (tempFile.exists()) {
            tempFile.deleteOnExit();
        }

        // 임시 파일 헤더 기록
        try (RandomAccessFile rafTemp = new RandomAccessFile(tempFile, "rw")) {
            rafTemp.writeInt(GlobalVariables.MAGIC_NUMBER.getValue());
            rafTemp.writeInt(GlobalVariables.VERSION.getValue());
        }

        // 원본에서 레코드 복사
        try (RandomAccessFile rafOrigin = new RandomAccessFile(originalFile, "r");
             RandomAccessFile rafTemp = new RandomAccessFile(tempFile, "rw")) {

            // 헤더 건너띄기
            int magicNumber = rafOrigin.readInt();
            int version = rafOrigin.readInt();

            rafTemp.seek(rafTemp.length());

            long fileLength = rafOrigin.length();
            long offset = rafOrigin.getFilePointer();

            while (offset < fileLength) {
                rafOrigin.seek(offset);

                boolean deleted = rafOrigin.readBoolean();
                int keySize = rafOrigin.readInt();
                String key = rafOrigin.readUTF();
                int valueSize = rafOrigin.readInt();
                String value = rafOrigin.readUTF();

                long recordEnd = rafOrigin.getFilePointer();

                if (!deleted) {
                    rafTemp.writeBoolean(false);
                    rafTemp.writeInt(keySize);
                    rafTemp.writeUTF(key);
                    rafTemp.writeInt(valueSize);
                    rafTemp.writeUTF(value);
                }

                offset = recordEnd;
            }
        }

        originalFile.delete();
        tempFile.renameTo(originalFile);
    }


}
