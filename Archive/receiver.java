import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.*;

public class receiver{
	// declare section
	private static DatagramSocket	recvSocket, sendSocket;
	private static DatagramPacket	recvPacket,	sendPacket;
	private static InetAddress		emu_Addr;
	private static int 			seq,		port;
	private	static BufferedWriter	log_file,	transfile;

	//constructor
	receiver(){
		//nothing just for creating instance
	}

	// receive helper
	public static void recv_help(){
		try{
			byte[] un_parsed_package = new byte[512];
			while(true){
				recvPacket = new DatagramPacket(un_parsed_package,un_parsed_package.length);
				recvSocket.receive(recvPacket);
				packet pack = packet.parseUDPdata(un_parsed_package);
				int p_type = pack.getType();
				if(p_type == 1){
					log_file.write(Integer.toString(pack.getSeqNum()));
					log_file.newLine();
					if(seq%32 == pack.getSeqNum()){
						seq++;
						byte[] recv_file_content = pack.getData();
						transfile.write(new String(recv_file_content));
						ack(packet.createACK(pack.getSeqNum()));
					}
					else{
						ack(packet.createACK((seq - 1)%32));
					}
				}
				else{
					ack(packet.createEOT(pack.getSeqNum()));
					break;
				}
			}
			log_file.close();
			transfile.close();
		} catch (Exception ex){
			System.exit(1);
		}

	}

	//ack helper
	public static void ack(packet pack){
		try{
			byte[] un_parse = pack.getUDPdata();
			sendSocket.send(new DatagramPacket(un_parse,un_parse.length,emu_Addr,port));
		} catch (Exception ex){
			System.exit(1);
		}
	}
	
	//main function:
	public static void main(String[] args){
		try{
			if(args.length != 4){
				System.err.println("Expected 4 args");
				System.exit(1);
			}
			receiver instance_recv = new receiver();
			emu_Addr = InetAddress.getByName(args[0]);
			port	 = Integer.parseInt(args[1]);
			recvSocket = new DatagramSocket(Integer.parseInt(args[2]));
			sendSocket = new DatagramSocket();
			seq = 0;
			log_file = new BufferedWriter(new FileWriter("arrival.log"));
			transfile = new BufferedWriter(new FileWriter(args[3]));

			receiver.recv_help();
		} catch (Exception ex){
			System.exit(1);
		}
	}
}