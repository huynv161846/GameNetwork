package edu.hust.soict.huynv.network;

import edu.hust.soict.huynv.GenericSpaceShooter;
import edu.hust.soict.huynv.entities.PlayerMP;
import edu.hust.soict.huynv.network.packets.Packet;
import edu.hust.soict.huynv.network.packets.PacketLogin;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class GameServer extends Thread {

    private DatagramSocket socket;
    private GenericSpaceShooter gss;
    private List<PlayerMP> connectedPlayers = new ArrayList<>();

    public GameServer(GenericSpaceShooter gss) {
        this.gss = gss;
        try {
            this.socket = new DatagramSocket(1331);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.parsePacket(packet.getData(), packet.getAddress(), packet.getPort());
        }
    }

    private void parsePacket(byte[] data, InetAddress address, int port) {
        String message = new String(data).trim();
        Packet.PacketTypes type = Packet.lookupPacket(message.substring(0, 2));
        Packet packet = null;
        switch (type) {
            default:
            case INVALID:
                break;
            case LOGIN:
                packet = new PacketLogin(data);
                System.out.println("[" + address.getHostAddress() + ":" + port + "] "
                        + ((PacketLogin) packet).getUsername() + " has connected...");
                PlayerMP player = new PlayerMP(300, 800, gss, ((PacketLogin) packet).getUsername(), address, port);
                this.addConnection(player, (PacketLogin) packet);
                break;
            case DISCONNECT:
//                packet = new Packet01Disconnect(data);
//                System.out.println("[" + address.getHostAddress() + ":" + port + "] "
//                        + ((Packet01Disconnect) packet).getUsername() + " has left...");
//                this.removeConnection((Packet01Disconnect) packet);
                break;
            case MOVE:
//                packet = new Packet02Move(data);
//                this.handleMove(((Packet02Move) packet));
        }
    }

    public void addConnection(PlayerMP player, PacketLogin packet) {
        boolean alreadyConnected = false;
        for (PlayerMP p : this.connectedPlayers) {
            if (player.getUsername().equalsIgnoreCase(p.getUsername())) {
                if (p.ipAddress == null) {
                    p.ipAddress = player.ipAddress;
                }
                if (p.port == -1) {
                    p.port = player.port;
                }
                alreadyConnected = true;
            } else {
                // relay to the current connected player that there is a new
                // player
                sendData(packet.getData(), p.ipAddress, p.port);

                // relay to the new player that the currently connect player
                // exists
                packet = new PacketLogin(p.getUsername(), (int) p.x, (int) p.y);
                sendData(packet.getData(), player.ipAddress, player.port);
            }
        }
        if (!alreadyConnected) {
            this.connectedPlayers.add(player);
            GenericSpaceShooter.standardHandler.addEntity(player);
        }
    }
//
//    public void removeConnection(Packet01Disconnect packet) {
//        this.connectedPlayers.remove(getPlayerMPIndex(packet.getUsername()));
//        packet.writeData(this);
//    }

    public PlayerMP getPlayerMP(String username) {
        for (PlayerMP player : this.connectedPlayers) {
            if (player.getUsername().equals(username)) {
                return player;
            }
        }
        return null;
    }

    public int getPlayerMPIndex(String username) {
        int index = 0;
        for (PlayerMP player : this.connectedPlayers) {
            if (player.getUsername().equals(username)) {
                break;
            }
            index++;
        }
        return index;
    }

    public void sendData(byte[] data, InetAddress ipAddress, int port) {

        DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendDataToAllClients(byte[] data) {
        for (PlayerMP p : connectedPlayers) {
            sendData(data, p.ipAddress, p.port);
        }
    }

//    private void handleMove(Packet02Move packet) {
//        if (getPlayerMP(packet.getUsername()) != null) {
//            int index = getPlayerMPIndex(packet.getUsername());
//            PlayerMP player = this.connectedPlayers.get(index);
//            player.x = packet.getX();
//            player.y = packet.getY();
//            player.setMoving(packet.isMoving());
//            player.setMovingDir(packet.getMovingDir());
//            player.setNumSteps(packet.getNumSteps());
//            packet.writeData(this);
//        }
//    }

}
