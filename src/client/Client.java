package client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("127.0.0.1", 12345);

            BufferedReader keyboardInputBR = new BufferedReader(new InputStreamReader(System.in));
            DataOutputStream fromClient = new DataOutputStream(socket.getOutputStream());
            BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            File arquivo = new File("/diretorio/" + "nome do arquivo");
            FileInputStream in = new FileInputStream(arquivo);
            OutputStream out = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(out);
            BufferedWriter writer = new BufferedWriter(osw);
            writer.write(arquivo.getName() + "\n");
            writer.flush();
            int tamanho = 4096; // buffer de 4KB  
            byte[] buffer = new byte[tamanho];
            int lidos = -1;
            while ((lidos = in.read(buffer, 0, tamanho)) != -1) {
                out.write(buffer, 0, lidos);
            }

            //envio
            new InputHandler(keyboardInputBR, fromClient, socket).start();

            //recebimento
            new OutputHandler(fromServer, socket).start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class InputHandler extends Thread {

    BufferedReader keyboardInputBR;
    DataOutputStream fromClient;
    Socket cliente;

    public InputHandler(BufferedReader br, DataOutputStream os, Socket cliente) {
        this.keyboardInputBR = br;
        this.fromClient = os;
        this.cliente = cliente;
    }

    @Override
    public void run() {
        try {
            while (!this.cliente.isClosed()) {
                keyboardInputBR = new BufferedReader(new InputStreamReader(System.in));
                String mensagemEnviar = keyboardInputBR.readLine();

                if (mensagemEnviar != null) {
                    fromClient.writeBytes(mensagemEnviar + "\n");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(InputHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class OutputHandler extends Thread {

    Socket cliente;
    BufferedReader fromServer;

    public OutputHandler(BufferedReader br, Socket cliente) {
        this.fromServer = br;
        this.cliente = cliente;
    }

    @Override
    public void run() {
        try {
            while (!this.cliente.isClosed()) {
                String mensagemRecebida = fromServer.readLine();
                if (mensagemRecebida != null) {
                    System.out.println(mensagemRecebida);
                    if (mensagemRecebida.toLowerCase().contains("voce acabou de se desconectar")) {
                        System.exit(0);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(InputHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
