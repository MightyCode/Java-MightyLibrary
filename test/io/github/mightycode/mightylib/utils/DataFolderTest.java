package io.github.mightycode.mightylib.utils;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class DataFolderTest {
    @Test
    void getFileContentKeepsTheLineBreaks() {
        File folder = new File(DataFolder.DATA_FOLDER_PATH);
        boolean folderExisted = folder.exists();
        String name = "DataFolderTest-" + System.nanoTime() + ".txt";
        File file = new File(folder, name);

        try {
            String content = "first line\nsecond line\r\n\nlast line\n";
            assertTrue(DataFolder.saveFile(content, name));
            assertEquals(content, DataFolder.getFileContent(name));
        } finally {
            assertTrue(!file.exists() || file.delete());
            if (!folderExisted)
                assertTrue(folder.delete());
        }
    }
}
