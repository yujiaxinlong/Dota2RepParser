package repvision;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import textwriter.TextWriter;

@UsesEntities
public class Main {


    private FieldPath mana;
    private FieldPath maxMana;
    private FieldPath visi;
    private FieldPath t;
    private Map<Integer, Float[]> wardPositionMap = new HashMap <Integer, Float[]>();
    private Double currentTime = 0.0;
    private Map<Integer, Integer[]> playerTimeMap = new HashMap <Integer, Integer[]>();
    private String[] playerHeroNames = new String[10];
    private int[] playerTeams = new int[10];    
    

    private boolean isHero(Entity e) {
        return e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero");
    }
    
    private boolean isVisible(Entity e){
    	return ((Integer)getEntityProperty(e,"m_iTaggedAsVisibleByTeam",null) == 30);
    }
    
    private boolean isWard(Entity e){
    	return e.getDtClass().getDtName().equals("CDOTA_NPC_Observer_Ward");
    }

//    private void ensureFieldPaths(Entity e) {
//        if (mana == null) {
//        	visi = e.getDtClass().getFieldPathForName("m_iTaggedAsVisibleByTeam");
//            mana = e.getDtClass().getFieldPathForName("m_flMana");
//            maxMana = e.getDtClass().getFieldPathForName("m_flMaxMana");
//            
//        }
//    }
    
    public int getTime(Context ctx){
    	Entity grp = ctx.getProcessor(Entities.class).getByDtName("CDOTAGamerulesProxy");
    	return Math.round( (Float) getEntityProperty(grp,"m_pGameRules.m_fGameTime",null));
    }
    
    public int getPlayerId(Entity e){
    	return  getEntityProperty(e,"m_iPlayerID",null);
    }
    
    public int getPlayerTeam(Context ctx,int playerId){
    	Entity pr = ctx.getProcessor(Entities.class).getByDtName("CDOTA_PlayerResource");
    	return getEntityProperty(pr, "m_vecPlayerData.%i.m_iPlayerTeam", playerId);
    }

    @OnEntityCreated
    public void onCreated(Context ctx, Entity e) throws IOException {
        if (!isHero(e)) {
            return;
        }
        int playerId = getEntityProperty(e,"m_iPlayerID",null);
        if(!playerTimeMap.containsKey(playerId)){
        	int time = getTime(ctx);
        	playerTimeMap.put(playerId, new Integer[]{time,0,time} );
        	playerHeroNames[playerId] = e.getDtClass().getDtName();
        	playerTeams[playerId] = getPlayerTeam(ctx,playerId);
        }
//        ensureFieldPaths(e);
//        Entity pr = ctx.getProcessor(Entities.class).getByDtName("CDOTA_PlayerResource");
//    	int playerId = getEntityProperty(e,"m_iPlayerID",null);
//    	int playerTeam = getEntityProperty(pr, "m_vecPlayerData.%i.m_iPlayerTeam", playerId);
//        Entity grp = ctx.getProcessor(Entities.class).getByDtName("CDOTAGamerulesProxy");
//        t = grp.getDtClass().getFieldPathForName("m_pGameRules.m_fGameTime");
//        int time = Math.round( (Float) grp.getPropertyForFieldPath(t));
//        writer.writeToFile(e.getDtClass().getDtName()+" visibility: "+e.getPropertyForFieldPath(visi)+" at: "+time+"s");
//        System.out.format("%s %s %s (%s/%s)  %s time = %s \n",playerTeam, playerId, e.getDtClass().getDtName(), e.getPropertyForFieldPath(mana), e.getPropertyForFieldPath(maxMana),e.getPropertyForFieldPath(visi),time);
    }

    @OnEntityUpdated
    public void onUpdated(Context ctx, Entity e, FieldPath[] updatedPaths, int updateCount) throws IOException {
        if (!isHero(e)) {
            return;
        }
        boolean update = false;
        for (int i = 0; i < updateCount; i++) {
            if ( updatedPaths[i].equals(visi)) {
                update = true;
                break;
            }
        }
        if (update) {
        	int time = getTime(ctx);
        	int playerId = getPlayerId(e);
        	Integer[] playerTime = playerTimeMap.get(playerId);
        	if(isVisible(e)){
        		playerTimeMap.put(playerId, new Integer[] {time,playerTime[1],playerTime[2]+time-playerTime[0]});
        	}
        	else{
        		playerTimeMap.put(playerId, new Integer[] {time,playerTime[1]+time-playerTime[0],playerTime[2]});
        	}
//        	Entity pr = ctx.getProcessor(Entities.class).getByDtName("CDOTA_PlayerResource");
//        	int playerId = getEntityProperty(e,"m_iPlayerID",null);
//        	int playerTeam = getEntityProperty(pr, "m_vecPlayerData.%i.m_iPlayerTeam", playerId);
//        	Entity grp = ctx.getProcessor(Entities.class).getByDtName("CDOTAGamerulesProxy");
//            t = grp.getDtClass().getFieldPathForName("m_pGameRules.m_fGameTime");
//            writer.writeToFile(e.getDtClass().getDtName()+" visibility: "+e.getPropertyForFieldPath(visi)+" at: "+time+"s");
//            System.out.format("%s %s %s (%s/%s)  %s time = %s \n",playerTeam, playerId, e.getDtClass().getDtName(), e.getPropertyForFieldPath(mana), e.getPropertyForFieldPath(maxMana),e.getPropertyForFieldPath(visi),time);
        }
    }
    
    
    public <T> T getEntityProperty(Entity e, String property, Integer idx) {
        if (e == null) {
            return null;
        }
        if (idx != null) {
            property = property.replace("%i", Util.arrayIdxToString(idx));
        }
        FieldPath fp = e.getDtClass().getFieldPathForName(property);
        return e.getPropertyForFieldPath(fp);
    }


    public void run(String[] args) throws Exception {
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        for(Entry<Integer,Integer[]> player : playerTimeMap.entrySet()){
        	int playerId = player.getKey();
        	Integer[] playerTime = player.getValue();
        	int playerTeam = playerTeams[playerId];
        	String playerHeroName = playerHeroNames[playerId];
        	System.out.println("player "+playerId+"  hero "+ playerHeroName +"  in team "+ playerTeam);
        	System.out.println("last time = "+playerTime[0]);
        	System.out.println("appears time = "+playerTime[1]);
        	System.out.println("disappear time = "+ playerTime[2]);
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
        
    }

}
