import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    t.cd(".");
     System.out.println(t.TargetDir.getAbsolutePath());
}
}