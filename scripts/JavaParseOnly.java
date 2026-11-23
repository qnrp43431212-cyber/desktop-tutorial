import javax.tools.*;
import com.sun.source.util.*;
import java.util.*;
import java.nio.file.*;
public class JavaParseOnly {
  public static void main(String[] args)throws Exception{
    JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diags=new DiagnosticCollector<>();
    try(StandardJavaFileManager manager=compiler.getStandardFileManager(diags,null,null)){
      var paths=Files.walk(Path.of(args[0])).filter(f->f.toString().endsWith(".java")).toList();
      var source=manager.getJavaFileObjectsFromPaths(paths);
      JavacTask task=(JavacTask)compiler.getTask(null,manager,diags,List.of("-proc:none"),null,source);
      int n=0;for(var unit:task.parse())n++;
      boolean bad=false;for(var d:diags.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR){System.err.println(d);bad=true;}
      if(bad)System.exit(1);
      System.out.println("PASS: "+n+" Java files parsed without syntax errors (not type-checked)");
    }
  }
}
