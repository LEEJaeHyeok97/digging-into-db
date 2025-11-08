import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {

        try {
            String filename = "database";
            FileWriter fileWriter = new FileWriter(filename);

            String test = "test";
            fileWriter.write(test);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}