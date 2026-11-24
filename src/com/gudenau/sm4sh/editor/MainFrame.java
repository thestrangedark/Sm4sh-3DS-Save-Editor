package com.gudenau.sm4sh.editor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.gudenau.sm4sh.editor.save.Save;
import com.gudenau.sm4sh.editor.util.FrameUtils;

public class MainFrame implements ActionListener {
	private JFrame frame;
	
	private JLabel currentFile;
	
	public JButton character;
	public JButton equipment;
	public JButton fix;
	
	private JMenuBar menuBar;
	
	private JMenu file;
	
	private JMenuItem open;
	private JMenuItem save;
	private JMenuItem close;
	
	private Save gameSave;
	private File currentAccount;
	private File currentSystem;
	
	public static BufferedImage icon;
	
	private JSpinner money;
	
	public MainFrame(LoadingScreen screen){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Throwable t){}
		
		try {
			Equipment.loadResources();
		} catch (Throwable e) {
			displayError("Error loading!");
			e.printStackTrace();
			return;
		}
		
		frame = new JFrame("Sm4sh 3DS Save Editor");
		
		frame.setIconImage(FrameUtils.icon);
		
		{
			menuBar = new JMenuBar();
			
			{
				file = new JMenu("File");
				
				open = new JMenuItem("Open");
				open.addActionListener(this);
				
				save = new JMenuItem("Save");
				save.addActionListener(this);
				
				close = new JMenuItem("Close");
				close.addActionListener(this);
				
				file.add(open);
				file.add(save);
				file.add(close);
			}
			
			menuBar.add(file);
		}
		
