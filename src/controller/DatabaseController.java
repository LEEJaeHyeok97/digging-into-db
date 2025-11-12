package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import model.Database;
import model.MenuAction;
import model.Record;
import model.Table;
import view.InputView;
import view.OutputView;

public class DatabaseController {

    private final Database db;
    private final String dbPath;
    private final InputView inputView;
    private final OutputView outputView;

    public DatabaseController(Database db, String dbPath, InputView inputView, OutputView outputView) {
        this.db = db;
        this.dbPath = dbPath;
        this.inputView = inputView;
        this.outputView = outputView;
    }

    public void run() {
        outputView.printWelcome();

        Table table = selectTable();
        if (table == null) {
            throw new IllegalArgumentException("[ERROR] 사용할 테이블이 없습니다. 프로그램을 종료합니다.");
        }

        mainLoop(table);
    }

    private void mainLoop(Table table) {
        while (true) {
            try {
                outputView.printTables(db.tableNames());
                outputView.printMenu();
                int selection = inputView.readMenuSelection(0, 8);

                if (selection == MenuAction.EXIT.code()) {
                    saveQuiet(); outputView.printMessage("종료합니다."); break;
                } else if (selection == MenuAction.LIST.code()) {
                    outputView.printRecords(table, table.selectAll());
                } else if (selection == MenuAction.FIND_BY_PK.code()) {
                    handleSelectByPk(table);
                } else if (selection == MenuAction.INSERT.code()) {
                    handleInsert(table);
                } else if (selection == MenuAction.PATCH.code()) {
                    handlePatchByPk(table);
                } else if (selection == MenuAction.DELETE.code()) {
                    handleDeleteByPk(table);
                } else if (selection == MenuAction.FIND_ALL_BY.code()) {
                    handleFindAllBy(table);
                } else if (selection == MenuAction.SAVE.code()) {
                    trySave();
                } else if (selection == MenuAction.PK_RANGE.code()) {
                    handleFindPkRange(table);
                } else {
                    throw new IllegalArgumentException("[ERROR] 잘못된 선택입니다.");
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("[ERROR] 잘못된 입력입니다.");
            } catch (Exception e) {
                outputView.printMessage("[ERROR] " + e.getMessage());
            }
        }
    }

    void handleSelectByPk(Table table) {
        String pkCol = table.getPrimaryKeyColumn();
        String key = inputView.readPrimaryKey(pkCol);
        var r = table.selectById(key);
        if (r == null) outputView.printMessage("(없음)"); else outputView.printRecord(table, r);
    }

    void handleInsert(Table table) {
        Map<String, String> values = inputView.readRecordValues(table.getColumns());
        table.insertRecord(new Record(values));
        outputView.printMessage("추가 완료");
    }

    void handlePatchByPk(Table table) {
        String pkCol = table.getPrimaryKeyColumn();
        String key = inputView.readPrimaryKey(pkCol);
        var old = table.selectById(key);
        if (old == null) { outputView.printMessage("[ERROR] 존재하지 않는 레코드: " + key); return; }

        var changes = inputView.readPatchPairs(Set.copyOf(table.getColumns()));
        changes.remove(pkCol); // PK 변경 방지

        Map<String, String> merged = new HashMap<>(old.values());
        for (var e : changes.entrySet()) merged.put(e.getKey(), e.getValue());

        table.updateById(key, new Record(merged));
        outputView.printRecord(table, table.selectById(key));
    }

    void handleDeleteByPk(Table table) {
        String pkCol = table.getPrimaryKeyColumn();
        String key = inputView.readPrimaryKey(pkCol);
        table.deleteById(key);
        outputView.printMessage("삭제 완료");
    }

    void handleFindAllBy(Table table) {
        String col = inputView.promptNonEmpty("검색 컬럼 ▶ ");
        String val = inputView.promptNonEmpty("값 ▶ ");
        outputView.printRecords(table, table.findAllBy(col, val));
    }

    void handleFindPkRange(Table table) {
        String from = inputView.promptNonEmpty("PK from ▶ ");
        String to   = inputView.promptNonEmpty("PK to   ▶ ");
        outputView.printRecords(table, table.findAllByPkBetween(from, true, to, true));
    }

    private Table selectTable() {
        ArrayList<String> names = new ArrayList<>(db.tableNames());
        if (names.isEmpty()) {
            return null;
        }
        if (names.size() == 1) {
            return db.getTable(names.get(0));
        }

        for (int i = 0; i < names.size(); i++) {
            System.out.printf("[%d] %s%n", i + 1, names.get(i));
        }

        int selection = inputView.readMenuSelection(1, names.size());
        return db.getTable(names.get(selection - 1));
    }

    private void save() throws IOException {
        db.saveToFile(dbPath);
    }

    private void saveQuiet() {
        try {
            save();
        } catch (IOException e) {
        }
    }

    private void trySave() {
        try { save(); outputView.printMessage("저장 완료"); }
        catch (IOException e) {
            throw new IllegalArgumentException("[ERROR] 저장 실패.");
        }
    }
}
