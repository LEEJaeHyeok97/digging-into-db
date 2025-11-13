package util.wal;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import util.wal.WalEntry;

public class Wal implements Closeable {

    private final File file;
    private final FileOutputStream fos;
    private final ObjectOutputStream oos;

    public Wal(String path) throws IOException {
        this.file = new File(path);
        boolean append = file.exists() && file.length() > 0;
        this.fos = new FileOutputStream(file, true);
        this.oos = append ? new AppendableObjectOutputStream(fos) : new ObjectOutputStream(fos);
    }

    public synchronized void append(WalEntry e) throws IOException {
        oos.writeObject(e);
        oos.flush();
        FileChannel ch = fos.getChannel();
        ch.force(true);
    }

    public static List<WalEntry> readAll(String path) throws IOException {
        File f = new File(path);
        if (!f.exists() || f.length() == 0) {
            return List.of();
        }

        ArrayList<WalEntry> out = new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            while (true) {
                Object obj = ois.readObject();
                out.add((WalEntry) obj);
            }
        } catch (EOFException e) {

        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        return out;
    }

    public static void truncate(String path) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path, false)) {
        }
    }

    @Override
    public void close() throws IOException {
        oos.close();
        oos.flush();
    }

    static class AppendableObjectOutputStream extends ObjectOutputStream {
        AppendableObjectOutputStream(OutputStream out) throws IOException { super(out); }
        @Override protected void writeStreamHeader() throws IOException { reset(); }
    }
}
