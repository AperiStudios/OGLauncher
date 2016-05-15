/*
 * Copyright 2016 Nathan Howard
 * 
 * This file is part of OpenGrave Launcher
 * 
 * OpenGrave Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenGrave Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with OpenGrave Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.opengrave.og.loader;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.JOptionPane;

public class ConfigFile {
	Random r = new Random();
	String acceptableExtras = "abcdefghijklmnopqrstuvwxyz0123456789";
	private ArrayList<Pack> packs = new ArrayList<Pack>();
	private String userName;
	private File config;
	
	public void save(){
		DataOutputStream dos = null;
		
		try {
			dos = new DataOutputStream(new FileOutputStream(config));
			dos.writeInt(userName.length());
			dos.writeChars(userName);
			dos.writeInt(packs.size());
			for(Pack p : packs){
				p.writePack(dos);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Pack getPack(String version) {
		for(Pack pack : packs){
			if(pack.getTokenName().equalsIgnoreCase(version)){
				return pack;
			}
		}
		return null;
	}
	
	public ConfigFile(File cache){
		config = new File(cache, "config");
		if(!config.isFile()){
			String s = (String)JOptionPane.showInputDialog(
			                    null,
			                    "Welcome to OpenGrave Launcher\n"
			                    + "What would you like to be your default Username?",
			                    "Choose name",
			                    JOptionPane.PLAIN_MESSAGE,
			                    null,
			                    null, "");
			if(s == null || s.length() == 0){
				s = "Lost Soul";
			}
			userName = s;
		}else{
			DataInputStream dis=null;
			try {
				dis =new DataInputStream(new FileInputStream(config));
				String name=""; 
				StringBuilder sb = new StringBuilder();
				int len = dis.readInt();
				for (int i = 0; i < len; i++) {
					sb.append(dis.readChar());
				}
				userName = sb.toString();
				packs.clear();
				len = dis.readInt();
				for(int i = 0; i < len; i++){
					Pack p = Pack.readPack(dis);
					packs.add(p);
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(dis!=null){
				try {
					dis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public ArrayList<Pack> getPacks() {
		return packs;
	}

	public void addPack(String s) {
		if(!s.startsWith("http")){ // Assume secure site should be used unless specified
			s = "https://"+s;
		}
		if(!s.endsWith("/")){
			s = s + "/";
		}
		String prefTag = null, display = null;
		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(
					Launcher.openConnection(s + "meta")));
			String inputLine = "";
			while ((inputLine = in.readLine()) != null) {
				if(prefTag == null){
					prefTag = inputLine;
				}else if(display == null){
					display = inputLine;
				}
			}
			prefTag = getAcceptableTag(prefTag);
			System.out.println("Adding new Pack : "+prefTag+" "+display);
			Pack p = new Pack(s, display, prefTag, userName);
			packs.add(p);
		}catch(IOException io){
			System.out.println("Could not connect to pack, Not added"); // TODO Alert user via window?
		}
		save();
		
	}

	private String getAcceptableTag(String prefTag) {
		if(getPack(prefTag)==null){
			return prefTag;
		}
		// Add a random extra letter/number to end
		return getAcceptableTag(prefTag+acceptableExtras.charAt(r.nextInt(acceptableExtras.length())));
	}

	public void removePack(String lastTag) {
		ArrayList<Pack> newList = new ArrayList<Pack>();
		for(Pack p : packs){
			if(p.getTokenName().equals(lastTag)){
				continue;
			}
			newList.add(p);
		}
		packs = newList;
		save();
	}
	
}
