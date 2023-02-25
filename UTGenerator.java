import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UTGenerator {
    public File targetFolder;
    public File resultFolder;
    public String path;
    public String name;
    public List<TSDataFile> dataLines = new ArrayList<TSDataFile>();


    public UTGenerator(File targetFolder, File resultFolder, String path, String name) {
        this.targetFolder = targetFolder;
        this.resultFolder = resultFolder;
        this.path = path;
        this.name = name;
    }

    private void openFileWriter() {
        try {
            String writeTo = resultFolder.getPath() + "\\" + name;
            FileWriter curFile = new FileWriter(writeTo);
            handlePerLine(curFile);
            curFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePerLine(FileWriter curFile) {
        dataLines.forEach(dataFile -> {
            try {
                if(dataFile.isNoParamFunction()) {
                    System.out.println(dataFile.lineText);
                    curFile.write(dataFile.creator.createStdUT());
                }
            } catch (IOException e) {e.printStackTrace();}
        });
    }

    private void readFile(String pathName) {
        try {
            File myObj = new File(pathName);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                dataLines.add(new TSDataFile(data));
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    ///////////// CORE ///////////////////////////////////////////////


    public void execute() {
        readFile(path);
        openFileWriter();
    }
}

class TSDataFile {
    public String lineText = "";
    public Creator creator = new Creator();

    public TSDataFile(String lineText) {
        this.lineText = lineText;
    }

    private boolean hasNotHas(CharSequence[] hasList, CharSequence[] notHasList) {
        for(CharSequence has : hasList) {
            if(!lineText.contains(has)) return false;
        }
        for(CharSequence notHas : notHasList) {
            if(lineText.contains(notHas)) return false;
        }
        return true;
    }

    public boolean isNoParamFunction() {
        if(isStartOfFunction()) {
            return !lineText.contains(",");
        }else return false;
    }

    public boolean isStartOfFunction() {
        CharSequence[] hasList = {"(", ")", "{"};
        CharSequence[] notHasList = {"if", "this", ";", "constructor", "catch", "get ", "set ", "setTimeout", "new "};
        return hasNotHas(hasList, notHasList);
    }

    class FunctionProp {
        public List<String> params = new ArrayList<>();
        public String functionName = "";

        public FunctionProp(String lineText) {
            Integer[] arr = {
                lineText.indexOf("("),
                lineText.indexOf(")"),
            };
            this.functionName = lineText.substring(0, arr[0]).trim();
        }
    }

    class Creator {
        public String createStdUT() {
            FunctionProp fun = new FunctionProp(lineText);
            String stdUT = newLiner(
                "it(\"Should Check '[METHOD]' method\", () => {".replace(DEF.METHOD, fun.functionName),
                "  const spyMethod = spyOn(component, '[METHOD]');\n  component.[METHOD]([PARAMS]);"
                        .replace(DEF.METHOD, fun.functionName)
                        .replace(DEF.PARAMS, ""),
                "  spyMethod.toHaveBeenCalled();",
                "})\n\n"
            );
            if(!isValidFunction(fun.functionName)) return "";
            return stdUT;
        }
        public Boolean isValidFunction(String functionName) {
            CharSequence[] has = {};
            CharSequence[] notHas = {"*", ":", ".", "for", "if", "switch"};
            return hasNotHas(has, notHas);
        }

        public String newLiner(String ... arr){
            String result = "";
            for(String text: arr) {
                result += text;
                result += "\n";
            };
            return result;
        }
    }

    static class DEF {
        public static String METHOD = "[METHOD]";
        public static String PARAMS = "[PARAMS]";
    }
}

