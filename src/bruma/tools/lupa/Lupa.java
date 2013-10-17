/*=========================================================================

    Copyright © 2011 BIREME/PAHO/WHO

    This file is part of Bruma.

    Bruma is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as 
    published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Bruma is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public 
    License along with Bruma. If not, see <http://www.gnu.org/licenses/>.

=========================================================================*/


/*
 * Lupa.java
 *
 * @author Heitor Barbieri
 * Created on 23/06/2009, 14:42:38
 */

package bruma.tools.lupa;

import bruma.BrumaException;
import bruma.impexp.*;
import bruma.iterator.AbstractRecordIterator;
import bruma.iterator.ISO2709RecordIterator;
import bruma.iterator.IdFileRecordIterator;
import bruma.master.Field;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import bruma.tools.Statistics;
import bruma.utils.Util;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author Heitor Barbieri
 */
public class Lupa extends javax.swing.JFrame {    
    private OpenProfile openProf;
    private OpenProfile lastOpenProf;
    private FilterProfile filterProf;
    private FilterProfile lastFilterProf;
    private int mfn;
    private int lastMfn;
    private Master master;
    private Search search;

    /** Creates new form Lupa. */
    public Lupa() {
        initComponents();
        myInitComponents();
    }

    private void myInitComponents() {
        openProf = new OpenProfile();
        lastOpenProf = null;
        filterProf = new FilterProfile();
        lastFilterProf = null;
        mfn = 0;
        lastMfn = 0;
        master = null;

        jComboBoxEncoding.addItem(Master.GUESS_ISO_IBM_ENCODING);
        for (String encoding : Charset.availableCharsets().keySet()) {
            jComboBoxEncoding.addItem(encoding);
        }
        
        for (String encoding : Charset.availableCharsets().keySet()) {
            jComboBoxExportEncoding.addItem(encoding);
        }
        for (String encoding : Charset.availableCharsets().keySet()) {
            jComboBoxCharEncoding.addItem(encoding);
        }

        /*
        Document doc = jTextAreaFields.getDocument();
        doc.addDocumentListener(
            new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {}

                public void removeUpdate(DocumentEvent e) {}

                public void changedUpdate(DocumentEvent e) {
                    int size = this.getLength();
                }
            }
        );*/

        //openProf.setEncoding(Master.DEFAULT_ENCODING);
        openProf.setEncoding(Master.GUESS_ISO_IBM_ENCODING);
        ImageIcon icon = new ImageIcon(getClass().getResource("/bruma/tools/lupa/lupa1.jpg"));
        setIconImage(icon.getImage());
        //updateOpen();
        clearFrame();

        /*
         * #   try {
#       fonte = Font.createFont(Font.TRUETYPE_FONT, getClass().getClassLoader().getResourceAsStream( "patdesk/times.ttf" ) );
#     }
#     catch (IOException ex1) {
#       ex1.printStackTrace();
#     }
#     catch (FontFormatException ex1) {
#       ex1.printStackTrace();
#     }
#
#     fonte = new Font( fonte.getFontName(), fonte.getStyle(), 11 );  
         */
    }

    private void updateOpen() {
        String path = "";
        boolean create = openProf.isCreate();

        if (openProf.getMaster() == null) {
            jButtonOk.setEnabled(false);
        } else {
            try {
                path = openProf.getMaster().getCanonicalPath();
                jButtonOk.setEnabled(true);
            } catch (IOException ioe) {
            }
        }
        jLabelMstName.setText(path);
        jCheckBoxSwapped.setSelected(openProf.isSwapped());    
        jCheckBoxFFI.setSelected(openProf.isFFI());

        if (create) {
            jDialogOpenDb.setTitle("Create Master");
            jLabelGigaSize.setVisible(true);
            jComboBoxGigaSize.setVisible(true);
            jComboBoxGigaSize.setSelectedItem(
                                      Integer.toString(openProf.getGigasize()));
            jCheckBoxSwapped.setVisible(true);
            jCheckBoxFFI.setVisible(true);
            jLabelAlignment.setVisible(true);
            jComboBoxAlignment.setVisible(true);
            jComboBoxEncoding.setSelectedItem(Master.DEFAULT_ENCODING);
        } else {
            jDialogOpenDb.setTitle("Open Master");
            jLabelGigaSize.setVisible(false);
            jComboBoxGigaSize.setVisible(false);
            jCheckBoxSwapped.setVisible(false);
            jCheckBoxFFI.setVisible(false);
            jLabelAlignment.setVisible(false);
            jComboBoxAlignment.setVisible(false);
            jComboBoxEncoding.setSelectedItem(openProf.getEncoding());
        }
        jComboBoxAlignment.setSelectedItem(
                                     Integer.toString(openProf.getAlignment()));

        if (create) {
            jButtonMstOpen.setText("Create...");
            jComboBoxGigaSize.setEnabled(true);
            jDialogOpenDb.setSize(630, 300);

        } else {
            jButtonMstOpen.setText("Open...");
            jComboBoxGigaSize.setEnabled(false);
            jDialogOpenDb.setSize(630, 230);
        }
    }

    private void clearFrame() {        
        jScrollBarMfn.setMinimum(0);
        jScrollBarMfn.setMaximum(0);
        jScrollBarMfn.setValue(0);
        jLabelMfnBegin.setText("0");
        jLabelMfnEnd.setText("0");
        jLabelCurMfn.setText("0");
        jLabelStatus.setText("deleted");
        jLabelLock.setVisible(false);
        jLabelDbEncoding.setText("");
        jLabelIsisType.setText("");
        jLabelDataAlignment.setText("");
        jLabelSwapped.setText("");
        jLabelMaxSize.setText("");
        openProf = new OpenProfile();
        lastOpenProf = null;
        //openProf.setEncoding(Master.DEFAULT_ENCODING);
        openProf.setEncoding(Master.GUESS_ISO_IBM_ENCODING);
        jTextAreaDump.setText("");
        jCheckBoxShowAll.setSelected(true);
        
        this.setTitle("Lupa (" + MasterFactory.VERSION + ")");
    }

