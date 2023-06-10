
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import static java.nio.file.StandardCopyOption.*;


public class Server {
    static int PortNo;
    public static void main(String args[]) throws Exception

    {
        PortNo=5210;
        ServerSocket soc=new ServerSocket(PortNo);
        ServerSocket datasoc=new ServerSocket(PortNo-1);
        System.out.println("FTP Server Started on Port Number "+ PortNo);
        while(true)
        {
            System.out.println("Waiting for Connection ...");
            Socket socket = soc.accept();
            transferfile t=new transferfile(socket,datasoc);
        }
    }
}

class transferfile extends Thread
{
    static Socket ClientSoc,dataSoc;
    static ServerSocket DataSoc;
    DataInputStream dinput;
    DataOutputStream doutput;

    transferfile(Socket soc,ServerSocket datasoc)
    {
        try
        {
            Connection conn= DBConnect.getConn();
            Statement stmt=conn.createStatement();

            ClientSoc=soc;
            DataSoc=datasoc;
            dinput=new DataInputStream(ClientSoc.getInputStream());
            doutput=new DataOutputStream(ClientSoc.getOutputStream());
            System.out.println("FTP Client Connected ...");
            String use=dinput.readUTF();
            String pass=dinput.readUTF();
            String sql="SELECT * from account where userName='"+use+"' and password='"+pass+"'";
            ResultSet rs=stmt.executeQuery(sql);
            int check=0;
            String role="";
            while (rs.next()) {
                check=1;
                role=rs.getString("role");
            }
            if (check==1){
                doutput.writeUTF("Success");
                doutput.writeUTF(role);
                System.out.println("User logged in successfully");
            }
            else{
                doutput.writeUTF("Fail");
                doutput.writeUTF(role);
            }
            start();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }
    void ReceiveFile() throws Exception
    {
        DataInputStream datain;
        DataOutputStream dataout;
        dataSoc=DataSoc.accept();
        datain=new DataInputStream(dataSoc.getInputStream());
        dataout=new DataOutputStream(dataSoc.getOutputStream());

        String filename=dinput.readUTF();
        File f=new File(filename);
        if(!f.exists())
        {
            doutput.writeUTF("File Not Found");
            return;
        }
        else
        {
            String path=f.getAbsolutePath();

            //String temppath="/tmp/";
            //File tmpfile = new File ("/tmp/"+filename);

            String temppath="C:\\TMP\\";
            File tmpfile = new File ("C:\\TMP\\"+filename);

            Path target = Paths.get(temppath + f.getName());
            Path source = Paths.get(path);
            try {

                Files.copy(source, target, REPLACE_EXISTING);

            } catch (IOException e) {

                e.printStackTrace();
            }

            doutput.writeUTF("READY");
            FileInputStream fin=new FileInputStream(tmpfile);
            doutput.writeDouble(f.length());
            int ch;
            do
            {
                ch=fin.read();
                dataout.writeUTF(String.valueOf(ch));
            }
            while(ch!=-1);
            fin.close();
            doutput.writeUTF("File Receive Successfully");
        }
    }

    void SendFile() throws Exception
    {
        DataInputStream datain;
        DataOutputStream dataout;
        dataSoc=DataSoc.accept();
        datain=new DataInputStream(dataSoc.getInputStream());
        dataout=new DataOutputStream(dataSoc.getOutputStream());

        String filename=dinput.readUTF();
        if(filename.compareTo("File not found")==0)
        {
            return;
        }
        File f=new File(filename);
        String option;

        if(f.exists())
        {
            doutput.writeUTF("File Already Exists");
            option=dinput.readUTF();
        }
        else
        {
            doutput.writeUTF("SendFile");
            option="Y";
        }

        if(option.compareTo("Y")==0)
        {
            String path=f.getAbsolutePath();

            String temppath="C:\\TMP\\";
            File tmpfile = new File ("C:\\TMP\\"+filename);

            //String temppath="/tmp/";
            //File tmpfile = new File ("/tmp/"+filename);

            FileOutputStream fout=new FileOutputStream(tmpfile);
            int ch;
            String temp;
            do
            {
                temp=datain.readUTF();
                ch=Integer.parseInt(temp);
                if(ch!=-1)
                {
                    fout.write(ch);
                }
            }while(ch!=-1);
            fout.close();
            //Path source = Paths.get(temppath + f.getName());
            Path source = Paths.get(temppath + f.getName());
            Path target = Paths.get(path);
            try {

                Files.move(source, target, REPLACE_EXISTING);

            } catch (IOException e) {

                e.printStackTrace();
            }

            File delfile = new File(temppath, filename);
            delfile.delete();
            doutput.writeUTF("File Send Successfully");
        }
        else
        {
            return;
        }

    }

    void Disconnect() throws Exception{
        DataInputStream datain;
        DataOutputStream dataout;
        dataSoc=DataSoc.accept();
        datain=new DataInputStream(dataSoc.getInputStream());
        dataout=new DataOutputStream(dataSoc.getOutputStream());
        dataSoc.close();
    }

    void Pwd() throws Exception{
        DataInputStream datain;
        DataOutputStream dataout;
        dataSoc=DataSoc.accept();
        datain=new DataInputStream(dataSoc.getInputStream());
        dataout=new DataOutputStream(dataSoc.getOutputStream());
        File file = new File(".");
        String dir = file.getAbsolutePath();
        System.out.println(dir);
        dataout.writeUTF(dir);
    }
    void getFiles() throws Exception{
        DataInputStream datain;
        DataOutputStream dataout;
        dataSoc=DataSoc.accept();
        datain=new DataInputStream(dataSoc.getInputStream());
        dataout=new DataOutputStream(dataSoc.getOutputStream());
        String dir = System.getProperty("user.dir");
        File folder= new File(dir);
        File[] listofFiles = folder.listFiles();

        int count = 0;
        for (int i=0;i<listofFiles.length;i++){
            if(listofFiles[i].isFile()){
                count++;
            }
        }
        doutput.writeInt(count);
        for (File file : listofFiles) {
            if (file.isFile()) {
                doutput.writeUTF(file.getName());
            }
        }

    }
    void getDir() throws Exception{
        DataInputStream datain;
        DataOutputStream dataout;
        dataSoc=DataSoc.accept();
        datain=new DataInputStream(dataSoc.getInputStream());
        dataout=new DataOutputStream(dataSoc.getOutputStream());
        String dir = System.getProperty("user.dir");
        File folder= new File(dir);
        File[] listofFiles = folder.listFiles();

        int count = 0;
        for (int i=0;i<listofFiles.length;i++){
            if(!listofFiles[i].isFile()){
                count++;
            }
        }
        doutput.writeInt(count);

        for (File file : listofFiles) {
            if (!file.isFile()) {
                doutput.writeUTF(file.getName());
            }
        }

    }
    void getList() throws Exception {
        DataInputStream datain;
        DataOutputStream dataout;
        dataSoc=DataSoc.accept();
        datain=new DataInputStream(dataSoc.getInputStream());
        dataout=new DataOutputStream(dataSoc.getOutputStream());
        String dir = System.getProperty("user.dir");
        File folder= new File(dir);
        File[] listofFiles = folder.listFiles();
        doutput.writeUTF(String.valueOf(listofFiles.length));
        for (int i = 0; i < listofFiles.length; i++) {
            if (listofFiles[i].isFile()) {
                doutput.writeUTF("1");
                doutput.writeUTF(listofFiles[i].getName());
                doutput.writeUTF(String.valueOf(listofFiles[i].length()));
            }
            else if (listofFiles[i].isDirectory()) {
                doutput.writeUTF("2");
                doutput.writeUTF("Dir -  "+ listofFiles[i].getName());
            }
        }

    }
    void setCD() throws Exception{
        DataInputStream datain;
        DataOutputStream dataout;
        dataSoc=DataSoc.accept();
        datain=new DataInputStream(dataSoc.getInputStream());
        dataout=new DataOutputStream(dataSoc.getOutputStream());
        String path=dinput.readUTF();
        File newdir = new File(path);
        if(newdir.exists()){
            System.setProperty("user.dir",path);
            doutput.writeUTF("true");}
        else{
            //System.out.println("No path");
            doutput.writeUTF("false");
        }
    }
    void deleteFile() throws Exception{
        DataInputStream datain;
        DataOutputStream dataout;
        dataSoc=DataSoc.accept();
        datain=new DataInputStream(dataSoc.getInputStream());
        dataout=new DataOutputStream(dataSoc.getOutputStream());
        String filename=dinput.readUTF();
        String dir=System.getProperty("user.dir");
        File delfile = new File(dir, filename);
        delfile.delete();
    }

    void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }

