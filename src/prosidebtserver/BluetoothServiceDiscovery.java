/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package prosidebtserver;

/**
 *
 * @author vs
 */
import java.io.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;
import java.net.*;
import java.util.*;
//import java.util.regex.*;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.obex.*;
import com.intel.bluetooth.RemoteDeviceHelper;
//import javax.bluetooth.RemoteDeviceHelper;

/**
 *
 * @author vs
 */
public class BluetoothServiceDiscovery implements DiscoveryListener{

    //object used for waiting
    private static Object lock=new Object();

    //vector containing the devices discovered
    private static Vector vecDevices=new Vector();
    public static Vector vecDevicesAvail=new Vector();
    public static Vector vecDevicesAvailAddr=new Vector();
    public static Vector vecDevicesAvailName=new Vector();
    public static Vector vecDevicesSent=new Vector();

    private static String connectionURL=null;
    public static BluetoothServiceDiscovery bluetoothServiceDiscovery;
    public static LocalDevice localDevice;
    public static DiscoveryAgent agent;

    private char EOF = (char)0x00;

    public static boolean valid = true;

    /**
     * Entry point.
     */
    public void start() throws IOException {
        // Message terminator
        bluetoothServiceDiscovery = new BluetoothServiceDiscovery();
        //display local device address and name
        localDevice = LocalDevice.getLocalDevice();

        System.out.println("Address: "+localDevice.getBluetoothAddress());
        System.out.println("Name: "+localDevice.getFriendlyName());

    }


    public String processMsg(String msg) throws IOException {
        if(msg.trim().equals("LISTA")){
            searchDevices();
            String retStr;
            retStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><quiosque id=\"1\"><botoes id=\"0\">";
            System.out.println("SIZE: " + vecDevicesAvailName.size());
            
            byte[] utf8;
            String str;

            int real,maxsize;
            // limitação de 10 devices
            maxsize = vecDevicesAvailName.size();
            if(maxsize > 10)
                maxsize = 10;
            for (int i = 0; i < maxsize; i++) {
                str = vecDevicesAvailName.get(i).toString();
                utf8 = str.getBytes("UTF-8");
                str = new String(utf8, "ISO-8859-1");
                real = i + 1;
                retStr += "<bot texto=\"" + str + "\" tipo=\"A\" idfila=\"" + real + "\" />";
            }
            retStr += "</botoes></quiosque>";
            return retStr;
        }
        if(msg.trim().matches("ENVIA\\s\\d\\s\\d")){
           String[] strArr = msg.split("\\s");
           //String devid = m.group();
           System.out.println("ENVIA DEVICE: " + strArr[1] + "Conteudo:" + strArr[2]);
           try{
                sendToDevice(Integer.parseInt(strArr[1]), Integer.parseInt(strArr[2]));
           }
            catch (Exception e)
            {
                System.out.println("Send to device error " + e.getMessage());
            }
        }
        return "";
    }

