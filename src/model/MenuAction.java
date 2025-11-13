package model;

import java.util.HashMap;
import java.util.Map;

public enum MenuAction {

    EXIT(0, "종료", true),
    LIST(1, "전체 조회"),
    FIND_BY_PK(2, "PK로 조회"),
    INSERT(3, "레코드 추가"),
    PATCH(4, "레코드 수정(Patch)"),
    DELETE(5, "레코드 삭제"),
    FIND_ALL_BY(6, "컬럼=값 검색(findAllBy)"),
    SAVE(7, "저장"),
    PK_RANGE(8, "PK 범위 조회"),
    BEGIN(9, "Tx Begin"),
    COMMIT(10, "Tx Commit"),
    ROLLBACK(11, "Tx Rollback");

    private final int code;
    private final String label;
    private final boolean exitsLoop;

    MenuAction(int code, String label) {
        this(code, label, false);
    }

    MenuAction(int code, String label, boolean exitsLoop) {
        this.code = code;
        this.label = label;
        this.exitsLoop = exitsLoop;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean exitsLoop() {
        return exitsLoop;
    }

    private static final Map<Integer, MenuAction> byCode = new HashMap<>();

    static {
        for (MenuAction a : values()) {
            byCode.put(a.code, a);
        }
    }

    public static MenuAction from(int code) {
        return byCode.get(code);
    }

    public static int minCode() {
        return 0;
    }

    public static int maxCode() {
        return 8;
    }
}
