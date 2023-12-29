import java.io.File;
import java.util.*;

public class Main {
    public static String version = "0.5";
    public static void main(String[] args) {
        // Target folder (.compoent.ts)
        File targetFolder = new File("C:\\project\\bcasek\\bcas_frontend_mobileview\\src\\app\\pages");
        // Result generated unit test (.components.spec.ts)
        File resultFolder = new File("C:\\project\\result-gen");
        
        if(!resultFolder.exists()) resultFolder.mkdirs();
        new Main(targetFolder, resultFolder);
    }

    public File targetFolder;
    public File resultFolder;
    List<GenLogger> loggerList = new ArrayList<>();

    public Main(File tgtFolder, File resultFolder){
        this.targetFolder = tgtFolder;
        this.resultFolder = resultFolder;
        this.listFilesForFolder(tgtFolder);
        printLogger();
    }

    private void printLogger() {
        String bold = "\033[0;1m";
        System.out.println(bold+"\nUT GENERATOR V."+version);
        System.out.println(bold+"================GENERATE_SUCCESS=====================");
        System.out.println("Result Folder in : ".concat(this.resultFolder.getPath()));
        System.out.println("From Target Folder : ".concat(this.targetFolder.getPath()));

        String nameLogs = "";
        List<String> fileNames = new ArrayList<>();
        List<String> valueFromFile = new ArrayList<>();
        for (GenLogger log : loggerList) {
            nameLogs = nameLogs.concat("\n  - ").concat(log.nameLog);
            fileNames.add(log.nameLog);
            String value = "";
            int valueCount = 0;
            for (String noParamFunc : log.mapLogs.get(FunctionType.FUNCTION_NO_PARAM.toString())) {
                if(valueCount > 60) {
                    value += "\n";
                    valueCount = 0;
                }
                value += noParamFunc + ", ";
                valueCount += noParamFunc.length();
            }
            valueFromFile.add(value);
        }
        System.out.println("\nFile List : ".concat(nameLogs));
        System.out.println("\nFile Detail List : ");
        for (int i = 0; i < fileNames.size(); i++) {
            System.out.println("\n"+fileNames.get(i) + ": ");
            System.out.println(valueFromFile.get(i));
        }

    }

    public void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                Boolean isTsFile = fileEntry.getName().endsWith(".component.ts");
                if(isTsFile) {
                    UTGenerator utGen = generateSpecTsFile(fileEntry);
                    utGen.execute();
                    loggerList.add(utGen.getLogger());
                }
            }
        }

    }

    public UTGenerator generateSpecTsFile(final File tsFile) {
        String path = tsFile.getPath();
        String name = tsFile.getName().replace(".component.ts", ".component.spec.ts");
        return new UTGenerator(targetFolder, resultFolder, path, name);
    }
}

class GenLogger {
    public String nameLog = "";
    public Map<String, List<String>> mapLogs = new HashMap<>();
    public GenLogger() {}
}