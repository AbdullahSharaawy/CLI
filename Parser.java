import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class Parser {
String FPartName;
String[] args;

public boolean parse(String input){
    if(input==null || input.isEmpty())
        return false;
    args=input.trim().split("\\s+");
    
    if(!IsValidFirstPart())
        return false;
    
    return true;
}
public String getFPartName(){
   return FPartName;
}
public String[] getArgs(){
    return args;
}
private boolean IsValidFirstPart()
{
    if(args[0]=="cd" || args[0]=="ls" || args[0]=="touch" || args[0]=="pwd" || args[0]=="zip" || args[0]=="unzip" 
    || args[0]=="wc" || args[0]=="cat" || args[0]=="rm" || args[0]=="cp" || args[0]=="rmdir" || args[0]=="mkdir" )
       return true;

    return false;
}
private boolean IsValidSecondPart()
{

    if("cd".equals(args[0]) )
    {
         if ("..".equals(args[1])) {
        return true; 
    }
        Path p=Paths.get(args[1]);
        return Files.exists(p) && Files.isDirectory(p);
    }
    else if("pwd".equals(args[0]))
    {
        if(args.length>1)
        {
            return false;
        }
        return true;
    }
    else if("touch".equals(args[0]))
    {
        if(args.length!=2)
          return false;
       return isValidToCreate(args[1]);
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
}