    public static boolean deleteDirr(File dir){
        File[] files = dir.listFiles();
        if(files != null){
            for(File file : files){
                if(file.isDirectory()){
                    deleteDirr(file);
                } else {
                    file.delete();
                }
            }
        }
        return dir.delete();
    }

    void delAllInDir() throws Exception{
        DataInputStream datain;
        DataOutputStream dataout;
        dataSoc=DataSoc.accept();
        datain=new DataInputStream(dataSoc.getInputStream());
        dataout=new DataOutputStream(dataSoc.getOutputStream());
        String filename=dinput.readUTF();
        String dir=System.getProperty("user.dir");
        File deldir = new File(dir, filename);
        File[] AllIn = deldir.listFiles();
        int countFile=0;
        int countDir=0;
        for(File file : AllIn) {
            if(file.isDirectory()) {
                countDir++;
            } else
                countFile++;
        }
        doutput.writeUTF(String.valueOf(countDir));
        doutput.writeUTF(String.valueOf(countFile));
        String Ok=dinput.readUTF();
        if(Ok.compareTo("YES")==0){
            deleteDirr(deldir);
            doutput.writeUTF("true");
        }else{
            doutput.writeUTF("false");
        }
    }

    void setNewDir() throws Exception{
        DataInputStream datain;
        DataOutputStream dataout;
        dataSoc=DataSoc.accept();
        datain=new DataInputStream(dataSoc.getInputStream());
        dataout=new DataOutputStream(dataSoc.getOutputStream());
        String dir = dinput.readUTF();
        File newdir = new File(dir);
        if (!newdir.exists()){
            newdir.mkdir();
            doutput.writeUTF("true");
        }
        else {
            doutput.writeUTF("false");
        }
    }
    void deleteDir() throws Exception{
        DataInputStream datain;
        DataOutputStream dataout;
        dataSoc=DataSoc.accept();
        datain=new DataInputStream(dataSoc.getInputStream());
        dataout=new DataOutputStream(dataSoc.getOutputStream());
        String filename=dinput.readUTF();
        String dir=System.getProperty("user.dir");
        File deldir = new File(dir, filename);
        System.out.println(deldir.length());
        File[] listofFiles = deldir.listFiles();

        if (listofFiles.length>0){
            doutput.writeUTF("false");
        }
        else{
            deldir.delete();
            doutput.writeUTF("true");
        }
    }

