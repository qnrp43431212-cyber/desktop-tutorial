import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.*;
import java.nio.file.*;
import java.util.*;
/** Checks duplicate Java declarations without needing Minecraft or NeoForge dependencies. */
public final class JavaDuplicateCheck {
    public static void main(String[] args) throws Exception {
        var compiler=ToolProvider.getSystemJavaCompiler();
        var diagnostics=new DiagnosticCollector<JavaFileObject>();
        int errors=0, sources=0;
        try(var manager=compiler.getStandardFileManager(diagnostics,null,null)) {
            var paths=Files.walk(Path.of(args[0])).filter(p->p.toString().endsWith(".java")).sorted().toList();
            var units=manager.getJavaFileObjectsFromPaths(paths);
            var task=(JavacTask)compiler.getTask(null,manager,diagnostics,List.of("-proc:none"),null,units);
            var positions=Trees.instance(task).getSourcePositions();
            for(var unit:task.parse()) {
                sources++;
                final String file=Path.of(unit.getSourceFile().toUri()).toString();
                final int[] fileErrors={0};
                new TreeScanner<Void,Void>() {
                    @Override public Void visitClass(ClassTree type,Void unused) {
                        Map<String,Long> seen=new HashMap<>();
                        for(var member:type.getMembers()) {
                            String key=null;
                            if(member instanceof VariableTree field) key="field:"+field.getName();
                            if(member instanceof ClassTree nested) key="nested-type:"+nested.getSimpleName();
                            if(member instanceof MethodTree method) {
                                StringJoiner args=new StringJoiner(",");
                                for(var param:method.getParameters())args.add(param.getType().toString());
                                key="method:"+method.getName()+"("+args+")";
                            }
                            if(key==null)continue;
                            long pos=positions.getStartPosition(unit,member);
                            if(seen.containsKey(key)) {
                                long line=unit.getLineMap().getLineNumber(pos);
                                System.err.println(file+":"+line+": DUPLICATE "+type.getSimpleName()+" "+key);
                                fileErrors[0]++;
                            } else seen.put(key,pos);
                        }
                        return super.visitClass(type,unused);
                    }
                }.scan(unit,null);
                errors+=fileErrors[0];
            }
            for(var d:diagnostics.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR) {
                System.err.println(d); errors++;
            }
        }
        if(errors!=0) {
            System.err.println("FAIL: "+errors+" Java duplicate/syntax errors in "+sources+" files");
            System.exit(1);
        }
        System.out.println("PASS: "+sources+" Java files, no duplicate fields/method signatures or syntax errors");
    }
}