		{
			JPanel panel = new JPanel(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			
			constraints.gridheight = 1;
			constraints.gridwidth = 1;
			constraints.gridx = 0;
			constraints.gridy = 0;
			constraints.insets = new Insets(2, 2, 2, 2);
			
			panel.add(new JLabel("Current save: "), constraints);
			
			constraints.gridx = 1;
			constraints.gridwidth = 3;
			currentFile = new JLabel("None");
			panel.add(currentFile, constraints);
			constraints.gridwidth = 1;
			
			constraints.gridx = 0;
			constraints.gridy = 1;
			character = new JButton("Characters");
			character.addActionListener(this);
			panel.add(character, constraints);
			
			constraints.gridx = 1;
			panel.add(new JLabel("Gold:"), constraints);
			
			constraints.gridx = 2;
			money = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
			EquipmentEditor.setupSpinner(money);
			panel.add(money, constraints);
			
			constraints.gridx = 3;
			equipment = new JButton("Equipment");
			equipment.addActionListener(this);
			panel.add(equipment, constraints);
			
			constraints.gridx = 0;
			constraints.gridy = 2;
			fix = new JButton("Fix save");
			fix.addActionListener(this);
			panel.add(fix, constraints);
			
			frame.add(panel);
		}
		
		updateCurrent(null);
		
		frame.setJMenuBar(menuBar);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
				if(confirmDiscard()){
					System.exit(0);
				}
			}
		});
		frame.setResizable(false);
		FrameUtils.center(frame);
		screen.close();
		frame.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		
		if(source == open){
			openFile();
		}else if(source == close){
			if(frames.character != null || frames.equipment != null){
				displayError("You must close all other windows before closing the save!");
			}else if(confirmDiscard()){
				updateCurrent(null);
			}
		}else if(source == save){
			saveFile();
		}else if(source == character){
			displayCharacterUnlocks();
		}else if(source == equipment){
			displayEquipment();
		}else if(source == fix){
			fixSave();
		}
	}

	private void saveFile() {
		saveFile(frame);
	}

	/** Copies everything shown in the open windows into {@link #gameSave}. */
	private void applyEditsToSave() {
		if(frames.equipment != null){
			frames.equipment.commit();
		}
		
		try{
			money.commitEdit();
		}catch(java.text.ParseException e){
			// Invalid text in the box: keep the last valid amount.
		}
		gameSave.money = ((Number)money.getValue()).longValue() & 0xFFFFFFFFL;
	}

	/** Copies both save files to *.bak, once per opened save, before the first write. */
	private boolean backedUp = false;

	private String backupIfNeeded() throws java.io.IOException {
		if(backedUp){
			return null;
		}
		for(File f : new File[]{currentAccount, currentSystem}){
			java.nio.file.Files.copy(f.toPath(), new File(f.getPath() + ".bak").toPath(),
					java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		backedUp = true;
		return currentSystem.getParentFile().getAbsolutePath();
	}

	/** Writes the open save to disk. {@code parent} is the window the result popup is shown over. */
	public boolean saveFile(java.awt.Component parent) {
		if(gameSave != null && currentAccount != null && currentSystem != null){
			try{
				applyEditsToSave();
				String backupDir = backupIfNeeded();
				gameSave.save(currentAccount, currentSystem);
				if(backupDir != null){
					JOptionPane.showMessageDialog(parent, "Saved!\n\nA backup of the save from before this edit was made as\naccount_data.bin.bak and system_data.bin.bak in:\n" + backupDir);
				}else{
					JOptionPane.showMessageDialog(parent, "Saved!");
				}
				return true;
			}catch(Exception e){
				displayError("Saving", e);
			}
		}
		return false;
	}

	/** True when the open windows hold changes that are not in the file yet. */
	private boolean hasUnsavedChanges() {
		if(gameSave == null || currentSystem == null){
			return false;
		}
		File temp = null;
		try{
			applyEditsToSave();
			// Do a save into a scratch copy and see if it comes out different from the real file.
			temp = File.createTempFile("sm4sh-check", ".bin");
			java.nio.file.Files.copy(currentSystem.toPath(), temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			gameSave.save(currentAccount, temp);
			return !java.util.Arrays.equals(java.nio.file.Files.readAllBytes(currentSystem.toPath()),
					java.nio.file.Files.readAllBytes(temp.toPath()));
		}catch(Exception e){
			e.printStackTrace();
			return true; // Can't tell, so ask to be safe.
		}finally{
			if(temp != null){
				temp.delete();
			}
		}
	}

	/** Asks about unsaved changes. Returns true if it is OK to go ahead and drop the open save. */
	private boolean confirmDiscard() {
		if(!hasUnsavedChanges()){
			return true;
		}
		int choice = JOptionPane.showConfirmDialog(frame, "You have unsaved changes. Save them first?",
				"Unsaved changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if(choice == JOptionPane.YES_OPTION){
			return saveFile(frame);
		}
		return choice == JOptionPane.NO_OPTION;
	}

	private void fixSave() {
		JFileChooser fileChooser = new JFileChooser(new File("."));
		
		fileChooser.setFileFilter(new FileFilter(){
			@Override
			public boolean accept(File file) {
				File parent = file.getParentFile();
				if(file.isDirectory()){
					return true;
				}
				
				switch(file.getName()){
				case "account_data.bin":
					return new File(parent, "system_data.bin").isFile() && file.isFile();
				case "system_data.bin":
					return new File(parent, "account_data.bin").isFile() && file.isFile();
				}
				
				return false;
			}

			@Override
			public String getDescription() {
				return "Sm4sh 3DS Save";
			}
		});
		
		if(gameSave == null || currentAccount == null || currentSystem == null){
			displayError("Open the save you want to fix first.");
			return;
		}
		
		fileChooser.setDialogTitle("Pick a working save from your 3DS to copy the ID from");
		
		switch(fileChooser.showOpenDialog(frame)){
		case JFileChooser.APPROVE_OPTION:
			try{
				String backupDir = backupIfNeeded();
				
				RandomAccessFile source = new RandomAccessFile(fileChooser.getSelectedFile(), "r");
				source.seek(0x12);
				byte[] data = new byte[4];
				source.read(data);
				source.close();
				
				RandomAccessFile dest = new RandomAccessFile(currentAccount, "rw");
				dest.seek(0x12);
				dest.write(data, 0, 4);
				dest.close();
				
				dest = new RandomAccessFile(currentSystem, "rw");
				dest.seek(0x12);
				dest.write(data, 0, 4);
				dest.close();
				
				JOptionPane.showMessageDialog(frame, "Save fixed!" + (backupDir != null
						? "\n\nA backup was made as account_data.bin.bak and system_data.bin.bak in:\n" + backupDir : ""));
			}catch(Throwable t){
				displayError("fixing the save", t);
			}
			return;
		}
	}

	private void displayEquipment() {
		frames.equipment = new EquipmentEditor(gameSave.equipment, this);
		equipment.setEnabled(false);
	}

	private void displayCharacterUnlocks() {
		frames.character = new CharacterUnlocks(gameSave.characters, this);
		character.setEnabled(false);
	}

	private void openFile() {
		if(frames.character != null || frames.equipment != null){
			displayError("You must close all other windows before loading a new save!");
			return;
		}
		
		if(!confirmDiscard()){
			return;
		}
		
		JFileChooser fileChooser = new JFileChooser(new File("."));
		
		fileChooser.setFileFilter(new FileFilter(){
			@Override
			public boolean accept(File file) {
				File parent = file.getParentFile();
				if(file.isDirectory()){
					return true;
				}
				
				switch(file.getName()){
				case "account_data.bin":
					return new File(parent, "system_data.bin").isFile() && file.isFile();
				case "system_data.bin":
					return new File(parent, "account_data.bin").isFile() && file.isFile();
				}
				
				return false;
			}

			@Override
			public String getDescription() {
				return "Sm4sh 3DS Save";
			}
		});
		
		switch(fileChooser.showOpenDialog(frame)){
		case JFileChooser.APPROVE_OPTION:
			try{
				updateCurrent(fileChooser.getSelectedFile());
			}catch(Throwable t){
				updateCurrent(null);
			}
			return;
		}
	}

	private void updateCurrent(File file) {
		if(file == null){
			gameSave = null;
			currentAccount = null;
			currentSystem = null;
			currentFile.setText("none");
			money.setValue(0);
			money.setEnabled(false);
			equipment.setEnabled(false);
			fix.setEnabled(false);
			backedUp = false;
			
			character.setEnabled(false);
		}else{
			try{
				backedUp = false;
				currentAccount = new File(file.getParentFile(), "account_data.bin");
				currentSystem = new File(file.getParentFile(), "system_data.bin");
				gameSave = Save.loadSave(currentAccount, currentSystem);
				currentFile.setText(file.getParentFile().getName());
				money.setValue((int) Math.min(gameSave.money, Integer.MAX_VALUE));
				money.setEnabled(true);
				equipment.setEnabled(true);
				fix.setEnabled(true);
			
				character.setEnabled(true);
			}catch(Exception e){
				displayError("loading", e);
				updateCurrent(null);
			}
		}
	}
	
	public static class frames {
		public static EquipmentEditor equipment;
		public static CharacterUnlocks character;		
	}
	
	private void displayError(String string, Throwable caught) {
		try{
			File file = new File(new File("."), Long.toString(System.currentTimeMillis()) + ".log");
			file.createNewFile();
			FileOutputStream out = new FileOutputStream(file);
			caught.printStackTrace(new PrintStream(out));
			out.close();
			
			displayError("There was an error while " + string + " please report it! The stack dump is in " + file.getAbsolutePath());
		}catch(Throwable t){
			displayError("There was an error displaying an error. :-(");
		}
	}
	
	private void displayError(String string) {
		JOptionPane.showMessageDialog(frame, string, "ERROR", JOptionPane.ERROR_MESSAGE);
	}
}
