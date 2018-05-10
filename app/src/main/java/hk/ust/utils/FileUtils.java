package hk.ust.utils;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FileUtils {

    public static String generateFilePath() {
        Date currentTime = new Date();
        SimpleDateFormat ft = new SimpleDateFormat ("hhmmssa_ddMMyyyy");

        return Environment.getExternalStorageDirectory() + "/GPSFingerprints/"
                + ft.format(currentTime) + "_fingerprints.txt";
    }

    public static void writeLines(List<String> lines, String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.getParentFile().exists())
            file.getParentFile().mkdirs();
        if (!file.exists()) {
            file.createNewFile();
        }
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
        for (String line : lines) {
            bufferedWriter.write(line + "\n");
            bufferedWriter.flush();
        }
        bufferedWriter.close();
    }

    public static void writeLine(String line, String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.getParentFile().exists())
            file.getParentFile().mkdirs();
        if (!file.exists()) {
            file.createNewFile();
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            bufferedWriter.write(line + "\n");
            bufferedWriter.flush();
            bufferedWriter.close();
        }
        else {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            bufferedWriter.write(line + "\n");
            bufferedWriter.flush();
            bufferedWriter.close();
        }
    }
}
