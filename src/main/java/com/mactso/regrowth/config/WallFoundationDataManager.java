package com.mactso.regrowth.config;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class WallFoundationDataManager {

	public static Hashtable<String, wallFoundationItem> wallFoundationsHashtable = new Hashtable<>();
	private static String defaultWallFoundationString = "hbm:default";
	private static String defaultWallFoundationKey = defaultWallFoundationString;

	public static wallFoundationItem getWallFoundationInfo(String key) {
		String iKey = key;

		if (wallFoundationsHashtable.isEmpty()) {
			wallFoundationsInit();
		}

		wallFoundationItem r = wallFoundationsHashtable.get(iKey);

		return r;
	}

	public static String getWallFoundationHashAsString() {
		String returnString="";
		String wallFoundationType;
		for (String key:wallFoundationsHashtable.keySet()) {
			wallFoundationType = wallFoundationsHashtable.get(key).wallFoundationType;
			String tempString = key+"," + wallFoundationType + ";";
			returnString += tempString;
		}
		return returnString;
	
	}

	public static void wallFoundationsInit() {
		
		List <String> dTL6464 = new ArrayList<>();
		
		int i = 0;
		String wallFoundationsLine6464 = "";
		// Forge Issue 6464 patch.
		StringTokenizer st6464 = new StringTokenizer(MyConfig.defaultWallFoundations6464, ";");

		while (st6464.hasMoreElements()) {
			wallFoundationsLine6464 = st6464.nextToken().trim();
			if (wallFoundationsLine6464.isEmpty()) continue;
			dTL6464.add(wallFoundationsLine6464);  
			i++;
		}

		MyConfig.defaultWallFoundations = dTL6464.toArray(new String[i]);

		i = 0;
		wallFoundationsHashtable.clear();
		while (i < MyConfig.defaultWallFoundations.length) {
			try {
				StringTokenizer st = new StringTokenizer(MyConfig.defaultWallFoundations[i], ",");
				String wallFoundationBlockKey = st.nextToken();
				String key = wallFoundationBlockKey;				
				wallFoundationsHashtable.put(key, new wallFoundationItem(wallFoundationBlockKey));
				if (!wallFoundationBlockKey.contentEquals("hbm:default") &&
				    !ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(wallFoundationBlockKey))
				   )  {
					System.out.println("Regrowth Debug: Wall Foundation Block: " + wallFoundationBlockKey + " not in Forge Entity Type Registry.  Mispelled?  Missing semicolon? ");
				}
			} catch (Exception e) {
				System.out.println("Regrowth Debug:  Bad Wall Foundation Config : " + MyConfig.defaultWallFoundations[i]);
			}
			i++;
		}

	}

	public static class wallFoundationItem {
		String wallFoundationType;  // a block like "minecraft:block" or "oby:dirt"
		
		public wallFoundationItem(String wallFoundationType) {
			this.wallFoundationType =  wallFoundationType;
		}

		public String getwallFoundationType() {
			return wallFoundationType.toLowerCase();
		}

	}
	
}
