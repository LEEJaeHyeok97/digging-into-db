package view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import util.Parser;

public class InputView {

    private final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    public String readLine() {
        try {
            return br.readLine();
        } catch (IOException e) {
            throw new IllegalArgumentException("[ERROR] 입력 오류", e);
        }
    }

    public String promptNonEmpty(String prompt) {
        while (true) {
            System.out.println(prompt);
            String input = readLine();
            if (input != null && !input.isBlank()) {
                return input.trim();
            }

            System.out.println("[ERROR] 비어 있을 수 없습니다. 다시 입력하세요.");
        }
    }

    public int readMenuSelection(int min, int max) {
        while (true) {
            System.out.print("선택 ▶ ");
            String input = readLine();
            try {
                int number = Integer.parseInt(input.trim());
                inRange(min, max, number);
                return number;
            } catch (Exception e) {
                System.out.printf("[ERROR] %d ~ %d 사이의 숫자를 입력하세요." + System.lineSeparator(), min, max);
            }
        }
    }

    public String readPrimaryKey(String pkColumn) {
        return promptNonEmpty("PK(" + pkColumn + ") 입력 ▶ ");
    }

    public Map<String, String> readRecordValues(List<String> columns) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String column : columns) {
            values.put(column, promptNonEmpty(column + " ▶ "));
        }

        return values;
    }

    public Map<String, String> readPatchPairs(Set<String> allowedColumns) {
        while (true) {
            System.out.println("수정 값 (예: name=Alice, age=24 ▶ ");
            String input = readLine();
            Map<String, String> out = Parser.parsePairs(input);
            if (out.isEmpty()) {
                System.out.println("[ERROR] 최소 1개 이상 입력하세요.");
                continue;
            }

            for (String key : out.keySet()) {
                if (!allowedColumns.contains(key)) {
                    System.out.println("[ERROR] 존재하지 않는 칼럼입니다.");
                    out.clear();
                    break;
                }
            }

            if (!out.isEmpty()) {
                return out;
            }
        }
    }

    private static void inRange(int min, int max, int number) {
        if (number < min || number > max) {
            throw new IllegalArgumentException("[ERROR] 선택 범위 안의 값을 입력해주세요.");
        }
    }
}
