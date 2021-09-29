package jmri.jmrix.dccpp.swing;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import jmri.jmrix.dccpp.DCCppListener;
import jmri.jmrix.dccpp.DCCppMessage;
import jmri.jmrix.dccpp.DCCppReply;
import jmri.jmrix.dccpp.DCCppSensorManager;
import jmri.jmrix.dccpp.DCCppTrafficController;
import jmri.jmrix.dccpp.DCCppTurnoutManager;
import jmri.util.JmriJFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * <hr>
 * This file is part of JMRI.
 * <p>
 * JMRI is free software; you can redistribute it and/or modify it under
 * the terms of version 2 of the GNU General Public License as published
 * by the Free Software Foundation. See the "COPYING" file for a copy
 * of this license.
 * <p>
 * JMRI is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * @author   Mark Underwood Copyright (C) 2011
 */
public class ConfigBaseStationFrame extends JmriJFrame implements DCCppListener {

    // Map of Mnemonic KeyEvent values to GUI Components
    private static final Map<String, Integer> Mnemonics = new HashMap<>();

    static {
        Mnemonics.put("SensorTab", KeyEvent.VK_E); // NOI18N
        Mnemonics.put("DCCTurnoutTab", KeyEvent.VK_T); // NOI18N
        Mnemonics.put("ServoTurnoutTab", KeyEvent.VK_R); // NOI18N
        Mnemonics.put("VpinTurnoutTab", KeyEvent.VK_V); // NOI18N
        Mnemonics.put("OutputTab", KeyEvent.VK_O); // NOI18N
        Mnemonics.put("ButtonAdd", KeyEvent.VK_A); // NOI18N
        Mnemonics.put("CloseButton", KeyEvent.VK_X); // NOI18N
        Mnemonics.put("SaveButton", KeyEvent.VK_S); // NOI18N
    }

    protected EventListenerList listenerList = new javax.swing.event.EventListenerList();

    private final DCCppTrafficController tc;

    private JTabbedPane tabbedPane;
    private JPanel sensorPanel;

    private SensorTableModel sensorModel;
    private DccTurnoutTableModel dccTurnoutModel;
    private ServoTurnoutTableModel servoTurnoutModel;
    private VpinTurnoutTableModel  vpinTurnoutModel;
    private OutputTableModel outputModel;
    private JTable sensorTable;
    private JTable dccTurnoutTable;
    private JTable servoTurnoutTable;
    private JTable vpinTurnoutTable;
    private JTable outputTable;
    private TableRowSorter<TableModel> sensorSorter;
    private TableRowSorter<TableModel> dccTurnoutSorter;
    private TableRowSorter<TableModel> servoTurnoutSorter;
    private TableRowSorter<TableModel> vpinTurnoutSorter;
    private TableRowSorter<TableModel> outputSorter;

    private List<JMenu> menuList;

