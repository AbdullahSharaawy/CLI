import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class Parser {
String commandName;
String[] args;

public boolean parse(String input){
    if(input==null || input.isEmpty())
        return false;
    
    args=input.trim().split("\\s+");
    commandName=args[0];
    
    if(!IsValidCommandName())
        return false;
    else if(!IsValidSecondPart())
    return false;
    return true;
}
public String getcommandName(){
   return commandName;
}
public String[] getArgs(){
    return args;
}
private boolean IsValidCommandName() {
    if (commandName.equals("cd") || commandName.equals("ls") || commandName.equals("touch") || commandName.equals("pwd") || commandName.equals("zip") || commandName.equals("unzip")
            || commandName.equals("wc") || commandName.equals("cat") || commandName.equals("rm") || commandName.equals("cp") || commandName.equals("rmdir") || commandName.equals("mkdir")) {
        return true;
    }

    return false;
}
private boolean IsValidSecondPart()
{

    if("cd".equals(commandName) )
    {
         if(args.length==1)
         return true;
         if ("..".equals(args[1])) {
        return true; 
    }
        Path p=Paths.get(args[1]);
        return Files.exists(p) && Files.isDirectory(p);
    }
    else if("pwd".equals(commandName))
    {
        if(args.length==1)
        {
            return true;
        }
        return false;
    }
    else if("touch".equals(commandName))
    {
        if(args.length!=2)
          return false;
       return isValidToCreate(args[1]);
    }
    else if("ls".equals(commandName))
    {
        if(args.length==1)
        return true;
        else if(">".equals(args[1]) || ">>".equals(args[1]))
        {
            if(args.length<=2)
            return false;
           return isWritableFile(args[2]);
        }
    }
    return false;
}

private boolean isValidToCreate(String path)
{
    try{
    Path file=Paths.get(path);
    Path parent=file.getParent();
    if (parent == null) {
            Path currentDirectory = Paths.get(".").toAbsolutePath();
            return Files.isWritable(currentDirectory);
        }
    else if( Files.isDirectory(parent) && Files.isWritable(parent))
    return true;
    
    }
    catch (Exception e) {
            return false; // Invalid path or unwritable directory
        }
        return false;
}
private boolean isWritableFile(String path)
{
    try{
    Path file=Paths.get(path);
    if( Files.isWritable(file))
       return true;
  
    }
    catch (Exception e) {
            return false; // Invalid path or unwritable directory
        }
        return false;
}
}