    public static void searchDevices() throws IOException{
        //find devices
        vecDevices.clear();
        vecDevicesAvail.clear();
        vecDevicesAvailAddr.clear();
        vecDevicesAvailName.clear();

        /* RETIRAR :: lista de equipamentos conhecidos a não enviar */
        vecDevicesSent.add("000D3C1E2A87");
        vecDevicesSent.add("0026687F0189");
        vecDevicesSent.add("001E52D1EDDF");
        vecDevicesSent.add("001F01025CC9");
        vecDevicesSent.add("00264AA1ADDE");
        vecDevicesSent.add("00031905B51F");
        vecDevicesSent.add("000319087693");

        agent = localDevice.getDiscoveryAgent();

        System.out.println("A iniciar pesquisa de equipamentos...");
        agent.startInquiry(DiscoveryAgent.GIAC, bluetoothServiceDiscovery);

        try {
            synchronized(lock){
                lock.wait();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }


        System.out.println("Pesquisa de equipamentos finalizada. ");

        //print all devices in vecDevices
        int deviceCount=vecDevices.size();

        if(deviceCount <= 0){
            System.out.println("Não foram encontrados equipamentos.");
        }
        else{
            //print bluetooth device addresses and names in the format [ No. address (name) ]
            System.out.println(deviceCount + " Equipamentos Bluetooth: ");
            int realpos=0;
            String devname, devaddr;
            devname = devaddr = "";
            for (int i = 0; i <deviceCount; i++) {
                if(!valid){
                    break;
                }
                // se já enviamos a este

                boolean deviceOk = true;
                RemoteDevice remoteDevice=(RemoteDevice)vecDevices.elementAt(i);

                if(vecDevicesSent.contains(remoteDevice)){
                    System.out.println("Pesquisa de device ignorada: "+i);
                    continue;
                }

                try{
                    //remoteDevice.
                    devname = remoteDevice.getFriendlyName(false);
                    devaddr = remoteDevice.getBluetoothAddress();
                    //System.out.println((i+1)+". "+remoteDevice.getBluetoothAddress()+" ("+remoteDevice.getFriendlyName(false)+")");
                } catch (IOException cantGetDeviceName) {
                    System.out.println("Nao se consegue sacar o nome:" + cantGetDeviceName.getMessage());
                    //vamos colocar na lista de devices com envio já efectuado
                    vecDevicesSent.add(remoteDevice);
                    deviceOk = false;
                } finally{
                    if(deviceOk && !vecDevicesAvail.contains(remoteDevice)){
                        vecDevicesAvail.add(realpos,remoteDevice);
                        vecDevicesAvailName.add(realpos, devname);
                        vecDevicesAvailAddr.add(realpos, devaddr);
                        realpos++;
                    }
                }

            }
        }

        // nova contagem
        deviceCount=vecDevicesAvail.size();

        System.out.println(deviceCount + " equipamentos validos");

        for (int i = 0; i < deviceCount; i++) {
            System.out.println((i+1)+". "+vecDevicesAvailAddr.get(i).toString()+" ("+vecDevicesAvailName.get(i).toString()+")");
        }

    }

    public static void sendToDevice(int index, int contentid) throws IOException{
        /*
        // para qual queremos enviar ??
        System.out.print("Choose the device to search for Obex Push service : ");
        BufferedReader bReader=new BufferedReader(new InputStreamReader(System.in));

        String chosenIndex=bReader.readLine();
        int index=Integer.parseInt(chosenIndex.trim());
        */
        connectionURL=null;

        if (index == 0) return;

        //check for obex service
        RemoteDevice remoteDevice=(RemoteDevice)vecDevicesAvail.elementAt(index-1);
        UUID[] uuidSet = new UUID[1];
        
        // OBEX Object Push Profile
        uuidSet[0]=new UUID("1105",true);
        
        // OBEX File Transfer Profile
        //uuidSet[0]=new UUID("1106",true);

        System.out.println("\nPesquisar serviço, equipamento numero "+index);
        agent.searchServices(null,uuidSet,remoteDevice,bluetoothServiceDiscovery);

        try {
            synchronized(lock){
                lock.wait();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(connectionURL==null){
            System.out.println("Device does not support Object Push.");
        }
        else{
            System.out.println("Device OK,URL:" + connectionURL);

            /*
            System.out.println("::Autentica");
            try{
                RemoteDeviceHelper.authenticate(remoteDevice, "0000");
            }catch(IOException Exauth){}
            */

            System.out.println("::Iniciar clientSession");
            ClientSession clientSession = (ClientSession) Connector.open(connectionURL,Connector.READ_WRITE);

            System.out.println("::Iniciar hsConnectReply");
            HeaderSet hsConnectReply = clientSession.connect(null);

            if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
                System.out.println("Failed to connect");
                return;
            }

            System.out.println("::Iniciar hsOperation");
            HeaderSet hsOperation = clientSession.createHeaderSet();

            /*
            // envia imagem
            File aFile = new File("C:\\tmp\\arvore.jpg");
            byte data[] = getBytesFromFile(aFile);
            hsOperation.setHeader(HeaderSet.NAME, "arvore.jpg");
            hsOperation.setHeader(HeaderSet.TYPE, "image/jpeg");
            hsOperation.setHeader(HeaderSet.LENGTH, new Long(data.length));
            */
            
            // envia texto
            //String fname = "C:\\Proside\\conteudos\\texto" + contentid + ".txt";
            String fname = "C:\\Proside\\conteudos\\texto0.txt";
            File aFile = new File(fname);
            byte data[] = getBytesFromFile(aFile);

            System.out.println("::Iniciar Headers");
            hsOperation.setHeader(HeaderSet.NAME, "proside.txt");
            hsOperation.setHeader(HeaderSet.TYPE, "text");
            //byte data[] = "ola mundo !!!".getBytes("iso-8859-1");
            
            //Create PUT Operation
            System.out.println("::Iniciar putOperation");
            Operation putOperation = clientSession.put(hsOperation);

            // Send some text to server
            System.out.println("::Iniciar openOutputStream");
            OutputStream os = putOperation.openOutputStream();
            os.write(data);
            os.close();
            putOperation.close();
            clientSession.disconnect(null);
            clientSession.close();
        }

    }

    public boolean sendToDeviceRandom() throws IOException{
        /*
        // enviar a random
        */
        int index=-1;
        int deviceCount=vecDevicesAvail.size();
        int deviceCountAddr=vecDevicesAvailAddr.size();

        if(deviceCount != deviceCountAddr){
            System.out.println("Numero de devices é diferente do numero de endereços");
            return false;
        }


        if(deviceCount <= 0){
            System.out.println("Não existem devices disponiveis para enviar");
            return false;
        }

        for (int i = 0; i <deviceCount; i++) {
            //if(!vecDevicesSent.contains(vecDevicesAvailAddr.get(i).toString())){
            if(!vecDevicesSent.contains(vecDevicesAvail.elementAt(i)) && !vecDevicesSent.contains(vecDevicesAvailAddr.get(i).toString())){
                // encontramos device que não foi enviado
                index = i+1;
                break;
            }
        }

        if(index == -1){
            // nao vamos mandar a ninguem
            System.out.println("Não vamos enviar a ninguem, já enviamos tudo ou não existem mais");
            return false;
        }
        else{
            // codigo de envio

            // adicionar à lista o device que vamos enviar
            vecDevicesSent.add(vecDevicesAvailAddr.get(index-1).toString());

            // vamos meter tambem o remotedevice para minimizar os subsequentes discovers
            RemoteDevice remoteDeviceTmp=(RemoteDevice)vecDevicesAvail.elementAt(index-1);
            vecDevicesSent.add(remoteDeviceTmp);

            try{
                System.out.println("Enviar ao index " + index + " NOME:" + vecDevicesAvailName.get(index-1).toString());
                sendToDevice(index,0);
            }catch(IOException Ex){}

            
            return true;
        }
    }

    /**
     * Called when a bluetooth device is discovered.
     * Used for device search.
     */
    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        //add the device to the vector
        if(!vecDevices.contains(btDevice)){
            vecDevices.addElement(btDevice);
        }
    }


    /**
     * Called when a bluetooth service is discovered.
     * Used for service search.
     */
    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        if(servRecord!=null && servRecord.length>0){
            connectionURL=servRecord[0].getConnectionURL(0,false);
        }
        synchronized(lock){
            lock.notify();
        }
    }


    /**
     * Called when the service search is over.
     */
    public void serviceSearchCompleted(int transID, int respCode) {
        synchronized(lock){
            lock.notify();
        }
    }


    /**
     * Called when the device search is over.
     */
    public void inquiryCompleted(int discType) {
        synchronized(lock){
            lock.notify();
        }

    }//end method

  public static String getContents(File aFile) {
    //...checks on aFile are elided
    StringBuilder contents = new StringBuilder();

    try {
      //use buffering, reading one line at a time
      //FileReader always assumes default encoding is OK!
      BufferedReader input =  new BufferedReader(new FileReader(aFile));
      try {
        String line = null; //not declared within while loop
        /*
        * readLine is a bit quirky :
        * it returns the content of a line MINUS the newline.
        * it returns null only for the END of the stream.
        * it returns an empty String if two newlines appear in a row.
        */
        while (( line = input.readLine()) != null){
          contents.append(line);
          contents.append(System.getProperty("line.separator"));
        }
      }
      finally {
        input.close();
      }
    }
    catch (IOException ex){
      ex.printStackTrace();
    }
    return contents.toString();
  }

  public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        // Get the size of the file
        long length = file.length();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }

        // Close the input stream and return bytes
        is.close();
         return bytes;
    }


}//end class 