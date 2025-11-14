import controller.DatabaseController;
import java.util.List;
import java.util.Map;
import model.Database;
import model.Record;
import model.Table;
import util.transaction.TransactionManager;
import util.wal.Wal;
import view.InputView;
import view.OutputView;

public class Main {

    private static final String DB_PATH = "database.db";
    private static final String WAL_PATH = "database.wal";

    public static void main(String[] args) throws Exception {

        Database db = Database.openOrCreate(DB_PATH, d -> {
            Table users = new Table("users", List.of("id", "name", "age"), "id");
            d.addTable(users);
        });

        var log = Wal.readAll(WAL_PATH);
        TransactionManager.recover(db, log);

        try (Wal wal = new Wal(WAL_PATH)) {
            TransactionManager tm = new TransactionManager(db, wal);

            Table users = db.getTable("users");
            long snap = db.currentCommitSequence();
            if (users.selectAllAt(snap).isEmpty()) {
                tm.begin();
                tm.insert("users", new Record(Map.of("id","1","name","Alice","age","23")));
                tm.insert("users", new Record(Map.of("id","2","name","Bob",  "age","28")));
                tm.commit();
                db.saveToFile(DB_PATH);
            }

            DatabaseController databaseController = new DatabaseController(db, DB_PATH, tm, new InputView(), new OutputView());
            databaseController.run();
        }
    }
}