package view;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import model.MenuAction;
import model.Record;
import model.Table;

public class OutputView {

    public static final String LINE_SEPARATOR = System.lineSeparator();

    public void printWelcome() {
        System.out.println(LINE_SEPARATOR + "==== Mini DB Console ====");
    }

    public void printTables(Set<String> tableNames) {
        System.out.println("테이블: " + tableNames);
    }

    public void printMenu() {
        whiteSpace();
        for (var a : MenuAction.values()) {
            System.out.printf("[%d] %s%n", a.code(), a.label());
        }
    }

    public void printMessage(String message) {
        System.out.println(message);
    }

    public void printRecords(Table table, List<Record> records) {
        List<String> columns = table.getColumns();
        int n = columns.size();
        int[] width = new int[n];
        for (int i = 0; i < n; i++) {
            width[i] = Math.max(4, columns.get(i).length());
        }

        for (Record r : records)
            for (int i = 0; i < n; i++)
                width[i] = Math.max(width[i], safe(r.get(columns.get(i))).length());

        StringBuilder header = new StringBuilder();
        for (int i = 0; i < n; i++)
            header.append(pad(columns.get(i), width[i])).append(i == n - 1 ? "" : " | ");
        String sep = "-".repeat(header.length());

        System.out.println(sep);
        System.out.println(header);
        System.out.println(sep);

        for (Record r : records) {
            StringBuilder row = new StringBuilder();
            for (int i = 0; i < n; i++)
                row.append(pad(safe(r.get(columns.get(i))), width[i])).append(i == n - 1 ? "" : " | ");
            System.out.println(row);
        }
        System.out.println(sep);
        System.out.println("rows: " + records.size());
    }

    public void printRecord(Table table, Record r) {
        List<Record> one = new ArrayList<>();
        if (r != null) one.add(r);
        printRecords(table, one);
    }

    private String pad(String s, int w) {
        if (s == null) s = "";
        if (s.length() >= w) return s;
        StringBuilder sb = new StringBuilder(w);
        sb.append(s);
        while (sb.length() < w) sb.append(' ');
        return sb.toString();
    }

    private String safe(String s) { return s == null ? "" : s; }

    private void whiteSpace() {
        System.out.println();
    }
}
