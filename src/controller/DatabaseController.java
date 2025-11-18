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
import util.transaction.TransactionManager;
import view.InputView;
import view.OutputView;

public class DatabaseController {

    private final Database db;
    private final String dbPath;
    private final TransactionManager tm;
    private final InputView inputView;
    private final OutputView outputView;

    public DatabaseController(Database db, String dbPath, TransactionManager tm, InputView inputView, OutputView outputView) {
        this.db = db;
        this.dbPath = dbPath;
        this.tm = tm;
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

                int selection = inputView.readMenuSelection(0, 11);

                if (selection == MenuAction.EXIT.code()) {
                    saveQuiet(); outputView.printMessage("종료합니다."); break;
                } else if (selection == MenuAction.LIST.code()) {
                    runList(table);
                } else if (selection == MenuAction.FIND_BY_PK.code()) {
                    runFindByPk(table);
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
                    runRange(table);
                } else if (selection == MenuAction.BEGIN.code()) {
                    handleBegin();
                } else if (selection == MenuAction.COMMIT.code()) {
                    handleCommit();
                } else if (selection == MenuAction.ROLLBACK.code()) {
                    handleRollback();
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

    void handleInsert(Table table) throws IOException {
        var values = inputView.readRecordValues(table.getColumns());
        var rec = new Record(values);
        inAutoTx(() -> tm.insert(table.getName(), rec)); // 항상 TM 경유
        outputView.printMessage("추가 완료");
    }

    void handlePatchByPk(Table table) throws IOException {
        String pkCol = table.getPrimaryKeyColumn();
        String key = inputView.readPrimaryKey(pkCol);
        long snap = db.currentCommitSequence();
        var old = table.selectByIdAt(key, snap);
        if (old == null) { outputView.printMessage("[ERROR] 존재하지 않는 레코드: " + key); return; }

        var changes = inputView.readPatchPairs(Set.copyOf(table.getColumns()));
        changes.remove(pkCol);
        Map<String,String> merged = new HashMap<>(old.values());
        merged.putAll(changes);
        var newRec = new Record(merged);

        inAutoTx(() -> tm.update(table.getName(), key, newRec));
        outputView.printMessage("수정 완료");
    }

    void handleDeleteByPk(Table table) throws IOException {
        String pkCol = table.getPrimaryKeyColumn();
        String key = inputView.readPrimaryKey(pkCol);
        inAutoTx(() -> tm.delete(table.getName(), key));
        outputView.printMessage("삭제 완료");
    }

    void handleFindAllBy(Table table) {
        String col = inputView.promptNonEmpty("검색 컬럼 ▶ ");
        String val = inputView.promptNonEmpty("값 ▶ ");
        long snap = db.currentCommitSequence();
        outputView.printRecords(table, table.findAllByAt(col, val, snap));
    }

    void handleBegin() {
        if (tm == null) {
            throw new IllegalArgumentException("[ERROR] 트랜잭션 매니저가 설정되지 않았습니다.");
        }
        if (tm.isActive()) {
            throw new IllegalArgumentException("[ERROR] 이미 트랜잭션이 진행중입니다.");
        }
        tm.begin();
        outputView.printMessage("Transaction Begin.");
    }

    void handleCommit() {
        validateOnRunningTransaction();

        try {
            tm.commit();
            db.saveToFile(dbPath);
            outputView.printMessage("COMMIT 완료");
        } catch (IOException e) {
            throw new IllegalArgumentException("[ERROR] 커밋에 실패했습니다.");
        }
    }

    void handleRollback() {
        validateOnRunningTransaction();
        tm.rollback();
        outputView.printMessage("ROLLBACK 완료");
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

    private void validateOnRunningTransaction() {
        if (tm == null || !tm.isActive()) {
            throw new IllegalArgumentException("[ERROR] 시작된 트랜잭션이 없습니다.");
        }
    }

    private void runList(Table table) {
        long snap = db.currentCommitSequence();
        outputView.printRecords(table, table.selectAllAt(snap));
    }

    private void runFindByPk(Table table) {
        String pkCol = table.getPrimaryKeyColumn();
        String key = inputView.readPrimaryKey(pkCol);
        long snap = db.currentCommitSequence();
        var r = table.selectByIdAt(key, snap);
        if (r == null) outputView.printMessage("(없음)"); else outputView.printRecord(table, r);
    }

    private void runRange(Table table) {
        String from = inputView.promptNonEmpty("PK from ▶ ");
        String to   = inputView.promptNonEmpty("PK to   ▶ ");
        long snap = db.currentCommitSequence();
        outputView.printRecords(table, table.findAllByPkBetweenAt(from, true, to, true, snap));
    }

    private void inAutoTx(Runnable r) throws IOException {
        if (tm.isActive()) { r.run(); return; }
        tm.begin();
        try { r.run(); tm.commit(); }
        catch (Exception e) { tm.rollback(); throw e; }
    }
}