    private void updateFrame() {
        final File mfile = openProf.getMaster();

        if (mfile != null) {
            try {
                if (master != null) {
                    master.close();
                }
                MasterFactory mfac = MasterFactory
                    .getInstance(mfile.getPath())
                    .setEncoding(openProf.getEncoding())
                    .setFFI(openProf.isFFI())
                    .setInMemoryXrf(openProf.isInmemory())
                    .setXrfWriteCommit(true)
                    .setMaxGigaSize(openProf.getGigasize())
                    .setSwapped(openProf.isSwapped())
                    .setDataAlignment(openProf.getAlignment());
                if (openProf.isCreate()) {
                    final Record rec;
                    master = (Master)mfac.create();
                    rec = master.newRecord();
                    rec.addField(1, "new record");
                    master.writeRecord(rec);
                } else {
                    master = mfac.open();
                }
                this.setTitle("Lupa (" + MasterFactory.VERSION + ") ["
                                               + master.getMasterName() + "]");
                lastMfn = master.getControlRecord().getNxtmfn() - 1;
                mfn = 1;
                //jTextAreaDump.setText("");
                jScrollBarMfn.setMinimum(1);
                jScrollBarMfn.setMaximum(lastMfn);
                jLabelMfnBegin.setText("1");
                jLabelMfnEnd.setText(Integer.toString(lastMfn));
                jLabelCurMfn.setText("1");
                jLabelDbEncoding.setText(master.getEncoding());
                jLabelMaxSize.setText("Max size: " + master.getGigaSize());
                jLabelIsisType.setText(master.isFFI() ? "Isis FFI"
                                                      : "Isis Standard");
                jLabelDataAlignment.setText("Data alignment: " +
                                                 master.getDataAlignment());
                jLabelSwapped.setText(master.isSwapped() ? "Swapped bytes"
                                                         : "Direct bytes");
                jScrollBarMfn.setValue(1);
                filterProf = new FilterProfile();
                lastFilterProf = null;
                showRecord(); // Necessario chamar qdo so se muda o encoding.
            } catch (Exception ex) {
                mfn = 0;
                master = null;
                clearFrame();
                JOptionPane.showMessageDialog(this,
                             ex.toString(),
                             "Open/Create error",
                             JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showRecord() {
        if ((mfn > 0) && (mfn <= lastMfn)) {
            try {
                final Record rec = master.getRecord(mfn);
                final Set<Integer> tags = filterProf.getShowFields();

                jLabelCurMfn.setText(Integer.toString(mfn));
                jLabelStatus.setText(rec.getStatus().toString());
                jLabelRecSize.setText(Integer.toString(
                    rec.getRecordLength(master.getEncoding(), master.isFFI())));
                
                if (!tags.isEmpty()) {
                    Iterator<Field> iterator = rec.getFields().iterator();
                    while (iterator.hasNext()) {
                        if (!tags.contains(iterator.next().getId())) {
                            iterator.remove();
                        }
                    }
                }
                if (filterProf.isSortFields()) {
                    rec.sortFields();
                }

                jTextAreaDump.setText(rec.toString());
                jTextAreaDump.setCaretPosition(0);                
                if ((rec.getStatus() == Record.Status.ACTIVE)
                    && (rec.getLockStatus() == Record.LockStatus.LOCKED)) {
                    jLabelLock.setVisible(true);
                } else {
                    jLabelLock.setVisible(false);
                }
            } catch (BrumaException ex) {
                JOptionPane.showMessageDialog(this,
                             ex.getMessage(),
                             "Show record error",
                             JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDialogOpenDb = new javax.swing.JDialog();
        jLabelMaster = new javax.swing.JLabel();
        jButtonMstOpen = new javax.swing.JButton();
        jCheckBoxSwapped = new javax.swing.JCheckBox();
        jComboBoxEncoding = new javax.swing.JComboBox();
        jLabelEncoding = new javax.swing.JLabel();
        jLabelGigaSize = new javax.swing.JLabel();
        jLabelAlignment = new javax.swing.JLabel();
        jComboBoxGigaSize = new javax.swing.JComboBox();
        jComboBoxAlignment = new javax.swing.JComboBox();
        jButtonCancel = new javax.swing.JButton();
        jButtonOk = new javax.swing.JButton();
        jLabelMstName = new javax.swing.JLabel();
        jCheckBoxFFI = new javax.swing.JCheckBox();
        jDialogFilter = new javax.swing.JDialog();
        jCheckBoxSortFields = new javax.swing.JCheckBox();
        jCheckBoxShowAll = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaFields = new javax.swing.JTextArea();
        jButtonOk2 = new javax.swing.JButton();
        jButtonCancel2 = new javax.swing.JButton();
        jDialogImport = new javax.swing.JDialog();
        jLabel2 = new javax.swing.JLabel();
        jButtonImportOk = new javax.swing.JButton();
        jButtonImportCancel = new javax.swing.JButton();
        jButtonOpenImport = new javax.swing.JButton();
        jInputFileLabel = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabelOutputDb = new javax.swing.JLabel();
        jButtonOutputDb = new javax.swing.JButton();
        jLabelCharEnc = new javax.swing.JLabel();
        jComboBoxCharEncoding = new javax.swing.JComboBox();
        jDialogSearch = new javax.swing.JDialog();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldSearch = new javax.swing.JTextField();
        jButtonSearch = new javax.swing.JButton();
        jButtonPrevHit = new javax.swing.JButton();
        jButtonNextHit = new javax.swing.JButton();
        jButtonSearchCancel = new javax.swing.JButton();
        jCheckBoxRegExp = new javax.swing.JCheckBox();
        jCheckBoxIgnoreCase = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldSearchTags = new javax.swing.JTextField();
        jCheckBoxAllTags = new javax.swing.JCheckBox();
        jLabelHitsFound = new javax.swing.JLabel();
        jDialogExport = new javax.swing.JDialog();
        jLabel6 = new javax.swing.JLabel();
        jLabelExportFile = new javax.swing.JLabel();
        jButtonExportOpen = new javax.swing.JButton();
        jLabelExportEncoding = new javax.swing.JLabel();
        jComboBoxExportEncoding = new javax.swing.JComboBox();
        jLabelExportAlignment = new javax.swing.JLabel();
        jComboBoxExportAlignment = new javax.swing.JComboBox();
        jLabelExportMstSize = new javax.swing.JLabel();
        jComboBoxExportMstSize = new javax.swing.JComboBox();
        jCheckBoxExportSwapped = new javax.swing.JCheckBox();
        jButtonExportOk = new javax.swing.JButton();
        jButtonExportCancel = new javax.swing.JButton();
        jCheckBoxExportDeleted = new javax.swing.JCheckBox();
        jCheckBoxExportFFI = new javax.swing.JCheckBox();
        jDialogStat = new javax.swing.JDialog();
        jRadioButtonDatabase = new javax.swing.JRadioButton();
        jRadioField = new javax.swing.JRadioButton();
        jTextFieldTag = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaStat = new javax.swing.JTextArea();
        jButtonStat = new javax.swing.JButton();
        jButtonStatCancel = new javax.swing.JButton();
        jDialogGizmo = new javax.swing.JDialog();
        jButtonGizmoCancel = new javax.swing.JButton();
        jButtonGizmoOk = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jButtonOpenGizmo = new javax.swing.JButton();
        jRadioButtonRecord = new javax.swing.JRadioButton();
        jRadioButtonFields = new javax.swing.JRadioButton();
        jLabel8 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        jTextField8 = new javax.swing.JTextField();
        jTextField9 = new javax.swing.JTextField();
        jLabelMfnBegin = new javax.swing.JLabel();
        jLabelMfnEnd = new javax.swing.JLabel();
        jScrollBarMfn = new javax.swing.JScrollBar();
        jLabelCurMfn = new javax.swing.JLabel();
        jScrollPaneDump = new javax.swing.JScrollPane();
        jTextAreaDump = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        jButtonOpen = new javax.swing.JButton();
        jButtonCreate = new javax.swing.JButton();
        jButtonExport = new javax.swing.JButton();
        jButtonNew = new javax.swing.JButton();
        jButtonUpdate = new javax.swing.JButton();
        jButtonFilter = new javax.swing.JButton();
        jButtonStatistics = new javax.swing.JButton();
        jButtonGizmo = new javax.swing.JButton();
        jButtonImport = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jButtonRegExp = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabelIsisType = new javax.swing.JLabel();
        jLabelLock = new javax.swing.JLabel();
        jLabelMaxSize = new javax.swing.JLabel();
        jLabelDbEncoding = new javax.swing.JLabel();
        jLabelSwapped = new javax.swing.JLabel();
        jLabelDataAlignment = new javax.swing.JLabel();
        jLabelStatus = new javax.swing.JLabel();
        jLabelRecSize = new javax.swing.JLabel();

        jDialogOpenDb.setTitle("Master File");
        jDialogOpenDb.setAlwaysOnTop(true);
        jDialogOpenDb.setMinimumSize(new java.awt.Dimension(400, 230));
        jDialogOpenDb.setModal(true);
        jDialogOpenDb.setName("dialogOpen"); // NOI18N
        jDialogOpenDb.setResizable(false);

        jLabelMaster.setText("Master:");

        jButtonMstOpen.setText("Open...");
        jButtonMstOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonMstOpenActionPerformed(evt);
            }
        });

        jCheckBoxSwapped.setSelected(true);
        jCheckBoxSwapped.setText("Swapped bytes");
        jCheckBoxSwapped.setToolTipText("Intell CPU - swapped bytes [yes]");
        jCheckBoxSwapped.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxSwappedItemStateChanged(evt);
            }
        });
        jCheckBoxSwapped.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxSwappedActionPerformed(evt);
            }
        });

        jComboBoxEncoding.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxEncodingItemStateChanged(evt);
            }
        });

        jLabelEncoding.setText("Character encoding:");

        jLabelGigaSize.setText("Max master size (gigabytes):");

        jLabelAlignment.setText("Memory word alignment:");
        jLabelAlignment.setToolTipText("0 (Windows 32 bits); 2 (Linux 32 bits); 4");

        jComboBoxGigaSize.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0 [512 megabytes]", "2", "4", "8", "16", "32", "64", "128", "256", "512" }));
        jComboBoxGigaSize.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxGigaSizeItemStateChanged(evt);
            }
        });

        jComboBoxAlignment.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0 [Windows 32 bits]", "2 [Linux 32 bits]", "4", "8" }));
        jComboBoxAlignment.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxAlignmentItemStateChanged(evt);
            }
        });

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        jButtonOk.setText("OK");
        jButtonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOkActionPerformed(evt);
            }
        });

        jLabelMstName.setText("xxxxxxx");

        jCheckBoxFFI.setText("FFI");
        jCheckBoxFFI.setToolTipText(" big size records");
        jCheckBoxFFI.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxFFIItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jDialogOpenDbLayout = new javax.swing.GroupLayout(jDialogOpenDb.getContentPane());
        jDialogOpenDb.getContentPane().setLayout(jDialogOpenDbLayout);
        jDialogOpenDbLayout.setHorizontalGroup(
            jDialogOpenDbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialogOpenDbLayout.createSequentialGroup()
                .addGroup(jDialogOpenDbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialogOpenDbLayout.createSequentialGroup()
                        .addGroup(jDialogOpenDbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jDialogOpenDbLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(jLabelMaster)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabelMstName, javax.swing.GroupLayout.PREFERRED_SIZE, 407, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 46, Short.MAX_VALUE)
                                .addComponent(jButtonMstOpen))
                            .addGroup(jDialogOpenDbLayout.createSequentialGroup()
                                .addComponent(jButtonCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonOk, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(19, 19, 19))
                    .addGroup(jDialogOpenDbLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jDialogOpenDbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jDialogOpenDbLayout.createSequentialGroup()
                                .addGroup(jDialogOpenDbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabelEncoding)
                                    .addComponent(jLabelAlignment)
                                    .addComponent(jLabelGigaSize))
                                .addGap(27, 27, 27)
                                .addGroup(jDialogOpenDbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jComboBoxGigaSize, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jComboBoxAlignment, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jComboBoxEncoding, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addComponent(jCheckBoxFFI)
                            .addComponent(jCheckBoxSwapped))))
                .addContainerGap())
        );
        jDialogOpenDbLayout.setVerticalGroup(
            jDialogOpenDbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialogOpenDbLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jDialogOpenDbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelMaster)
                    .addComponent(jLabelMstName, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonMstOpen))
                .addGap(12, 12, 12)
                .addGroup(jDialogOpenDbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelEncoding)
                    .addComponent(jComboBoxEncoding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jDialogOpenDbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelAlignment)
                    .addComponent(jComboBoxAlignment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jDialogOpenDbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelGigaSize)
                    .addComponent(jComboBoxGigaSize, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jCheckBoxFFI)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCheckBoxSwapped)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jDialogOpenDbLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonOk))
                .addContainerGap())
        );

        jDialogFilter.setTitle("Show Fields");
        jDialogFilter.setMinimumSize(new java.awt.Dimension(483, 338));
        jDialogFilter.setModal(true);
        jDialogFilter.setName("dialogFilter"); // NOI18N

        jCheckBoxSortFields.setText("Sort fields");

        jCheckBoxShowAll.setText("Show all fields");
        jCheckBoxShowAll.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxShowAllItemStateChanged(evt);
            }
        });

        jLabel1.setText("Tags of fields to be showed");

        jTextAreaFields.setColumns(20);
        jTextAreaFields.setRows(5);
        jScrollPane1.setViewportView(jTextAreaFields);

        jButtonOk2.setText("OK");
        jButtonOk2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOk2ActionPerformed(evt);
            }
        });

        jButtonCancel2.setText("Cancel");
        jButtonCancel2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancel2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jDialogFilterLayout = new javax.swing.GroupLayout(jDialogFilter.getContentPane());
        jDialogFilter.getContentPane().setLayout(jDialogFilterLayout);
        jDialogFilterLayout.setHorizontalGroup(
            jDialogFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialogFilterLayout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jDialogFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jDialogFilterLayout.createSequentialGroup()
                        .addGroup(jDialogFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBoxShowAll)
                            .addComponent(jCheckBoxSortFields))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 305, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialogFilterLayout.createSequentialGroup()
                        .addGroup(jDialogFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                            .addGroup(jDialogFilterLayout.createSequentialGroup()
                                .addComponent(jButtonCancel2, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonOk2, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(27, 27, 27))
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 205, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jDialogFilterLayout.setVerticalGroup(
            jDialogFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialogFilterLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBoxSortFields)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCheckBoxShowAll)
                .addGap(24, 24, 24)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jDialogFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonCancel2)
                    .addComponent(jButtonOk2))
                .addContainerGap())
        );

        jDialogImport.setTitle("Import dabase");
        jDialogImport.setMinimumSize(new java.awt.Dimension(610, 210));
        jDialogImport.setModal(true);
        jDialogImport.setResizable(false);

        jLabel2.setText("Input file: ");

        jButtonImportOk.setText("OK");
        jButtonImportOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonImportOkActionPerformed(evt);
            }
        });

        jButtonImportCancel.setText("Cancel");
        jButtonImportCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonImportCancelActionPerformed(evt);
            }
        });

        jButtonOpenImport.setText("Open...");
        jButtonOpenImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenImportActionPerformed(evt);
            }
        });

        jInputFileLabel.setText("jLabel3");

        jLabel3.setText("Output db:");

        jLabelOutputDb.setText("jLabel6");

        jButtonOutputDb.setText("Open...");
        jButtonOutputDb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOutputDbActionPerformed(evt);
            }
        });

        jLabelCharEnc.setText("Character encoding:");

        javax.swing.GroupLayout jDialogImportLayout = new javax.swing.GroupLayout(jDialogImport.getContentPane());
        jDialogImport.getContentPane().setLayout(jDialogImportLayout);
        jDialogImportLayout.setHorizontalGroup(
            jDialogImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialogImportLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jDialogImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jDialogImportLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabelOutputDb, javax.swing.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE))
                    .addGroup(jDialogImportLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jInputFileLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE))
                    .addComponent(jButtonImportCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jDialogImportLayout.createSequentialGroup()
                        .addComponent(jLabelCharEnc)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxCharEncoding, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jDialogImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jButtonImportOk, javax.swing.GroupLayout.DEFAULT_SIZE, 77, Short.MAX_VALUE)
                    .addComponent(jButtonOpenImport, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonOutputDb, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jDialogImportLayout.setVerticalGroup(
            jDialogImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialogImportLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jDialogImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jDialogImportLayout.createSequentialGroup()
                        .addGroup(jDialogImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jInputFileLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jDialogImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabelCharEnc)
                            .addComponent(jComboBoxCharEncoding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jButtonOpenImport))
                .addGap(18, 18, 18)
                .addGroup(jDialogImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jDialogImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(jLabelOutputDb, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButtonOutputDb))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jDialogImportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonImportOk)
                    .addComponent(jButtonImportCancel))
                .addContainerGap())
        );

        jDialogSearch.setTitle("Text Search");
        jDialogSearch.setLocationByPlatform(true);
        jDialogSearch.setMinimumSize(new java.awt.Dimension(440, 310));
        jDialogSearch.setModal(true);
        jDialogSearch.setResizable(false);

        jLabel4.setText("Expression:");

        jTextFieldSearch.setMinimumSize(new java.awt.Dimension(12, 27));
        jTextFieldSearch.setPreferredSize(new java.awt.Dimension(12, 25));

        jButtonSearch.setText("Search");
        jButtonSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSearchActionPerformed(evt);
            }
        });

        jButtonPrevHit.setText("<<");
        jButtonPrevHit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPrevHitActionPerformed(evt);
            }
        });

        jButtonNextHit.setText(">>");
        jButtonNextHit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNextHitActionPerformed(evt);
            }
        });

        jButtonSearchCancel.setText("Cancel");
        jButtonSearchCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSearchCancelActionPerformed(evt);
            }
        });

        jCheckBoxRegExp.setText("Regular Expression");

        jCheckBoxIgnoreCase.setText("Ignore case");

        jLabel5.setText("Only in these field tags:");

        jTextFieldSearchTags.setMinimumSize(new java.awt.Dimension(12, 42));
        jTextFieldSearchTags.setPreferredSize(new java.awt.Dimension(12, 25));
        jTextFieldSearchTags.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
                jTextFieldSearchTagsInputMethodTextChanged(evt);
            }
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
            }
        });

        jCheckBoxAllTags.setText("All field tags");
        jCheckBoxAllTags.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jCheckBoxAllTagsStateChanged(evt);
            }
        });

        jLabelHitsFound.setFont(new java.awt.Font("DejaVu Sans", 0, 10)); // NOI18N
        jLabelHitsFound.setForeground(java.awt.Color.blue);
        jLabelHitsFound.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelHitsFound.setText("jLabel6");

        javax.swing.GroupLayout jDialogSearchLayout = new javax.swing.GroupLayout(jDialogSearch.getContentPane());
        jDialogSearch.getContentPane().setLayout(jDialogSearchLayout);
        jDialogSearchLayout.setHorizontalGroup(
            jDialogSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialogSearchLayout.createSequentialGroup()
                .addContainerGap(181, Short.MAX_VALUE)
                .addComponent(jButtonPrevHit)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonNextHit)
                .addGap(165, 165, 165))
            .addGroup(jDialogSearchLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jDialogSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jDialogSearchLayout.createSequentialGroup()
                        .addGroup(jDialogSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jDialogSearchLayout.createSequentialGroup()
                                .addComponent(jCheckBoxAllTags)
                                .addGap(39, 39, 39)
                                .addComponent(jLabelHitsFound, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(jTextFieldSearch, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                            .addComponent(jCheckBoxRegExp, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jCheckBoxIgnoreCase, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldSearchTags, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSearch))
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialogSearchLayout.createSequentialGroup()
                .addContainerGap(325, Short.MAX_VALUE)
                .addComponent(jButtonSearchCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jDialogSearchLayout.setVerticalGroup(
            jDialogSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialogSearchLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jDialogSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSearch))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxRegExp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxIgnoreCase)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldSearchTags, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jDialogSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxAllTags)
                    .addComponent(jLabelHitsFound))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                .addGroup(jDialogSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonNextHit)
                    .addComponent(jButtonPrevHit))
                .addGap(4, 4, 4)
                .addComponent(jButtonSearchCancel)
                .addContainerGap())
        );

        jDialogExport.setTitle("Export database");
        jDialogExport.setMinimumSize(new java.awt.Dimension(650, 220));
        jDialogExport.setModal(true);

        jLabel6.setText("Output file:");

        jButtonExportOpen.setText("Open...");
        jButtonExportOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportOpenActionPerformed(evt);
            }
        });

        jLabelExportEncoding.setText("Character encoding:");

        jLabelExportAlignment.setText("Memory word alignment:");

        jComboBoxExportAlignment.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0 [Windows 32 bits]", "2 [Linux 32 bits]", "4", "8" }));

        jLabelExportMstSize.setText("Max master size (gigabytes):");

        jComboBoxExportMstSize.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0 [512 megabytes]", "2", "4", "8", "16", "32", "64", "128", "256", "512" }));

        jCheckBoxExportSwapped.setSelected(true);
        jCheckBoxExportSwapped.setText("Swapped bytes");

        jButtonExportOk.setText("OK");
        jButtonExportOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportOkActionPerformed(evt);
            }
        });

        jButtonExportCancel.setText("Cancel");
        jButtonExportCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportCancelActionPerformed(evt);
            }
        });

        jCheckBoxExportDeleted.setSelected(true);
        jCheckBoxExportDeleted.setText("Export deleted records");

        jCheckBoxExportFFI.setText("FFI");
        jCheckBoxExportFFI.setToolTipText("big size records");

        javax.swing.GroupLayout jDialogExportLayout = new javax.swing.GroupLayout(jDialogExport.getContentPane());
        jDialogExport.getContentPane().setLayout(jDialogExportLayout);
        jDialogExportLayout.setHorizontalGroup(
            jDialogExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialogExportLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jDialogExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jDialogExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jDialogExportLayout.createSequentialGroup()
                            .addComponent(jLabelExportMstSize, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jComboBoxExportMstSize, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jDialogExportLayout.createSequentialGroup()
                            .addComponent(jLabelExportAlignment, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jComboBoxExportAlignment, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jDialogExportLayout.createSequentialGroup()
                            .addComponent(jLabelExportEncoding, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jComboBoxExportEncoding, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jCheckBoxExportFFI)
                    .addComponent(jCheckBoxExportSwapped)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialogExportLayout.createSequentialGroup()
                        .addGroup(jDialogExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jDialogExportLayout.createSequentialGroup()
                                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabelExportFile, javax.swing.GroupLayout.PREFERRED_SIZE, 436, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jDialogExportLayout.createSequentialGroup()
                                .addComponent(jCheckBoxExportDeleted)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 327, Short.MAX_VALUE)
                                .addComponent(jButtonExportCancel)))
                        .addGap(10, 10, 10)
                        .addGroup(jDialogExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jButtonExportOk, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButtonExportOpen, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jDialogExportLayout.setVerticalGroup(
            jDialogExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialogExportLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jDialogExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jLabelExportFile)
                    .addComponent(jButtonExportOpen))
                .addGap(18, 18, 18)
                .addGroup(jDialogExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelExportEncoding)
                    .addComponent(jComboBoxExportEncoding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jDialogExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelExportAlignment)
                    .addComponent(jComboBoxExportAlignment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jDialogExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelExportMstSize)
                    .addComponent(jComboBoxExportMstSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jCheckBoxExportFFI)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCheckBoxExportSwapped)
                .addGroup(jDialogExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jDialogExportLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 61, Short.MAX_VALUE)
                        .addGroup(jDialogExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButtonExportOk)
                            .addComponent(jButtonExportCancel))
                        .addContainerGap())
                    .addGroup(jDialogExportLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jCheckBoxExportDeleted)
                        .addContainerGap())))
        );

        jDialogExport.getAccessibleContext().setAccessibleParent(null);

        jDialogStat.setTitle("Statistics");
        jDialogStat.setMinimumSize(new java.awt.Dimension(700, 500));
        jDialogStat.setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        jRadioButtonDatabase.setSelected(true);
        jRadioButtonDatabase.setText("Database");
        jRadioButtonDatabase.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jRadioButtonDatabaseItemStateChanged(evt);
            }
        });

        jRadioField.setText("Field tag");

        jTextAreaStat.setColumns(20);
        jTextAreaStat.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        jTextAreaStat.setRows(5);
        jScrollPane2.setViewportView(jTextAreaStat);

        jButtonStat.setText("Start");
        jButtonStat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStatActionPerformed(evt);
            }
        });

        jButtonStatCancel.setText("Cancel");
        jButtonStatCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStatCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jDialogStatLayout = new javax.swing.GroupLayout(jDialogStat.getContentPane());
        jDialogStat.getContentPane().setLayout(jDialogStatLayout);
        jDialogStatLayout.setHorizontalGroup(
            jDialogStatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialogStatLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jDialogStatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 932, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialogStatLayout.createSequentialGroup()
                        .addComponent(jRadioField)
                        .addGroup(jDialogStatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jDialogStatLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 658, Short.MAX_VALUE)
                                .addComponent(jButtonStatCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonStat, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jDialogStatLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextFieldTag, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(jRadioButtonDatabase))
                .addContainerGap())
        );
        jDialogStatLayout.setVerticalGroup(
            jDialogStatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialogStatLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jRadioButtonDatabase)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jDialogStatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioField)
                    .addComponent(jTextFieldTag, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(22, 22, 22)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(jDialogStatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonStat)
                    .addComponent(jButtonStatCancel))
                .addContainerGap())
        );

        jDialogGizmo.setModal(true);
        jDialogGizmo.setResizable(false);

        jButtonGizmoCancel.setText("Cancel");

        jButtonGizmoOk.setText("OK");

        jLabel7.setText("jLabel7");

        jButtonOpenGizmo.setText("Open...");

        jRadioButtonRecord.setText("Registro inteiro");

        jRadioButtonFields.setText("Alguns campos");

        jLabel8.setText("Aplicação do gizmo:");

        jLabel9.setText("Gizmo via arquivo:");

        jLabel10.setText("Gizmo via campos");

        jLabel11.setText("Texto origem:");

        jLabel12.setText("Texto destino:");

        javax.swing.GroupLayout jDialogGizmoLayout = new javax.swing.GroupLayout(jDialogGizmo.getContentPane());
        jDialogGizmo.getContentPane().setLayout(jDialogGizmoLayout);
        jDialogGizmoLayout.setHorizontalGroup(
            jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialogGizmoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialogGizmoLayout.createSequentialGroup()
                        .addComponent(jButtonGizmoOk, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonGizmoCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialogGizmoLayout.createSequentialGroup()
                        .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 678, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonOpenGizmo, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(jDialogGizmoLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addContainerGap(653, Short.MAX_VALUE))
                    .addGroup(jDialogGizmoLayout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addContainerGap(639, Short.MAX_VALUE))
                    .addGroup(jDialogGizmoLayout.createSequentialGroup()
                        .addComponent(jRadioButtonRecord)
                        .addContainerGap(643, Short.MAX_VALUE))
                    .addGroup(jDialogGizmoLayout.createSequentialGroup()
                        .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE)
                        .addGap(641, 641, 641))
                    .addGroup(jDialogGizmoLayout.createSequentialGroup()
                        .addGroup(jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jDialogGizmoLayout.createSequentialGroup()
                                .addGroup(jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel11)
                                    .addComponent(jTextField2, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
                                    .addComponent(jTextField4)
                                    .addComponent(jTextField6)
                                    .addComponent(jTextField8))
                                .addGap(23, 23, 23)
                                .addGroup(jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jTextField9)
                                    .addComponent(jTextField7)
                                    .addComponent(jLabel12)
                                    .addComponent(jTextField3, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)
                                    .addComponent(jTextField5)))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jDialogGizmoLayout.createSequentialGroup()
                                .addComponent(jRadioButtonFields)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField1)))
                        .addContainerGap(60, Short.MAX_VALUE))))
        );
        jDialogGizmoLayout.setVerticalGroup(
            jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialogGizmoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addGap(12, 12, 12)
                .addGroup(jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jButtonOpenGizmo))
                .addGap(24, 24, 24)
                .addComponent(jLabel8)
                .addGap(12, 12, 12)
                .addComponent(jRadioButtonRecord)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jDialogGizmoLayout.createSequentialGroup()
                        .addComponent(jRadioButtonFields)
                        .addGap(44, 44, 44)
                        .addComponent(jLabel10))
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(jLabel12))
                .addGap(13, 13, 13)
                .addGroup(jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE)
                .addGroup(jDialogGizmoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonGizmoCancel)
                    .addComponent(jButtonGizmoOk))
                .addContainerGap())
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabelMfnBegin.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabelMfnBegin.setText("0");

        jLabelMfnEnd.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabelMfnEnd.setText("000000");

        jScrollBarMfn.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
        jScrollBarMfn.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                jScrollBarMfnAdjustmentValueChanged(evt);
            }
        });

        jLabelCurMfn.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabelCurMfn.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelCurMfn.setText("000000");
        jLabelCurMfn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jTextAreaDump.setColumns(20);
        jTextAreaDump.setFont(new java.awt.Font("Arial Unicode MS", 0, 13)); // NOI18N
        jTextAreaDump.setRows(5);
        jScrollPaneDump.setViewportView(jTextAreaDump);

        jScrollPane3.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane3.setHorizontalScrollBar(null);
        jScrollPane3.setPreferredSize(new java.awt.Dimension(130, 514));

        jPanel2.setBorder(new javax.swing.border.MatteBorder(null));
        jPanel2.setPreferredSize(new java.awt.Dimension(264, 512));

        jButtonOpen.setText("Open...");
        jButtonOpen.setMaximumSize(new java.awt.Dimension(105, 30));
        jButtonOpen.setMinimumSize(new java.awt.Dimension(105, 30));
        jButtonOpen.setPreferredSize(new java.awt.Dimension(105, 30));
        jButtonOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenActionPerformed(evt);
            }
        });

        jButtonCreate.setText("Create...");
        jButtonCreate.setMaximumSize(new java.awt.Dimension(105, 30));
        jButtonCreate.setMinimumSize(new java.awt.Dimension(105, 30));
        jButtonCreate.setPreferredSize(new java.awt.Dimension(105, 30));
        jButtonCreate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCreateActionPerformed(evt);
            }
        });

        jButtonExport.setText("Export...");
        jButtonExport.setMaximumSize(new java.awt.Dimension(105, 30));
        jButtonExport.setMinimumSize(new java.awt.Dimension(105, 30));
        jButtonExport.setPreferredSize(new java.awt.Dimension(105, 30));
        jButtonExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportActionPerformed(evt);
            }
        });

        jButtonNew.setText("New");
        jButtonNew.setMaximumSize(new java.awt.Dimension(105, 30));
        jButtonNew.setMinimumSize(new java.awt.Dimension(105, 30));
        jButtonNew.setPreferredSize(new java.awt.Dimension(105, 30));
        jButtonNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNewActionPerformed(evt);
            }
        });

        jButtonUpdate.setText("Update");
        jButtonUpdate.setMaximumSize(new java.awt.Dimension(105, 30));
        jButtonUpdate.setMinimumSize(new java.awt.Dimension(105, 30));
        jButtonUpdate.setPreferredSize(new java.awt.Dimension(105, 30));
        jButtonUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUpdateActionPerformed(evt);
            }
        });

        jButtonFilter.setText("Filter");
        jButtonFilter.setMaximumSize(new java.awt.Dimension(105, 30));
        jButtonFilter.setMinimumSize(new java.awt.Dimension(105, 30));
        jButtonFilter.setPreferredSize(new java.awt.Dimension(105, 30));
        jButtonFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFilterActionPerformed(evt);
            }
        });

        jButtonStatistics.setText("Statistics");
        jButtonStatistics.setMaximumSize(new java.awt.Dimension(105, 30));
        jButtonStatistics.setMinimumSize(new java.awt.Dimension(105, 30));
        jButtonStatistics.setPreferredSize(new java.awt.Dimension(105, 30));
        jButtonStatistics.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStatisticsActionPerformed(evt);
            }
        });

        jButtonGizmo.setText("Gizmo");
        jButtonGizmo.setEnabled(false);
        jButtonGizmo.setMaximumSize(new java.awt.Dimension(105, 30));
        jButtonGizmo.setMinimumSize(new java.awt.Dimension(105, 30));
        jButtonGizmo.setPreferredSize(new java.awt.Dimension(105, 30));

        jButtonImport.setText("Import...");
        jButtonImport.setMaximumSize(new java.awt.Dimension(105, 30));
        jButtonImport.setMinimumSize(new java.awt.Dimension(105, 30));
        jButtonImport.setPreferredSize(new java.awt.Dimension(105, 30));
        jButtonImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonImportActionPerformed(evt);
            }
        });

        jButtonDelete.setText("Delete");
        jButtonDelete.setMaximumSize(new java.awt.Dimension(105, 30));
        jButtonDelete.setMinimumSize(new java.awt.Dimension(105, 30));
        jButtonDelete.setPreferredSize(new java.awt.Dimension(105, 30));
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });

        jButtonRegExp.setText("Search");
        jButtonRegExp.setMaximumSize(new java.awt.Dimension(105, 30));
        jButtonRegExp.setMinimumSize(new java.awt.Dimension(105, 30));
        jButtonRegExp.setPreferredSize(new java.awt.Dimension(105, 30));
        jButtonRegExp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRegExpActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel1.setPreferredSize(new java.awt.Dimension(105, 160));

        jLabelIsisType.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabelIsisType.setForeground(new java.awt.Color(0, 0, 153));
        jLabelIsisType.setText("Isis Standard");

        jLabelLock.setIcon(new javax.swing.ImageIcon(getClass().getResource("/bruma/tools/lupa/images.jpg"))); // NOI18N

        jLabelMaxSize.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabelMaxSize.setForeground(new java.awt.Color(0, 0, 153));
        jLabelMaxSize.setText("Max size : 00");

        jLabelDbEncoding.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabelDbEncoding.setForeground(new java.awt.Color(0, 0, 153));
        jLabelDbEncoding.setText("ISO8859-1");

        jLabelSwapped.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabelSwapped.setForeground(new java.awt.Color(0, 0, 153));
        jLabelSwapped.setText("Swapped bytes");

        jLabelDataAlignment.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabelDataAlignment.setForeground(new java.awt.Color(0, 0, 153));
        jLabelDataAlignment.setText("Data Alignment: 0");

        jLabelStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelStatus.setText("deleted");
        jLabelStatus.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addComponent(jLabelLock))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabelIsisType))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabelDbEncoding, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabelMaxSize))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabelSwapped))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabelDataAlignment, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabelStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabelLock, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelIsisType)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelDbEncoding)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelMaxSize)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelSwapped)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelDataAlignment)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelStatus)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jButtonOpen, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonCreate, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonImport, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonExport, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonNew, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonUpdate, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonDelete, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonRegExp, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonFilter, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonStatistics, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonGizmo, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(145, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jButtonOpen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonCreate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonImport, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonExport, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonNew, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonDelete, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jButtonRegExp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonStatistics, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonGizmo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jButtonOpen.getAccessibleContext().setAccessibleName("jButtonOpen");
        jButtonUpdate.getAccessibleContext().setAccessibleName("jButtonUpdate");
        jButtonFilter.getAccessibleContext().setAccessibleName("jButtonFilter");
        jButtonDelete.getAccessibleContext().setAccessibleName("jButtonDelete");

        jScrollPane3.setViewportView(jPanel2);

        jLabelRecSize.setText("000000");
        jLabelRecSize.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabelMfnBegin)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollBarMfn, javax.swing.GroupLayout.DEFAULT_SIZE, 701, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabelMfnEnd))
                    .addComponent(jScrollPaneDump, javax.swing.GroupLayout.DEFAULT_SIZE, 808, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabelCurMfn, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabelRecSize)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE)
                    .addComponent(jScrollPaneDump, javax.swing.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabelMfnBegin)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabelMfnEnd)
                        .addComponent(jLabelCurMfn, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabelRecSize))
                    .addComponent(jScrollBarMfn, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jLabelMfnBegin.getAccessibleContext().setAccessibleName("jLabelBegin");
        jLabelMfnEnd.getAccessibleContext().setAccessibleName("jLabelEnd");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUpdateActionPerformed
        if (master != null) {
            try {
                if (!filterProf.getShowFields().isEmpty()) {
                    throw new BrumaException(
                        "'Show all fields' Filter option should be checked.");
                }
                final Record rec = Record.fromString(jTextAreaDump.getText());
                rec.setMfn(mfn);
                master.writeRecord(rec);
            } catch (BrumaException ex) {
                 JOptionPane.showMessageDialog(this,
                                 ex.getMessage(),
                                 "Update record error",
                                 JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_jButtonUpdateActionPerformed

    private void jButtonNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNewActionPerformed
        if (master != null) {
            try {
                final Record rec = master.newRecord();

                rec.addField(1, "new record");
                master.writeRecord(rec);
                jTextAreaDump.setText("");
                jScrollBarMfn.setMaximum(++lastMfn);
                jLabelMfnBegin.setText("1");
                jLabelMfnEnd.setText(Integer.toString(lastMfn));
                jLabelCurMfn.setText(Integer.toString(lastMfn));
                mfn = lastMfn;
                jScrollBarMfn.setValue(lastMfn);
            } catch (BrumaException ex) {
                 JOptionPane.showMessageDialog(this,
                                 ex.getMessage(),
                                 "New record error",
                                 JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_jButtonNewActionPerformed

    private void jCheckBoxSwappedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxSwappedActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBoxSwappedActionPerformed

    private void jButtonOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenActionPerformed
        lastOpenProf = openProf;
        openProf = new OpenProfile();
        openProf.setMstDir(lastOpenProf.getMstDir());
        openProf.setCreate(false);
        updateOpen();
        jDialogOpenDb.setVisible(true);
    }//GEN-LAST:event_jButtonOpenActionPerformed

    private void jButtonMstOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonMstOpenActionPerformed
        final FileNameExtensionFilter filter = new FileNameExtensionFilter(
                                            "Isis Master", "mst");
        final File dir = openProf.getMstDir();
        final JFileChooser chooser = new JFileChooser();

        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(dir);
        jButtonOk.setEnabled(false);

        final int returnVal = openProf.isCreate()
                               ? chooser.showSaveDialog(jDialogOpenDb)
                               : chooser.showOpenDialog(jDialogOpenDb);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            openProf.setMaster(chooser.getSelectedFile());
            //jButtonOk.setEnabled(true);
            updateOpen();
        } else {
            jButtonOk.setEnabled(chooser.getSelectedFile() != null);
        }
    }//GEN-LAST:event_jButtonMstOpenActionPerformed

    private void jCheckBoxSwappedItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxSwappedItemStateChanged
        openProf.setSwapped(!openProf.isSwapped());
    }//GEN-LAST:event_jCheckBoxSwappedItemStateChanged

    private void jComboBoxEncodingItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBoxEncodingItemStateChanged
        openProf.setEncoding((String) evt.getItem());
    }//GEN-LAST:event_jComboBoxEncodingItemStateChanged

    private void jComboBoxAlignmentItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBoxAlignmentItemStateChanged
        final String[] split = ((String) evt.getItem()).split("\\s+", 2);
        openProf.setAlignment(Integer.parseInt(split[0]));
    }//GEN-LAST:event_jComboBoxAlignmentItemStateChanged

    private void jComboBoxGigaSizeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBoxGigaSizeItemStateChanged
        final String[] split = ((String) evt.getItem()).split("\\s+", 2);
        openProf.setGigasize(Integer.parseInt(split[0]));
    }//GEN-LAST:event_jComboBoxGigaSizeItemStateChanged

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        openProf = lastOpenProf;
        lastOpenProf = new OpenProfile();
        lastOpenProf.setMstDir(openProf.getMstDir());
        jDialogOpenDb.setVisible(false);
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jButtonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOkActionPerformed
        if (openProf.getMaster() == null) {
            openProf = lastOpenProf;
        }
        lastOpenProf = new OpenProfile();
        lastOpenProf.setMstDir(openProf.getMstDir());
        jDialogOpenDb.setVisible(false);
        updateFrame();
    }//GEN-LAST:event_jButtonOkActionPerformed

    private void jScrollBarMfnAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_jScrollBarMfnAdjustmentValueChanged
        if (master != null) {
            mfn = evt.getValue();
            showRecord();
        }
    }//GEN-LAST:event_jScrollBarMfnAdjustmentValueChanged

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteActionPerformed
        if (mfn > 0) {
            try {
                master.deleteRecord(mfn);
                showRecord();
            } catch (BrumaException ex) {
                 JOptionPane.showMessageDialog(this,
                             ex.getMessage(),
                             "Delete record error",
                             JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_jButtonDeleteActionPerformed

    private void jButtonCreateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCreateActionPerformed
        lastOpenProf = openProf;
        openProf = new OpenProfile();
        openProf.setMstDir(lastOpenProf.getMstDir());
        openProf.setCreate(true);
        updateOpen();
        jDialogOpenDb.setVisible(true);
    }//GEN-LAST:event_jButtonCreateActionPerformed

    private void jButtonFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonFilterActionPerformed
        int num = 0;
        final Set<Integer> tags;
        final StringBuilder builder = new StringBuilder();

        lastFilterProf = filterProf;
        filterProf = new FilterProfile();
        filterProf.setSortFields(lastFilterProf.isSortFields());
        tags = lastFilterProf.getShowFields();
        filterProf.setShowFields(tags);
        for (Integer tag : tags) {
            if (++num % 10 == 0) {
                builder.append("\n");
            }
            builder.append(tag);
            builder.append(" ");
        }
        jCheckBoxSortFields.setSelected(lastFilterProf.isSortFields());
        jCheckBoxShowAll.setSelected(tags.isEmpty());
        jTextAreaFields.setText(builder.toString());
        jDialogFilter.setVisible(true);
    }//GEN-LAST:event_jButtonFilterActionPerformed

    private void jButtonCancel2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancel2ActionPerformed
        filterProf = lastFilterProf;
        jDialogFilter.setVisible(false);
    }//GEN-LAST:event_jButtonCancel2ActionPerformed

    private void jButtonOk2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOk2ActionPerformed
        final String text = jTextAreaFields.getText().trim();
        final Set<Integer> tags = filterProf.getShowFields();

        tags.clear();

        if (!text.isEmpty()) {
            final String[] split = text.split("[^\\d]+");

            for (String tag : split) {
                tags.add(new Integer(tag));
            }
        }

        filterProf.setSortFields(jCheckBoxSortFields.isSelected());
        jDialogFilter.setVisible(false);

        showRecord();
    }//GEN-LAST:event_jButtonOk2ActionPerformed

    private void jCheckBoxShowAllItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxShowAllItemStateChanged
        if (jCheckBoxShowAll.isSelected()) {
           jTextAreaFields.setText("");
        }
    }//GEN-LAST:event_jCheckBoxShowAllItemStateChanged

    private void jButtonImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonImportActionPerformed
        jInputFileLabel.setText("");
        jLabelOutputDb.setText("");
        //jComboBoxCharEncoding.setSelectedItem("ISO-8859-1");
        jComboBoxCharEncoding.setSelectedItem(Master.GUESS_ISO_IBM_ENCODING);
        jButtonImportOk.setEnabled(false);
        jDialogImport.setVisible(true);
    }//GEN-LAST:event_jButtonImportActionPerformed

    private void jButtonImportCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonImportCancelActionPerformed
        jInputFileLabel.setText("");
        jLabelOutputDb.setText("");
        jDialogImport.setVisible(false);
        clearFrame();
    }//GEN-LAST:event_jButtonImportCancelActionPerformed

    private void jButtonOpenImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenImportActionPerformed
        final JFileChooser chooser = new JFileChooser();
        final FileNameExtensionFilter filterIso = new FileNameExtensionFilter(
                                                "Import file ISO2709", "iso");
        final FileNameExtensionFilter filterId = new FileNameExtensionFilter(
                                                "Import file ID", "id");
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(filterIso);
        chooser.addChoosableFileFilter(filterId);

        final int returnVal = chooser.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                jInputFileLabel.setText(chooser.getSelectedFile().getCanonicalPath());
            } catch (IOException ex) {
                 JOptionPane.showMessageDialog(this,
                             ex.getMessage(),
                             "Open file error",
                             JOptionPane.ERROR_MESSAGE);
            }
            jButtonImportOk.setEnabled(!jInputFileLabel.getText().isEmpty() &&
                                       !jLabelOutputDb.getText().isEmpty());
        }
    }//GEN-LAST:event_jButtonOpenImportActionPerformed

    private void jButtonImportOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonImportOkActionPerformed
       final String fName = jInputFileLabel.getText();
       final String encoding = (String)jComboBoxCharEncoding.getSelectedItem();
       final AbstractRecordIterator iterator = (fName.endsWith(".iso")
          ? new ISO2709RecordIterator(fName, encoding)
          : new IdFileRecordIterator(new File(fName), encoding));
        int auxmfn = 0;

        try {
            if (iterator == null) {
                throw new BrumaException("open import file error");
            }
            for (Record rec : iterator) {
                rec.setMfn(++auxmfn); // Já existia registro ativo na base destino.
                master.writeRecord(rec);
                //jLabelMfnEnd.setText(Integer.toString(++xmfn));
            }
            lastMfn = master.getControlRecord().getNxtmfn() - 1;
            mfn = 1;
            //jTextAreaDump.setText("");
            jScrollBarMfn.setMinimum(1);
            jScrollBarMfn.setMaximum(lastMfn);
            jLabelMfnBegin.setText("1");
            jLabelMfnEnd.setText(Integer.toString(lastMfn));
            jLabelCurMfn.setText("1");
            jLabelDbEncoding.setText(master.getEncoding());
            jLabelMaxSize.setText("Max size: " + master.getGigaSize());
            jLabelIsisType.setText(master.isFFI() ? "Isis FFI"
                                                  : "Isis Standard");
            jLabelDataAlignment.setText("Data alignment: " +
                                             master.getDataAlignment());
            jLabelSwapped.setText(master.isSwapped() ? "Swapped bytes"
                                                     : "Direct bytes");
            jScrollBarMfn.setValue(1);
            jDialogImport.setVisible(false);
            filterProf = new FilterProfile();
            lastFilterProf = null;
            showRecord(); // Necessario chamar qdo so se muda o encoding.

        } catch (BrumaException ex) {
            JOptionPane.showMessageDialog(this,
                             ex.getMessage(),
                             "Import file error",
                             JOptionPane.ERROR_MESSAGE);
       }
    }//GEN-LAST:event_jButtonImportOkActionPerformed

    private void jButtonPrevHitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPrevHitActionPerformed
        if ((search != null) && (search.hasPrevious())) {
            jScrollBarMfn.setValue(search.getPrevious());
            jLabelHitsFound.setText("Hits:" + (search.getCurrent() + 1) + "/"
                                                          + search.numOfHits());
        }
    }//GEN-LAST:event_jButtonPrevHitActionPerformed

    private void jButtonRegExpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRegExpActionPerformed
        jTextFieldSearch.setText("");
        jCheckBoxIgnoreCase.setSelected(false);
        jCheckBoxRegExp.setSelected(false);
        jTextFieldSearchTags.setText("");
        jCheckBoxAllTags.setSelected(true);
        jLabelHitsFound.setText("");
        jDialogSearch.setVisible(true);
    }//GEN-LAST:event_jButtonRegExpActionPerformed

    private void jButtonSearchCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSearchCancelActionPerformed
        jDialogSearch.setVisible(false);
    }//GEN-LAST:event_jButtonSearchCancelActionPerformed

    private void jButtonSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSearchActionPerformed
        final String exp = jTextFieldSearch.getText().trim();

        if (!exp.isEmpty() && (master != null)) {
            Set<Integer> tags = null;

            if (!jCheckBoxAllTags.isSelected()) {
                String content = jTextFieldSearchTags.getText().trim();

                if (!content.isEmpty()) {
                    tags = new HashSet<Integer>();
                    for (String tag : content.split("[^\\d]+")) {
                        tags.add(Integer.valueOf(tag));
                    }
                }
            }
            try {
                if (search == null) {
                    search = new Search(master);
                }
                if (jCheckBoxRegExp.isSelected()) {
                    search.searchRegExp(exp, tags, 
                                             jCheckBoxIgnoreCase.isSelected());
                } else {
                    search.search(exp, tags, jCheckBoxIgnoreCase.isSelected());
                }
                if (search.hasNext()) {
                    jScrollBarMfn.setValue(search.getNext());
                }
                jLabelHitsFound.setText("Hits:" + (search.getCurrent() + 1)
                                                  + "/" + search.numOfHits());
            } catch (BrumaException zex) {
                JOptionPane.showMessageDialog(this,
                             zex.getMessage(),
                             "Search error",
                             JOptionPane.ERROR_MESSAGE);
            }

        }
    }//GEN-LAST:event_jButtonSearchActionPerformed

    private void jButtonNextHitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNextHitActionPerformed
        if ((search != null) && (search.hasNext())) {
            jScrollBarMfn.setValue(search.getNext());
            jLabelHitsFound.setText("Hits:" + (search.getCurrent() + 1) + "/"
                                                          + search.numOfHits());
        }
    }//GEN-LAST:event_jButtonNextHitActionPerformed

    private void jButtonOutputDbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOutputDbActionPerformed
        jButtonCreateActionPerformed(null);
        if (openProf == lastOpenProf) { // Cancel was clicked
            // Apagar base recem criada.           
            jLabelOutputDb.setText("");
        } else {
            jLabelOutputDb.setText(openProf.getMaster().getPath());
        }
        jButtonImportOk.setEnabled(!jInputFileLabel.getText().isEmpty() &&
                                   !jLabelOutputDb.getText().isEmpty());
    }//GEN-LAST:event_jButtonOutputDbActionPerformed

    private void jButtonExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportActionPerformed
        if (master != null) {
            jLabelExportFile.setText("");
            jComboBoxExportEncoding.setSelectedItem("ISO-8859-1");
            jLabelExportAlignment.setVisible(true);
            jComboBoxExportAlignment.setVisible(true);
            jLabelExportMstSize.setVisible(true);
            jComboBoxExportMstSize.setVisible(true);
            jCheckBoxExportSwapped.setVisible(true);
            jLabelExportAlignment.setVisible(false);
            jComboBoxExportAlignment.setVisible(false);
            jLabelExportMstSize.setVisible(false);
            jComboBoxExportMstSize.setVisible(false);
            jCheckBoxExportSwapped.setVisible(false);
            jCheckBoxExportFFI.setVisible(false);
            jCheckBoxExportDeleted.setVisible(false);
            jDialogExport.setSize(new java.awt.Dimension(650, 220));
            jDialogExport.setVisible(true);
        }
    }//GEN-LAST:event_jButtonExportActionPerformed

    private void jButtonExportOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportOpenActionPerformed
        final FileNameExtensionFilter filterIsis = new FileNameExtensionFilter(
                                            "Isis Master", "mst");
        final FileNameExtensionFilter filterIso = new FileNameExtensionFilter(
                                            "ISO 2708", "iso");
        final FileNameExtensionFilter filterId = new FileNameExtensionFilter(
                                            "Id File", "id");
        final FileNameExtensionFilter filterXml = new FileNameExtensionFilter(
                                            "XML", "xml");
        final FileNameExtensionFilter filterJSON = new FileNameExtensionFilter(
                                            "JSON", "json");
        final File dir = openProf.getMstDir();
        final JFileChooser chooser = new JFileChooser();        
        final String selectedExtension;        
        final FileNameExtensionFilter filter;
        String selectedFile;
        String auxName;

        chooser.addChoosableFileFilter(filterIso);
        chooser.addChoosableFileFilter(filterId);
        chooser.addChoosableFileFilter(filterXml);
        chooser.addChoosableFileFilter(filterJSON);
        chooser.setFileFilter(filterIsis);
        chooser.setMultiSelectionEnabled(false);

        chooser.setCurrentDirectory(dir);
        jButtonExportOk.setEnabled(false);

        chooser.showSaveDialog(this);
        filter = (FileNameExtensionFilter)chooser.getFileFilter();
        try {
            auxName = (chooser.getSelectedFile() == null)
                           ? "" : chooser.getSelectedFile().getCanonicalPath();
            selectedExtension = filter.getExtensions()[0];
            selectedFile = Util.changeFileExtension(auxName, selectedExtension);
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(this,
                             ex.getMessage(),
                             null,
                             JOptionPane.ERROR_MESSAGE);
            selectedFile = "";
        }
        
        /*selectedFile = (auxName.matches("[^\\.]+\\.[^\\.]+"))
                        ? auxName
                        : auxName.isEmpty()
                          ? auxName : auxName + "." + selectedExtension;*/
        
        jLabelExportFile.setText(selectedFile);

        if (selectedFile.endsWith(".mst")) {
            jLabelExportAlignment.setVisible(true);
            jComboBoxExportAlignment.setVisible(true);
            jLabelExportMstSize.setVisible(true);
            jComboBoxExportMstSize.setVisible(true);
            jCheckBoxExportSwapped.setVisible(true);
            jCheckBoxExportFFI.setVisible(true);
            jCheckBoxExportDeleted.setVisible(true);
            jDialogExport.setSize(new java.awt.Dimension(650, 335));
        } else {
            jLabelExportAlignment.setVisible(false);
            jComboBoxExportAlignment.setVisible(false);
            jLabelExportMstSize.setVisible(false);
            jComboBoxExportMstSize.setVisible(false);
            jCheckBoxExportSwapped.setVisible(false);
            jCheckBoxExportFFI.setVisible(false);
            jCheckBoxExportDeleted.setVisible(false);
            jDialogExport.setSize(new java.awt.Dimension(650, 220));
        }
        jButtonExportOk.setEnabled(!selectedFile.isEmpty());
    }//GEN-LAST:event_jButtonExportOpenActionPerformed

    private void jButtonExportOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportOkActionPerformed
        final String selectedFile = jLabelExportFile.getText();
        final String selectedExtension = selectedFile.substring(
                                            selectedFile.lastIndexOf('.') + 1);
        if (!selectedFile.isEmpty()) {
            exportDatabase(selectedFile, selectedExtension);
        }
        jDialogExport.setVisible(false);
    }//GEN-LAST:event_jButtonExportOkActionPerformed

    private void jButtonExportCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportCancelActionPerformed
        jDialogExport.setVisible(false);
    }//GEN-LAST:event_jButtonExportCancelActionPerformed

    private void jButtonStatisticsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStatisticsActionPerformed
        jTextAreaStat.setText("");
        jDialogStat.setVisible(true);
    }//GEN-LAST:event_jButtonStatisticsActionPerformed

    private void jRadioButtonDatabaseItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jRadioButtonDatabaseItemStateChanged
        jTextFieldTag.setText(null);
    }//GEN-LAST:event_jRadioButtonDatabaseItemStateChanged

    private void jButtonStatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStatActionPerformed
        String out = "";
        final Statistics stat = new Statistics();

        try {
            if (jRadioButtonDatabase.isSelected()) {
                if (master != null) {
                   out = stat.stat(master.getMasterName(),
                                   master.getEncoding(),
                                   1,
                                   Integer.MAX_VALUE,
                                   0);
                }
            } else {
                if (!jTextFieldTag.getText().isEmpty()) {
                    final int tag = Integer.parseInt(jTextFieldTag.getText());
                    out = stat.tab(master.getMasterName(),
                                   master.getEncoding(),
                                   1,
                                   Integer.MAX_VALUE,
                                   tag, 0);
                }
            }
        } catch(Exception ex) {
            out = ex.toString();
        }

        jTextAreaStat.setText(out);
    }//GEN-LAST:event_jButtonStatActionPerformed

    private void jButtonStatCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStatCancelActionPerformed
        jDialogStat.setVisible(false);
    }//GEN-LAST:event_jButtonStatCancelActionPerformed

    private void jCheckBoxAllTagsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jCheckBoxAllTagsStateChanged

    }//GEN-LAST:event_jCheckBoxAllTagsStateChanged

    private void jTextFieldSearchTagsInputMethodTextChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_jTextFieldSearchTagsInputMethodTextChanged

    }//GEN-LAST:event_jTextFieldSearchTagsInputMethodTextChanged

    private void jCheckBoxFFIItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxFFIItemStateChanged
        openProf.setFFI(jCheckBoxFFI.isSelected());
    }//GEN-LAST:event_jCheckBoxFFIItemStateChanged

    private void exportDatabase(final String outFile,
                                  final String extension) {
        assert outFile != null;
        assert extension != null;

        AbstractMasterExport export = null;
        final String toEncoding =
                              (String)jComboBoxExportEncoding.getSelectedItem();

        try {
            if (extension.equals("mst")) {
                final int dataAlignment = Integer.parseInt(
                          ((String)jComboBoxExportAlignment.getSelectedItem())
                          .split("\\s+", 2)[0]);
                final int maxGigaSize = Integer.parseInt(
                          ((String)jComboBoxExportMstSize.getSelectedItem())
                          .split("\\s+", 2)[0]);
                export = new IsisMasterExport(
                                           master,
                                           outFile,
                                           jCheckBoxExportFFI.isSelected(),
                                           dataAlignment,
                                           toEncoding,
                                           maxGigaSize,
                                           jCheckBoxExportSwapped.isSelected(),
                                           jCheckBoxExportDeleted.isSelected());
            } else if (extension.equals("iso")) {
                export = new ISO2709Export(master, outFile, toEncoding);
            } else if (extension.equals("id")) {
                export = new IdMasterExport(master, outFile, toEncoding);
            } else if (extension.equals("xml")) {
                export = new XmlMasterExport(master, outFile, toEncoding, true);
            } else if (extension.equals("json")) {
                export = 
                     new JSONMasterExport(master, outFile, toEncoding, true, 0);
            }

            if (export != null) {
                export.export(-1);
            }
        } catch(BrumaException ex) {
            JOptionPane.showMessageDialog(this,
                             ex.getMessage(),
                             "Export error",
                             JOptionPane.ERROR_MESSAGE);
        }
        jDialogExport.setVisible(false);
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance()) {
            System.out.println("\n  Bruma version:" + MasterFactory.VERSION + "\n");
            
            System.out.println("  use: java -cp <BrumaDir>/Bruma.jar <option>\n");
            System.out.println("  bruma.tools.lupa.Lupa - not available in this environment");
            System.out.println("  bruma.examples.DumpDbIII - dump database records");
            System.out.println("  bruma.tools.CopyMaster - copy a master file");
            System.out.println("  bruma.tools.Isis2Couch - export a database to CouchDb");
            System.out.println("  bruma.tools.ExportMaster - export a master file to other formats");
            System.out.println("  bruma.tools.ImportMaster - import a master file from other formats");
            System.out.println("  bruma.tools.Statistics - show statistics of a master file");
            System.out.println("  bruma.tools.lupa.Search - search a regular expression in records");
            System.out.println("");
            System.out.println("");
            System.out.println("");
        } else {
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new Lupa().setVisible(true);
                }
            });
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonCancel2;
    private javax.swing.JButton jButtonCreate;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JButton jButtonExport;
    private javax.swing.JButton jButtonExportCancel;
    private javax.swing.JButton jButtonExportOk;
    private javax.swing.JButton jButtonExportOpen;
    private javax.swing.JButton jButtonFilter;
    private javax.swing.JButton jButtonGizmo;
    private javax.swing.JButton jButtonGizmoCancel;
    private javax.swing.JButton jButtonGizmoOk;
    private javax.swing.JButton jButtonImport;
    private javax.swing.JButton jButtonImportCancel;
    private javax.swing.JButton jButtonImportOk;
    private javax.swing.JButton jButtonMstOpen;
    private javax.swing.JButton jButtonNew;
    private javax.swing.JButton jButtonNextHit;
    private javax.swing.JButton jButtonOk;
    private javax.swing.JButton jButtonOk2;
    private javax.swing.JButton jButtonOpen;
    private javax.swing.JButton jButtonOpenGizmo;
    private javax.swing.JButton jButtonOpenImport;
    private javax.swing.JButton jButtonOutputDb;
    private javax.swing.JButton jButtonPrevHit;
    private javax.swing.JButton jButtonRegExp;
    private javax.swing.JButton jButtonSearch;
    private javax.swing.JButton jButtonSearchCancel;
    private javax.swing.JButton jButtonStat;
    private javax.swing.JButton jButtonStatCancel;
    private javax.swing.JButton jButtonStatistics;
    private javax.swing.JButton jButtonUpdate;
    private javax.swing.JCheckBox jCheckBoxAllTags;
    private javax.swing.JCheckBox jCheckBoxExportDeleted;
    private javax.swing.JCheckBox jCheckBoxExportFFI;
    private javax.swing.JCheckBox jCheckBoxExportSwapped;
    private javax.swing.JCheckBox jCheckBoxFFI;
    private javax.swing.JCheckBox jCheckBoxIgnoreCase;
    private javax.swing.JCheckBox jCheckBoxRegExp;
    private javax.swing.JCheckBox jCheckBoxShowAll;
    private javax.swing.JCheckBox jCheckBoxSortFields;
    private javax.swing.JCheckBox jCheckBoxSwapped;
    private javax.swing.JComboBox jComboBoxAlignment;
    private javax.swing.JComboBox jComboBoxCharEncoding;
    private javax.swing.JComboBox jComboBoxEncoding;
    private javax.swing.JComboBox jComboBoxExportAlignment;
    private javax.swing.JComboBox jComboBoxExportEncoding;
    private javax.swing.JComboBox jComboBoxExportMstSize;
    private javax.swing.JComboBox jComboBoxGigaSize;
    private javax.swing.JDialog jDialogExport;
    private javax.swing.JDialog jDialogFilter;
    private javax.swing.JDialog jDialogGizmo;
    private javax.swing.JDialog jDialogImport;
    private javax.swing.JDialog jDialogOpenDb;
    private javax.swing.JDialog jDialogSearch;
    private javax.swing.JDialog jDialogStat;
    private javax.swing.JLabel jInputFileLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelAlignment;
    private javax.swing.JLabel jLabelCharEnc;
    private javax.swing.JLabel jLabelCurMfn;
    private javax.swing.JLabel jLabelDataAlignment;
    private javax.swing.JLabel jLabelDbEncoding;
    private javax.swing.JLabel jLabelEncoding;
    private javax.swing.JLabel jLabelExportAlignment;
    private javax.swing.JLabel jLabelExportEncoding;
    private javax.swing.JLabel jLabelExportFile;
    private javax.swing.JLabel jLabelExportMstSize;
    private javax.swing.JLabel jLabelGigaSize;
    private javax.swing.JLabel jLabelHitsFound;
    private javax.swing.JLabel jLabelIsisType;
    private javax.swing.JLabel jLabelLock;
    private javax.swing.JLabel jLabelMaster;
    private javax.swing.JLabel jLabelMaxSize;
    private javax.swing.JLabel jLabelMfnBegin;
    private javax.swing.JLabel jLabelMfnEnd;
    private javax.swing.JLabel jLabelMstName;
    private javax.swing.JLabel jLabelOutputDb;
    private javax.swing.JLabel jLabelRecSize;
    private javax.swing.JLabel jLabelStatus;
    private javax.swing.JLabel jLabelSwapped;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JRadioButton jRadioButtonDatabase;
    private javax.swing.JRadioButton jRadioButtonFields;
    private javax.swing.JRadioButton jRadioButtonRecord;
    private javax.swing.JRadioButton jRadioField;
    private javax.swing.JScrollBar jScrollBarMfn;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPaneDump;
    private javax.swing.JTextArea jTextAreaDump;
    private javax.swing.JTextArea jTextAreaFields;
    private javax.swing.JTextArea jTextAreaStat;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JTextField jTextFieldSearch;
    private javax.swing.JTextField jTextFieldSearchTags;
    private javax.swing.JTextField jTextFieldTag;
    // End of variables declaration//GEN-END:variables

}
