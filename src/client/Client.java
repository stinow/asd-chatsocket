package client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client
{
    public static void main(String[] args)
    {
       try {
            Socket socket = new Socket("127.0.0.1", 12345);

            BufferedReader keyboardInputBR = new BufferedReader(new InputStreamReader(System.in));
            DataOutputStream fromClient = new DataOutputStream(socket.getOutputStream());
            BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));         
            
            //envio
            new InputHandler(keyboardInputBR, fromClient, socket).start();

            //recebimento
            new OutputHandler(fromServer, socket).start();
        } 
        catch (UnknownHostException e) 
        {
            e.printStackTrace();
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        } 
    }
}

class InputHandler extends Thread{
    BufferedReader keyboardInputBR;
    DataOutputStream fromClient;
    Socket cliente;
    
    public InputHandler(BufferedReader br, DataOutputStream os, Socket cliente){
        this.keyboardInputBR = br;
        this.fromClient = os;
        this.cliente = cliente;
    }
    
    @Override
    public void run(){
        try {
            while(!this.cliente.isClosed()){
                keyboardInputBR = new BufferedReader(new InputStreamReader(System.in));
                String mensagemEnviar = keyboardInputBR.readLine();
                
                if(mensagemEnviar != null){
                    fromClient.writeBytes(mensagemEnviar + "\n");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(InputHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class OutputHandler extends Thread{
    Socket cliente;
    BufferedReader fromServer;
    
    public OutputHandler(BufferedReader br, Socket cliente){
        this.fromServer = br;
        this.cliente = cliente;
    }
    
    @Override
    public void run(){
        try {
            while(!this.cliente.isClosed()){
                String mensagemRecebida = fromServer.readLine();
                if(mensagemRecebida != null){
                    System.out.println(mensagemRecebida);
                    if(mensagemRecebida.toLowerCase().contains("voce acabou de se desconectar")){
                        System.exit(0);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(InputHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}