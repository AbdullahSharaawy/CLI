import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class Parser {
    String commandName;
    String[] args;

    public static final String[] VALID_COMMANDS = {
        "cd", "ls", "touch", "pwd", "zip", "unzip", "wc", "cat",
        "rm", "cp", "rmdir", "mkdir", "append", "redirect", "heredoc",
        "help", "exit"
    };

    private static final Set<String> COMMAND_SET = new HashSet<>(Arrays.asList(VALID_COMMANDS));


    public boolean parse(String input) {
        if (input == null || input.isEmpty())
            return false;

        input = input.trim();

        String operator = null;
        if (input.contains(">>")) {
            operator = ">>";
        }
        else if (input.contains("<<")) {
            operator = "<<";
        } 
        else if (input.contains(">")) {
            operator = ">";
        }

        if (operator != null) {
            int idx = input.indexOf(operator);
            String left = input.substring(0, idx).trim();
            String right = input.substring(idx + operator.length()).trim();
            if (left.isEmpty() || right.isEmpty()) {
                return false;
            }
            switch (operator) {
                case ">>": commandName = "append"; break;
                case "<<": commandName = "heredoc"; break;
                case ">": commandName = "redirect"; break;
            }
            args = new String[]{ left, right };
            return true;
        }

        args = input.split("\\s+");
        commandName = args[0];

        return IsValidCommandName() && IsValidSecondPart();
    }

    public String getcommandName() {
        return commandName;
    }

    public String[] getArgs() {
        return args;
    }

    private boolean IsValidCommandName() {
        return COMMAND_SET.contains(commandName);
    }

    private Path getResolvedPath(String path) {
        try {
            return Terminal.TargetDir.toPath().resolve(path).normalize();
        } catch (InvalidPathException e) {
            return null; 
        }
    }

    private boolean pathExists(String path) {
        Path p = getResolvedPath(path);
        return p != null && Files.exists(p);
    }

    private boolean directoryExists(String path) {
        Path p = getResolvedPath(path);
        return p != null && Files.exists(p) && Files.isDirectory(p);
    }

    private boolean fileExists(String path) {
        Path p = getResolvedPath(path);
        return p != null && Files.exists(p) && Files.isRegularFile(p);
    }

    private boolean parentDirectoryIsValid(String path) {
        Path p = getResolvedPath(path);
        if (p == null) return false;

        Path parent = p.getParent();
        if (parent == null) {
            return Files.isWritable(Terminal.TargetDir.toPath());
        }
        return Files.exists(parent) && Files.isDirectory(parent) && Files.isWritable(parent);
    }

    private boolean isValidSource(String path) {
        return pathExists(path); 
    }

   private boolean isValidDestination(String path) {
       return parentDirectoryIsValid(path);
   }

    private boolean isValidToCreate(String path) {
        return parentDirectoryIsValid(path);
    }

    private boolean IsValidSecondPart() {

        if ("cd".equals(commandName)) {
            if (args.length == 1) return true;
            if ("..".equals(args[1])) return true;
            return directoryExists(args[1]);
        }
        
        else if ("pwd".equals(commandName)) {
            return args.length == 1;
        }
        
        else if ("touch".equals(commandName)) {
            return (args.length == 2) && isValidToCreate(args[1]);
        }
        
        else if ("cp".equals(commandName)) {
            if (args.length > 1 && "-r".equals(args[1])) {
                return (args.length == 4) && isValidSource(args[2]) && isValidDestination(args[3]);
            }
            else {
                return (args.length == 3) && isValidSource(args[1]) && isValidDestination(args[2]);
            }
        }
        
        
        else if ("rm".equals(commandName)) {
            if (args.length > 1 && "-r".equals(args[1])) {
                return (args.length == 3) && isValidSource(args[2]);
            } else {
                return (args.length == 2) && isValidSource(args[1]);
            }
        }
        
        else if ("ls".equals(commandName)) {
            return args.length == 1; 
        }
        
        else if ("zip".equals(commandName)) {
            if (args.length > 1 && "-r".equals(args[1])) {
                return (args.length >= 4) && parentDirectoryIsValid(args[2]);
            }
             else {
                return (args.length >= 3) && parentDirectoryIsValid(args[1]);
            }
        }
        
        else if ("unzip".equals(commandName)) {
            if (args.length == 2) {
                return fileExists(args[1]);
            }
            if (args.length == 4 && "-d".equals(args[2])) {
                return fileExists(args[1]) && parentDirectoryIsValid(args[3]);
            }
            return false;
        }

        else if ("mkdir".equals(commandName)) {
            if (args.length < 2) {
                return false;
            }
            return Arrays.stream(args, 1, args.length).allMatch(this::isValidToCreate);
        }
        
        else if ("rmdir".equals(commandName)) {
            if (args.length != 2) {
                return false; 
            }
            return ("*".equals(args[1]) || directoryExists(args[1]));
        }
        else if ("append".equals(commandName)) {
            return args.length == 2;
        }
        
        else if ("heredoc".equals(commandName)) {
            return args.length == 2;
        }
    
        else if ("cat".equals(commandName)){
            if (args.length == 2) {
                return fileExists(args[1]);
            }
            else if (args.length == 3) {
                return fileExists(args[1]) && fileExists(args[2]);
            }
            return false; 
        }
        
        else if ("wc".equals(commandName)){
            return (args.length == 2) && fileExists(args[1]);

        }
        
        else if ("redirect".equals(commandName)) {
            return args.length == 2;
        }
        
        else if ("help".equals(commandName)) {
            return args.length == 1;
        }
    
        else if ("exit".equals(commandName)) {
            return args.length == 1;
        }

        return false;
    }
}


