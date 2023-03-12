
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

enum FunctionType {
    FUNCTION_NO_PARAM,
    FUNCTION_WITH_PARAM,
    ASYNC_FUNCTION
}
public class UTGenerator {
    public File targetFolder;
    public File resultFolder;
    public String path;
    public String name;
    public List<TSDataFile> dataLines = new ArrayList<TSDataFile>();
    public GenLogger logger = new GenLogger();
    private List<String> existingSpecLines = new ArrayList<String>();


    public UTGenerator(File targetFolder, File resultFolder, String path, String name) {
        this.targetFolder = targetFolder;
        this.resultFolder = resultFolder;
        this.path = path;
        this.name = name;

        this.logger.nameLog = this.name;
        this.logger.mapLogs.put(FunctionType.FUNCTION_NO_PARAM.toString(), new ArrayList<>());
        this.logger.mapLogs.put(FunctionType.FUNCTION_WITH_PARAM.toString(), new ArrayList<>());
        this.logger.mapLogs.put(FunctionType.ASYNC_FUNCTION.toString(), new ArrayList<>());
    }

    public GenLogger getLogger() {return logger;}

    public void addLogger(FunctionType type, String value) {
        this.logger.mapLogs.get(type.toString()).add(value);
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
                    FunctionProp fun = new FunctionProp(dataFile.lineText);
                    if(!isNotBeRecreate(fun)) {
                        addLogger(FunctionType.FUNCTION_NO_PARAM, fun.functionName);
                        curFile.write(dataFile.creator.createStdUT(fun));
                    }
                }
            } catch (IOException e) {e.printStackTrace();}
        });
    }

    private boolean isNotBeRecreate(FunctionProp funProp) {
        Optional<String> dataExist = existingSpecLines.stream()
                .filter(e -> e.contains(funProp.functionName))
                .findFirst();
        return dataExist.isPresent();
    }

    private void readFileComponent(String pathName) {
        try {
            File componentFile = new File(pathName);
            Scanner myReader = new Scanner(componentFile);
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

    private void readFileExistingSpec(String pathName) {
        try {
            File componentFile = new File(pathName);
            Scanner myReader = new Scanner(componentFile);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                existingSpecLines.add(data.concat("\n"));
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    ///////////// CORE ///////////////////////////////////////////////


    public void execute() {
        readFileComponent(path);
        readFileExistingSpec(path.replace("component.ts", "component.spec.ts"));
        openFileWriter();
    }
}

class FunctionProp {
    public List<String> params = new ArrayList<>();
    public String functionName = "";

    public FunctionProp(String lineText) {
        Integer[] arr = {
                lineText.indexOf("("),
                lineText.indexOf(")"),
        };
        this.functionName = lineText.substring(0, arr[0]).trim().replace("async ", "");
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
        CharSequence[] notHasList = FunctionDefinition.notHaveList;
        return hasNotHas(hasList, notHasList);
    }

    class Creator {
        public String createStdUT(FunctionProp fun) {
            String stdUT = newLiner(
                "test(\"Should Check '[METHOD]' method\", () => {".replace(DEF.METHOD, fun.functionName),
                "  const spyMethod = jest.spyOn(component, '[METHOD]');\n  component.[METHOD]([PARAMS]);"
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

