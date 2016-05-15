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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class Pack {
	private String url;
	private String displayName;
	private String tokenName; // Also sub-directory name
	private String username;
	
	public Pack(String url, String displayName, String tokenName, String username){
		this.url = url; this.displayName = displayName; this.tokenName=tokenName; this.username = username;
	}
	
	public String getUrl(){
		return url;
	}
	
	public String getDisplayName(){ // TODO : Care about i18n. Possibly have the pack moderator/creator give multiple display names dependant on language.
		return displayName;
	}
	
	/**
	 * @return the subdirectory name for this instance of a pack. The same pack can be added multiple times with different save files if the tokens don't match
	 */
	public String getTokenName(){
		return tokenName;
	}
	
	public String getUserName(){
		return username;
	}

	public File getCache(File cache) {
		return new File(cache, tokenName);
	}

	public static Pack readPack(DataInputStream dis) throws IOException {
		String u,d,t,uN;
		u = readString(dis);
		d = readString(dis);
		t = readString(dis);
		uN = readString(dis);
		return new Pack(u,d,t,uN);
	}
	
	public void writePack(DataOutputStream dos) throws IOException {
		writeString(dos, url);
		writeString(dos, displayName);
		writeString(dos, tokenName);
		writeString(dos, username);
	}
	
	private void writeString(DataOutputStream dos, String s) throws IOException {
		dos.writeInt(s.length());
		dos.writeChars(s);
	}

	public static String readString(DataInputStream dis) throws IOException{
		StringBuilder sb = new StringBuilder();
		int len = dis.readInt();
		for (int i = 0; i < len; i++) {
			sb.append(dis.readChar());
		}
		return sb.toString();

	}
}
