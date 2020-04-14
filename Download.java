import java.io.*;
import java.net.*;
import java.net.HttpURLConnection;
import java.util.*;

//this class Downloads a file from a URL
public class Download extends Observable implements Runnable{
    //Max size of download buffer
    private static final int MAX_BUFFER_SIZE = 1024;

    //These are the status name
    public static final String STATUSES[] = {"Downloading",
            "Paused",
            "Complete",
            "Cancelled",
            "Error"};
    
    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;

    private URL url;        //download URL
    private int size;      //sizer of download in byte
    private int downloaded; //number of bytes download
    private int status;     //current status of downloading

    //Constructor for downloading
    public Download(URL url) {
        this.url = url;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;
        //Begin the download
        download();
    }

    //Get this download's URL
    public String getURL() {
        return url.toString();
    }

    //Get this download's size
    public int getSize() {
        return size;
    }

    //Get this download's progress
    public float getProgress() {
        return ((float) downloaded / size) * 100;
    }

    //Get this download status
    public int getStatus() {
        return status;
    }

    //pause this download
    public void pause() {
        status = PAUSED;
        stateChanged();
    }

    //Resume this download
    public void resume() {
        status = DOWNLOADING;
        stateChanged();
        download();
    }

    //Cancel this download
    public void cancel() {
        status = CANCELLED;
        stateChanged();
    }

    //Mark this download having an error
    private void error() {
        status = ERROR;
        stateChanged();
    }

    //Start or resume downloading
    private void download() {
        Thread thread = new Thread(this);
        thread.start();
    }

    //Get file name portion of the URL
    private String getFileName(URL url) {
        String fileName = url.getFile();
        return fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    //Download file
    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;

        try {
            //Open Connection to URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            //Specify what portion of files to download
            connection.setRequestProperty("Range ", "bytes = " + downloaded + "-");

            //connect to server
            connection.connect();

            //Make sure response code is in 200 range
            if ((connection.getResponseCode() / 100) != 2) {
                error();
            }

            //check for a valid content length
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                error();
            }

            /* Set the size for this download if it is not haven't been already set*/
            if (size == -1) {
                size = contentLength;
                stateChanged();
            }

            //Open file and seek to the end of it
            file = new RandomAccessFile(getFileName(url), "rw");
            file.seek(downloaded);

            stream = connection.getInputStream();
            while (status == DOWNLOADING) {
                /*Size buffer according to how much of the file is left to download*/
                byte buffer[];
                if ((size - downloaded) > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];     //if downloaded file gets out of buffer then initialize new buffer size
                } else {
                    buffer = new byte[size - downloaded];     //otherwise initialize the byte array dynamically
                }

                //Read from server into buffer
                int read = stream.read(buffer);             //read from the buffer
                if (read == -1) {
                    break;
                }
                //Write buffer to file.
                file.write(buffer, 0, read);
                downloaded += read;
                stateChanged();
            }

            /* Change status to complete if this point was reached because downloading has finished*/
            if (status == DOWNLOADING) {
                status = COMPLETE;
                stateChanged();
            }
        } catch (Exception e) {
            error();
        } finally {
            //close the file
            if (file != null) {
                try {
                    file.close();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
            //Close connection to the server
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
    }
    private void stateChanged() {
        setChanged();
        notifyObservers();
    }
}