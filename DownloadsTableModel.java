import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

//This class manages the download table's data.
public class DownloadsTableModel extends AbstractTableModel implements Observer{
    //Name of the table column
    private  static final String[] columnNames = { "URL", "Size", "Progress", "Status"};

    //These are the classes for each column's values
    private static final Class[] columnClasses = {String.class, String.class, JProgressBar.class, String.class};

    //the table list of downloads
    private ArrayList<Download> downloadList = new ArrayList<Download>();

    //Add a new Download to the table
    public void addDownload(Download download) {
        //Register to be notified when the download changes
        download.addObserver(this);
        downloadList.add(download);
        //Fire table row insertion notification to the table
        fireTableRowsInserted(getRowCount() -1 , getRowCount() -1);
    }

    //Get a download for the specific row
    public Download getDownload(int row){
        return downloadList.get(row);
    }

    //Remove a download from the list
    public void clearDownload(int row){
        downloadList.remove(row);
        //Fire table row deletion notification to table
        fireTableRowsDeleted(row , row);
    }

    //Get a column's name
    public String getColumnName(int col){
        return columnNames[col];
    }

    //Get tables column count.
    public int getColumnCount(){
        return columnNames.length;
    }

    //Get a columnn's class
    public Class getColumnClass (int col){
        return columnClasses[col];
    }

    //Get table's row count
    public int getRowCount(){
        return downloadList.size();
    }

    //Get value for a specific row and column combination.
    public Object getValueAt (int row, int col){
        Download download = downloadList.get(row);
        if (col == 0){
            return download.getURL();   //return the URL
        }
        else if (col == 1){
            int size = download.getSize();
            if (size == -1){
                return "";
            }
            else{
                return Integer.toString(size);
            }
        }
        else if (col == 2){
            return new Float(download.getProgress());
        }
        else if (col == 3){
            return Download.STATUSES[download.getStatus()];
        }
        return "";
    }

    //Update is called when a Download notifies it's observers of any chances

    public void update(Observable o, Object arg){
        int index = downloadList.indexOf(o);
        //Push/Fire table row update notification to table
        fireTableRowsUpdated(index, index);
    }
}
