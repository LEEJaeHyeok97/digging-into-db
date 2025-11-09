package model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class Database implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, Table> tables = new HashMap<>();

    public void addTable(Table table) {
        tables.put(table.getName(), table);
    }

    public Table getTable(String name) {
        return tables.get(name);
    }

    public Set<String> tableNames() {
        return Collections.unmodifiableSet(tables.keySet());
    }

    public void saveToFile(String path) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(path)))) {
            oos.writeObject(this);
            oos.flush();
        }
    }

    public static Database loadFromFile(String path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(path)))) {
            return (Database) ois.readObject();
        }
    }

    public static Database openOrCreate(String path, Consumer<Database> init) throws IOException {
        File file = new File(path);
        if (file.exists() && file.length() > 0) {
            try {
                return loadFromFile(path);
            } catch (Exception e) {
                File backup = new File(path + ".backup");
                file.renameTo(backup);
            }
        }

        Database database = new Database();
        if (init != null) {
            init.accept(database);
        }

        database.saveToFile(path);
        return database;
    }
}
