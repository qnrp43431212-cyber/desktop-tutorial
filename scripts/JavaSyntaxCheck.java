import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;
public class JavaSyntaxCheck {
 public static void main(String[] args) throws Exception {
  JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
  DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
  try(StandardJavaFileManager manager=compiler.getStandardFileManager(diagnostics,null,java.nio.charset.StandardCharsets.UTF_8)) {
   List<File> files=new ArrayList<>();
   try(var walk=Files.walk(Path.of(args.length==0?"src/main/java":args[0]))) {walk.filter(p->p.toString().endsWith(".java")).forEach(p->files.add(p.toFile()));}
   JavaCompiler.CompilationTask task=compiler.getTask(null,manager,diagnostics,List.of("-proc:none","-encoding","UTF-8"),null,manager.getJavaFileObjectsFromFiles(files));
   ((JavacTask)task).parse();
   long failed=diagnostics.getDiagnostics().stream().filter(d->d.getKind()==Diagnostic.Kind.ERROR).count();
   for(var d:diagnostics.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR)System.err.println(d);
   if(failed>0)System.exit(1);
   System.out.println("PASS Java syntax parse: "+files.size()+" NeoForge source files (API type checks NOT performed)");
  }
 }
}
