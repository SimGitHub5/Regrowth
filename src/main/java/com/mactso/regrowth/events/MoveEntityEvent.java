package com.mactso.regrowth.events;

import java.util.Optional;

import com.mactso.regrowth.config.MyConfig;
import com.mactso.regrowth.config.RegrowthEntitiesManager;
import com.mactso.regrowth.config.WallFoundationsManager;

import net.minecraft.block.AirBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.DoublePlantBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TallGrassBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.village.PointOfInterestData;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber()
public class MoveEntityEvent {
	
	static int[]dx= {1,0,-1,0};
	static int[]dz= {0,1,0,-1};
	static int TICKS_PER_SECOND = 20;	
    static int[][]facingArray = {{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1},{1,0},{1,1}};
	static int lastTorchX=0;
	static int lastTorchY=0;
	static int lastTorchZ=0;
	
	@SubscribeEvent
    public void doEntityMove(LivingUpdateEvent event) { 

	    // need to find the type of entity here.
		if (event.getEntity() == null) {
			return;
		}

		Entity eventEntity = event.getEntity();
		World world = eventEntity.world;
		
		if (world == null) {
			return;
		}
		
		if (!(world instanceof ServerWorld)) {
			return;
		}

		BlockPos debugBlockPos = getBlockPos(eventEntity);
		Block footBlock = world.getBlockState(getBlockPos(eventEntity)).getBlock();
		if (footBlock == null) {
			return;
		}

		Block groundBlock;

		if (footBlock == Blocks.GRASS_PATH) {
			groundBlock = footBlock;
		} else {
			groundBlock = world.getBlockState(getBlockPos(eventEntity).down()).getBlock();	
		}

		if (groundBlock == null) {
			return;
		}

		BlockPos eventEntityPos = getBlockPos(eventEntity); // floats
		int eventEntityX =  eventEntityPos.getX(); // Int
		int eventEntityY =  eventEntityPos.getY(); // Int
		int eventEntityZ =  eventEntityPos.getZ(); // Int
		Biome localBiome = world.getBiome(eventEntityPos);
		EntityType<?> tempType = eventEntity.getType();
		ResourceLocation registryName = tempType.getRegistryName();
		String registryNameAsString = registryName.toString();
		RegrowthEntitiesManager.RegrowthMobItem currentRegrowthMobItem = RegrowthEntitiesManager.getRegrowthMobInfo(registryNameAsString);
		if (currentRegrowthMobItem==null) {  // This mob isn't configured to do Regrowth Events
			return;
		}
		
		String regrowthType = currentRegrowthMobItem.getRegrowthType();
	
		if (isImpossibleRegrowthEvent(footBlock, regrowthType)) {
			return;
		}
		
		double regrowthEventOdds = 2/currentRegrowthMobItem.getRegrowthEventSeconds() * TICKS_PER_SECOND;
		double randomD100Roll = eventEntity.world.rand.nextDouble() * 100; 

		if (randomD100Roll <= regrowthEventOdds) {
			if (eventEntity instanceof VillagerEntity) {
				VillagerEntity ve = (VillagerEntity) eventEntity;
				// if onGround
				if ((ve.isOnGround())) {
					doVillagerRegrowthEvents(ve, footBlock, groundBlock, eventEntityPos, registryNameAsString, regrowthType,
							eventEntityX, eventEntityY, eventEntityZ, world);
				}
			}
		}
	
		doMobRegrowthEvents(eventEntity, footBlock, groundBlock, registryNameAsString, regrowthType, regrowthEventOdds, randomD100Roll,
							eventEntityX, eventEntityY, eventEntityZ, world);

		int debugBreakPoint = 0;

	}

