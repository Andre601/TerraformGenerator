package org.terraform.schematic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.util.Vector;
import org.terraform.data.SimpleBlock;
import org.terraform.main.TerraformGeneratorPlugin;
import org.terraform.utils.BlockUtils;

public class TerraSchematic {
	
	HashMap<Vector,BlockData> data = new HashMap<>();
	Location refPoint;
	BlockFace face = BlockFace.NORTH;
	//North is always the default blockface.
	//
	public TerraSchematic(Location refPoint){
		this.refPoint = refPoint;
	}
	
	public void registerBlock(Block b){
		Vector rel = b.getLocation().toVector().subtract(refPoint.toVector());
		//String coords = rel.getBlockX() + "," + rel.getBlockY() + "," + rel.getBlockZ();
		data.put(rel,b.getBlockData());
	}
	
	public void apply(){
		ArrayList<Vector> multiFace = new ArrayList<>();
		
		for(Vector key:data.keySet()){
			Vector pos = key.clone();
			BlockData bd = data.get(key);
			if(face == BlockFace.WEST){
				int x = pos.getBlockX();
				pos.setX(pos.getZ()*-1);
				pos.setZ(x);
			}else if(face == BlockFace.SOUTH){
				pos.setX(pos.getX()*-1);
				pos.setZ(pos.getZ()*-1);
			}else if(face == BlockFace.EAST){
				int x = pos.getBlockX();
				pos.setX(pos.getZ());
				pos.setZ(x*-1);
			}
			
			if(face != BlockFace.NORTH)
				if(bd instanceof Orientable){
					Orientable o = (Orientable) bd;
					if(face == BlockFace.EAST || face == BlockFace.WEST){
						if(o.getAxis() == Axis.X){
							o.setAxis(Axis.Z);
						}else if(o.getAxis() == Axis.Z){
							o.setAxis(Axis.X);
						}
					}
				}else if(bd instanceof Rotatable){
					Rotatable r = (Rotatable) bd;
					if(face == BlockFace.SOUTH){
						r.setRotation(r.getRotation().getOppositeFace());
					}else if(face == BlockFace.EAST){
						r.setRotation(BlockUtils.getAdjacentFaces(r.getRotation())[1]);
					}else if(face == BlockFace.WEST){
						r.setRotation(BlockUtils.getAdjacentFaces(r.getRotation())[0]);
					}
				}else if(bd instanceof Directional){
					Directional r = (Directional) bd;
					if(BlockUtils.directBlockFaces.contains(r.getFacing()))
						if(face == BlockFace.SOUTH){
							r.setFacing(r.getFacing().getOppositeFace());
						}else if(face == BlockFace.EAST){
							r.setFacing(BlockUtils.getAdjacentFaces(r.getFacing())[1]);
						}else if(face == BlockFace.WEST){
							r.setFacing(BlockUtils.getAdjacentFaces(r.getFacing())[0]);
						}
				}else if(bd instanceof MultipleFacing){
					multiFace.add(pos);
				}
			
			refPoint.getBlock().getRelative(pos.getBlockX(),pos.getBlockY(),pos.getBlockZ()).setBlockData(bd);
		}
		
		//Multiple-facing blocks are just gonna be painful.
		for(Vector pos:multiFace){
			Block block = refPoint.getBlock().getRelative(pos.getBlockX(),pos.getBlockY(),pos.getBlockZ());
			SimpleBlock b = new SimpleBlock(block);
			BlockUtils.correctSurroundingMultifacingData(b);
		}
	}
	
	public void export(String path) throws IOException{
		File fout = new File(path);
		FileOutputStream fos = new FileOutputStream(fout);
	 
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
	 
		for (Entry<Vector,BlockData> entry:data.entrySet()) {
			String v = entry.getKey().getBlockX() + "," +  entry.getKey().getBlockY() + "," +  entry.getKey().getBlockZ();
			bw.write(v + ":@:" + entry.getValue().getAsString());
			bw.newLine();
		}
	 
		bw.close();
	}
	
	public static TerraSchematic load(String internalPath, Location refPoint) throws FileNotFoundException{
		TerraSchematic schem = new TerraSchematic(refPoint);
		InputStream is= TerraformGeneratorPlugin.class.getResourceAsStream("/"+internalPath+".terra");   
		Scanner sc=new Scanner(is);    //file to be scanned  
		//returns true if there is another line to read  
		while(sc.hasNextLine())  
		{  
			String line = sc.nextLine();
			if(line.equals("")) continue;
			String[] cont = line.split(":@:");
			String[] v = cont[0].split(",");
			Vector key = new Vector(Integer.parseInt(v[0]),Integer.parseInt(v[1]),Integer.parseInt(v[2]));
			BlockData value = Bukkit.createBlockData(cont[1]);
			schem.data.put(key,value);
		}  
		sc.close();  
		return schem;
	}
	
	public void rotate(BlockFace face){
		this.face = face;
	}

}
