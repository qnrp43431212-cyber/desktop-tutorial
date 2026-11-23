package kr.murimlife.worldgen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Built from v1.16's real /function build scripts; only whitelisted template IDs and blocks. */
public final class SiteCatalog {
    private SiteCatalog() { }
    public record Paint(int ax, int ay, int az, int bx, int by, int bz, ResourceLocation block) {
        public Paint {
            if(ax>bx || ay>by || az>bz)throw new IllegalArgumentException("Invalid painted cuboid");
            int volume=Math.multiplyExact(Math.multiplyExact(bx-ax+1,by-ay+1),bz-az+1);
            if(volume>1_000_000)throw new IllegalArgumentException("Paint exceeds safety budget");
        }
        public int size() { return (bx-ax+1)*(by-ay+1)*(bz-az+1); }
        public BlockAt at(int index) {
            int dz=bz-az+1, dy=by-ay+1;
            int z=az+index%dz;int y=ay+(index/dz)%dy;int x=ax+index/(dz*dy);
            return new BlockAt(x,y,z);
        }
    }
    public record BlockAt(int x,int y,int z) { }
    public record Piece(ResourceLocation template,int x,int y,int z) { }
    public record Site(String id,boolean unique,String category,int width,int depth,int permittedRelief,int gap,
                       List<Piece> pieces,List<Paint> paints) {
        public int samplesPerAxisX() { return (width-1)/16+2; }
        public int samplesPerAxisZ() { return (depth-1)/16+2; }
    }
    private static int val(JsonObject o,String key) {return o.get(key).getAsInt();}
    private static int[] xyz(JsonArray a){return new int[]{a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt()};}
    public static List<Site> read() {
        try(var stream=SiteCatalog.class.getResourceAsStream("/murimworldgen/sites.json")) {
            if(stream==null)throw new IllegalStateException("Missing worldgen sites.json");
            JsonObject root=JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Site> sites=new ArrayList<>();
            for(var el:root.getAsJsonArray("sites")) {
                JsonObject j=el.getAsJsonObject();
                List<Piece> pieces=new ArrayList<>();
                for(var p:j.getAsJsonArray("pieces")) {
                    var o=p.getAsJsonObject();
                    pieces.add(new Piece(ResourceLocation.parse(o.get("template").getAsString()),val(o,"x"),val(o,"y"),val(o,"z")));
                }
                List<Paint> paints=new ArrayList<>();
                for(var p:j.getAsJsonArray("paints")) {
                    var o=p.getAsJsonObject();int[] a=xyz(o.getAsJsonArray("a")),b=xyz(o.getAsJsonArray("b"));
                    paints.add(new Paint(a[0],a[1],a[2],b[0],b[1],b[2],ResourceLocation.parse(o.get("block").getAsString())));
                }
                JsonArray size=j.getAsJsonArray("size");
                int siteW=size.get(0).getAsInt(),siteD=size.get(1).getAsInt();
                if(siteW<16 || siteD<16 || siteW>1024 || siteD>1024)
                    throw new IllegalArgumentException("Invalid site footprint: "+j.get("id"));
                for(var piece:pieces)
                    if(piece.x()<0 || piece.z()<0 || piece.x()>=siteW || piece.z()>=siteD)
                        throw new IllegalArgumentException("Out-of-bounds piece origin: "+piece);
                for(var paint:paints)
                    if(paint.ax()<0 || paint.az()<0 || paint.bx()>=siteW || paint.bz()>=siteD)
                        throw new IllegalArgumentException("Out-of-bounds paint: "+paint);
                sites.add(new Site(j.get("id").getAsString(),j.get("unique").getAsBoolean(),
                        j.get("category").getAsString(),size.get(0).getAsInt(),size.get(1).getAsInt(),
                        val(j,"maxRelief"),val(j,"minimumGap"),List.copyOf(pieces),List.copyOf(paints)));
            }
            return List.copyOf(sites);
        }catch(Exception ex){throw new IllegalStateException("Invalid MURIM LIFE site catalog",ex);}
    }
}
