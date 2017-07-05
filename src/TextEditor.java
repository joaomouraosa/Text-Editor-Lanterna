import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TextEditor {

    private static Path path;
    private static BufferView bufferView;
    private static FileBuffer fileBuffer;

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            path = Paths.get(args[0]);
            fileBuffer = new FileBuffer(path);
        }
        else {
            fileBuffer = new FileBuffer();
        }
        bufferView = new BufferView(fileBuffer);
    }
}

