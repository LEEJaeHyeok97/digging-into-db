import controller.DatabaseController;
import java.util.List;
import java.util.Map;
import model.Database;
import model.Record;
import model.Table;
import view.InputView;
import view.OutputView;

public class Main {

    private static final String DB_PATH = "database.db";

    public static void main(String[] args) throws Exception {

        Database db = Database.openOrCreate(DB_PATH, d -> {
            Table users = new Table("users", List.of("id", "name", "age"), "id");
            d.addTable(users);
        });

        Table users = db.getTable("users");
        if (users.size() == 0) {
            users.insertRecord(new Record(Map.of("id", "1", "name", "Alice", "age", "23")));
            users.insertRecord(new Record(Map.of("id", "2", "name", "Bob",   "age", "28")));
            db.saveToFile(DB_PATH);
        }

        DatabaseController databaseController = new DatabaseController(db, DB_PATH, new InputView(), new OutputView());
        databaseController.run();
    }
}