public class Terminal {
    public static File TargetDir;
    Parser parser;
    private Scanner sc;

    public Terminal() {
        TargetDir = new File(Paths.get("").toAbsolutePath().toString());
        parser = new Parser();
        sc = new Scanner(System.in);
    }

    public String pwd() {
        String CurrentPath = TargetDir.getAbsolutePath().toString();
        return CurrentPath;
    }

    public void cd(String[] args) {
        if (args.length == 1) {
                String homePath = System.getProperty("user.home");
                TargetDir = new File(homePath);
                return;
            }
            else if (args[1].equals("..") && TargetDir.getParentFile() != null) {
                TargetDir = TargetDir.getParentFile();
                return;
            }
            else if (args[1].equals("."))
                return;
            Path p = Terminal.TargetDir.toPath().resolve(args[1]).normalize();
            File Validargs = new File(p.toString());
            TargetDir = Validargs;
            return;
    }

    private List<Path> LSHelper() {
        try (Stream<Path> files = Files.walk(TargetDir.toPath(), 1)) {
            return files.filter(path -> !path.equals(TargetDir.toPath())).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<String> ls() {
        List<Path> paths = LSHelper();
        List<String> subDir = new ArrayList<>();
        for (Path p : paths) {
            String FileName = (p.getFileName().toString());

            if (Files.isSymbolicLink(p)) {
                FileName += "@";
            }
            else if (Files.isDirectory(p)) {
                FileName += "/";
            }
            else if (Files.isExecutable(p)) {
                FileName += "*";
            }
            FileName = FileName.replace(" ", ".");
            subDir.add(FileName);
        }
        return subDir;
    }

    public List<String> minusLS() {
        List<Path> paths = LSHelper();
        List<String> subDir = new ArrayList<>();
        for (Path p : paths) {
            String FileName = (p.getFileName().toString());
            if (Files.isExecutable(p)) {
                FileName += "*";
                FileName = FileName.replace(" ", ".");
                subDir.add(FileName);
            }
        }
        return subDir;
    }

    public boolean touch(String FilePath) {
        try {
            Path path = Paths.get(FilePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Path p = Terminal.TargetDir.toPath().resolve(FilePath).normalize();
            File NewFile = new File(p.toString());

            if (NewFile.createNewFile()) {
                return true;
            } else {
                return false;
            }
        } catch (IOException | InvalidPathException e) {
            return false;
        }
    }

    private void cp(String[] args) {
        boolean recursive = false;
        String source, destination;
        
        if (args.length == 4 && "-r".equals(args[1])) {
            recursive = true;
            source = args[2];
            destination = args[3];
        }
        else if (args.length == 3) {
            source = args[1];
            destination = args[2];
        }
        else {
            System.err.println("Usage: cp [source] [destination]  or  cp -r [source] [destination]");
            return;
        }
        
        Path srcPath = Terminal.TargetDir.toPath().resolve(source).normalize();
        Path dstPath = Terminal.TargetDir.toPath().resolve(destination).normalize();
        
        try {
            if (!Files.exists(srcPath)) {
                System.err.println("cp: " + source + ": No such file or directory");
                return;
            }
            
            if (Files.isDirectory(srcPath)) {
                if (!recursive) {
                    System.err.println("cp: " + source + " is a directory (not copied)");
                    System.err.println("Use cp -r to copy directories recursively");
                    return;
                }
                copyDirectoryRecursive(srcPath, dstPath);
            } else {
                copyFile(srcPath, dstPath);
            }
            System.out.println("Copied: " + source + " → " + destination);
        } catch (IOException e) {
            System.err.println("cp error: " + e.getMessage());
        }
    }

    private void copyFile(Path source, Path destination) throws IOException {
        if (Files.isDirectory(destination)) {
            destination = destination.resolve(source.getFileName());
        }
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyDirectoryRecursive(Path source, Path destination) throws IOException {
        if (Files.isRegularFile(destination)) {
            throw new IOException("Cannot copy directory to a file");
        }
        
        if (!Files.exists(destination)) {
            Files.createDirectories(destination);
        }
        
        Files.walk(source).forEach(srcPath -> {
            try {
                Path relativePath = source.relativize(srcPath);
                Path dstPath = destination.resolve(relativePath);
                
                if (Files.isDirectory(srcPath)) {
                    if (!Files.exists(dstPath)) {
                        Files.createDirectories(dstPath);
                    }
                } else {
                    Files.copy(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy: " + srcPath, e);
            }
        });
    }

    private void rm(String[] args) {
        boolean recursive = false;
        String target;

        if (args.length == 3 && "-r".equals(args[1])) {
            recursive = true;
            target = args[2];
        } else if (args.length == 2) {
            if ("-r".equals(args[1])) {
                 System.err.println("Usage: rm -r [directory]");
                 return;
            }
            recursive = false;
            target = args[1];
        } else {
            System.err.println("Usage: rm [file]  or  rm -r [directory]");
            return;
        }
        
        Path targetPath = Terminal.TargetDir.toPath().resolve(target).normalize();
        
        try {
            if (!Files.exists(targetPath)) {
                System.err.println("rm: " + target + ": No such file or directory");
                return;
            }
            
            if (Files.isDirectory(targetPath)) {
                if (recursive) {
                    deleteDirectoryRecursive(targetPath);
                    System.out.println("Removed directory: " + target);
                } else {
                    try (Stream<Path> entries = Files.list(targetPath)) {
                        if (entries.findAny().isPresent()) {
                            System.err.println("rm: " + target + ": Is a directory (use -r to delete)");
                            return;
                        }
                    }
                    Files.delete(targetPath);
                    System.out.println("Removed empty directory: " + target);
                }
            } else {
                if(recursive){
                    System.err.println("rm: -r has no effect on files: " + target);
                }
                Files.delete(targetPath);
                System.out.println("Removed file: " + target);
            }
            
        } catch (IOException e) {
            System.err.println("rm error: " + e.getMessage());
        }
    }

    private void deleteDirectoryRecursive(Path path) throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        System.err.println("Failed to delete " + p + ": " + e.getMessage());
                    }
                });
        }
    }

    private void sys_zip(String[] args) {
        boolean recursive = false;
        int argsOffset = 0; 
        if (args.length > 0 && "-r".equals(args[0])) {
            recursive = true;
            argsOffset = 1; 
        }

        if (args.length - argsOffset < 2) {
            System.err.println("Usage: zip [-r] [output.zip] [file1] [file2]...");
            return;
        }

        String output = args[argsOffset + 0]; 
        List<String> inputs = new ArrayList<>(); 
        for (int i = argsOffset + 1; i < args.length; i++) {
            inputs.add(args[i]);
        }
        
        Path outputPath = Terminal.TargetDir.toPath().resolve(output).normalize();

        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
                ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (String inFile : inputs) {
                Path path = Terminal.TargetDir.toPath().resolve(inFile).normalize();
                if (!Files.exists(path)) {
                    System.err.println("Missing file: " + inFile);
                    continue;
                }

                if (Files.isDirectory(path)) {
                    if (!recursive) {
                        System.err.println("Skipping directory (use -r to recurse): " + inFile);
                        continue;
                    }
                    Path baseName = path.getFileName(); 
                    Files.walk(path).filter(p -> Files.isRegularFile(p)).forEach(p -> {
                        try {
                            Path rel = path.relativize(p);
                            String entryName = baseName.toString() + "/" + rel.toString().replace("\\", "/");
                            ZipEntry entry = new ZipEntry(entryName);
                            entry.setTime(Files.getLastModifiedTime(p).toMillis());
                            zos.putNextEntry(entry);
                            Files.copy(p, zos);
                            zos.closeEntry();
                            System.out.println("zipped: " + p.toString());
                        } catch (IOException e) {
                            System.err.println("zip error: " + e.getMessage());
                        }
                    });
                } else { 
                    ZipEntry entry = new ZipEntry(path.getFileName().toString());
                    zos.putNextEntry(entry);
                    Files.copy(path, zos);
                    zos.closeEntry();
                    System.out.println("zipped: " + inFile);
                }
            }

            System.out.println("ZIP completed → " + outputPath.toString());
        } catch (IOException e) {
            System.err.println("sys_zip error: " + e.getMessage());
        }
    }

private void sys_unzip(String[] args) {
        String inputZip;
        Path destinationPath;
        if (args.length == 3 && "-d".equals(args[1])) {
            inputZip = args[0];
            destinationPath = Terminal.TargetDir.toPath().resolve(args[2]).normalize();
        } else if (args.length == 1) {
            inputZip = args[0];
            destinationPath = Terminal.TargetDir.toPath(); // <-- المسار الحالي
        } else {
            System.err.println("Usage: unzip [input.zip] [-d destination_path]");
            return;
        }

        Path zipPath = Terminal.TargetDir.toPath().resolve(inputZip).normalize();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = destinationPath.resolve(entry.getName()).normalize();
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    if (outPath.getParent() != null) {
                        Files.createDirectories(outPath.getParent());
                    }
                    Files.copy(zis, outPath, StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("extracted: " + entry.getName() + " to " + outPath.toString());
                zis.closeEntry();
            }
            System.out.println("UNZIP completed.");
        } catch (IOException e) {
            System.err.println("sys_unzip error: " + e.getMessage());
        }
    }

    private String cat(String[] args){
        StringBuilder output = new StringBuilder(); 
        try {
            for(int i = 1; i < args.length; i++){
                Path filePath = Terminal.TargetDir.toPath().resolve(args[i]).normalize();
                BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()));  
                String line;
                while ( (line = reader.readLine()) != null){
                    output.append(line).append(System.lineSeparator());
                }
                reader.close();
            }
        } catch (IOException e){
            e.printStackTrace();
            return "Error reading file: " + e.getMessage(); 
        }
        return output.toString(); 
    }

