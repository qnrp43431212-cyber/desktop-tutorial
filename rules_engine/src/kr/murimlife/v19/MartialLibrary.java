package kr.murimlife.v19;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Source-aware and game-data-driven catalog. Names alone are not evidence of usage rights. */
public final class MartialLibrary {
    private final Map<String,MurimEngine.Martial> byId=new LinkedHashMap<>();
    private final Map<String,List<MurimEngine.Martial>> byName=new LinkedHashMap<>();
    public MartialLibrary(Path tsv) throws IOException {
        try(BufferedReader in=Files.newBufferedReader(tsv,StandardCharsets.UTF_8)){
            String head=in.readLine();
            if(head==null||!head.startsWith("id\t"))throw new IOException("Invalid martial header");
            String line;
            while((line=in.readLine())!=null){
                if(line.isBlank()||line.startsWith("#"))continue;
                String[] p=line.split("\t",-1);
                if(p.length!=12)throw new IOException("Bad skill row: "+line);
                MurimEngine.Martial art=new MurimEngine.Martial(p[0],p[1],p[2],p[3],p[4],p[5],p[6],
                    Double.parseDouble(p[7]),Double.parseDouble(p[8]),Integer.parseInt(p[9]),Integer.parseInt(p[10]),p[11]);
                if(byId.putIfAbsent(art.id(),art)!=null)throw new IOException("Duplicate martial id "+art.id());
                byName.computeIfAbsent(art.name(),ignored->new ArrayList<>()).add(art);
            }
        }
    }
    public Collection<MurimEngine.Martial> all(){return Collections.unmodifiableCollection(byId.values());}
    public Optional<MurimEngine.Martial> id(String id){return Optional.ofNullable(byId.get(id));}
    public List<MurimEngine.Martial> name(String name){return List.copyOf(byName.getOrDefault(name,List.of()));}
    public List<MurimEngine.Martial> sect(String name){return byId.values().stream().filter(m->m.sect().equals(name)).toList();}
    public boolean learn(MurimEngine.Actor actor,MurimEngine.Martial art,MurimEngine.LearningSource source){
        if(!byId.containsKey(art.id()))return false;
        // World provenance is tracked per skill, and legendary manuals cannot be bought from an ordinary stall.
        if(art.legendary() && source==MurimEngine.LearningSource.BOOKSELLER)return false;
        if(source==MurimEngine.LearningSource.SECT_TEACHER && !actor.sect.equals(art.sect()))return false;
        return actor.knownMartials.add(art.id());
    }
}
