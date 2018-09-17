package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server extends Thread {

    public static void main(String args[]) {
//        Map<String, Socket> clientes = new HashMap<String, Socket>();
        List<Modelo> clientes = new ArrayList<Modelo>();
        
        try {
            ServerSocket server = new ServerSocket(12345);
            System.out.println("@SERVIDOR: Iniciado na porta " + server.getLocalPort());
            
            while(true){
                Socket cliente = server.accept();
                String nickDefault = "S" + cliente.getPort();
                clientes.add(new Modelo(nickDefault, cliente));
                new ClientHandler(nickDefault, cliente, clientes).start();
            }

        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }   
}

class Modelo {
    public String Nickname;
    public Socket Cliente;
    
    public Modelo(String nickname, Socket cliente){
        this.Nickname = nickname;
        this.Cliente = cliente;
    }
}

class ClientHandler extends Thread {
    
    List<Modelo> clientes;
    Socket cliente;
    
    public ClientHandler(String title, Socket cliente, List<Modelo> clientes){
        super(title);
        this.cliente = cliente;
        this.clientes = clientes;
    }
    
    @Override
    public void run() {
        System.out.println("@SERVIDOR: Cliente conectado de: " + this.cliente.getInetAddress().getHostAddress() + " " + this.cliente.getPort());
        enviaMsgAll("@SERVIDOR: Cliente " + getNicknameByClient(this.cliente) + " conectado de: "
                + this.cliente.getInetAddress().getHostAddress() + " " + this.cliente.getPort(), this.cliente, false);
        try {
            BufferedReader fromCliente = new BufferedReader(new InputStreamReader(this.cliente.getInputStream()));
                
            while(!this.cliente.isClosed()){
                if(fromCliente.ready()){
                    String msgDoCliente = fromCliente.readLine();
                    System.out.println("@SERVIDOR: Mensagem recebida: " + msgDoCliente);

                    trataInput(msgDoCliente);
                }
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void trataInput(String mensagem){
        
        if(mensagem == null){
            System.out.println("ALGO ERRADO");
        } 
        else if(mensagem.toUpperCase().startsWith("MSG")){
            enviaMsgAll(mensagem, this.cliente, true);
        }
        else if(mensagem.toUpperCase().startsWith("PRIVATE")){
            enviaMsgPrivado(mensagem, this.cliente);
        }
        else if(mensagem.toUpperCase().startsWith("NICK")){
            trataNick(mensagem, this.cliente);
        }
        else if(mensagem.toUpperCase().startsWith("EXIT")){
            desconectaCliente(this.cliente);
        }
        else{
            
        }        
    }
    
    private void enviaMsgAll(String mensagem, Socket quemEnviou, Boolean apareceNick){
        String nickname = getNicknameByClient(quemEnviou).toUpperCase();
        
        if(apareceNick){
            mensagem = mensagem.substring("MSG: ".length());
        }
        
        for(Modelo obj : this.clientes)
        {
            Socket clienteLoop = obj.Cliente;
            if(clienteLoop != quemEnviou){
                try {              
                    DataOutputStream toCliente = new DataOutputStream(clienteLoop.getOutputStream());
                    
                    System.out.println("@SERVIDOR: Mensagem enviada para TODOS: " + mensagem);
                    if(apareceNick){
                        toCliente.writeBytes(nickname + ">> " + mensagem + "\n");
                    }else{
                        toCliente.writeBytes(mensagem + "\n");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    private void enviaMsgPrivado(String mensagem, Socket quemEnviou){
        String nickname = getNicknameByClient(quemEnviou).toUpperCase();
        
        mensagem = mensagem.substring("PRIVATE: ".length());
        String[] partes = mensagem.split(">");
        String nicknameDestino = partes[0].trim().toUpperCase();
        
        try {              
            DataOutputStream toCliente = new DataOutputStream(getClientByNickname(nicknameDestino).getOutputStream());

            System.out.println("@SERVIDOR: Mensagem enviada para " + nicknameDestino.toUpperCase() + ": " + mensagem);
            toCliente.writeBytes("(p)" + nickname + ">> " + partes[1].trim() + "\n");
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void trataNick(String mensagem, Socket quemSolicitou)
    {
        String novoNickname = mensagem.substring("NICK: ".length());
        Modelo modelo = getModeloByClient(quemSolicitou);
        
        DataOutputStream toCliente;
        try {
            toCliente = new DataOutputStream(quemSolicitou.getOutputStream());
            if(getClientByNickname(novoNickname) == null){
                System.out.println("@SERVIDOR: Nickname de " + modelo.Nickname + " alterado para: " + novoNickname);
                enviaMsgAll("@SERVIDOR: Nickname de " + modelo.Nickname + " alterado para: " + novoNickname, quemSolicitou, false);
                toCliente.writeBytes("@SERVIDOR: Nickname alterado para: " + novoNickname + "\n");
                modelo.Nickname = novoNickname;
            }else{
                System.out.println("@SERVIDOR: Não foi possível alterar o nick de " + modelo.Nickname + " para " + novoNickname 
                        + " pois já existe.");
                toCliente.writeBytes("@SERVIDOR: Não é possível alterar seu nick para: " + novoNickname + ". Este nick está em uso. \n");
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void desconectaCliente(Socket quemSaiu){
        DataOutputStream toCliente;
        try {
            Modelo modelo = getModeloByClient(quemSaiu);
            System.out.println("@SERVIDOR: " + modelo.Nickname + " acabou de se desconectar.");
            enviaMsgAll("@SERVIDOR: " + modelo.Nickname + " acabou de se desconectar.", quemSaiu, false);
            toCliente = new DataOutputStream(quemSaiu.getOutputStream());
            toCliente.writeBytes("@SERVIDOR: Voce acabou de se desconectar." + "\n");
            this.clientes.remove(modelo);
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
            
    }
    
    private String getNicknameByClient(Socket client){
        Modelo modelo = this.clientes.stream().filter(cliente -> cliente.Cliente.equals(client)).findFirst().orElse(null);
        if(modelo != null){
            return modelo.Nickname;
        }else{
            return null;
        }
    }
    
    private Socket getClientByNickname(String nickname){
        Modelo modelo = this.clientes.stream().filter(cliente -> cliente.Nickname.toUpperCase().equals(nickname.toUpperCase())).findFirst().orElse(null);
        if(modelo != null){
            return modelo.Cliente;
        }else{
            return null;
        }
    }
    
    private Modelo getModeloByClient(Socket client){
        return this.clientes.stream().filter(cliente -> cliente.Cliente.equals(client)).findFirst().orElse(null);
    }
}
