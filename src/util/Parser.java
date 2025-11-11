package util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Parser {

    public static Map<String, String> parsePairs(String line) {
        Map<String, String> map = new LinkedHashMap<>();
        if (line == null) {
            return map;
        }

        String[] parts = line.split(",");
        for (String part : parts) {
            String trimedPart = part.trim();
            if (trimedPart.isEmpty()) {
                continue;
            }

            int eq = trimedPart.indexOf("=");
            if (eq <= 0 || eq == trimedPart.length() - 1) {
                System.out.println("[ERROR] 형식: key=value (쉼표로 구분)");
                return Collections.emptyMap();
            }

            String key = trimedPart.substring(0, eq).trim();
            String value = trimedPart.substring(eq + 1).trim();
            map.put(key, value);
        }

        return map;
    }
}
