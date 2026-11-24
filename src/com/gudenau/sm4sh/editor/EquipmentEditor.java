package com.gudenau.sm4sh.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.gudenau.sm4sh.editor.save.EquipmentSave;
import com.gudenau.sm4sh.editor.util.SortedComboBoxModel;

public class EquipmentEditor implements WindowListener, ListSelectionListener, ItemListener, ActionListener {
	private JFrame frame;
	private MainFrame main;
	
	private JComboBox<Equipment.Item> type;
	private JSpinner level;
	private JComboBox<Equipment.Bonus> bonus;
	
	private JList<EquipmentSave.Item> list;
	private JLabel icon;
	private JTextArea effect;
	
	private JSpinner attack;
	private JSpinner defence;
	private JSpinner speed;
	/** Strength rank: the game only uses 1, 5, 9, 13, 17, 21, 25 (1 = strongest). */
	private JComboBox<Integer> range;
	private static final int[] RANKS = {1, 5, 9, 13, 17, 21, 25};
	
	private JButton saveButton;
	private JButton addButton;
	private EquipmentSave save;
	
	private int current = -1;
	private boolean loadedOk = false;
	
	public EquipmentEditor(EquipmentSave save, MainFrame main){
		frame = new JFrame("Equipment");
		frame.setIconImage(MainFrame.icon);
		frame.addWindowListener(this);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.main = main;
		this.save = save;
		
		JPanel panel = new JPanel(new GridBagLayout());
		{
			GridBagConstraints constraints = new GridBagConstraints();
			
			list = new JList<EquipmentSave.Item>(save.equpment);
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			list.setSelectedIndex(0);
			list.addListSelectionListener(this);
			
			constraints.gridheight = 3;
			constraints.gridwidth = 1;
			constraints.gridx = 0;
			constraints.gridy = 0;
			JScrollPane jsp = new JScrollPane(list);
			jsp.setPreferredSize(new Dimension(290, 192));
			panel.add(jsp, constraints);
			
			{
				JPanel temp = new JPanel(new GridBagLayout());
				GridBagConstraints c = new GridBagConstraints();
				c.anchor = GridBagConstraints.SOUTH;
				
				c.gridheight = 2;
				c.gridwidth = 1;
				c.gridx = 0;
				c.gridy = 0;
				
				icon = new JLabel();
				icon.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
				temp.add(icon, c);
				
				c.gridheight = 1;
				c.gridx = 1;
				temp.add(new JLabel("Type"), c);
				c.gridx = 2;
				temp.add(new JLabel("Level"), c);
				
				c.gridy = 1;
				c.gridx = 1;
				c.anchor = GridBagConstraints.NORTH;
				type = new JComboBox<Equipment.Item>(new SortedComboBoxModel<Equipment.Item>());
				type.addItem(Equipment.invalid);
				for(Equipment.Item i : Equipment.equipment){
					if(i != null){
						type.addItem(i);
					}
				}
				
				type.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						// Show the picture for the newly chosen type straight away.
						Equipment.Item chosen = (Equipment.Item) type.getSelectedItem();
						if(e.getStateChange() == ItemEvent.SELECTED && chosen != null && chosen != Equipment.invalid && chosen.icon != null){
							icon.setIcon(chosen.icon);
						}
					}
				});
				temp.add(type, c);
				
				c.gridx = 2;
				level = new JSpinner(new SpinnerNumberModel(0, 0, 2, 1));
				level.setToolTipText("Set automatically from Strength, the way the game does it");
				temp.add(level, c);
				