    public void run()
    {

        while(true)
        {
            try
            {
                String Command=dinput.readUTF();

                if(Command.compareTo("GET")==0)
                {
                    System.out.println("\tGET Command Received ...");
                    ReceiveFile();
                    continue;
                }
                else if(Command.compareTo("SEND")==0)
                {
                    System.out.println("\tSEND Command Receiced ...");
                    SendFile();
                    continue;
                }
                else if(Command.compareTo("DISCONNECT")==0)
                {
                    System.out.println("\tDisconnect Command Received ...");
                    doutput.flush();
                    ClientSoc.close();
                    Disconnect();
                    //System.exit(1);
                }
                else if (Command.compareTo("PWD")==0){
                    System.out.println("\tPWD Command Received ...");
                    Pwd();
                    continue;
                }
                else if (Command.compareTo("getFiles")==0){
                    System.out.println("\tgetFiles Command Received ...");
                    getFiles();
                    continue;
                }
                else if (Command.compareTo("getList")==0){
                    System.out.println("\tgetList command Reveived ...");
                    getList();
                    continue;
                }
                else if (Command.compareTo("CD")==0){
                    System.out.println("\tCD Command Received ...");
                    setCD();
                    continue;
                }
                else if (Command.compareTo("Delete")==0){
                    System.out.println("\tDelete Command Received ...");
                    deleteFile();
                    continue;
                }
                else if (Command.compareTo("mkdir")==0){
                    System.out.println("\tMkdir Command Received ...");
                    setNewDir();
                    continue;
                }
                else if (Command.compareTo("getDir")==0){
                    System.out.println("\tgetdir Command Received ...");
                    getDir();
                    continue;
                }
                else if (Command.compareTo("rmdir")==0){
                    System.out.println("\tRmdir Command Received ...");
                    deleteDir();
                    continue;
                }
                else if (Command.compareTo("rmdirAll")==0){
                    System.out.println("\tRmdir Command Received All...");
                    delAllInDir();
                    continue;
                }
            }
            catch(Exception ex)
            {
            }
        }
    }

}