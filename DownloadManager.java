import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;


public class DownloadManager extends JFrame implements Observer{
    //Add download text field
    private JTextField addTextField;

    //Download table's data model
    private DownloadsTableModel tableModel;

    //Table listing downloads
    private JTable table;

    //These are buttons for managing the selected downloads
    private JButton pauseButton, resumeButton, cancelButton, clearButton;

    //Currently selected download
    private Download selectedDownload;

    //Flag for whether or not table selection is being cleard
    private boolean clearing;

    //Constructor for Download Manager
    public DownloadManager () {
        //set application title
        setTitle("Download Manager");
        //set window size
        setSize(640, 480);

        //Handle window closing events
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });

        //Set up file menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem fileExitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionExit();
            }
        });
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        //Set up add panel
        JPanel addPanel = new JPanel();
        addTextField = new JTextField(30);
        addPanel.add(addTextField);
        JButton addButton = new JButton("Add Download");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent et) {
                actionAdd();
            }
        });
        addPanel.add(addButton);

        //set up Download Table
        tableModel = new DownloadsTableModel();
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                tableSelectionChanged();
            }
        });

        //Allow only one row at a time to be selected.
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        //set up ProgressBar as renderer for progress colum
        ProgressRenderer renderer = new ProgressRenderer(0, 100);
        renderer.setStringPainted(true);                        //show progress text
        table.setDefaultRenderer(JProgressBar.class, renderer);

        //set table's row height large enough to fit JProgressBar.
        table.setRowHeight((int) renderer.getPreferredSize().getHeight());

        //set up download panel
        JPanel downloadsPanel = new JPanel();
        downloadsPanel.setBorder(BorderFactory.createTitledBorder("Downloads"));
        downloadsPanel.setLayout(new BorderLayout());
        downloadsPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        //set up buttons panel
        JPanel buttonsPanel = new JPanel();
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionPause();
            }
        });
        pauseButton.setEnabled(false);
        buttonsPanel.add(pauseButton);
        resumeButton = new JButton("Resume");
        resumeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionResume();
            }
        });
        resumeButton.setEnabled(false);
        buttonsPanel.add(resumeButton);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                actionCancel();
            }
        });
        cancelButton.setEnabled(false);
        buttonsPanel.add(cancelButton);
        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                actionClear();
            }
        });
        clearButton.setEnabled(false);
        buttonsPanel.add(clearButton);

        //Add panels to display
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(addPanel, BorderLayout.NORTH);
        getContentPane().add(downloadsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    //Exit the program.
    private void actionExit(){
        System.exit(0);
    }

    //Add a new Download
    private void actionAdd(){
        URL verifiedUrl = verifyUrl (addTextField.getText());
        if(verifiedUrl != null){
            tableModel.addDownload(new Download(verifiedUrl));
            addTextField.setText("");           //reset add text field
        }
        else{
            JOptionPane.showMessageDialog(this, "Invalid option URL", "Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    //Verify download URL
    private URL verifyUrl(String url){
        //Only downloads via HTTPS protocol
        //BTW who now use http protocol?
        if(!url.toLowerCase().startsWith("https://")){
            return null;
        }
        //verify format of URL
        URL verifiedUrl = null;
        try{
            verifiedUrl = new URL(url);
        }
        catch (Exception e){
            return null;
        }
        //Make sure URL specifies a file
        if (verifiedUrl.getFile().length() < 2){
            return null;
        }
        return verifiedUrl;
    }

    //Called when table row selection changed
    private void tableSelectionChanged(){
        //Unregistered from receiving notifications from the last selected download
        if(selectedDownload != null){
            selectedDownload.deleteObserver(DownloadManager.this);
        }
        //if not in the middle of clearing a download, set the selected download and register to receive notification from it
        if(!clearing && table.getSelectedRow() > - 1){
            selectedDownload = tableModel.getDownload(table.getSelectedRow());
            selectedDownload.addObserver(DownloadManager.this);
            updateButtons();
        }
    }

    //Pause the selected download
    private void actionPause(){
        selectedDownload.pause();
        updateButtons();
    }

    //Resume the selected Donload
    private void actionResume(){
        selectedDownload.resume();
        updateButtons();
    }

    //Cancel the selected download
    private void actionCancel(){
        selectedDownload.cancel();
        updateButtons();
    }

    //clear the selected download
    private void actionClear(){
        clearing = true;
        tableModel.clearDownload(table.getSelectedRow());
        clearing = false;
        selectedDownload = null;
        updateButtons();
    }

    /**
     * Update each button's state based off the currently selected download's status
     */
    private void updateButtons(){
        if(selectedDownload !=null){
            int status = selectedDownload.getStatus();
            switch (status){
                case Download.DOWNLOADING:
                    pauseButton.setEnabled(true);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                    break;
                case Download.PAUSED:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                    break;
                case Download.ERROR:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
                    break;
                default:                        //Complete or cancel
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
            }
        }
        else{
            //No download is selected in this table
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
            cancelButton.setEnabled(false);
            clearButton.setEnabled(false);
        }
    }

    
    //Update is called when a Download notifies its observes of any changes

    public void update(Observable obs, Object arg){
        //Update buttons if update if the selected download has changed.
        if(selectedDownload !=null && selectedDownload.equals(obs)){
            updateButtons();
        }
    }

    //Main method
    public static void main(String []  args){
        SwingUtilities.invokeLater(new Runnable() {
            public void run(){
                DownloadManager objManager = new DownloadManager();
                objManager.setVisible(true);
            }
        });
    }
}