private String wc(String[] args){
        try{
            Path filePath = Terminal.TargetDir.toPath().resolve(args[1]).normalize();
            BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()));
            String line;

            int lineCount = 0;
            int wordCount = 0;
            int charCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;
                charCount += line.length();
                String[] words = line.trim().split("\\s+");
                if (!line.trim().isEmpty()) {
                    wordCount += words.length;
                }
            }
            reader.close();
            return "Lines: " + lineCount + ", Words: " + wordCount + ", Chars: " + charCount + " in " + args[1];
        
        } catch (IOException e){
            e.printStackTrace();
            return "Error reading file: " + e.getMessage();
        }
    }

    private void handleRedirect(String[] args) {
        String innerCommand = args[0];
        String fileName = args[1];
        Path target = Terminal.TargetDir.toPath().resolve(fileName).normalize();

        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
           
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream ps = new java.io.PrintStream(baos);
            PrintStream oldOut = System.out;
            System.setOut(ps);  
           
            Parser innerParser = new Parser();
            boolean parsed = innerParser.parse(innerCommand);

            if (parsed) {
                this.parser = innerParser;
                chooseCommandAction();
                System.out.flush();
                System.setOut(oldOut);
            } else {
                System.setOut(oldOut);
                System.out.println(baos.toString());
                return;
            }
          
            try (FileWriter outfile = new FileWriter(target.toFile(), false)) {
                outfile.write(baos.toString());
            }

            System.out.println("Output written to: " + target.toString());

        } catch (IOException e) {
            System.err.println("sys_redirect error: " + e.getMessage());
        }
    }
    
    private void handleAppend(String[] args) {
        String commandOrText = args[0];
        String file = args[1];
        Path target = Terminal.TargetDir.toPath().resolve(file).normalize();

        Parser innerParser = new Parser();
        boolean isCommand = innerParser.parse(commandOrText);
        String textToAppend;

        if (isCommand) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                PrintStream oldOut = System.out;
                System.setOut(ps); 

                this.parser = innerParser; 
                chooseCommandAction(); 

                System.out.flush();
                System.setOut(oldOut); 
                textToAppend = baos.toString();

            } catch (Exception e) {
                System.err.println("append error (inner command failed): " + e.getMessage());
                return;
            }
            
        } else {
            textToAppend = commandOrText + System.lineSeparator();
        }

        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            try (FileWriter fw = new FileWriter(target.toFile(), true)) { 
                fw.write(textToAppend);
                System.out.println("Appended to: " + target.toString());
            }
        } catch (IOException e) {
            System.err.println("sys_append error: " + e.getMessage());
        }
    }

    private void handleHereDoc(String[] args, Scanner sc) {
        String file = args[0];
        String delim = args[1];

        System.out.println("Enter text, end with line: " + delim);
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = sc.nextLine();
            if (line.equals(delim))
                break;
            sb.append(line).append(System.lineSeparator());
        }
        Path out = Terminal.TargetDir.toPath().resolve(file).normalize();
        try {
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
            Files.write(out, sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Written to: " + out.toString());
        } catch (IOException e) {
            System.err.println("sys_heredoc error: " + e.getMessage());
        }
    }

    private void mkdir(String[] args) {
        for (int i = 1; i < args.length; i++) {
            String dirName = args[i];
            try {
                Path path = Terminal.TargetDir.toPath().resolve(dirName).normalize();
                Files.createDirectory(path);
                System.out.println("Directory created: " + dirName);
            } catch (IOException e) {
                System.err.println("mkdir error for '" + dirName + "': " + e.getMessage());
            }
        }
    }

    private boolean isDirectoryEmpty(Path path) {
        if (!Files.isDirectory(path)) {
            return false;
        }
        try (Stream<Path> entries = Files.list(path)) {
            return !entries.findAny().isPresent();
        } catch (IOException e) {
            return false; 
        }
    }
