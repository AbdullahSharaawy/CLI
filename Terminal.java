import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Scanner;
public class Terminal {
    public static File TargetDir;
    Parser parser;

    public Terminal()
    {
        TargetDir=new File(Paths.get("").toAbsolutePath().toString());
        parser=new Parser();
    }
    public String pwd()
    {
        String CurrentPath = TargetDir.getAbsolutePath().toString();
        return CurrentPath;
    }
    public void cd(String[] args)
    {
        if ( args.length==1) 
        {
          TargetDir=new File(pwd());
          return;
        }
        else if(args[1].equals("..") && TargetDir.getParentFile()!=null   )
        {
           TargetDir=TargetDir.getParentFile();
            return;
        }
        else if(args[1].equals("."))
        return;
        Path p = Terminal.TargetDir.toPath().resolve(args[1]).normalize();
        File Validargs=new File(p.toString());
        TargetDir=Validargs;
         return;
    }
    private List<Path> LSHelper() {
       
        try ( Stream<Path> files = Files.walk(TargetDir.toPath(),1)){
           
            return files
            .filter(path->!path.equals(TargetDir.toPath())).collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
            return List.of(); 
        }
    }
    public List<String> ls()
    {
        List<Path> paths = LSHelper();
        List<String> subDir=new ArrayList<>();
        for(Path p:paths)
        {
            String FileName=(p.getFileName().toString());
           
            // add slach if it is folder not file
             // Mark symbolic links with '@'
        if (Files.isSymbolicLink(p)) {
            FileName += "@";
        }
        else if(Files.isDirectory(p))
            {
                FileName+="/";
            }
        else if(Files.isExecutable(p))
            {
                FileName+="*";
            }
            
            // replace each space with dot
            FileName=FileName.replace(" ", ".");
            subDir.add(FileName);
            
        }
        return subDir;
    }
    public List<String> minusLS()
    {
        List<Path> paths = LSHelper();
        List<String> subDir=new ArrayList<>();
        for(Path p:paths)
        {
            String FileName=(p.getFileName().toString());
           
            // add slach if it is folder not file
             // Mark symbolic links with '@'
        
       if(Files.isExecutable(p))
            {
                FileName+="*";
                // replace each space with dot
                FileName=FileName.replace(" ", ".");
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

        File NewFile = new File(FilePath);

        
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
            
            String FilePath=Paths.get(path).toString();
            int DotIndex=FilePath.lastIndexOf('.');
            if(DotIndex<=0 || DotIndex==FilePath.length()-1)
            return false;
            String extenstion=FilePath.substring(DotIndex+1).toLowerCase();

              String[] allowedExtensions = {
                "txt", "pdf", "doc", "docx", "csv", "java", "cpp", "py", "html", "xml"
            };

            for (String allowed : allowedExtensions) {
                if (extenstion.equals(allowed))
                    return true;
            }

            return false; 
        } catch (InvalidPathException ex) {
            return false;
        }
    }

public void chooseCommandAction(){
    if(parser.commandName.equals("pwd") && parser.args.length==1)
    {
      System.out.println(pwd());
      return;
    }   
    else if(parser.commandName.equals("cd"))
    {
       
        cd(parser.args);
        return;
    }
    else if(parser.commandName.equals("ls") && parser.args.length==1)
    {
        List<String> subDir=ls();
        subDir.forEach(item->System.out.println(item));
        return;
    }
    else if(parser.commandName.equals("touch"))
    {
       boolean done=touch(parser.args[1]);
       if(done)
       return;
       else
        {
            System.out.println(parser.commandName +parser.args[1]+": command not found.");
            return;
        }
    }
}
  public static void main(String[] args) {
   Terminal terminal=new Terminal();
  
   Scanner scanner = new Scanner(System.in);
    while (true) {
    System.out.print("> ");
    String instruction = scanner.nextLine(); 
    if(!terminal.parser.parse(instruction))
    {
    System.out.println(instruction+": command not found.");
    continue;
    }
    terminal.chooseCommandAction();
    // System.out.println();
    // System.out.println(terminal.TargetDir.getAbsolutePath().toString());
    }
   
}
}