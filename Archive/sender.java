import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Timer;

public class sender implements ActionListener{
	//delcare section
	private static ArrayList<packet>	p_list;
	private static BufferedWriter log_seq, log_ack;
	private static InetAddress emu_Addr;
	private static int port;
	private static DatagramSocket recvSocket, sendSocket;
	private static volatile javax.swing.Timer timer;
	private static boolean eot_acked;
	private static String file_name;
	private static volatile int startPoint;

	//constructor
	sender(){
		timer = new javax.swing.Timer(200,this);
		recv_thread recvThd = new recv_thread();
		recvThd.start();
	}

	//packet file 
	public static void packet_file(){
		try{
			BufferedReader input = new BufferedReader(new FileReader(file_name));
			packet pack;
			int seq = 0;
			int result;
			char[] content = new char[500];
			while((result = input.read(content,0,500)) != -1){
				pack = packet.createPacket(seq,String.valueOf(content,0,result));
				seq++;
				p_list.add(pack);
			}
			input.close();
		//	System.err.println("pack size:		" + p_list.size());
		} catch (Exception ex){
			System.err.println("packet_file err");
			System.exit(1);
		}
	}
	//send helper
	public static synchronized void send_help(packet pack) throws IOException{
		byte[] data = pack.getUDPdata();
		try{
			sendSocket.send(new DatagramPacket(data,data.length,emu_Addr,port));
		}	catch(Exception e){
			System.err.println("error");
			System.exit(1);
		}
		if(pack.getType() == 1){
			log_seq.write(Integer.toString(pack.getSeqNum()));
			log_seq.newLine();
		}
	}
	// multi-thread for getting ack back
	class recv_thread extends Thread{
		@Override
		public void run(){
				try{			
					packet pack;
					super.run();
					while(!eot_acked){
						byte[] un_parsed = new byte[512];
						recvSocket.receive(new DatagramPacket(un_parsed,un_parsed.length));
						pack = packet.parseUDPdata(un_parsed);
						if(pack.getType() == 0){
							log_ack.write(Integer.toString(pack.getSeqNum()));
								log_ack.newLine();
							if(check(startPoint%32,pack.getSeqNum()) ){
							//	System.err.println("acked  startPoint  "+ pack.getSeqNum() +" "+startPoint%32);
								int tmp = startPoint;
								startPoint = 1+ startPoint + distance_p(startPoint%32,pack.getSeqNum());
								timer.restart();
								for(int i = tmp ; (i +10) < p_list.size() && i < (startPoint - tmp); i++){
									send_help(p_list.get(i+10));
								}
							}
						//	System.err.println("actual startPoint:	"+ startPoint);
							if(startPoint >= p_list.size()) eot_acked = true;
						}
					}
					log_ack.close();
				} catch (Exception ex){
					System.err.println("thread err");
					System.exit(1);
				}
		}
	}

	public static boolean check(int a, int b){
		if(a + 10 < 32){
			if(a < b && ((a+10)>b)){
					return true;
			}
		}
		else{
			if(b > a){
				return true;
			}
			if(b <= (a+10)%32) return true; 
		}
		return false;
	} 

	public static int distance_p(int a, int b){
		if(a+10<32){
			return b -a ;
		}
		if(b>= 9){
			return b - a;
		}
		else{
			return (31-a) + b;
		}
	}


	// first send
	public static void first_send(){
		try{
			timer.start();
			for(int i = 0 ; i < 10 && i < p_list.size(); i++){
				send_help(p_list.get(startPoint + i));
			}
			while(!eot_acked){
				Thread.yield();
			}
			send_help(packet.createEOT(1));
			byte[] un_parsed = new byte[512];
			recvSocket.receive(new DatagramPacket(un_parsed,un_parsed.length));
			timer.stop();
			log_seq.close();
			recvSocket.close();
			sendSocket.close();
		} catch (Exception ex){
			System.err.println("first send err");
			System.exit(1);
		}
	}

	@Override
	public synchronized void actionPerformed(ActionEvent e){
		try{
			for(int i = 0 ; i < 10 && ((i + startPoint) < p_list.size()); i++){
				send_help(p_list.get(startPoint + i));
			}
		} catch(Exception ex){
			System.err.println("ap err");
			System.exit(1);
		}
	}

	//main function
	public static void main(String[] args){
		try{
			if(args.length != 4){
				System.err.println("Expected 4 args");
				System.exit(1);
			}
			p_list = new ArrayList<packet>();
			file_name = args[3];
			sendSocket = new DatagramSocket();
			recvSocket = new DatagramSocket(Integer.parseInt(args[2]));
			emu_Addr = InetAddress.getByName(args[0]);
			port = Integer.parseInt(args[1]);
			log_seq = new BufferedWriter(new FileWriter("seqnum.log"));
			log_ack = new BufferedWriter(new FileWriter("ack.log"));
			packet_file();
			eot_acked = false;
			sender instance_send = new sender();
			instance_send.first_send();
		} catch (Exception ex){
			System.err.println("main err");
			System.exit(1);
		}
	}
}