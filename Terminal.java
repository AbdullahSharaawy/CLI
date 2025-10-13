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
public class Terminal {
    public File TargetDir;

    public Terminal()
    {
        TargetDir=new File(pwd());
    }
    public String pwd()
    {
        String CurrentPath = Paths.get("").toAbsolutePath().toString();
        return CurrentPath;
    }
    public void cd(String path)
    {
        if (path == null || path.isEmpty()) 
        TargetDir=new File(pwd());
        else if(path.equals("..") && TargetDir.getParentFile()!=null   )
        {
           TargetDir=TargetDir.getParentFile();
            return;
        }
        else if(path.equals("."))
        return;
        File ValidPath=new File(path);
        if(ValidPath.exists() && ValidPath.isDirectory())
        {
           TargetDir=ValidPath;
        }
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
    if (!isValidFilePath(FilePath)) {
        return false;
    }

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


  public static void main(String[] args) {
    Terminal t=new Terminal(); 
    List<String>subdirectories= t.ls();
    subdirectories.forEach(System.out::println);
    
}
}