    private enum CurrentTab {
        SENSOR, DCCTURNOUT, SERVOTURNOUT, VPINTURNOUT, OUTPUT
    }
    private CurrentTab cTab;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "2D array of different types passed as complex parameter. "
            + "Better to switch to passing use-specific objects rather than "
            + "papering this over with a deep copy of the arguments. "
            + "In any case, there's no risk of exposure here.")
    public ConfigBaseStationFrame(DCCppSensorManager sm,
            DCCppTurnoutManager tm,
            DCCppTrafficController t) {
        super(false, false);
        tc = t;
        initGui();
    }

    private void initGui() {

        // NOTE: Look at jmri.jmrit.vsdecoder.swing.ManageLocationsFrame
        // for how to add a tab for turnouts and other things.
        this.setTitle(Bundle.getMessage("FieldManageBaseStationFrameTitle"));
        this.buildMenu();

        // Panel for managing sensors
        sensorPanel = new JPanel();
        sensorPanel.setLayout(new GridBagLayout());

        JButton addButton = new JButton(Bundle.getMessage("ButtonAddX", Bundle.getMessage("BeanNameSensor")));
        addButton.setToolTipText(Bundle.getMessage("ToolTipButtonMSFAdd"));
        addButton.setMnemonic(Mnemonics.get("ButtonAdd")); // NOI18N
        addButton.addActionListener((ActionEvent e) -> {
            addButtonPressed(e);
        });

        JButton closeButton = new JButton(Bundle.getMessage("ButtonClose"));
        closeButton.setToolTipText(Bundle.getMessage("ToolTipButtonClose"));
        closeButton.setMnemonic(Mnemonics.get("CloseButton")); // NOI18N
        closeButton.addActionListener((ActionEvent e) -> {
            closeButtonPressed(e);
        });
        JButton saveButton = new JButton(Bundle.getMessage("ButtonSaveX", Bundle.getMessage("Sensors")));
        saveButton.setToolTipText(Bundle.getMessage("ToolTipButtonMSFSave"));
        saveButton.setMnemonic(Mnemonics.get("SaveButton")); // NOI18N
        saveButton.addActionListener((ActionEvent e) -> {
            saveButtonPressed(e);
        });

        //SENSOR TAB ---------------------
        JScrollPane sensorScrollPanel = new JScrollPane();
        sensorModel = new SensorTableModel();
        sensorTable = new JTable(sensorModel);
        sensorTable.setFillsViewportHeight(true);
        sensorScrollPanel.getViewport().add(sensorTable);
        sensorTable.setPreferredScrollableViewportSize(new Dimension(520, 200));
        sensorTable.getColumn(Bundle.getMessage("ColumnDelete")).setCellRenderer(new ButtonRenderer());
        sensorTable.removeColumn(sensorTable.getColumn("isNew"));
        sensorTable.removeColumn(sensorTable.getColumn("isDirty"));
        sensorTable.removeColumn(sensorTable.getColumn("isDelete"));
        sensorTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                handleTableMouseClick(sensorTable, evt);
            }
        });
        sensorTable.setAutoCreateRowSorter(true);
        sensorSorter = new TableRowSorter<>(sensorTable.getModel());
        sensorTable.setRowSorter(sensorSorter);
        List<RowSorter.SortKey> sensorSortKeys = new ArrayList<>();
        sensorSortKeys.add(new RowSorter.SortKey(sensorTable.getColumn(Bundle.getMessage("FieldTableIndexColumn")).getModelIndex(), SortOrder.ASCENDING));
        sensorSorter.setSortKeys(sensorSortKeys);
        sensorSorter.sort();
        sensorSorter.setSortable(sensorTable.getColumn(Bundle.getMessage("ColumnDelete")).getModelIndex(), false);

        //TURNOUT TAB ---------------------
        JScrollPane dccTurnoutScrollPanel = new JScrollPane();
        dccTurnoutModel = new DccTurnoutTableModel();
        dccTurnoutTable = new JTable(dccTurnoutModel);
        dccTurnoutTable.setFillsViewportHeight(true);
        dccTurnoutScrollPanel.getViewport().add(dccTurnoutTable);
        dccTurnoutTable.setPreferredScrollableViewportSize(new Dimension(520, 200));
        dccTurnoutTable.getColumn(Bundle.getMessage("ColumnDelete")).setCellRenderer(new ButtonRenderer());
        dccTurnoutTable.removeColumn(dccTurnoutTable.getColumn("isNew"));
        dccTurnoutTable.removeColumn(dccTurnoutTable.getColumn("isDirty"));
        dccTurnoutTable.removeColumn(dccTurnoutTable.getColumn("isDelete"));
        dccTurnoutTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                handleTableMouseClick(dccTurnoutTable, evt);
            }
        });
        dccTurnoutTable.setAutoCreateRowSorter(true);
        dccTurnoutSorter = new TableRowSorter<>(dccTurnoutTable.getModel());
        dccTurnoutTable.setRowSorter(dccTurnoutSorter);
        List<RowSorter.SortKey> dccTurnoutSortKeys = new ArrayList<>();
        dccTurnoutSortKeys.add(new RowSorter.SortKey(dccTurnoutTable.getColumn(Bundle.getMessage("FieldTableIndexColumn")).getModelIndex(), SortOrder.ASCENDING));
        dccTurnoutSorter.setSortKeys(dccTurnoutSortKeys);
        dccTurnoutSorter.setSortable(dccTurnoutTable.getColumn(Bundle.getMessage("ColumnDelete")).getModelIndex(), false);
        dccTurnoutSorter.sort();

        //SERVO TURNOUT TAB ---------------------
        JScrollPane servoTurnoutScrollPanel = new JScrollPane();
        servoTurnoutModel = new ServoTurnoutTableModel();
        servoTurnoutTable = new JTable(servoTurnoutModel);
        servoTurnoutTable.setFillsViewportHeight(true);
        servoTurnoutScrollPanel.getViewport().add(servoTurnoutTable);
        servoTurnoutTable.setPreferredScrollableViewportSize(new Dimension(520, 200));
        servoTurnoutTable.getColumn(Bundle.getMessage("ColumnDelete")).setCellRenderer(new ButtonRenderer());
        servoTurnoutTable.removeColumn(servoTurnoutTable.getColumn("isNew"));
        servoTurnoutTable.removeColumn(servoTurnoutTable.getColumn("isDirty"));
        servoTurnoutTable.removeColumn(servoTurnoutTable.getColumn("isDelete"));
        servoTurnoutTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                handleTableMouseClick(servoTurnoutTable, evt);
            }
        });
        servoTurnoutTable.setAutoCreateRowSorter(true);
        servoTurnoutSorter = new TableRowSorter<>(servoTurnoutTable.getModel());
        servoTurnoutTable.setRowSorter(servoTurnoutSorter);
        List<RowSorter.SortKey> servoTurnoutSortKeys = new ArrayList<>();
        servoTurnoutSortKeys.add(new RowSorter.SortKey(servoTurnoutTable.getColumn(Bundle.getMessage("FieldTableIndexColumn")).getModelIndex(), SortOrder.ASCENDING));
        servoTurnoutSorter.setSortKeys(servoTurnoutSortKeys);
        servoTurnoutSorter.setSortable(servoTurnoutTable.getColumn(Bundle.getMessage("ColumnDelete")).getModelIndex(), false);
        servoTurnoutSorter.sort();

        //VPIN TURNOUT TAB ---------------------
        JScrollPane vpinTurnoutScrollPanel = new JScrollPane();
        vpinTurnoutModel = new  VpinTurnoutTableModel();
        vpinTurnoutTable = new JTable(vpinTurnoutModel);
        vpinTurnoutTable.setFillsViewportHeight(true);
        vpinTurnoutScrollPanel.getViewport().add(vpinTurnoutTable);
        vpinTurnoutTable.setPreferredScrollableViewportSize(new Dimension(520, 200));
        vpinTurnoutTable.getColumn(Bundle.getMessage("ColumnDelete")).setCellRenderer(new ButtonRenderer());
        vpinTurnoutTable.removeColumn(vpinTurnoutTable.getColumn("isNew"));
        vpinTurnoutTable.removeColumn(vpinTurnoutTable.getColumn("isDirty"));
        vpinTurnoutTable.removeColumn(vpinTurnoutTable.getColumn("isDelete"));
        vpinTurnoutTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                handleTableMouseClick(vpinTurnoutTable, evt);
            }
        });
        vpinTurnoutTable.setAutoCreateRowSorter(true);
        vpinTurnoutSorter = new TableRowSorter<>(vpinTurnoutTable.getModel());
        vpinTurnoutTable.setRowSorter(vpinTurnoutSorter);
        List<RowSorter.SortKey> vpinTurnoutSortKeys = new ArrayList<>();
        vpinTurnoutSortKeys.add(new RowSorter.SortKey(vpinTurnoutTable.getColumn(Bundle.getMessage("FieldTableIndexColumn")).getModelIndex(), SortOrder.ASCENDING));
        vpinTurnoutSorter.setSortKeys(vpinTurnoutSortKeys);
        vpinTurnoutSorter.setSortable(vpinTurnoutTable.getColumn(Bundle.getMessage("ColumnDelete")).getModelIndex(), false);
        vpinTurnoutSorter.sort();

        //OUTPUT TAB ---------------------
        JScrollPane outputScrollPanel = new JScrollPane();
        outputModel = new OutputTableModel();
        outputTable = new JTable(outputModel);
        outputTable.setFillsViewportHeight(true);
        outputScrollPanel.getViewport().add(outputTable);
        outputTable.setPreferredScrollableViewportSize(new Dimension(520, 200));
        outputTable.getColumn(Bundle.getMessage("ColumnDelete")).setCellRenderer(new ButtonRenderer());
        outputTable.removeColumn(outputTable.getColumn("isNew"));
        outputTable.removeColumn(outputTable.getColumn("isDirty"));
        outputTable.removeColumn(outputTable.getColumn("isDelete"));
        outputTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                handleTableMouseClick(outputTable, evt);
            }
        });
        outputTable.setAutoCreateRowSorter(true);
        outputSorter = new TableRowSorter<>(outputTable.getModel());
        outputTable.setRowSorter(outputSorter);
        List<RowSorter.SortKey> outputSortKeys = new ArrayList<>();
        outputSortKeys.add(new RowSorter.SortKey(sensorTable.getColumn(Bundle.getMessage("FieldTableIndexColumn")).getModelIndex(), SortOrder.ASCENDING));
        outputSorter.setSortKeys(outputSortKeys);
        outputSorter.setSortable(sensorTable.getColumn(Bundle.getMessage("ColumnDelete")).getModelIndex(), false);
        outputSorter.sort();

        // add the 5 tabs to the window
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(Bundle.getMessage("Sensors"), sensorScrollPanel);
        tabbedPane.setToolTipTextAt(0, Bundle.getMessage("ToolTipSensorTab"));
        tabbedPane.setMnemonicAt(0, Mnemonics.get("SensorTab")); // NOI18N
        tabbedPane.addTab(Bundle.getMessage("DCCTurnouts"), dccTurnoutScrollPanel);
        tabbedPane.setToolTipTextAt(1, Bundle.getMessage("ToolTipDccTurnoutTab"));
        tabbedPane.setMnemonicAt(1, Mnemonics.get("DCCTurnoutTab")); // NOI18N
        tabbedPane.addTab(Bundle.getMessage("ServoTurnouts"), servoTurnoutScrollPanel);
        tabbedPane.setToolTipTextAt(2, Bundle.getMessage("ToolTipServoTurnoutTab"));
        tabbedPane.setMnemonicAt(2, Mnemonics.get("ServoTurnoutTab")); // NOI18N
        tabbedPane.addTab(Bundle.getMessage("VpinTurnouts"), vpinTurnoutScrollPanel);
        tabbedPane.setToolTipTextAt(3, Bundle.getMessage("ToolTipVpinTurnoutTab"));
        tabbedPane.setMnemonicAt(3, Mnemonics.get("VpinTurnoutTab")); // NOI18N
        tabbedPane.addTab(Bundle.getMessage("FieldOutputsTabTitle"), outputScrollPanel);
        tabbedPane.setToolTipTextAt(4, Bundle.getMessage("ToolTipOutputTab"));
        tabbedPane.setMnemonicAt(4, Mnemonics.get("OutputTab")); // NOI18N
        cTab = CurrentTab.SENSOR;
        tabbedPane.setSelectedIndex(0);
        tabbedPane.addChangeListener((ChangeEvent e) -> {
            switch (tabbedPane.getSelectedIndex()) { // set button text and tooltips for selected tabs
                case 4:
                    cTab = CurrentTab.OUTPUT;
                    addButton.setText(Bundle.getMessage("ButtonAddX", Bundle.getMessage("Output")));
                    addButton.setToolTipText(Bundle.getMessage("ToolTipButtonMOFAdd"));
                    saveButton.setText(Bundle.getMessage("ButtonSaveX", Bundle.getMessage("FieldOutputsTabTitle")));
                    saveButton.setToolTipText(Bundle.getMessage("ToolTipButtonMOFSave"));
                    log.debug("Current Tab is: {}", tabbedPane.getSelectedIndex());
                    break;
                case 3:
                    cTab = CurrentTab.VPINTURNOUT;
                    addButton.setText(Bundle.getMessage("ButtonAddX", Bundle.getMessage("VpinTurnouts")));
                    addButton.setToolTipText(Bundle.getMessage("ToolTipButtonMTFAdd"));
                    saveButton.setText(Bundle.getMessage("ButtonSaveX", Bundle.getMessage("VpinTurnouts")));
                    saveButton.setToolTipText(Bundle.getMessage("ToolTipButtonMTFSave"));
                    log.debug("Current Tab is: {}", tabbedPane.getSelectedIndex());
                    break;
                case 2:
                    cTab = CurrentTab.SERVOTURNOUT;
                    addButton.setText(Bundle.getMessage("ButtonAddX", Bundle.getMessage("ServoTurnouts")));
                    addButton.setToolTipText(Bundle.getMessage("ToolTipButtonMTFAdd"));
                    saveButton.setText(Bundle.getMessage("ButtonSaveX", Bundle.getMessage("ServoTurnouts")));
                    saveButton.setToolTipText(Bundle.getMessage("ToolTipButtonMTFSave"));
                    log.debug("Current Tab is: {}", tabbedPane.getSelectedIndex());
                    break;
                case 1:
                    cTab = CurrentTab.DCCTURNOUT;
                    addButton.setText(Bundle.getMessage("ButtonAddX", Bundle.getMessage("DCCTurnouts")));
                    addButton.setToolTipText(Bundle.getMessage("ToolTipButtonMTFAdd"));
                    saveButton.setText(Bundle.getMessage("ButtonSaveX", Bundle.getMessage("DCCTurnouts")));
                    saveButton.setToolTipText(Bundle.getMessage("ToolTipButtonMTFSave"));
                    log.debug("Current Tab is: {}", tabbedPane.getSelectedIndex());
                    break;
                case 0:
                default:
                    cTab = CurrentTab.SENSOR;
                    addButton.setText(Bundle.getMessage("ButtonAddX", Bundle.getMessage("BeanNameSensor")));
                    addButton.setToolTipText(Bundle.getMessage("ToolTipButtonMSFAdd"));
                    saveButton.setText(Bundle.getMessage("ButtonSaveX", Bundle.getMessage("Sensors")));                    
                    saveButton.setToolTipText(Bundle.getMessage("ToolTipButtonMSFSave"));
                    log.debug("Current Tab is: {}", tabbedPane.getSelectedIndex());
            }
        });

        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel buttonPane2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        buttonPane.add(addButton);
        buttonPane.add(saveButton);
        buttonPane2.add(closeButton);

        this.getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        this.getContentPane().add(tabbedPane);
        this.getContentPane().add(buttonPane);
        this.getContentPane().add(buttonPane2);
        this.pack();
        this.setVisible(true);
    }

    private void buildMenu() {
        JMenu fileMenu = new JMenu(Bundle.getMessage("MenuFile"));

        JMenu editMenu = new JMenu(Bundle.getMessage("MenuEdit"));
        menuList = new ArrayList<>(3);

        menuList.add(fileMenu);
        menuList.add(editMenu);

        this.setJMenuBar(new JMenuBar());
        this.getJMenuBar().add(fileMenu);
        this.getJMenuBar().add(editMenu);
        //this.addHelpMenu("package.jmri.jmrit.vsdecoder.swing.ManageLocationsFrame", true); // NOI18N
    }

    // DCCppListener Methods
    @Override
    public void message(DCCppReply r) {
        // When we get a SensorDefReply message, add the
        // sensor information to the data map for the model.
        if (r.isSensorDefReply()) {
            List<Object> v = new ArrayList<>();
            v.add(r.getSensorDefNumInt());
            v.add(r.getSensorDefPinInt());
            v.add(r.getSensorDefPullupBool());
            sensorModel.insertData(v, false);
            sensorSorter.sort();
        } else if (r.isTurnoutDefReply() || r.isTurnoutDefDCCReply()) {
            List<Object> v = new ArrayList<>();
            v.add(r.getTOIDInt());
            v.add(r.getTurnoutDefAddrInt());
            v.add(r.getTurnoutDefSubAddrInt());
            dccTurnoutModel.insertData(v, false);
            dccTurnoutSorter.sort();
        } else if (r.isTurnoutDefServoReply()) {
            List<Object> v = new ArrayList<>();
            v.add(r.getTOIDInt());
            v.add(r.getTOPinInt());
            v.add(r.getTOThrownPositionInt());
            v.add(r.getTOClosedPositionInt());
            v.add(r.getTOProfileInt());
            servoTurnoutModel.insertData(v, false);
            servoTurnoutSorter.sort();
        } else if (r.isTurnoutDefVpinReply()) {
            List<Object> v = new ArrayList<>();
            v.add(r.getTOIDInt());
            v.add(r.getTOPinInt());
            vpinTurnoutModel.insertData(v, false);
            vpinTurnoutSorter.sort();
        } else if (r.isOutputDefReply()) {
            List<Object> v = new ArrayList<>();
            v.add(r.getOutputNumInt());
            v.add(r.getOutputListPinInt());
            v.add((r.getOutputListIFlagInt() & 0x01) == 1); // (bool) Invert
            v.add((r.getOutputListIFlagInt() & 0x02) == 2); // (bool) Restore State
            v.add((r.getOutputListIFlagInt() & 0x04) == 4); // (bool) Force High
            outputModel.insertData(v, false);
            outputSorter.sort();
        }
    }

    @Override
    public void message(DCCppMessage m) {
        // Do nothing
    }

    @Override
    public void notifyTimeout(DCCppMessage m) {
        // Do nothing
    }

    /**
     * Handle mouse clicks within a table.
     * <p>
     * This is currently the workings behind the "Delete" button in the table.
     *
     * @param table the table where the event occurred
     * @param evt   the mouse click
     */
    private void handleTableMouseClick(JTable table, java.awt.event.MouseEvent evt) {
        int row = table.rowAtPoint(evt.getPoint());
        int col = table.columnAtPoint(evt.getPoint());
        if (row < 0 || col < 0) {
            return;
        }
        DCCppTableModel model = (DCCppTableModel) table.getModel();
        if (col == table.convertColumnIndexToView(model.getDeleteColumn())) {
            // This is a row delete action.  Handle it as such.
            int sel = table.convertRowIndexToModel(row);
            int idx = (int) model.getValueAt(sel, 0);
            log.debug("idx = {}", sel);
            int value = JOptionPane.showConfirmDialog(null, Bundle.getMessage("DeleteWarningMessage", Integer.toString(idx)),
                    Bundle.getMessage("WarningTitle"),
                    JOptionPane.OK_CANCEL_OPTION);
            if (value == JOptionPane.OK_OPTION) {
               if (null != cTab) {
                    switch (cTab) {
                        case SENSOR:
                            tc.sendDCCppMessage(DCCppMessage.makeSensorDeleteMsg(idx), this);
                            sensorModel.removeRow(sel);
                            log.debug("Delete sensor {}", idx);
                            break;
                        case DCCTURNOUT:
                            DCCppMessage m = new DCCppMessage("T " + Integer.toString(idx));
                            tc.sendDCCppMessage(m, this);
                            log.debug("Sending: {}", m);
                            dccTurnoutModel.removeRow(sel);
                            break;
                        case SERVOTURNOUT:
                            m = new DCCppMessage("T " + Integer.toString(idx));
                            tc.sendDCCppMessage(m, this);
                            log.debug("Sending: {}", m);
                            servoTurnoutModel.removeRow(sel);
                            break;
                        case VPINTURNOUT:
                            m = new DCCppMessage("T " + Integer.toString(idx));
                            tc.sendDCCppMessage(m, this);
                            log.debug("Sending: {}", m);
                            vpinTurnoutModel.removeRow(sel);
                            break;
                        case OUTPUT:
                            tc.sendDCCppMessage(DCCppMessage.makeOutputDeleteMsg(idx), this);
                            outputModel.removeRow(sel);
                            break;
                        default:
                            jmri.util.LoggingUtil.warnOnce(log, "Unexpected cTab value = {}", cTab);
                            break;
                   }
                }
            }

        }
    }

    /**
     * Responder for pressing the "Add" button. Response depends on which tab is
     * active.
     *
     * @param e the press event
     */
    private void addButtonPressed(ActionEvent e) {
        if (null != cTab) {
            switch (cTab) {
                case SENSOR: {
                    List<Object> v = new ArrayList<>();
                    v.add(0);     // Index
                    v.add(0);     // Pin
                    v.add(false); // Pullup
                    sensorModel.insertData(v, true);
                    break;
                }
                case DCCTURNOUT: {
                    List<Object> v = new ArrayList<>();
                    v.add(0); // Index
                    v.add(0); // Address
                    v.add(0); // Subaddress
                    dccTurnoutModel.insertData(v, true);
                    break;
                }
                case SERVOTURNOUT: {
                    List<Object> v = new ArrayList<>();
                    v.add(0); // Index
                    v.add(0); // Address
                    v.add(0); // Subaddress
                    servoTurnoutModel.insertData(v, true);
                    break;
                }
                case VPINTURNOUT: {
                    List<Object> v = new ArrayList<>();
                    v.add(0); // Index
                    v.add(0); // Pin
                    vpinTurnoutModel.insertData(v, true);
                    break;
                }
                case OUTPUT: {
                    List<Object> v = new ArrayList<>();
                    v.add(0); // Index
                    v.add(0); // Pin
                    v.add(false); // Invert
                    v.add(false); // Restore state
                    v.add(false); // Force high/low
                    outputModel.insertData(v, true);
                    break;
                }
                default:
                    break;
            }
        }
    }

    /**
     * Respond to the user pressing the "Save Sensors/Turnouts/Outputs" button.
     *
     * @param e the button press event
     */
    private void saveButtonPressed(ActionEvent e) {
        int value = JOptionPane.showConfirmDialog(null, Bundle.getMessage("FieldMCFSaveDialogConfirmMessage"),
                Bundle.getMessage("ConfirmSaveDialogTitle"),
                JOptionPane.YES_NO_OPTION);
        if (sensorTable.getCellEditor() != null) {
            sensorTable.getCellEditor().stopCellEditing();
        }
        if (dccTurnoutTable.getCellEditor() != null) {
            dccTurnoutTable.getCellEditor().stopCellEditing();
        }
        if (servoTurnoutTable.getCellEditor() != null) {
            servoTurnoutTable.getCellEditor().stopCellEditing();
        }
        if (vpinTurnoutTable.getCellEditor() != null) {
            vpinTurnoutTable.getCellEditor().stopCellEditing();
        }
        if (outputTable.getCellEditor() != null) {
            outputTable.getCellEditor().stopCellEditing();
        }
        if (value == JOptionPane.YES_OPTION) {
            saveTableValues();
        }
    }

    /**
     * Save the values for the currently selected tab.
     */
    private void saveTableValues() {
        if (null != cTab) {
            switch (cTab) {
                case SENSOR:
                    for (int i = 0; i < sensorModel.getRowData().size(); i++) {

                        List<Object> r = sensorModel.getRowData().get(i);
                        boolean isnew = (boolean) r.get(4);
                        boolean isdirty = (boolean) r.get(5);
                        boolean isdelete = (boolean) r.get(6);
                        int row = sensorModel.getRowData().indexOf(r);
                        if (isnew) {
                            tc.sendDCCppMessage(DCCppMessage.makeSensorAddMsg((int) r.get(0),
                                    (int) r.get(1),
                                    ((boolean) r.get(2) ? 1 : 0)), this);
                            sensorModel.setNewRow(row, false);
                        } else if (isdelete) {
                            tc.sendDCCppMessage(DCCppMessage.makeSensorDeleteMsg((int) r.get(0)), this);
                            sensorModel.getRowData().remove(r);
                        } else if (isdirty) {
                            // Send a Delete, then an Add (for now).
                            tc.sendDCCppMessage(DCCppMessage.makeSensorDeleteMsg((int) r.get(0)), this);
                            // WARNING: Conversions here are brittle. Be careful.
                            tc.sendDCCppMessage(DCCppMessage.makeSensorAddMsg((int) r.get(0),
                                    (int) r.get(1),
                                    ((boolean) r.get(2) ? 1 : 0)), this);
                            sensorModel.setNewRow(row, false);
                            sensorModel.setDirtyRow(row, false);
                        }
                    }
                    break;
                case DCCTURNOUT:
                    for (int i = 0; i < dccTurnoutModel.getRowData().size(); i++) {

                        List<Object> r = dccTurnoutModel.getRowData().get(i);
                        boolean isnew = (boolean) r.get(4);
                        boolean isdirty = (boolean) r.get(5);
                        boolean isdelete = (boolean) r.get(6);
                        int row = dccTurnoutModel.getRowData().indexOf(r);
                        if (isnew) {
                            // WARNING: Conversions here are brittle. Be careful.
                            tc.sendDCCppMessage(DCCppMessage.makeTurnoutAddMsg((int) r.get(0),
                                    (int) r.get(1), (int) r.get(2)), this);
                            dccTurnoutModel.setNewRow(row, false);
                        } else if (isdelete) {
                            DCCppMessage m = new DCCppMessage("T " + Integer.toString((int) r.get(0)));
                            tc.sendDCCppMessage(m, this);
                            log.debug("Sending: {}", m);
                            dccTurnoutModel.getRowData().remove(r);
                        } else if (isdirty) {
                            tc.sendDCCppMessage(DCCppMessage.makeTurnoutDeleteMsg((int) r.get(0)), this);
                            // Send a Delete, then an Add (for now).
                            // WARNING: Conversions here are brittle. Be careful.
                            tc.sendDCCppMessage(DCCppMessage.makeTurnoutAddMsg((int) r.get(0),
                                    (int) r.get(1), (int) r.get(2)), this);
                            dccTurnoutModel.setNewRow(row, false);
                            dccTurnoutModel.setDirtyRow(row, false);
                        }
                    }
                    break;
                case SERVOTURNOUT:
                    for (int i = 0; i < servoTurnoutModel.getRowData().size(); i++) {

                        List<Object> r = servoTurnoutModel.getRowData().get(i);
                        boolean isnew = (boolean) r.get(4);
                        boolean isdirty = (boolean) r.get(5);
                        boolean isdelete = (boolean) r.get(6);
                        int row = servoTurnoutModel.getRowData().indexOf(r);
                        if (isnew) {
                            // WARNING: Conversions here are brittle. Be careful.
                            tc.sendDCCppMessage(DCCppMessage.makeTurnoutAddMsg((int) r.get(0),
                                    (int) r.get(1), (int) r.get(2)), this);
                            servoTurnoutModel.setNewRow(row, false);
                        } else if (isdelete) {
                            DCCppMessage m = new DCCppMessage("T " + Integer.toString((int) r.get(0)));
                            tc.sendDCCppMessage(m, this);
                            log.debug("Sending: {}", m);
                            servoTurnoutModel.getRowData().remove(r);
                        } else if (isdirty) {
                            tc.sendDCCppMessage(DCCppMessage.makeTurnoutDeleteMsg((int) r.get(0)), this);
                            // Send a Delete, then an Add (for now).
                            // WARNING: Conversions here are brittle. Be careful.
                            tc.sendDCCppMessage(DCCppMessage.makeTurnoutAddMsg((int) r.get(0),
                                    (int) r.get(1), (int) r.get(2)), this);
                            servoTurnoutModel.setNewRow(row, false);
                            servoTurnoutModel.setDirtyRow(row, false);
                        }
                    }
                    break;
                case VPINTURNOUT:
                    for (int i = 0; i < vpinTurnoutModel.getRowData().size(); i++) {

                        List<Object> r = vpinTurnoutModel.getRowData().get(i);
                        boolean isnew = (boolean) r.get(3);
                        boolean isdirty = (boolean) r.get(4);
                        boolean isdelete = (boolean) r.get(5);
                        int row = vpinTurnoutModel.getRowData().indexOf(r);
                        if (isnew) {
                            // WARNING: Conversions here are brittle. Be careful.
                            DCCppMessage m = new DCCppMessage("T "+(int)r.get(0)+" VPIN "+(int)r.get(1));
                            log.debug("Sending: {}", m);
                            tc.sendDCCppMessage(m, this);
                            vpinTurnoutModel.setNewRow(row, false);
                        } else if (isdelete) {
                            DCCppMessage m = new DCCppMessage("T " + (int)r.get(0));
                            log.debug("Sending: {}", m);
                            tc.sendDCCppMessage(m, this);
                            vpinTurnoutModel.getRowData().remove(r);
                        } else if (isdirty) {
                            // Send a Delete, then an Add (for now).
                            DCCppMessage m = new DCCppMessage("T " + (int)r.get(0));
                            log.debug("Sending: {}", m);
                            tc.sendDCCppMessage(m, this);
                            vpinTurnoutModel.setNewRow(row, false);
                            vpinTurnoutModel.setDirtyRow(row, false);
                        }
                    }
                    break;
                case OUTPUT:
                    for (int i = 0; i < outputModel.getRowData().size(); i++) {

                        List<Object> r = outputModel.getRowData().get(i);
                        boolean isnew = (boolean) r.get(6);
                        boolean isdirty = (boolean) r.get(7);
                        boolean isdelete = (boolean) r.get(8);
                        int row = outputModel.getRowData().indexOf(r);
                        if (isnew) {
                            // WARNING: Conversions here are brittle. Be careful.
                            int f = ((boolean) r.get(2) ? 1 : 0); // Invert
                            f += ((boolean) r.get(3) ? 2 : 0); // Restore
                            f += ((boolean) r.get(4) ? 4 : 0); // Force
                            tc.sendDCCppMessage(DCCppMessage.makeOutputAddMsg((int) r.get(0),
                                    (int) r.get(1), f), this);
                            outputModel.setNewRow(row, false);
                        } else if (isdelete) {
                            tc.sendDCCppMessage(DCCppMessage.makeOutputDeleteMsg((int) r.get(0)), this);
                            outputModel.getRowData().remove(r);
                        } else if (isdirty) {
                            // Send a Delete, then an Add (for now).
                            tc.sendDCCppMessage(DCCppMessage.makeOutputDeleteMsg((int) r.get(0)), this);
                            int f = ((boolean) r.get(2) ? 1 : 0); // Invert
                            f += ((boolean) r.get(3) ? 2 : 0); // Restore
                            f += ((boolean) r.get(4) ? 4 : 0); // Force
                            tc.sendDCCppMessage(DCCppMessage.makeOutputAddMsg((int) r.get(0),
                                    (int) r.get(1), f), this);
                            outputModel.setNewRow(row, false);
                            outputModel.setDirtyRow(row, false);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        // Offer to write the changes to EEPROM
        int value = JOptionPane.showConfirmDialog(null, Bundle.getMessage("FieldMCFCloseDialogConfirmMessage"),
                Bundle.getMessage("FieldMCFCloseDialogTitle"),
                JOptionPane.YES_NO_OPTION);

        if (value == JOptionPane.YES_OPTION) {
            tc.sendDCCppMessage(new DCCppMessage("E"), this);
            log.debug("Sending: <E> (Write To EEPROM)");
            // These might not actually be necessary... TODO: check this
            sensorModel.fireTableDataChanged();
            dccTurnoutModel.fireTableDataChanged();
            servoTurnoutModel.fireTableDataChanged();
            vpinTurnoutModel.fireTableDataChanged();
            outputModel.fireTableDataChanged();
        }
    }

    /**
     * Respond to the user pressing the "Close" button.
     *
     * @param e the button press event
     */
    private void closeButtonPressed(ActionEvent e) {
        // If clicked while editing, stop the cell editor(s)
        if (sensorTable.getCellEditor() != null) {
            sensorTable.getCellEditor().stopCellEditing();
        }
        if (dccTurnoutTable.getCellEditor() != null) {
            dccTurnoutTable.getCellEditor().stopCellEditing();
        }
        if (servoTurnoutTable.getCellEditor() != null) {
            servoTurnoutTable.getCellEditor().stopCellEditing();
        }
        if (vpinTurnoutTable.getCellEditor() != null) {
            vpinTurnoutTable.getCellEditor().stopCellEditing();
        }
        if (outputTable.getCellEditor() != null) {
            outputTable.getCellEditor().stopCellEditing();
        }

        // If clicked while changes not saved to BaseStation, offer
        // the option of saving.
        if (sensorModel.isDirty() || dccTurnoutModel.isDirty() || servoTurnoutModel.isDirty() 
                || vpinTurnoutModel.isDirty() || outputModel.isDirty()) {
            int value = JOptionPane.showConfirmDialog(null, Bundle.getMessage("FieldMCFSaveDialogConfirmMessage"),
                    Bundle.getMessage("ConfirmSaveDialogTitle"),
                    JOptionPane.YES_NO_OPTION);
            if (value == JOptionPane.YES_OPTION) {
                saveTableValues();
            }

            // Offer to write the changes to EEPROM
            value = JOptionPane.showConfirmDialog(null, Bundle.getMessage("FieldMCFCloseDialogConfirmMessage"),
                    Bundle.getMessage("FieldMCFCloseDialogTitle"),
                    JOptionPane.YES_NO_OPTION);

            if (value == JOptionPane.YES_OPTION) {
                tc.sendDCCppMessage(new DCCppMessage("E"), this);
                log.debug("Sending: <E> (Write To EEPROM)");
                // These might not actually be necessary... TODO: check this
                sensorModel.fireTableDataChanged();
                dccTurnoutModel.fireTableDataChanged();
                servoTurnoutModel.fireTableDataChanged();
                vpinTurnoutModel.fireTableDataChanged();
                outputModel.fireTableDataChanged();
            }

        } else {
            JOptionPane.showMessageDialog(null, Bundle.getMessage("FieldMCFCloseNoChangesDialog"));
        }

        // Close the window
        dispose();
    }

    private final static Logger log = LoggerFactory.getLogger(ConfigBaseStationFrame.class);

    /**
     * Private class to serve as TableModel for Sensors.
     */
    private static class SensorTableModel extends DCCppTableModel {

        public SensorTableModel() {
            super(4, 5, 6, 7);
            // Use i18n-ized column titles.
            columnNames = new String[7];
            columnNames[0] = Bundle.getMessage("FieldTableIndexColumn");
            columnNames[1] = Bundle.getMessage("FieldTablePinColumn");
            columnNames[2] = Bundle.getMessage("FieldTablePullupColumn");
            columnNames[3] = Bundle.getMessage("ColumnDelete");
            columnNames[4] = "isNew";       // hidden column // NOI18N
            columnNames[5] = "isDirty";     // hidden column // NOI18N
            columnNames[6] = "isDelete";    // hidden column // NOI18N
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                case 1:
                    return Integer.class;
                case 2:
                    return Boolean.class;
                case 3:
                    return ButtonEditor.class;
                case 4:
                case 5:
                case 6:
                    return Boolean.class;
                default:
                    return super.getColumnClass(columnIndex);
            }
        }
    }

    /**
     * Private class to serve as TableModel for DCC Turnouts
     */
    private static class DccTurnoutTableModel extends DCCppTableModel {

        public DccTurnoutTableModel() {
            super(4, 5, 6, 7);
            // Use i18n-ized column titles.
            columnNames = new String[7];
            columnNames[0] = Bundle.getMessage("FieldTableIndexColumn");
            columnNames[1] = Bundle.getMessage("AddressCol");
            columnNames[2] = Bundle.getMessage("FieldTableSubaddrColumn");
            columnNames[3] = Bundle.getMessage("ColumnDelete");
            columnNames[4] = "isNew";        // hidden column // NOI18N
            columnNames[5] = "isDirty";      // hidden column // NOI18N
            columnNames[6] = "isDelete";     // hidden column // NOI18N
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                case 1:
                case 2:
                    return Integer.class;
                case 3:
                    return ConfigBaseStationFrame.ButtonEditor.class;
                case 4:
                case 5:
                case 6:
                    return Boolean.class;
                default:
                    return super.getColumnClass(columnIndex);
            }
        }
    }

    /**
     * Private class to serve as TableModel for Servo Turnouts
     */
    private static class ServoTurnoutTableModel extends DCCppTableModel {

        public ServoTurnoutTableModel() {
            super(4, 5, 6, 7);
            // Use i18n-ized column titles.
            columnNames = new String[7];
            columnNames[0] = Bundle.getMessage("FieldTableIndexColumn");
            columnNames[1] = Bundle.getMessage("AddressCol");
            columnNames[2] = Bundle.getMessage("FieldTableSubaddrColumn");
            columnNames[3] = Bundle.getMessage("ColumnDelete");
            columnNames[4] = "isNew";        // hidden column // NOI18N
            columnNames[5] = "isDirty";      // hidden column // NOI18N
            columnNames[6] = "isDelete";     // hidden column // NOI18N
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                case 1:
                case 2:
                    return Integer.class;
                case 3:
                    return ConfigBaseStationFrame.ButtonEditor.class;
                case 4:
                case 5:
                case 6:
                    return Boolean.class;
                default:
                    return super.getColumnClass(columnIndex);
            }
        }
    }

    /**
     * Private class to serve as TableModel for Vpin Turnouts
     */
    private static class VpinTurnoutTableModel extends DCCppTableModel {

        public VpinTurnoutTableModel() {
            super(3, 4, 5, 6);
            // Use i18n-ized column titles.
            columnNames = new String[6];
            columnNames[0] = Bundle.getMessage("FieldTableIndexColumn");
            columnNames[1] = Bundle.getMessage("PinCol");
            columnNames[2] = Bundle.getMessage("ColumnDelete");
            columnNames[3] = "isNew";        // hidden column // NOI18N
            columnNames[4] = "isDirty";      // hidden column // NOI18N
            columnNames[5] = "isDelete";     // hidden column // NOI18N
        }

        @Override
        public int getDeleteColumn() {
            return (2);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                case 1:
                    return Integer.class;
                case 2:
                    return ConfigBaseStationFrame.ButtonEditor.class;
                case 3:
                case 4:
                case 5:
                    return Boolean.class;
                default:
                    return super.getColumnClass(columnIndex);
            }
        }
    }

    /**
     * Private class to serve as TableModel for Outputs
     */
    private static class OutputTableModel extends DCCppTableModel {

        public OutputTableModel() {
            super(6, 7, 8, 9);
            // Use i18n-ized column titles.
            columnNames = new String[9];
            columnNames[0] = Bundle.getMessage("FieldTableIndexColumn");
            columnNames[1] = Bundle.getMessage("FieldTablePinColumn");
            columnNames[2] = Bundle.getMessage("FieldTableInvertColumn");
            columnNames[3] = Bundle.getMessage("FieldTableOutputRestoreStateColumn");
            columnNames[4] = Bundle.getMessage("FieldTableOutputForceToColumn");
            columnNames[5] = Bundle.getMessage("ColumnDelete");
            columnNames[6] = "isNew";        // hidden column // NOI18N
            columnNames[7] = "isDirty";      // hidden column // NOI18N
            columnNames[8] = "isDelete";     // hidden column // NOI18N
        }

        @Override
        public int getDeleteColumn() {
            return (5);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                case 1:
                    return Integer.class;
                case 2:
                case 3:
                case 4:
                    return Boolean.class;
                case 5:
                    return ConfigBaseStationFrame.ButtonEditor.class;
                case 6:
                case 7:
                case 8:
                    return Boolean.class;
                default:
                    return super.getColumnClass(columnIndex);
            }
        }
    }

    static class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer() {
            super.setOpaque(true);
            super.setSelected(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(UIManager.getColor("Button.background"));
            }
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    /**
     * Button Editor class to replace the DefaultCellEditor in the table for the
     * delete button.
     * <p>
     * NOTE: This isn't actually used anymore except as being a unique class
     * type that can be returned from the TableModel classes for the column that
     * includes the Delete buttons.
     */
    class ButtonEditor extends DefaultCellEditor {

        protected JButton button;
        private String label;
        public ButtonEditor(JCheckBox checkBox, JTable t) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener((ActionEvent e) -> {
                //fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            if (isSelected) {
                button.setForeground(table.getSelectionForeground());
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setForeground(table.getForeground());
                button.setBackground(table.getBackground());
            }
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return label;
        }
    }

}
