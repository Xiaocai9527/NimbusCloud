package space.xiaocai.processor;

import java.io.File;
import java.io.IOException;

public interface FileProcessor {

    void handleFile(File file) throws IOException;

}
