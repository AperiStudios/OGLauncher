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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JFrame;
import javax.swing.JProgressBar;

import com.opengrave.og.MainThreadInterface;

public class Launcher {

	//public static String urlBase;

	private static SSLContext sslContext;

	private static SSLSocketFactory sslSocketFactory;

	JProgressBar bar = new JProgressBar();
	JFrame frame = new JFrame("OpenGrave Launcher");

	public static InputStream openConnection(String urlS) throws IOException {
		if(urlS.startsWith("https:")){
			if (sslSocketFactory == null) {
				prepSSL();
			}
			HttpsURLConnection conn = null;
			try {
				URL url = new URL(urlS);
				conn = (HttpsURLConnection) url.openConnection();
				conn.setSSLSocketFactory(sslSocketFactory);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return conn.getInputStream();			
		}else if(urlS.startsWith("http:")){
			
		}else if(urlS.startsWith("ftp")){
			// Does anyone seriously use FTP anymore?
			System.out.println("Stop living in the past");
		}
		return null;
	}

	public Launcher(File cache, Pack pack) {
		cache = new File(cache, pack.getTokenName());
		if(!cache.isDirectory()){
			if(cache.isFile()){
				System.out.println("File "+cache.getAbsolutePath()+" blocking creation of directory");
				return;
			}
			cache.mkdirs();
		}

		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.add(bar);
		bar.setStringPainted(true);
		bar.setString("Starting...");
		bar.setMaximum(100);
		bar.setValue(0);
		frame.pack();
		frame.setVisible(true);
		frame.setSize(350, 50);

		// Check each file!
		ArrayList<String> fileList = new ArrayList<String>();
		try {
			bar.setString("Getting update list");
			BufferedReader in = new BufferedReader(new InputStreamReader(
					openConnection(pack.getUrl() + "checksums")));
			String inputLine = "";
			while ((inputLine = in.readLine()) != null) {
				inputLine = inputLine.replaceFirst(" +", " ");
				fileList.add(inputLine);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		bar.setMaximum(fileList.size());
		int i = 0;
		for (String string : fileList) {
			String[] s = string.split(" ");
			bar.setString("Checking " + s[1]);
			String upstreamHash = s[0];
			String currentHash = MessageDigestForFile.getDigest(new File(cache,
					s[1]).getAbsolutePath());
			if (!upstreamHash.equals(currentHash)) {
				bar.setString("Downloading " + s[1]);
				downloadAndSave(cache, pack.getUrl() + s[1], s[1]);
			}
			i++;
			bar.setValue(i);
		}
		frame.setVisible(false);
		launch(cache, pack);
	}

	private void launch(File cache, Pack pack) {
		String path = new File(cache, "bin").getAbsolutePath();
		System.setProperty("org.lwjgl.librarypath", path);
		try {
			addDir(path);
		} catch (IOException e2) {
		}

		URLClassLoader loader = (URLClassLoader) ClassLoader
				.getSystemClassLoader();
		MyClassLoader l = new MyClassLoader(loader.getURLs());
		for (String s : readJarList(cache).split("\n")) {
			addJar(l, cache, s);
		}
		addJar(l, cache, "bin/og.jar");
		// for(String s : )
		@SuppressWarnings("rawtypes")
		Class c = null;
		try {
			c = l.loadClass("com.opengrave.MainThread");
			MainThreadInterface hgti = (MainThreadInterface) c.newInstance();

			hgti.startApp(cache, pack.getUserName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private String readJarList(File cache) {
		File f = new File(cache, "jarlist");
		String contents = "";
		try (FileReader in = new FileReader(f.getAbsolutePath());
				BufferedReader br = new BufferedReader(in)) {
			String line = br.readLine();
			while (line != null) {
				contents = contents + line + "\n";
				try {
					line = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
					line = null;
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("Cannot open file " + f.getAbsolutePath());
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}

		return contents;
	}

	private void addJar(MyClassLoader l, File cache, String name) {
		File f = new File(cache, name);
		try {
			l.addURL(f.toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public static void prepSSL() {
		try {
			final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public void checkClientTrusted(final X509Certificate[] chain,
						final String authType) {
				}

				@Override
				public void checkServerTrusted(final X509Certificate[] chain,
						final String authType) {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			} };
			sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts,
					new java.security.SecureRandom());
			sslSocketFactory = sslContext.getSocketFactory();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		prepSSL();
		File cache = new File(defaultDirectory(), ".opengrave");
		if (cache.exists() && cache.isDirectory()) {
			System.out.println("Using " + cache.getAbsolutePath());
		} else if (cache.exists() && !cache.isDirectory()) {
			System.out.println("There's a file called .opengrave in here. Move or delete it");
			return;
		} else {
			System.out.println("Creating " + cache.getAbsolutePath());
			cache.mkdir();
			cache = new File(defaultDirectory(), ".opengrave");
		}
		ConfigFile cf = new ConfigFile(cache);
		String version = null;
		boolean update = false;
		boolean valid = true;
		for (String arg : args) {
			arg=arg.toLowerCase();
			if(arg.startsWith("--run=")){
				version = arg.split("=")[1];
			}else if(arg.equals("--noupdate")){
				update = false;
			}else{
				valid = false;
			}
			System.out.println(arg);
		}
		if(!valid){
			System.out.println("Unknown arguement used");
			System.out.println(" --run=PACKTOKEN   Run a specific pack, using it's token. Cannot contain spaces");
			System.out.println(" --noupdate        Do not check integrity of files or download any missing files. Best for quick testing but not intended for users");
			System.exit(0);
		}
		if(version == null){
			Chooser choose = new Chooser(cf, cache);
			version = choose.getToken();
		}
		if(version==null){
			System.exit(0);
		}
		Pack pack = cf.getPack(version);
		if(pack == null){
			System.out.println("No installed pack with the token : "+version);
			System.exit(1);
		}
		System.out.println("Using version : " + version);
		String urlBase = "https://aperistudios.co.uk/hg/" + version + "/";
		new Launcher(cache, pack);
	}

	public static void downloadAndSave(File cache, String url, String local) {
		System.out.println(cache);
		System.out.println(local);
		System.out.println(url);
		File f = new File(cache, local);
		File parent = f.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}

		System.out.println(cache + " " + url);
		byte[] buffer = new byte[8 * 1024];
		InputStream input = null;
		OutputStream output = null;
		try {
			input = openConnection(url);
			output = new FileOutputStream(
					new File(cache, local).getAbsolutePath());
			int bytesRead;
			while ((bytesRead = input.read(buffer)) != -1) {
				output.write(buffer, 0, bytesRead);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (output != null) {
					output.close();
				}
				if (input != null) {
					input.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String defaultDirectory() {
		String OS = System.getProperty("os.name").toUpperCase();
		if (OS.contains("WIN"))
			return System.getenv("APPDATA");
		else if (OS.contains("MAC"))
			return System.getProperty("user.home") + "/Library/Application "
					+ "Support";
		else if (OS.contains("NUX"))
			return System.getProperty("user.home");
		return System.getProperty("user.dir");
	}
	
	public static void addDir(String s) throws IOException {
		try {
			Field field = ClassLoader.class.getDeclaredField("usr_paths");
			field.setAccessible(true);
			String[] paths = (String[])field.get(null);
			for (int i = 0; i < paths.length; i++) {
				if (s.equals(paths[i])) {
					return;
				}
			}
			String[] tmp = new String[paths.length+1];
			System.arraycopy(paths,0,tmp,0,paths.length);
			tmp[paths.length] = s;
			field.set(null,tmp);
			System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + s);
		} catch (IllegalAccessException e) {
			throw new IOException("Failed to get permissions to set library path");
		} catch (NoSuchFieldException e) {
			throw new IOException("Failed to get field handle to set library path");
		}
	}
}
