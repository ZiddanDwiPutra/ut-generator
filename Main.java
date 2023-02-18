import java.io.File;

public class Main {
    public static void main(String[] args) {
        File targetFolder = new File("C:\\Projects\\no-ads-link\\src");
        File resultFolder = new File("C:\\Projects\\result");
        if(!resultFolder.exists()) resultFolder.mkdirs();

        new Main(targetFolder, resultFolder);
    }

    public File targetFolder;
    public File resultFolder;
    public Main(File tgtFolder, File resultFolder){
        this.targetFolder = tgtFolder;
        this.resultFolder = resultFolder;
        this.listFilesForFolder(tgtFolder);
    }

    public void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                Boolean isTsFile = fileEntry.getName().endsWith(".component.ts");
                if(isTsFile) {
                    generateSpecTsFile(fileEntry);
                    System.out.println("\n================GENERATE_SUCCESS=====================");
                    System.out.println(fileEntry.getName());
                    System.out.println(fileEntry.getPath());
                }
            }
        }
    }

    public void generateSpecTsFile(final File tsFile) {
        String path = tsFile.getPath();
        String name = tsFile.getName().replace(".component.ts", ".component.spec.ts");
        new UTGenerator(targetFolder, resultFolder, path, name)
                .execute();

    }
}