
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            handleDescriber(curFile, name);
            curFile.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDescriber(FileWriter curFile, String name) {
        List<String> lines = new ArrayList<>();

        AtomicBoolean isConstructorLine = new AtomicBoolean(false);
        ServiceProp serviceProp = new ServiceProp();

        dataLines.forEach(dataFile -> {
            if(dataFile.isConstructor()) isConstructorLine.set(true);
            if(isConstructorLine.get() == true) {
                if(dataFile.lineText.contains("{")) isConstructorLine.set(false);
                serviceProp.scan(dataFile.lineText);
            }else {
                serviceProp.scanUsage(dataFile.lineText);
            }
            if(dataFile.isPublicFunction()) {
                FunctionProp fun = new FunctionProp(dataFile.lineText);
                if(!isNotBeRecreate(fun)) {
                    addLogger(FunctionType.FUNCTION_NO_PARAM, fun.functionName);
                    lines.add(dataFile.creator.createStdUT(fun));
                }
            }
        });

        try {
//            curFile.write(serviceProp.usages.toString());
            curFile.write("describe('"+ name +"', () => {\n");

            for (String service : serviceProp.services) {
                curFile.write(Config.tab + "const mock"+service+" = jasmine.createSpyObj('"+service+"', []);\n");
            }

            curFile.write("\n"+Config.tab + "beforeEach(async () => {\n" +
                    Config.inlineTab + "await " +Config.configureTestname + "({\n"+
                    Config.inlineTab + Config.tab + "providers: ["
            );

            for (String service : serviceProp.services) {
                curFile.write("\n"+Config.inlineTab + Config.inlineTab + "{provide: " + service + ", useValue: mock" + service + "},");
            }
            curFile.write("\n" + Config.inlineTab + Config.tab + "],\n");
            curFile.write(Config.inlineTab + "});\n");
            curFile.write(Config.tab + "});\n");

            for (String line : lines) {
                curFile.write(line);
            }
            curFile.write("\n});");
        } catch (IOException e) {e.printStackTrace();}
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

class ParamDef {
    public String name = "";
    public String type = "";

    public ParamDef(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public static String[] extract(String p) {
        String param = p.substring(0, p.indexOf(":")).trim();
        String typeData = p.substring(p.indexOf(":")+1, p.length()).trim();
        return new String[]{param, typeData};
    }
}
class FunctionProp {
    public List<ParamDef> params = new ArrayList<>();
    public String functionName = "";

    private void setFunctionName(String lineText) {
        String name = lineText;
        String[] allPrefix = {"async ", "public "};
        for (String prefix : allPrefix) name = name.replace(prefix, "");
        this.functionName = name;
    }

    private void setParams(String lineText) {
        for (String p : lineText.split(",")) {
            if(p.contains(":")) {
                String[] ext = ParamDef.extract(p);
                this.params.add(new ParamDef(ext[0], ext[1]));
            }else this.params.add(new ParamDef(p, ""));
        }
    }

    public FunctionProp(String lineText) {
        if(lineText.contains(")") && lineText.contains("(")) {
            Integer[] arr = {
                    lineText.indexOf("("),
                    lineText.indexOf(")"),
            };
            setFunctionName(lineText.substring(0, arr[0]).trim());
            String paramsLine = lineText.substring(arr[0]+1, arr[1]);
            if(paramsLine.length() > 0 ) setParams(paramsLine);
        }
    }
}

class ServiceProp {
    public List<String> services = new ArrayList<>();
    public List<String> usages = new ArrayList<>();
    public List<ParamDef> params = new ArrayList<>();

    public ServiceProp() {}

    public void scan(String line) {
        FunctionProp fun = new FunctionProp(line);
        List<ParamDef> allService = fun.params.stream().filter(e -> e.type.contains(Config.serviceNaming)).collect(Collectors.toList());
        services.addAll(allService.stream().map(e -> e.type).collect(Collectors.toList()));
        if(allService.size() > 0) params.addAll(allService);
        else {
            if(line.contains(":") && line.contains(Config.serviceNaming)) {
                String[] ext = ParamDef.extract(line);
                String variable = ext[0].trim().replace("private ", "").replace("public ", "");
                this.params.add(new ParamDef(variable, ext[1].replace(",", "")));
                this.services.add(ext[1].replace(",", ""));
            }
        }
    }
    public void scanUsage(String line) {
        for (ParamDef param : this.params) {
            if(line.contains(param.name)) {
                int start = line.indexOf(param.name + ".");
                if(start > 0) {
                    String usg = line.substring(start, line.length());

                    usages.add(usg);
                }
            };
        }
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
            return lineText.contains("()");
        }else return false;
    }

    public boolean isPublicFunction() {
        if(isStartOfFunction()) {
            return !(lineText.contains("protected ") || lineText.contains("private "));
        }else return false;
    }

    public boolean isStartOfFunction() {
        CharSequence[] hasList = {"(", ")", "{"};
        CharSequence[] notHasList = FunctionDefinition.notHaveList;
        return hasNotHas(hasList, notHasList);
    }

    public boolean isConstructor() {
        return this.lineText.trim().startsWith("constructor");
    }

    class Creator {

        public String createParams(FunctionProp fun) {
           List<String> result = new ArrayList<>();
           fun.params.forEach(e -> {
               if(Lib.asListString(Config.typeData).contains(e.type)) {
                   result.add(Config.inlineTab + "let "+ e.name + ": " + e.type + setterOfTypeData(e.type));
               }else {
                   result.add(Config.inlineTab + "let "+ e.name + ": any = {}");
               }
           });
           return result.stream().distinct().collect(Collectors.joining(";\n")) + ";";
        }

        public String setterOfTypeData(String type) {
            String result = "";
            if(Lib.asListString(Config.typeData).contains(type)) {
                if(type.equals("string")) result = " = \"\"";
                else if(type.equals("number"))  result = " = 0";
                else if(type.equals("any"))  result = " = {id: \"\"}";
                else if(type.equals("boolean"))  result = " = false";
            }
            return result;
        }

        public String createStdUT(FunctionProp fun) {
            String definerParams = createParams(fun).equals(";") ? "" : createParams(fun);
            String params = fun.params.stream().map(e -> e.name).distinct().collect(Collectors.joining(", "));
            String stdUT = newLiner( " ",
                Config.tab + Config.startOfTest+"(\"Should Check '[METHOD]' method\", () => {".replace(DEF.METHOD, fun.functionName),
                definerParams,
                Config.inlineTab + "const spyMethod = " +
                    Config.mainOfSpyOn +"spyOn(component, '[METHOD]');\n"
                    .replace(DEF.METHOD, fun.functionName)
                    .replace(DEF.PARAMS, params) +
                    Config.inlineTab + "component.[METHOD]([PARAMS]);"
                    .replace(DEF.METHOD, fun.functionName)
                    .replace(DEF.PARAMS, params),
                Config.inlineTab +"expect(spyMethod).toHaveBeenCalled();",
                Config.tab + "});"
            );
            if(!isValidFunction(fun.functionName)) return "";
            return stdUT;
        }
        public Boolean isValidFunction(String functionName) {
            CharSequence[] has = {};
            CharSequence[] notHas = {"*", ".", "for", "if", "switch"};
            return hasNotHas(has, notHas);
        }

        public String newLiner(String ... arr){
            String result = "";
            for(String text: arr) {
                if(text.equals(""))continue;
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