	private void doMobRegrowthEvents(Entity eventEntity, Block footBlock, Block groundBlock, String key,
			String regrowthType, double regrowthEventOdds, double randomD100Roll,
			int eX, int eY, int eZ, World world) {

		// all remaining actions currently require a grass block underfoot so if not a grass block- can exit now.

		if ((groundBlock != Blocks.GRASS_BLOCK)) {
			return;
		}

		
		if ((regrowthType.equals("tall")) && (randomD100Roll <= regrowthEventOdds)){
			entityGrowTallGrassToDoubleGrass(eventEntity, footBlock, regrowthType);
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " tall at " +  eX +", "+ eY +", "+ eZ +".");
			}
			return;
		}	
		
		if ( ((regrowthType.equals("grow")) || (regrowthType.equals("both"))) && (randomD100Roll <= regrowthEventOdds) ) {
			if (footBlock instanceof AirBlock ) {
	        	IGrowable ib = (IGrowable) groundBlock;
	        	if (ib == null) {
	        		return;
	        	}
				if (MyConfig.aDebugLevel > 1) {
					System.out.println(key + " trying to grow plants sat " +  eX +", "+ eY +", "+ eZ +".");
				}
	        	ib.grow((ServerWorld)eventEntity.world, eventEntity.world.rand, getBlockPos(eventEntity), eventEntity.world.getBlockState(getBlockPos(eventEntity)));    			
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " grow at " +  eX +", "+ eY +", "+ eZ +".");
				}
				return;
			}
		}

		double eatingOdds = regrowthEventOdds;
		if  ((regrowthType.contentEquals("eat")) ||(regrowthType.contentEquals("both"))) {
			// Balance eating and growth odds/timing for "both" case.
			if  ((regrowthType.contentEquals("both"))) {
				eatingOdds = regrowthEventOdds * 15;				
			}
			if (eventEntity instanceof AbstractHorseEntity) {
				AbstractHorseEntity h = (AbstractHorseEntity) eventEntity;
				BlockPos debugPos = getBlockPos(eventEntity);
				if (!(h.isEatingHaystack())) {
					eatingOdds = 0.0;  // Horse, Donkey, Mule, (Llama?) only eat if animation eating.
				} else {
					eatingOdds = regrowthEventOdds * 25;   // Increase eating odds during eating animation.
				}
			}			
			if ((randomD100Roll <= eatingOdds)) {
				if (MyConfig.aDebugLevel > 1) {
					System.out.println(key + " trying to eat at " +  eX +", "+ eY +", "+ eZ +".");
				}
				if (entityEatGrassOrFlower(eventEntity, getBlockPos(eventEntity), regrowthType, footBlock, groundBlock)) {
 					if (MyConfig.aDebugLevel > 0) {
						System.out.println(key + " eat at " +  eX +", "+ eY +", "+ eZ +".");
					}
				}
				return;
			}
		}
		return;
	}

	private void doVillagerRegrowthEvents(VillagerEntity ve, Block footBlock, Block groundBlock, BlockPos eventEntityPos, String key, String regrowthType,
											int veX, int veY, int veZ, World world)
	{
		// Villagers hopping, falling, etc. are doing improvements.
		if (!(isOnGround (ve))) {
			return;
		}
		// Give custom debugging names to nameless villagers.
		if (MyConfig.aDebugLevel > 0) {
			ITextComponent tName = new StringTextComponent ("");
			float veYaw=ve.getYaw(1.0f);
			tName = new StringTextComponent ("Reg-"+ veX+","+veZ+": " + veYaw);
			ve.setCustomName(tName);
		}
		else { // remove custom debugging names added by Regrowth
			if (ve.getCustomName() != null) {
				if (ve.getCustomName().toString().contains("Reg-")){
					ve.setCustomName(null);
				}
			}
		}
		// note all villagers may not have a home.  poor homeless villagers.
		//default = repair farms
		if (improveFarm(ve,groundBlock,footBlock, regrowthType,veX,veY,veZ)) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " farm improved at " +  +  veX +", "+veY +", "+veZ+", ");
			}	
		};
		// cut lea'v'es.
		// remove leaves if facing head height leaves

		if (regrowthType.contains("v")) {
			improveLeaves(ve, groundBlock, key, veX, veY, veZ);
		}
		
		// c = cut down grass (but not flowers for now)
		// to do - maybe remove flowers unless by a road or elevated (air next to them as in the flower beds)
		// to do - replace "c" with a meaningful constant.
		
		if(regrowthType.contains("c")) {
			if ((footBlock instanceof TallGrassBlock)||(footBlock instanceof DoublePlantBlock)) {

				ve.world.destroyBlock(eventEntityPos, false);
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " cut at " +  veX +", "+veY+", "+veZ+".");
				}					
			}
		}
		// improve roads  
		// to do - replace "r" with a meaningful constant.f
		if(regrowthType.contains("r")) {
			improveRoads(ve, footBlock, groundBlock, key);
		}


		// note villages may not have a meeting place.  Sometimes they change.  Sometimes they take a few minutes to form.
		if ((regrowthType.contains("w")||(regrowthType.contains("p")))) {
				improveWalls(ve, footBlock, groundBlock, key, regrowthType,veX,veY,veZ);
				// "jump" villagers away if they are inside a wall or fence block.
				if ((footBlock instanceof WallBlock) || (footBlock instanceof FenceBlock)) {
					float veYaw=ve.getYaw(1.0f)/45;
					int facingNdx = Math.round(veYaw);
					if (facingNdx<0) {
						facingNdx = Math.abs(facingNdx);
					}
					facingNdx %= 8;
					double dx = (facingArray[facingNdx][0])/2.0;
					double dz = (facingArray[facingNdx][1])/2.0;
					ve.setMotion(dx, 0.55, dz);
				}
		}
		

		if((regrowthType.contains("t")&&(footBlock != Blocks.TORCH))) {
			if (improveLighting(ve, footBlock, groundBlock, veX, veY, veZ)) {
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key +"- "+ footBlock +", " + groundBlock +" pitch: "+ve.rotationPitch + " improve lighting at " +  ve.getPosX() +", "+ve.getPosY()+", "+ve.getPosZ());
				}				
			}
		}
	}

	private void improveLeaves(VillagerEntity ve, Block groundBlock, String key, int veX, int veY, int veZ) {
		float veYaw=ve.getYaw(1.0f)/45;
		int facingNdx = Math.round(veYaw);
		if (facingNdx<0) {
			facingNdx = Math.abs(facingNdx);
		}
		facingNdx %= 8;

		// when standing on a grass path- game reports you 1 block lower.  Adjust.
		if (groundBlock == Blocks.GRASS_PATH) {
			veY +=1;
		}
		int dx = facingArray[facingNdx][0];
		int dz = facingArray[facingNdx][1];
		BlockPos tmpBP = null;
		BlockState tempBS = null;
		Block tempBlock = null;
		boolean destroyBlock = false;
		for (int iY=0;iY<2;iY++) {
			tmpBP = new BlockPos (veX+dx,veY+iY,veZ+dz);
			tempBS = ve.world.getBlockState(tmpBP);
			tempBlock = tempBS.getBlock();
			if (tempBlock instanceof LeavesBlock) {
				boolean persistantLeaves = tempBS.get(LeavesBlock.PERSISTENT);
				if (!(persistantLeaves)) {
					destroyBlock = true;
				}
			}
			if 	((tempBlock instanceof CactusBlock)) {
				destroyBlock = true;
			}
			if (destroyBlock) {
				ve.world.destroyBlock(tmpBP, false);
				destroyBlock = false;
				if (MyConfig.aDebugLevel > 0) {
					System.out.println(key + " clear "+ tempBlock.getTranslationKey().toString() +" at" +  +  veX +", "+veY+iY +", "+veZ+", ");
				}
			}
		}
	}

	private boolean entityEatGrassOrFlower(Entity eventEntity, BlockPos eventEntityPos, String regrowthType, Block footBlock, Block groundBlock) {

		if (!(isGrassOrFlower(footBlock))) {
			return false;
		}
		if ( !(regrowthType.equals("eat")) && 
			 !(regrowthType.equals("both")) ) {
			return false;
		}
		eventEntity.world.destroyBlock(eventEntityPos, false);
		double randomD100Roll = eventEntity.world.rand.nextDouble() * 100;
		if ((randomD100Roll >60) && (groundBlock instanceof GrassBlock)) {
			eventEntity.world.setBlockState(eventEntityPos.down(), Blocks.DIRT.getDefaultState());
		}
		LivingEntity le = (LivingEntity) eventEntity;
		if (eventEntity instanceof AgeableEntity) {
			AgeableEntity ae = (AgeableEntity) eventEntity;
			if (ae.isChild()) {
				ae.setGrowingAge(ae.getGrowingAge() + 30);
			}
		}
		if (le.getMaxHealth() > le.getHealth() && (MyConfig.aEatingHeals == 1)) {
    		EffectInstance ei = new EffectInstance (Effects.INSTANT_HEALTH,1,0, false, true );
    		le.addPotionEffect(ei);
		}
		return true;
	}

	private boolean entityGrowTallGrassToDoubleGrass(Entity eventEntity, Block footBlock, String regrowthType) {
		if (footBlock instanceof TallGrassBlock ) {
			IGrowable ib = (IGrowable) footBlock;
			ib.grow((ServerWorld)eventEntity.world, eventEntity.world.rand, getBlockPos(eventEntity), eventEntity.world.getBlockState(getBlockPos(eventEntity)));    			
			return true;
		}
		return false;
	}

	// if a grassblock in village has farmland next to it on the same level- retill it. 
	// todo add hydration check before tilling land.
	private boolean improveFarm(VillagerEntity ve, Block groundBlock, Block footBlock, String regrowthType,
								int veX, int veY, int veZ) {
		boolean nextToFarmBlock = false;
		boolean nextToWaterBlock = false;
		boolean torchExploit = false;
		
		
		if ( ve.getVillagerData().getProfession() == VillagerProfession.FARMER) {
			if ((lastTorchX==veX) && (lastTorchY == veY) && (lastTorchZ == veZ)) {
				torchExploit = true;
			}
			for(int i=0; i<4; i++) {
				Block tempBlock = ve.world.getBlockState(new BlockPos (veX+dx[i],veY-1,veZ+dz[i])).getBlock();
				if (tempBlock == Blocks.FARMLAND) {
					nextToFarmBlock = true;
					i = 4;
				}
			}
			

			if ((BlockTags.LOGS.contains(groundBlock))){
				for(int i=0; i<4; i++) {
					Block tempBlock = ve.world.getBlockState(new BlockPos (veX+dx[i],veY-1,veZ+dz[i])).getBlock();
					if (tempBlock == Blocks.WATER) {
						nextToWaterBlock = true;
						i = 4;
					}
				}
				if (regrowthType.contains("t") && (nextToWaterBlock)) {
					if ((footBlock == Blocks.AIR) && (!torchExploit)) {
						ve.world.setBlockState( getBlockPos(ve), Blocks.TORCH.getDefaultState());
						lastTorchX=veX;
						lastTorchY=veY;
						lastTorchZ=veZ;
					}
				}
			}

			if (nextToFarmBlock) {
				if (groundBlock == Blocks.SMOOTH_SANDSTONE) {
					if (regrowthType.contains("t")) {
						ve.world.setBlockState(getBlockPos(ve), Blocks.TORCH.getDefaultState());
						lastTorchX=veX;
						lastTorchY=veY;
						lastTorchZ=veZ;
					}
				}
				if (groundBlock instanceof GrassBlock) {
					ve.world.setBlockState(getBlockPos(ve).down(), Blocks.FARMLAND.getDefaultState());
					return true;
				}
			}
			
		}
		return false;
	}


	private boolean improveLighting(VillagerEntity ve, Block footBlock, Block groundBlock,
							int veX, int veY, int veZ ) {
		int skylightValue = ve.world.getLightFor(LightType.SKY, getBlockPos(ve));
		Biome.Category villageBiomeCategory = ve.world.getBiome(getBlockPos(ve)).getCategory();
		if (ve.isSleeping()) { 
			return false;
		}
		if (footBlock instanceof BedBlock) {
			return false;
		}
		
		if ((groundBlock == Blocks.OAK_PLANKS) ||
			(groundBlock == Blocks.SPRUCE_PLANKS) ||
			(groundBlock == Blocks.BIRCH_PLANKS) ||
			(groundBlock == Blocks.JUNGLE_PLANKS) ||
			(groundBlock == Blocks.ACACIA_PLANKS) ||
			((groundBlock == Blocks.SMOOTH_SANDSTONE) && (skylightValue<14)) ||
			((groundBlock == Blocks.COBBLESTONE) && (skylightValue<14) && (villageBiomeCategory == Biome.Category.TAIGA))
			) {
				int blockLightValue = ve.world.getLightFor(LightType.BLOCK, getBlockPos(ve));
				if (blockLightValue < 8) {
 					ve.world.setBlockState(getBlockPos(ve), Blocks.TORCH.getDefaultState());
					return true;
				}

		}
		return false;
	}


	private void improveRoads(VillagerEntity ve, Block footBlock, Block groundBlock, String key) {
		BlockPos vePos = getBlockPos(ve);
		int veX =  vePos.getX();
		int veY =  vePos.getY();
		int veZ =  vePos.getZ();
		
		if (improveRoadsFixPotholes(ve, groundBlock,veX, veY, veZ)) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " fix road " +  veX +", "+veY+", "+veZ+". ");
			}	
		}
		if (improveRoadsSmoothHeight(ve, footBlock, groundBlock, veX, veY, veZ)) {
			if (MyConfig.aDebugLevel > 0) {
				System.out.println(key + " smooth road slope" +  veX +", "+ veY+", "+veZ+", ");
			}
			
		}
	}

	private boolean improveRoadsFixPotholes(VillagerEntity ve, Block groundBlock,  
			int veX, int veY, int veZ) {
		// fix pot holes - grass spots in road with 3-4 grass blocks orthagonal to them.
		// to do remove "tempBlock" and put in iff statement.  Extract as method.
		// add biome support for dirt, sand, and podzol
		int grassPathCount = 0;
		int fixHeight = 2;
		if (Biome.Category.TAIGA == ve.world.getBiome(getBlockPos(ve)).getCategory()) {
			fixHeight = 4;
		}
		
		int pitVeY = veY;
		if (groundBlock == Blocks.GRASS_PATH ) {
			pitVeY = veY+ 1;
		}
		if ((groundBlock == Blocks.DIRT)||
				(groundBlock == Blocks.GRASS_BLOCK)||
				(groundBlock == Blocks.GRASS_PATH)) {
			grassPathCount = 0;
			for(int i=0; i<4; i++) {
				Block tempBlock = ve.world.getBlockState(new BlockPos (veX+dx[i],pitVeY,veZ+dz[i])).getBlock();
				if (tempBlock == Blocks.GRASS_PATH) {
					grassPathCount += 1;
				}
			}
			if (grassPathCount==4) {
				ve.world.setBlockState(new BlockPos (veX, pitVeY, veZ), Blocks.GRASS_PATH.getDefaultState());
				ve.setMotion(0.0, 0.4, 0.0);
				return true;
			}

		} 
			
		if ((groundBlock == Blocks.DIRT)||(groundBlock instanceof GrassBlock)) {
			grassPathCount = 0;
			for(int dy=-1; dy<=fixHeight; dy++) { // on gentle slopes too
				for(int i=0;i<4; i++) {
					Block tempBlock = ve.world.getBlockState(new BlockPos (veX+dx[i],veY+dy,veZ+dz[i])).getBlock();
					if (tempBlock == Blocks.GRASS_PATH) {
						grassPathCount += 1;
					}
				}
		    } 
			if (grassPathCount >= 3) {
				ve.world.setBlockState(getBlockPos(ve).down(), Blocks.GRASS_PATH.getDefaultState());
				return true;
			}
		}
		return false;
	}
	
	private boolean improveRoadsSmoothHeight(VillagerEntity ve, Block footBlock, Block groundBlock, 
													int veX, int veY, int veZ) {
		// to do remove "tempBlock" and put in iff statement.  Extract as method.
		boolean doRoadSmoothing = false;
		BlockState smoothingBlockState = Blocks.GRASS_PATH.getDefaultState();
		Block smoothingBlock = Blocks.GRASS_PATH;
		
		// is the block stood on a road block which can see the sky.
		// (alternatively could check for open air 9 spaces above)
		// removed because houses are made of road material.
//		if ((groundBlock == Blocks.SMOOTH_SANDSTONE) &&
//			(ve.world.getLightFor(LightType.SKY, ve.getPosition()) == 15) &&
//			(ve.world.getBiome(ve.getPosition()).getCategory() == Biome.Category.DESERT ) 
//			)
//			{
//			smoothingBlockState = Blocks.SMOOTH_SANDSTONE.getDefaultState();
//			smoothingBlock = Blocks.SMOOTH_SANDSTONE;
//			veY = veY - 1;  // grasspath is 0.9 tall so off set for smooth sandstone is 1.0 tall.
//			doRoadSmoothing = true;
//		}
		if (footBlock == Blocks.GRASS_PATH) {
			doRoadSmoothing = true;
		}
		// Check for higher block to smooth up towards
		if (doRoadSmoothing) {
			for (int dy=2;dy<6;dy++) {
				for(int i=0;i<4; i++) {
					Block tempBlock = ve.world.getBlockState(new BlockPos (veX+dx[i],veY+dy,veZ+dz[i])).getBlock();
					if (tempBlock == smoothingBlock) {
						ve.world.setBlockState(new BlockPos (veX,veY+1,veZ), smoothingBlockState);
						ve.setMotion(0.0, 0.4, 0.0);
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean improveWallForMeetingPlace(VillagerEntity ve, 
			String regrowthType, BlockPos villageMeetingPlaceBlockPos, Block groundBlock) {

		Biome villageBiome = ve.world.getBiome(villageMeetingPlaceBlockPos);		
		int wallPerimeter = 31;
		int wallTorchSpacing = 8;
		final int wallCenter = 0;
		BlockState wallType = Blocks.COBBLESTONE_WALL.getDefaultState();
		BlockState gateBlockType = Blocks.GRASS_PATH.getDefaultState();
		
		if (villageBiome.getCategory() == Biome.Category.DESERT ) {
			wallPerimeter = 23;
			wallType = Blocks.SANDSTONE_WALL.getDefaultState();
			gateBlockType = Blocks.SMOOTH_SANDSTONE.getDefaultState();  // 16.1 mojang change
		}
		if (villageBiome.getCategory() == Biome.Category.TAIGA) {
			wallPerimeter = 23;
			wallType = Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState();
		}
		
		int absvx = (int) Math.abs(ve.getPosX() - villageMeetingPlaceBlockPos.getX());
		int absvz = (int) Math.abs(ve.getPosZ() - villageMeetingPlaceBlockPos.getZ());
		boolean buildCenterGate = true;
		if (placeOneWallPiece(ve, regrowthType, wallPerimeter, wallTorchSpacing, wallCenter, gateBlockType, buildCenterGate, wallType,
				absvx, absvz)) {
			if (regrowthType.contains("t")) {
				if (isValidTorchLocation(wallPerimeter, wallTorchSpacing, absvx, absvz, ve.world.getBlockState(getBlockPos(ve)).getBlock() )) {
					ve.world.setBlockState(getBlockPos(ve).up(), Blocks.TORCH.getDefaultState());
				}
			}
			return true;
		}
		return false;
	}

	// villagers build protective walls around their homes. currently 32 out.
	// to do- reduce distance of wall from home.
	private boolean improveWallForPersonalHome(VillagerEntity ve, BlockPos vHomePos,String regrowthType, Block groundBlock ) {

		Biome villageBiome = ve.world.getBiome(vHomePos);
		int wallPerimeter = 5;
		int wallTorchSpacing = 4;
		final int wallCenter = 0;
		BlockState wallType = Blocks.OAK_FENCE.getDefaultState();
		BlockState gateBlockType = Blocks.GRASS_PATH.getDefaultState();
		
		if (villageBiome.getCategory() == Biome.Category.DESERT ) {
			wallPerimeter += 2;
			wallTorchSpacing = 6;
			wallType = Blocks.ACACIA_FENCE.getDefaultState();
			gateBlockType = Blocks.SANDSTONE.getDefaultState();
		}
		if (villageBiome.getCategory() == Biome.Category.TAIGA) {
			wallTorchSpacing = 3;
			wallType = Blocks.SPRUCE_FENCE.getDefaultState();
			gateBlockType = Blocks.SANDSTONE.getDefaultState();
		}

		int absvx = (int) Math.abs(ve.getPosX() - vHomePos.getX());
		int absvz = (int) Math.abs(ve.getPosZ() - vHomePos.getZ());
		boolean buildCenterGate = false;
		
		if (placeOneWallPiece(ve, regrowthType, wallPerimeter, wallTorchSpacing, wallCenter, gateBlockType, buildCenterGate, wallType,
				absvx, absvz)) {
			if (regrowthType.contains("t")) {
				if (isValidTorchLocation(wallPerimeter, wallTorchSpacing, absvx, absvz, groundBlock)) {
					ve.world.setBlockState(getBlockPos(ve).up(), Blocks.TORCH.getDefaultState());
				}
			}			
			return true;
		}
		return false;

	}

	private void improveWalls(VillagerEntity ve, Block footBlock, Block groundBlock, String key, String regrowthType,
								int veX, int veY, int veZ ) {
		
		Brain<VillagerEntity> vb = ve.getBrain();
		Optional<GlobalPos> vMeetingPlace = vb.getMemory(MemoryModuleType.MEETING_POINT);

		if (isOkayToBuildWallHere(ve, footBlock, groundBlock, vMeetingPlace, veX, veY, veZ)) {
			GlobalPos gVMP = vMeetingPlace.get();
			BlockPos villageMeetingPlaceBlockPos = gVMP.getPos();

			// place one block of a wall on the perimeter around village meeting place
			// Don't block any roads or paths regardless of biome.

			if (regrowthType.contains("w")) {
				if (improveWallForMeetingPlace(ve, regrowthType, villageMeetingPlaceBlockPos, groundBlock)) {
					if (MyConfig.aDebugLevel > 0) {
						System.out.println(key + " Meeting Place wall at " +  veX +", "+veY+", "+veZ+".");
					}
				}
			}

			// build a wall on perimeter of villager's home
			if (regrowthType.contains("p")) {
				Optional<GlobalPos> villagerHome = vb.getMemory(MemoryModuleType.HOME);
				if (villagerHome.isPresent()) {
					GlobalPos gVHP = villagerHome.get();
					BlockPos villagerHomePos = gVHP.getPos();
					// don't build personal walls inside the village wall perimeter.
					// don't build personal walls until the village has a meeting place.
					if (isOutsideMeetingPlaceWall(ve, vMeetingPlace, vMeetingPlace.get().getPos(), veX, veY, veZ)) {
						if (improveWallForPersonalHome(ve, villagerHomePos, regrowthType, groundBlock)) {
							if (MyConfig.aDebugLevel > 0) {
								System.out.println(key + " personal wall at " +veX+", "+veY+", "+veZ+".");
							}
						}
					}
				}
			}
		}
	}


	private boolean isFootBlockOkayToBuildIn (Block footBlock) {
		if (footBlock instanceof AirBlock) {
			return true;		
		}
		return false;
	}

	private boolean isGrassOrFlower(Block footBlock) {
		if (footBlock instanceof TallGrassBlock) 
		{
			return true;
		}
		if (footBlock instanceof FlowerBlock) 
		{
			return true;
		}
		if (footBlock instanceof DoublePlantBlock) 
		{
			return true;
		}
		if (footBlock == Blocks.FERN) 
		{
			return true;
		}
		if (footBlock == Blocks.LARGE_FERN) 
		{
			return true;
		}

		return false;
	}

	private boolean isImpossibleRegrowthEvent(Block footBlock, String regrowthType) {
		if ((regrowthType.equals("eat")) && (footBlock instanceof AirBlock)) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (footBlock instanceof TallGrassBlock)) {
			return true;
		}
		if ((regrowthType.equals("grow")) && (footBlock instanceof FlowerBlock)) {
			return true;
		}    		
		if ((regrowthType.equals("tall")) && (!(footBlock instanceof TallGrassBlock))) {
			return true;
		}
		return false;
	}

	private boolean isOkayToBuildWallHere(VillagerEntity ve, Block footBlock, Block groundBlock,
			Optional<GlobalPos> vMeetingPlace, int veX, int veY, int veZ) {
		boolean okayToBuildWalls = true;
		if (!(vMeetingPlace.isPresent())) {
			okayToBuildWalls = false;
		}
		if (!(isOnGround(ve))) {
			okayToBuildWalls = false;
		}
		if (!(isFootBlockOkayToBuildIn(footBlock))) {
			okayToBuildWalls = false;
		}
		if (!(isValidGroundBlockToBuildWallOn(ve, groundBlock))) {
			okayToBuildWalls = false;			
		}
		return okayToBuildWalls;
	}

	private boolean isOutsideMeetingPlaceWall(VillagerEntity ve, Optional<GlobalPos> vMeetingPlace, BlockPos meetingPlacePos,
												int veX, int veY, int veZ) {
		int absVMpX;  // these just making debugging easier.  remove them later?
		int absVMpZ;
			absVMpX = (int) Math.abs(veX - meetingPlacePos.getX());
			absVMpZ = (int) Math.abs(veZ - meetingPlacePos.getZ());
			if (( absVMpX < 32 )&&( absVMpZ < 32 )) return false;
		return true;
	}

	private boolean isValidGroundBlockToBuildWallOn (VillagerEntity ve,Block groundBlock) {

		int blockSkyLightValue = ve.world.getLightFor(LightType.SKY, getBlockPos(ve));

		if (blockSkyLightValue < 14) return false;
		
		String key = groundBlock.getRegistryName().toString(); // broken out for easier debugging
		WallFoundationsManager.wallFoundationItem currentWallFoundationItem = WallFoundationsManager.getWallFoundationInfo(key);
		if (currentWallFoundationItem==null) return false;		
		
		return true;
		
	}
	
//	ItemStack iStk = new ItemStack(Items.BONE_MEAL,1);
//	BoneMealItem.applyBonemeal(iStk, e.world,e.getPosition());
// (likely 12.2 and 14.4 call?)	ib.func_225535_a_((ServerWorld)e.world, e.world.rand, e.getPosition(), w.getBlockState(e.getPosition()));\
	
	private boolean isValidTorchLocation(int wallPerimeter,int wallTorchSpacing, int absvx, int absvz, Block wallFenceBlock) {

		boolean hasAWallUnderIt = false;
			if (wallFenceBlock instanceof WallBlock) {
				hasAWallUnderIt = true;
			}
			if (wallFenceBlock instanceof FenceBlock) {
				hasAWallUnderIt = true;
			}
			if (!(hasAWallUnderIt)) {
				return false;
			}
			if  ((absvx == wallPerimeter ) && ((absvz % wallTorchSpacing) == 1)) {
				return true;
			}
			if  (((absvx % wallTorchSpacing) == 1) && (absvz == wallPerimeter )) {
				return true;
			}
			if ((absvx == wallPerimeter) && (absvz == wallPerimeter)) {
				return true;
			}

			return false;
	}

	private boolean placeOneWallPiece(VillagerEntity ve, String regrowthType, 
			int wallPerimeter, int wallTorchSpacing, final int wallCenter, 
			BlockState gateBlockType, boolean buildCenterGate, BlockState wallType, 
			int absvx,int absvz) 
	{
		BlockState bs = ve.world.getBlockState(getBlockPos(ve).down());
		Block blockBs = bs.getBlock();
		
		// do not place walls on stairs to avoid blocking doors
		if (blockBs instanceof StairsBlock) {
			return false;
		}
		
		if ((absvx != wallPerimeter) && (absvz != wallPerimeter)) return false;

		// check for nearby multiple bell cases.
		int scan = (int) wallPerimeter - 2;
		int scanPosX = (int) ve.getPosX();
		int scanPosZ = (int) ve.getPosZ();
		int scanPosY = (int) ve.getPosY();
		boolean noOtherBells = true;
		for (int dy = (int) scanPosY -1; dy < scanPosY + 3; dy++) {
			for (int dx = (int) scanPosX - scan; dx < scanPosX + scan ; dx++) {
				for (int dz = (int) scanPosZ - scan; dz < scanPosZ + scan +1; dz++) {
					BlockPos pos = new BlockPos (dx, dy, dz);
					if (ve.world.getBlockState(pos).getBlock() == Blocks.BELL) {
						return false;
					}
				}
			}
		}

		// Build North and South Walls (and corners)
		if (absvx == wallPerimeter) {
			if (absvz <= wallPerimeter) {
				if (absvz == wallCenter) {
					if (buildCenterGate) {
						ve.world.setBlockState(getBlockPos(ve).down(), gateBlockType);
						return true;
					} else {
						return false;
					}
				} else {
					BlockState b = ve.world.getBlockState(getBlockPos(ve).down());
					Block block = b.getBlock();
					if (	(block instanceof AirBlock)||
							(block instanceof TallGrassBlock)
							) {
						ve.world.setBlockState(getBlockPos(ve).down(), wallType);
					} else {
						ve.world.setBlockState(getBlockPos(ve), wallType);
					}
					return true;
				}
			}
		}
		// Build East and West Walls (and corners)
		if (absvz == wallPerimeter) {
			if (absvx <= wallPerimeter) {
				if (absvx == wallCenter) {
					if (buildCenterGate) {
						ve.world.setBlockState(getBlockPos(ve).down(), gateBlockType);
						return true;
					} else {
						return false;
					}
				} else {
					BlockState b = ve.world.getBlockState(getBlockPos(ve).down());
					Block block = b.getBlock();
					if (	(block instanceof AirBlock)||
							(block instanceof TallGrassBlock)
							) {
						ve.world.setBlockState(getBlockPos(ve).down(), wallType);
					} else {
						ve.world.setBlockState(getBlockPos(ve), wallType);
					}
					return true;
				}
			}
		}
		return false;
	}
	
	private BlockPos getBlockPos (Entity e) {
		return e.getPosition();
	}
	
	private boolean isOnGround (Entity e) {
		return e.isOnGround();
	}

}
	

