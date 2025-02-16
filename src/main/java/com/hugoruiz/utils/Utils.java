package com.hugoruiz.utils;

import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import com.madgag.gif.fmsware.AnimatedGifEncoder;

public class Utils {

  public static boolean cleanFolder(String folderPath) {
    File folder = new File(folderPath);

    if (folder.exists()) {
      for (File file : folder.listFiles()) {
        file.delete();
      }
      folder.delete();
    }

    return folder.mkdirs();
  }

  public static void createGif(List<String> imagePaths, String outputFilePath, int delayMs) throws Exception {
    AnimatedGifEncoder encoder = new AnimatedGifEncoder();
    encoder.start(outputFilePath);
    encoder.setDelay(delayMs);
    encoder.setRepeat(0);

    for (String imagePath : imagePaths) {
      BufferedImage img = ImageIO.read(new File(imagePath));
      encoder.addFrame(img);
    }

    encoder.finish();
  }

  public static String getInitialStringCode() {
    String code = "import java.util.HashMap;\n" +
        "public class MyProgram {\n" +
        "    public static void main(String[] args) {\n" +
        "        String s = \"pwwkew\";\n" +
        "        HashMap<Character, Integer> map = new HashMap<Character, Integer>();\n" +
        "        int max=0;\n" +
        "        for (int i=0, j=0; i<s.length(); ++i){\n" +
        "            if (map.containsKey(s.charAt(i))){\n" +
        "                j = Math.max(j,map.get(s.charAt(i))+1);\n" +
        "            }\n" +
        "            map.put(s.charAt(i),i);\n" +
        "            max = Math.max(max,i-j+1);\n" +
        "        }\n" +
        "    }\n" +
        "}";
    return code;
  }
}
