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

import java.net.URL;
import java.net.URLClassLoader;

public class MyClassLoader extends URLClassLoader {

	public MyClassLoader(URL[] urls) {
		super(urls);
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}

}
