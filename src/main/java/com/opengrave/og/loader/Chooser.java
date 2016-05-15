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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.*;

public class Chooser implements ActionListener, MouseListener {
	String chosenToken = null, lastTag = null;
	private JFrame frame;
	private JScrollPane leftScroll, rightScroll;
	private JButton button, addPackButton, delPackButton;
	private ConfigFile conf;
	private JPanel leftPane, rightPane;
	private HashMap<JLabel, String> labels = new HashMap<JLabel, String>();

	public Chooser(ConfigFile conf, File cache) {
		this.conf = conf;
		createLayout();
		addPacks();

		waitWindow();
	}

	private void waitWindow() {
		while (true) {
			if (frame.isVisible()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					break;
				}
			}else{
				return;
			}
		}
	}

	private void addPacks() {
		leftPane.removeAll();
		labels.clear();
		for (Pack pack : conf.getPacks()) {
			Panel cont = new Panel();
			cont.setLayout(new BoxLayout(cont, BoxLayout.X_AXIS));
			// File imageFile = new File(pack.getUrl(), "smallLogo.png");
			try {
				InputStream imageStream = Launcher.openConnection(pack.getUrl()
						+ "smallLogo.png");
				BufferedImage img = ImageIO.read(imageStream);
				if (img.getWidth() == 64 || img.getHeight() == 64) {
					ImageIcon icon = new ImageIcon(img);
					JLabel jicon = new JLabel(icon);
					cont.add(jicon, 0);
					labels.put(jicon, pack.getTokenName());
					jicon.addMouseListener(this);
				} else {
					System.out.println("Icon is not 64x64 : " + pack.getUrl()
							+ "smallLogo.png");
				}
			} catch (IOException e) {
				System.out.println("Could not open icon : " + pack.getUrl()
						+ "smallLogo.png");
			}
			JLabel tag = new JLabel(pack.getDisplayName());
			tag.addMouseListener(this);
			tag.setText(pack.getDisplayName());
			labels.put(tag, pack.getTokenName());
			cont.add(tag);
			leftPane.add(cont);
			System.out.println("Adding left side : " + pack.getTokenName());
		}
		leftPane.revalidate();
		leftPane.repaint();
		frame.revalidate();
		frame.repaint();
		if(conf.getPacks().size()>0){
			setTag(conf.getPacks().get(0).getTokenName());
		}else{
			setTag(null);
		}
	}

	private void createLayout() {
		frame = new JFrame();
		frame.setLayout(new BorderLayout());

		leftPane = new JPanel();
		leftPane.setLayout(new BoxLayout(leftPane, BoxLayout.Y_AXIS));

		leftScroll = new JScrollPane(leftPane);
		frame.add(leftScroll, BorderLayout.WEST);

		rightPane = new JPanel();
		rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.Y_AXIS));

		rightScroll = new JScrollPane(rightPane);
		frame.add(rightScroll, BorderLayout.CENTER);

		Panel p = new Panel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		
		button = new JButton("Run"); // TODO : i18n
		delPackButton = new JButton("Delete Pack");
		addPackButton = new JButton("Add Pack");
		
		frame.add(addPackButton, BorderLayout.NORTH);
				
		p.add(delPackButton);
		p.add(button);
		
		frame.add(p, BorderLayout.SOUTH);

		button.addActionListener(this);
		addPackButton.addActionListener(this);
		delPackButton.addActionListener(this);

		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.setVisible(true);

		button.setEnabled(false);
		delPackButton.setEnabled(false);
	}

	public String getToken() {
		return chosenToken;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == button) { // Check it's the Run button
			chosenToken = lastTag;
			frame.setVisible(false);
		} else if (event.getSource() == addPackButton) {
			String s = (String) JOptionPane.showInputDialog(null,
					"Please paste the Pack URL\n", "Choose name",
					JOptionPane.PLAIN_MESSAGE, null, null, "");
			if (s == null || s.length() == 0) {
				return;
			}
			conf.addPack(s);
			addPacks();
		} else if (event.getSource() == delPackButton){
			int n = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this pack, all config and saves associated with it?");
			System.out.println(n);
			if(n == 0){
				// Yes. 1 = No, 2 = Cancel
				conf.removePack(lastTag);
			}
			addPacks();
		}
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		if (event.getSource() instanceof JLabel) {
			JLabel label = (JLabel) event.getSource();
			if (labels.containsKey(label)) {
				setTag(labels.get(label));
			}
		}
	}

	private void setTag(String tag) {
		lastTag = tag;
		rightPane.removeAll();
		JTextArea area = new JTextArea();
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		if (tag != null) {
			Pack pack = conf.getPack(tag);
			button.setEnabled(true);
			delPackButton.setEnabled(true);
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						Launcher.openConnection(pack.getUrl() + "desc")));
				String inputLine = "";

				String full = "", sep = "";
				while ((inputLine = in.readLine()) != null) {
					full = full +sep + inputLine;
					sep = "\n";
				}
				area.setText(full);
			} catch (IOException ex) {
				area.setText("This pack has no description");
			}
			rightPane.add(area);

		} else {
			button.setEnabled(false);
			delPackButton.setEnabled(false);
			area.setText("No pack selected");
			rightPane.add(area);
		}
		rightPane.revalidate();
		rightPane.repaint();
		frame.revalidate();
		frame.repaint();

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {

	}

	@Override
	public void mouseExited(MouseEvent arg0) {

	}

	@Override
	public void mousePressed(MouseEvent arg0) {

	}

	@Override
	public void mouseReleased(MouseEvent arg0) {

	}
}