				constraints.gridheight = 1;
				constraints.gridx = 1;
				panel.add(temp, constraints);
			}
			
			{
				JPanel temp = new JPanel(new GridBagLayout());
				GridBagConstraints c = new GridBagConstraints();
				
				c.gridheight = 1;
				c.gridwidth = 1;
				c.gridx = 0;
				c.gridy = 0;
				
				temp.add(new JLabel("Attack"), c);
				
				c.gridx = 1;
				temp.add(new JLabel("Defence"), c);
				
				c.gridx = 2;
				temp.add(new JLabel("Speed"), c);
				
				c.gridx = 0;
				c.gridy = 1;
				
				attack = new JSpinner(new SpinnerNumberModel(0, Short.MIN_VALUE, Short.MAX_VALUE, 1));
				temp.add(attack, c);
				
				c.gridx = 1;
				defence= new JSpinner(new SpinnerNumberModel(0, Short.MIN_VALUE, Short.MAX_VALUE, 1));
				temp.add(defence, c);
				
				c.gridx = 2;
				speed = new JSpinner(new SpinnerNumberModel(0, Short.MIN_VALUE, Short.MAX_VALUE, 1));
				temp.add(speed, c);
				
				
				c.gridx = 0;
				c.gridy = 2;
				c.gridwidth = 2;
				temp.add(new JLabel("Bonus"), c);
				
				c.gridx = 2;
				c.gridwidth = 1;
				temp.add(new JLabel("Strength"), c);
				
				c.gridx = 0;
				c.gridy = 3;
				c.gridwidth = 2;
				
				bonus = new JComboBox<Equipment.Bonus>(new SortedComboBoxModel<Equipment.Bonus>());
				bonus.addItem(Equipment.noBounus);
				bonus.addItemListener(this);
				
				for(Equipment.Bonus b : Equipment.bonus){
					if(b != null){
						bonus.addItem(b);
					}
				}
				temp.add(bonus, c);
				
				c.gridx = 2;
				c.gridwidth = 1;
				range = new JComboBox<Integer>();
				for(int r : RANKS){
					range.addItem(r);
				}
				range.setToolTipText("<html>The game's strength rank, used by \"sort by strength\".<br>"
						+ "1 is the strongest, 25 the weakest. It also sets the item's level (its name).</html>");
				range.setRenderer(new javax.swing.DefaultListCellRenderer() {
					@Override
					public java.awt.Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean sel, boolean focus) {
						String text = String.valueOf(value);
						if(value instanceof Integer){
							int r = (Integer) value;
							if(r == RANKS[0]){
								text = r + " (strongest)";
							}else if(r == RANKS[RANKS.length - 1]){
								text = r + " (weakest)";
							}else if(levelForRank(r) == -1){
								text = r + " (not a game value)";
							}
						}
						return super.getListCellRendererComponent(l, text, index, sel, focus);
					}
				});
				range.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						if(e.getStateChange() == ItemEvent.SELECTED){
							updateLevel();
						}
					}
				});
				temp.add(range, c);
				
				constraints.gridy = 1;
				panel.add(temp, constraints);
			}
			
			{
				JPanel temp = new JPanel(new BorderLayout());
				temp.add(new JLabel("Bonus Effect", SwingConstants.CENTER), BorderLayout.NORTH);
				effect = new JTextArea("None");
				effect.setWrapStyleWord(true);
				effect.setLineWrap(true);
				effect.setOpaque(false);
				effect.setEditable(false);
				effect.setFocusable(false);
				effect.setBackground(UIManager.getColor("Label.background"));
				effect.setFont(UIManager.getFont("Label.font"));
				effect.setBorder(UIManager.getBorder("Label.border"));
				effect.setPreferredSize(new Dimension(230, 14*3));
				temp.add(effect, BorderLayout.CENTER);
				
				constraints.gridy = 2;
				panel.add(temp, constraints);
			}
			
			{
				addButton = new JButton("Add new item");
				addButton.setToolTipText("Put a new piece of equipment in the first empty slot");
				addButton.addActionListener(this);
				
				saveButton = new JButton("Save");
				saveButton.setToolTipText("Write the selected item (and the rest of the save) to the file");
				saveButton.addActionListener(this);
				
				JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
				buttons.add(addButton);
				buttons.add(saveButton);
				
				GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 3;
				c.gridwidth = 2;
				c.anchor = GridBagConstraints.SOUTHEAST;
				c.insets = new Insets(4, 4, 4, 4);
				panel.add(buttons, c);
			}
		}
		
		setupSpinner(attack);
		setupSpinner(defence);
		setupSpinner(speed);
		setupSpinner(level);
		
		refresh(0);
		
		frame.add(panel);
		frame.setResizable(false);
		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public void windowOpened(WindowEvent e) {}

	@Override
	public void windowClosing(WindowEvent e) {
		// Keep the edits to the item on screen, otherwise they are lost when the window closes.
		commit();
		MainFrame.frames.equipment = null;
		main.equipment.setEnabled(true);
		frame.dispose();
	}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}

	@Override
	public void valueChanged(ListSelectionEvent event) {
		refresh(list.getSelectedIndex());
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if(event.getSource() == saveButton){
			commit();
			main.saveFile(frame);
		}else if(event.getSource() == addButton){
			addItem();
		}
	}

	/** Asks which equipment to add, adds it, and selects it so its stats can be edited. */
	private void addItem() {
		commit();
		
		JComboBox<Equipment.Item> choice = new JComboBox<Equipment.Item>(new SortedComboBoxModel<Equipment.Item>());
		for(Equipment.Item i : Equipment.equipment){
			if(i != null){
				choice.addItem(i);
			}
		}
		
		JPanel ask = new JPanel(new BorderLayout(0, 6));
		ask.add(new JLabel("Which equipment do you want to add?"), BorderLayout.NORTH);
		ask.add(choice, BorderLayout.CENTER);
		ask.add(new JLabel("<html>It starts with normal stats and no bonus;<br>change them after it is added, then Save.</html>"), BorderLayout.SOUTH);
		
		if(javax.swing.JOptionPane.showConfirmDialog(frame, ask, "Add new item",
				javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE) != javax.swing.JOptionPane.OK_OPTION){
			return;
		}
		
		Equipment.Item picked = (Equipment.Item) choice.getSelectedItem();
		if(picked == null){
			return;
		}
		
		int slot = save.addItem(picked.type, picked.id);
		if(slot == -1){
			javax.swing.JOptionPane.showMessageDialog(frame, "All 3000 equipment slots are full. Sell or remove something in the game first.",
					"No room", javax.swing.JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// The new item has nothing on screen to commit yet, so load it fresh.
		loadedOk = false;
		current = -1;
		if(list.getSelectedIndex() == slot){
			refresh(slot);  // Already selected (it was the empty slot), so no change event will come.
		}else{
			list.setSelectedIndex(slot);
		}
		list.ensureIndexIsVisible(slot);
		list.repaint();
	}

	/**
	 * Shows plain numbers (no thousands commas) and selects the whole number when the
	 * box is clicked or tabbed into, so typing replaces the old value instead of being
	 * inserted into it (which made an out-of-range number that got thrown away).
	 */
	static void setupSpinner(JSpinner spinner) {
		spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
		final javax.swing.JFormattedTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
		field.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusGained(java.awt.event.FocusEvent e) {
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						field.selectAll();
					}
				});
			}
		});
	}

	/** The level the game pairs with a strength rank: 1,5 -> 0; 9,13,17 -> 1; 21,25 -> 2. -1 if it isn't a game value. */
	static int levelForRank(int rank) {
		switch(rank){
		case 1: case 5: return 0;
		case 9: case 13: case 17: return 1;
		case 21: case 25: return 2;
		default: return -1;
		}
	}

	private int selectedRank() {
		Integer r = (Integer) range.getSelectedItem();
		return r == null ? RANKS[2] : r;
	}

	/** Shows a rank, adding it to the list (marked as unusual) if the game never uses it, so it isn't silently changed. */
	private void showRank(int rank) {
		for(int i = range.getItemCount() - 1; i >= 0; i--){
			if(levelForRank(range.getItemAt(i)) == -1 && range.getItemAt(i) != rank){
				range.removeItemAt(i);
			}
		}
		boolean found = false;
		for(int i = 0; i < range.getItemCount(); i++){
			if(range.getItemAt(i) == rank){
				found = true;
			}
		}
		if(!found){
			range.addItem(rank);
		}
		range.setSelectedItem(rank);
	}

	/** Keeps the Level box in step with Strength and Bonus. The box itself is display-only. */
	private void updateLevel() {
		level.setEnabled(false);
		Equipment.Bonus b = (Equipment.Bonus) bonus.getSelectedItem();
		if(b != null && b.id != -1){
			level.setValue(2);
			return;
		}
		int lv = levelForRank(selectedRank());
		if(lv != -1){
			level.setValue(lv);
		}
	}

	/** Pushes any text typed into a spinner into its value, so it is not lost. */
	private static void commitSpinner(JSpinner spinner) {
		try{
			spinner.commitEdit();
		}catch(java.text.ParseException e){
			// Invalid text: keep the last valid value.
		}
	}

	/** Copies what is shown on screen into the currently selected save item. */
	public void commit() {
		if(current == -1 || !loadedOk){
			return;
		}
		
		try{
			EquipmentSave.Item item = list.getModel().getElementAt(current);
			if(!item.valid){
				return;
			}
			
			Equipment.Item item2 = (Equipment.Item)type.getSelectedItem();
			if(item2 == null || item2 == Equipment.invalid){
				return;
			}
			
			commitSpinner(attack);
			commitSpinner(defence);
			commitSpinner(speed);
			commitSpinner(level);
			
			item.attack = ((Number)attack.getValue()).shortValue();
			item.defence = ((Number)defence.getValue()).shortValue();
			item.speed = ((Number)speed.getValue()).shortValue();
			
			item.category = (byte) item2.type;
			item.id = (byte) item2.id;
			
			int rank = selectedRank();
			item.range = (byte) rank;
			
			Equipment.Bonus b = (Equipment.Bonus)bonus.getSelectedItem();
			if(b == null || b.id == -1){
				item.bonus = -1;
				item.unknown0 = -1;
				item.unknown1 = 0;
				int lv = levelForRank(rank);
				// An unusual rank (only possible from older edits) keeps the level it had.
				item.level = (byte) (lv != -1 ? lv : Math.max(0, Math.min(2, ((Number)level.getValue()).intValue())));
			}else{
				item.bonus = (byte) b.id;
				item.unknown0 = (short) (((b.id & 0xFF) << 8) | 2);
				item.unknown1 = -1;
				item.level = -1;
			}
			
			item.valid = (item.category != -1);
			list.repaint();
		}catch(Throwable t){
			t.printStackTrace();
		}
	}

	private void refresh(int index) {
		commit();
		
		current = index;
		loadedOk = false;
		
		try{
			EquipmentSave.Item item = list.getSelectedValue();
			
			boolean editable = item != null && item.valid && item.id >= 0 && Equipment.equipment[item.id] != null;
			
			attack.setEnabled(editable);
			defence.setEnabled(editable);
			speed.setEnabled(editable);
			type.setEnabled(editable);
			range.setEnabled(editable);
			bonus.setEnabled(editable);
			level.setEnabled(false);
			
			if(!editable){
				attack.setValue(0);
				defence.setValue(0);
				speed.setValue(0);
				
				type.setSelectedItem(Equipment.invalid);
				
				showRank(RANKS[2]);
				bonus.setSelectedItem(Equipment.noBounus);
				level.setValue(0);
				
				icon.setIcon(Equipment.other);
				return;
			}
			
			attack.setValue((int) item.attack);
			defence.setValue((int) item.defence);
			speed.setValue((int) item.speed);
			
			type.setSelectedItem(Equipment.equipment[item.id]);
			
			showRank(item.range);
			if(item.bonus == -1 || Equipment.bonus[item.bonus & 0xFF] == null){
				bonus.setSelectedItem(Equipment.noBounus);
				level.setValue(Math.max(0, Math.min(2, (int) item.level)));
				updateLevel();
			}else{
				bonus.setSelectedItem(Equipment.bonus[item.bonus & 0xFF]);
				level.setValue(2);
			}
			
			icon.setIcon(Equipment.equipment[item.id].icon);
			
			loadedOk = true;
		}catch(Throwable t){
			t.printStackTrace();
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Equipment.Bonus bonus = (Equipment.Bonus)this.bonus.getSelectedItem();
		
		if(bonus.id == -1){
			effect.setText("None");
		}else{
			effect.setText(bonus.effect);
		}
		updateLevel();
	}
}
