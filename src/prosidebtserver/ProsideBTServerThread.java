/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package prosidebtserver;

/**
 *
 * @author vs
 */

import java.net.*;
import java.io.*;


public class ProsideBTServerThread extends Thread {
    private Socket socket = null;

    public ProsideBTServerThread(Socket socket) {
        super("ProsideBTServerThread");
        this.socket = socket;
    }

    public void run() {
        char EOF = (char)0x00;

	try {
	    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

	    BufferedReader in = new BufferedReader(
				    new InputStreamReader(
				    socket.getInputStream()));

        String inputLine, outputLine;

        // discover class;
        BluetoothServiceDiscovery discorverbt = new BluetoothServiceDiscovery();
	    discorverbt.start();
        //KnockKnockProtocol kkp = new KnockKnockProtocol();
	    //outputLine = kkp.processInput(null);
	    //out.println(outputLine);
        boolean quit = false;
        while (!quit) {
            //(inputLine = in.readLine()) != null
            inputLine = in.readLine();
            if(inputLine == null)
                break;

            System.out.println("Recebido do flash:" + inputLine);
            //outputLine = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><quiosque id=\"1\"><botoes id=\"0\"><bot texto=\"APedra\" tipo=\"A\" idfila=\"1\" /><bot texto=\""+str+"\" tipo=\"A\" idfila=\"2\" /></botoes></quiosque>";
            outputLine = discorverbt.processMsg(inputLine);
            if(!outputLine.equals("")){
                outputLine = outputLine + EOF;
                out.print(outputLine);
                out.flush();
                System.out.println("Eviado para o flash:" + outputLine);
            }

            //outputLine = kkp.processInput(inputLine);
            //out.println(outputLine);
            //if (outputLine.equals("Bye"))
            //    break;
	    }
	    out.close();
	    in.close();
	    socket.close();

	} catch (IOException e) {
	    e.printStackTrace();
	}
  }
}
