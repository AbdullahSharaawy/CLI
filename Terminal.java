import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class Terminal {
    public static File TargetDir;
    Parser parser;
    private Scanner sc; // for heredoc

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
            TargetDir = new File(pwd());
            return;
        } else if (args[1].equals("..") && TargetDir.getParentFile() != null) {
            TargetDir = TargetDir.getParentFile();
            return;
        } else if (args[1].equals("."))
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

            // add slach if it is folder not file
            // Mark symbolic links with '@'
            if (Files.isSymbolicLink(p)) {
                FileName += "@";
            } else if (Files.isDirectory(p)) {
                FileName += "/";
            } else if (Files.isExecutable(p)) {
                FileName += "*";
            }

            // replace each space with dot
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

            // add slach if it is folder not file
            // Mark symbolic links with '@'

            if (Files.isExecutable(p)) {
                FileName += "*";
                // replace each space with dot
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

    private boolean isValidFilePath(String path) {
        try {

            String FilePath = Paths.get(path).toString();
            int DotIndex = FilePath.lastIndexOf('.');
            if (DotIndex <= 0 || DotIndex == FilePath.length() - 1)
                return false;
            String extenstion = FilePath.substring(DotIndex + 1).toLowerCase();

            String[] allowedExtensions = { "txt", "pdf", "doc", "docx", "csv", "java", "cpp", "py", "html", "xml" };

            for (String allowed : allowedExtensions) {
                if (extenstion.equals(allowed))
                    return true;
            }

            return false;
        } catch (InvalidPathException ex) {
            return false;
        }
    }

    public void chooseCommandAction() {
        if (parser.commandName.equals("pwd") && parser.args.length == 1) {
            System.out.println(pwd());
            return;
        } else if (parser.commandName.equals("cd")) {

            cd(parser.args);
            return;
        } else if (parser.commandName.equals("ls") && parser.args.length == 1) {
            List<String> subDir = ls();
            subDir.forEach(item -> System.out.println(item));
            return;
        } else if (parser.commandName.equals("touch")) {
            boolean done = touch(parser.args[1]);
            if (done)
                return;
            else {
                System.out.println(parser.commandName + parser.args[1] + ": command not found.");
                return;
            }
        } else if (parser.commandName.equals("zip")) {
            // pass args except command name
            String[] callArgs = new String[parser.args.length - 1];
            System.arraycopy(parser.args, 1, callArgs, 0, callArgs.length);
            sys_zip(callArgs);
            return;
        } else if (parser.commandName.equals("unzip")) {
            String[] callArgs = new String[parser.args.length - 1];
            System.arraycopy(parser.args, 1, callArgs, 0, callArgs.length);
            sys_unzip(callArgs);
            return;
        } else if (parser.commandName.equals("append")) {
            // args = [text, file]
            handleAppend(parser.args);
            return;
        } else if (parser.commandName.equals("heredoc")) {
            handleHereDoc(parser.args, sc);
            return;
        }
    }

    // ===== New system calls copied/adapted from MiniTerminal.java =====

    // sys_zip behaves like: zip output.zip input1 input2 ...
    // supports -r (recursive). Recognizes -r anywhere in args.
    private void sys_zip(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: zip [output.zip] [file1] [file2]...  (or zip -r output.zip dir1 dir2 ...)");
            return;
        }

        // detect -r
        boolean recursive = false;
        List<String> argList = new ArrayList<>();
        for (String a : args) {
            if ("-r".equals(a) || "--recurse".equals(a))
                recursive = true;
            else
                argList.add(a);
        }

        if (argList.size() < 2) {
            System.err.println("Usage after removing flags: zip [output.zip] [file1] ...");
            return;
        }

        String output = argList.get(0);
        List<String> inputs = argList.subList(1, argList.size());

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
                    // walk directory and add files
                    Path baseName = path.getFileName(); // directory name to prefix entries
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
                } else { // single file
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

    // sys_unzip behaves like: unzip input.zip
    private void sys_unzip(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: unzip [input.zip]");
            return;
        }
        String inputZip = args[0];
        Path zipPath = Terminal.TargetDir.toPath().resolve(inputZip).normalize();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = Terminal.TargetDir.toPath().resolve(entry.getName()).normalize();
                // create parent dirs if needed
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    if (outPath.getParent() != null) {
                        Files.createDirectories(outPath.getParent());
                    }
                    Files.copy(zis, outPath, StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("extracted: " + entry.getName());
                zis.closeEntry();
            }
            System.out.println("UNZIP completed.");
        } catch (IOException e) {
            System.err.println("sys_unzip error: " + e.getMessage());
        }
    }

    // Append: args = [text, file]
    private void handleAppend(String[] args) {
        String text = args[0];
        String file = args[1];
        Path target = Terminal.TargetDir.toPath().resolve(file).normalize();
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            try (FileWriter fw = new FileWriter(target.toFile(), true)) {
                fw.write(text + System.lineSeparator());
                System.out.println("Appended text to: " + target.toString());
            }
        } catch (IOException e) {
            System.err.println("sys_append error: " + e.getMessage());
        }
    }

    // Heredoc: args = [file, delim]
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

    public static void main(String[] args) {
        Terminal terminal = new Terminal();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String instruction = scanner.nextLine();
            if (!terminal.parser.parse(instruction)) {
                System.out.println(instruction + ": command not found.");
                continue;
            }
            terminal.chooseCommandAction();
        }

    }
}