private void rmdir(String[] args) {
    String target = args[1];
    if ("*".equals(target)) {
        List<Path> dirsInCurrent;
        try (Stream<Path> entries = Files.list(TargetDir.toPath())) {
            dirsInCurrent = entries.filter(Files::isDirectory).collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("rmdir error: Failed to read current directory: " + e.getMessage());
            return;
        }

        if (dirsInCurrent.isEmpty()) {
            System.out.println("rmdir: No directories found in current location.");
            return;
        }
        int removedCount = 0;
        for (Path path : dirsInCurrent) {
            if (isDirectoryEmpty(path)) {
                try {
                    Files.delete(path);
                    System.out.println("Removed empty directory: " + path.getFileName());
                    removedCount++;
                } catch (IOException e) {
                    System.err.println("rmdir error deleting " + path.getFileName() + ": " + e.getMessage());
                }
            } else {
                System.err.println("rmdir error: Directory is not empty: " + path.getFileName());
            }
        }
        if(removedCount == 0 && !dirsInCurrent.isEmpty()){
            System.out.println("rmdir: No empty directories to remove.");
        }
    }
    else {
        Path path = Terminal.TargetDir.toPath().resolve(target).normalize();
        
        if (isDirectoryEmpty(path)) {
            try {
                Files.delete(path);
                System.out.println("Directory removed: " + target);
            } catch (IOException e) {
                System.err.println("rmdir error: " + e.getMessage());
            }
        } else {
            System.err.println("rmdir error: Directory is not empty: " + target);
        }
    }
}

    private List<String> handleHelp() {
        List<String> helpLines = new ArrayList<>();
        helpLines.add("Available commands:");
        for (String cmd : Parser.VALID_COMMANDS) {
            switch (cmd) {
                case "cd": helpLines.add("- cd [dir]: Change directory (e.g., cd .., cd /)"); break;
                case "ls": helpLines.add("- ls: List files in current directory"); break;
                case "touch": helpLines.add("- touch [file]: Create an empty file"); break;
                case "pwd": helpLines.add("- pwd: Print working directory"); break;
                case "cp": helpLines.add("- cp [-r] [src] [dest]: Copy file or directory"); break;
                case "rm": helpLines.add("- rm [-r] [target]: Remove file or directory"); break;
                case "mkdir": helpLines.add("- mkdir [dir]: Create a new directory"); break;
                case "rmdir": helpLines.add("- rmdir [dir]: Remove an empty directory"); break;
                case "cat": helpLines.add("- cat [file1] [file2]: Show file contents"); break;
                case "wc": helpLines.add("- wc [file]: Count lines, words, and chars"); break;
                case "zip": helpLines.add("- zip [-r] [out.zip] [in...]: Zip files/dirs"); break;
                case "unzip": helpLines.add("- unzip [file.zip]: Unzip a file"); break;
                case "redirect": helpLines.add("- [cmd] > [file]: Redirect command output to file"); break;
                case "append": helpLines.add("- [cmd] >> [file]: Append command output to file"); break;
                case "heredoc": helpLines.add("- [file] << [DELIM]: Write to file until DELIM"); break;
                case "help": helpLines.add("- help: Show this help message"); break;
                case "exit": helpLines.add("- exit: Exit the terminal"); break;
            }
        }
        return helpLines;
    }

    public void chooseCommandAction() {
        if (parser.commandName.equals("pwd")) {
            System.out.println(pwd());
            return;
        } 
        
        else if (parser.commandName.equals("cd")) {
            cd(parser.args);
            return;
        }
        
        else if (parser.commandName.equals("ls")) {
            List<String> subDir = ls();
            subDir.forEach(item -> System.out.println(item));
            return;
        }
        
        else if (parser.commandName.equals("touch")) {
            boolean done = touch(parser.args[1]);
            if (!done) {
                System.out.println(parser.commandName + parser.args[1] + ": command not found.");
            }
            return;
        }
        
        else if (parser.commandName.equals("cp")) {
            cp(parser.args);
            return;
        }
        
        else if (parser.commandName.equals("rm")) {
            rm(parser.args);
            return;
        }
        
        else if (parser.commandName.equals("mkdir")) {
            mkdir(parser.args);
            return;
        }
        
        else if (parser.commandName.equals("rmdir")) {
            rmdir(parser.args);
            return;
        }
        
        else if (parser.commandName.equals("help")) {
            handleHelp().forEach(line -> System.out.println(line));
            return;
        }
        
        else if (parser.commandName.equals("zip")) {
            String[] callArgs = new String[parser.args.length - 1];
            System.arraycopy(parser.args, 1, callArgs, 0, callArgs.length);
            sys_zip(callArgs);
            return;
        }
        
        else if (parser.commandName.equals("unzip")) {
            String[] callArgs = new String[parser.args.length - 1];
            System.arraycopy(parser.args, 1, callArgs, 0, callArgs.length);
            sys_unzip(callArgs);
            return;
        }
        
        else if (parser.commandName.equals("append")) {
            handleAppend(parser.args);
            return;
        }
        
        else if (parser.commandName.equals("heredoc")) {
            handleHereDoc(parser.args, sc);
            return;
        }
        
        else if (parser.commandName.equals("cat")){
            System.out.print(cat(parser.args));
            return;
        }
        
        else if (parser.commandName.equals("wc")){
            System.out.println(wc(parser.args));
            return;
        }
        
        else if (parser.commandName.equals("redirect")){
            handleRedirect(parser.args);
            return;
        }
    }

    public static void main(String[] args) {
        Terminal terminal = new Terminal();

        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("> ");
            
            String instruction = scanner.nextLine();
            instruction=instruction.strip();
            if (!terminal.parser.parse(instruction)) {
                System.out.println("Error: command not found or invalid paramaters are entered.");
                continue;
            }
            if (terminal.parser.getcommandName().equals("exit")) {
                System.out.println("Exiting terminal.");
                break;
            }
            terminal.chooseCommandAction();
        }
        scanner.close();
    }
}

