
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileReader;
class Parser {
    String commandName;
    String[] args;

    public boolean parse(String input) {
        if (input == null || input.isEmpty())
            return false;

        input = input.trim();

       //handles redirect functions ">"
        if (input.contains(">")) {
            int idx = input.indexOf(">");
            String left = input.substring(0, idx).trim();
            String right = input.substring(idx + 1).trim();
            if (right.isEmpty())
                return false;
            commandName = "redirect";
            args = new String[]{ left, right };
            return true;
        }

        
        // Handle append: text >> file
        if (input.contains(">>")) {
            int idx = input.indexOf(">>");
            String left = input.substring(0, idx).trim();
            String right = input.substring(idx + 2).trim();
            if (right.isEmpty())
                return false;
            commandName = "append";
            args = new String[] { left, right };
            return true;
        }

        // Handle heredoc: file << DELIM
        if (input.contains("<<")) {
            int idx = input.indexOf("<<");
            String left = input.substring(0, idx).trim(); // target file
            String right = input.substring(idx + 2).trim(); // delim
            if (left.isEmpty() || right.isEmpty())
                return false;
            commandName = "heredoc";
            args = new String[] { left, right };
            return true;
        }

        // Normal tokenization
        args = input.split("\\s+");
        commandName = args[0];

        if (!IsValidCommandName())
            return false;
        else if (!IsValidSecondPart())
            return false;
        return true;
    }

    public String getcommandName() {
        return commandName;
    }

    public String[] getArgs() {
        return args;
    }

    private boolean IsValidCommandName() {
        if (commandName.equals("cd") || commandName.equals("ls") || commandName.equals("touch") || commandName.equals("pwd")
                || commandName.equals("zip") || commandName.equals("unzip") || commandName.equals("wc") || commandName.equals("cat")
                || commandName.equals("rm") || commandName.equals("cp") || commandName.equals("rmdir") || commandName.equals("mkdir")
                || commandName.equals("append") || commandName.equals("redirect") || commandName.equals("heredoc")) {
            return true;
        }

        return false;
    }

    private boolean IsValidSecondPart() {

        if ("cd".equals(commandName)) {
            if (args.length == 1)
                return true;
            if ("..".equals(args[1])) {
                return true;
            }

            Path p = Terminal.TargetDir.toPath().resolve(args[1]).normalize();
            return Files.exists(p) && Files.isDirectory(p);
        } else if ("pwd".equals(commandName)) {
            if (args.length == 1) {
                return true;
            }
            return false;
        } else if ("touch".equals(commandName)) {
            if (args.length != 2)
                return false;
            return isValidToCreate(args[1]);
        }  else if ("cp".equals(commandName)) {
        if (args.length < 2) return false;
                if ("-r".equals(args[1])) {
            if (args.length != 4) return false; 
            return isValidSource(args[2]) && isValidDestination(args[3]);
        } else {
            if (args.length != 3) return false; 
            return isValidSource(args[1]) && isValidDestination(args[2]);
        }
    } else if ("rm".equals(commandName)) {
        if (args.length != 2) return false;
        return isValidSource(args[1]);
    } else if ("ls".equals(commandName)) {
            if (args.length == 1)
                return true;
            else if (">".equals(args[1]) || ">>".equals(args[1])) {
                if (args.length <= 2)
                    return false;
                return isWritableFile(args[2]);
            }
        } else if ("zip".equals(commandName)) {
            // minimal validation: require at least 2 params (output + input(s)) or allow -r switch
            if (args.length < 2)
                return false;
            return true;
        } else if ("unzip".equals(commandName)) {
            // unzip input.zip
            if (args.length != 2)
                return false;
            return true;
        } else if ("append".equals(commandName)) {
            // args = [text, file]
            if (args.length != 2)
                return false;
            return true;
        } else if ("heredoc".equals(commandName)) {
            // args = [file, delim]
            if (args.length != 2)
                return false;
            return true;
        }else if ("cat".equals(commandName)){
            //args = [file, || file]
            if (args.length < 2 || args.length > 3){
                System.out.println("Error: wrong number of argument");
                return false;
            }else if (args.length == 2){
                if(!isWritableFile(args[1])){
                    System.out.println("Error: not a file");
                    return false;
                }
            }else if (args.length == 3){
               if(!isWritableFile(args[1]) || !isWritableFile(args[2])){
                    System.out.println("Error: not a file");
                    return false;
                }
            }
            return true;
        }else if ("wc".equals(commandName)){
            //args = [file]
            if(args.length != 2){
                return false;
            }else if (!isValidSource(args[1])){
                System.out.println("Error: not a file");
                return false;
            }return true; 
        }else if ("redirect".equals(commandName)) {
            if (args.length != 2)
                return false;
            return true;
        }

        return false;
    }

    private boolean isValidToCreate(String path) {
        try {
            Path file = Paths.get(path);
            Path parent = file.getParent();
            if (parent == null) {
                Path currentDirectory = Paths.get(".").toAbsolutePath();
                return Files.isWritable(currentDirectory);
            } else if (Files.isDirectory(parent) && Files.isWritable(parent))
                return true;

        } catch (Exception e) {
            return false; // Invalid path or unwritable directory
        }
        return false;
    }

    private boolean isWritableFile(String path) {
        try {
            Path file = Paths.get(path);
            if (Files.isWritable(file))
                return true;

        } catch (Exception e) {
            return false; // Invalid path or unwritable directory
        }
        return false;
    }

    private boolean isValidSource(String path) {
    try {
        Path srcPath = Terminal.TargetDir.toPath().resolve(path).normalize();
        return Files.exists(srcPath);
    } catch (Exception e) {
        return false;
    }
    }

   private boolean isValidDestination(String path) {
    try {
        Path dstPath = Terminal.TargetDir.toPath().resolve(path).normalize();
        // For destination, we only need to check if the parent directory exists and is writable
        Path parent = dstPath.getParent();
        if (parent == null) {
            // If no parent, it's current directory
            return Files.isWritable(Terminal.TargetDir.toPath());
        }
        return Files.exists(parent) && Files.isWritable(parent);
    } catch (Exception e) {
        return false;
    }
    }
} 
