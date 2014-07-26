import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.util.*;

public class router{
	private static int router_id;
	private static int nse_port;
	private int router_port;
	private static InetAddress nse_host;
	private static HashMap<Integer,circuit_DB> topo_list;
	private static HashMap<Integer,RIB_MAP>		RIB;
	private static BufferedWriter log_file;
	private static DatagramSocket socket;
	public static int[] aL;
	private  static Queue<Integer> dij_que;

	//constructor
	public router(int router_id, int nse_port, int router_port, InetAddress nse_host) throws Exception{
		this.router_id = router_id;
		this.nse_port = nse_port;
		this.router_port = router_port;
		this.nse_host = nse_host;
		this.socket = new DatagramSocket(router_port);
		topo_list = new HashMap<Integer,circuit_DB>();
		aL = new int[25];
		RIB = new HashMap<Integer,RIB_MAP>();
		for(int i = 1 ; i <= 5; i++){
			if(i == router_id) continue;
			topo_list.put(i, new circuit_DB());
		}
		for(int i = 1; i <=5; i++){
			RIB.put(i, new RIB_MAP());
		}
		dij_que = new LinkedList<Integer>();
	}

	public static void sendInit() throws IOException{
		ByteBuffer init_buffer = ByteBuffer.allocate(4);
        init_buffer.order(ByteOrder.LITTLE_ENDIAN);
        init_buffer.putInt(router_id);
        byte[] data = init_buffer.array();
        DatagramPacket packet = new DatagramPacket(data, data.length, nse_host, nse_port);
        socket.send(packet);
        log_file.write("R"+router_id+" sends the INIT packet.");
        log_file.newLine();
	}
	public static void recieveDB() throws IOException{
		byte[] recieve_buffer = new byte[64];
		DatagramPacket recieve_packet = new DatagramPacket(recieve_buffer,recieve_buffer.length);
		socket.receive(recieve_packet);
		log_file.write("R"+router_id+" receives circuit database.");
		log_file.newLine();
		byte[] data = recieve_packet.getData();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int nbr_link = buffer.getInt();
		link_cost[] linkcost= new link_cost[5];
		for(int i = 0; i < nbr_link;i++){
			linkcost[i] = new link_cost(buffer.getInt(),buffer.getInt());
		}
		topo_list.put(router_id, new circuit_DB(nbr_link, linkcost));
		
	}




	public static void start() throws IOException{
		sendInit();
		recieveDB();
		sendHello();
		build_topology();
	}




	private static void sendHello() throws IOException {
		// TODO Auto-generated method stub
		for(int i = 0; i < topo_list.get(router_id).nbr_link;i++){
			ByteBuffer init_buffer = ByteBuffer.allocate(8);
	        init_buffer.order(ByteOrder.LITTLE_ENDIAN);
	        init_buffer.putInt(router_id);
	        init_buffer.putInt(topo_list.get(router_id).linkcost[i].link);
	        byte[] data = init_buffer.array();
	        DatagramPacket packet = new DatagramPacket(data, data.length, nse_host, nse_port);
	        socket.send(packet);
	        log_file.write("R"+router_id+" sends the Hello packet.");
	        log_file.newLine();
		}
	}

	private static void build_topology() throws IOException {
		// TODO Auto-generated method stub
		while(true){
			byte[] recieve_buffer = new byte[20];
			DatagramPacket recieve_packet = new DatagramPacket(recieve_buffer,recieve_buffer.length);
			socket.receive(recieve_packet);
			log_file.newLine();
			byte[] data = recieve_packet.getData();
	        ByteBuffer buffer = ByteBuffer.wrap(data);
	        buffer.order(ByteOrder.LITTLE_ENDIAN);
	        if(recieve_packet.getLength() == 8){
	        	process_hello(buffer);
	        }
	        else{
	        	process_lspdu(buffer);
	        }
		}
		
	}

	private static void process_lspdu(ByteBuffer buffer) throws IOException {
		// TODO Auto-generated method stub
		int lspdu_sender = buffer.getInt();
		int lspdu_router_id = buffer.getInt();
		int lspdu_link_id = buffer.getInt();
		int lspdu_cost = buffer.getInt();
		int lspdu_via = buffer.getInt();
		for(int i = 0; i < 5; i++){
			if(topo_list.get(lspdu_router_id).linkcost[i].link == lspdu_link_id) return;
		}
		topo_list.get(lspdu_router_id).linkcost[topo_list.get(lspdu_router_id).nbr_link].link = lspdu_link_id;
		topo_list.get(lspdu_router_id).linkcost[topo_list.get(lspdu_router_id).nbr_link].cost = lspdu_cost;
		topo_list.get(lspdu_router_id).nbr_link = topo_list.get(lspdu_router_id).nbr_link+1;
		log_file.write("R"+router_id+" receives LSPDU from R"+lspdu_sender+"\n");
		for(int i = 0; i < topo_list.get(router_id).nbr_link;i++){
			int temp_id = topo_list.get(router_id).linkcost[i].link;
			if(temp_id == lspdu_via || aL[temp_id] != 1) continue;
			ByteBuffer init_buffer = ByteBuffer.allocate(20);
	        init_buffer.order(ByteOrder.LITTLE_ENDIAN);
	        init_buffer.putInt(router_id);
	        init_buffer.putInt(lspdu_router_id);
	        init_buffer.putInt(lspdu_link_id);
	        init_buffer.putInt(lspdu_cost);
	        init_buffer.putInt(temp_id);
	        byte[] data = init_buffer.array();
	        DatagramPacket packet = new DatagramPacket(data, data.length, nse_host, nse_port);
	        socket.send(packet);
	        log_file.write("R"+router_id+" sends the  LSPDU packet.");
	        log_file.newLine();
		}
		conpute_dijstra();
		print();
		log_file.flush();
	}

	private static void print() throws IOException {
		log_file.write("# Topology\n");
		for(int i = 1; i <= 5; i++){
			int tmp_nbr = topo_list.get(i).nbr_link;
			log_file.write("R"+router_id+"->R"+i+" nbr link"+ tmp_nbr+"\n");
			for(int j = 0; j < tmp_nbr;j++){
				log_file.write("R" + router_id+"->R"+i+" link "+topo_list.get(i).linkcost[j].link+" cost "+topo_list.get(i).linkcost[j].cost+"\n");
			}
			
		}
		log_file.write("# RIB \n");
		log_file.write("R" + router_id+"->R1-> "+ RIB.get(1).cost +"\n");
		log_file.write("R" + router_id+"->R2-> "+ RIB.get(2).cost+"\n");
		log_file.write("R" + router_id+"->R3-> "+ RIB.get(3).cost+"\n");
		log_file.write("R" + router_id+"->R4-> "+ RIB.get(4).cost+"\n");
		log_file.write("R" + router_id+"->R5-> "+ RIB.get(5).cost+"\n");
	}

	private static void conpute_dijstra() {
		// TODO Auto-generated method stub
		dij_que.add(router_id);
		RIB.get(router_id).cost = 0;
		for(int i = 1; i <= 5;i++){
			if(i == router_id ) continue;
			int temp_return = isneighbour(router_id,i);
			RIB.get(i).cost = temp_return;
			RIB.get(i).start_point = router_id;
		}
		while(dij_que.size() <= 6){
			int tmp = choosemin();
			if(tmp == -1) break;
			dij_que.add(tmp);
			for(int i =1;i<=5;i++){
				if(dij_que.contains(i)) continue;
				int result = isneighbour(tmp,i);
				if(result == Integer.MAX_VALUE){
					continue;
				}
				if((RIB.get(tmp).cost + result) < RIB.get(i).cost){
					RIB.get(i).cost = RIB.get(tmp).cost + isneighbour(tmp,i);
					RIB.get(i).start_point = tmp;
				}
			}
			
		}
		dij_que = new LinkedList<Integer>();
	}
	
	
	
	private static int choosemin() {
		int tmp = Integer.MAX_VALUE;
		int id = -1;
		for(int i = 1; i <= 5; i++){
			if(RIB.get(i).cost <= tmp && !dij_que.contains(i)){
				tmp = RIB.get(i).cost;
				id = i;
			}
		}
		return id;
	}

	private static int isneighbour(int own, int b) {
		// TODO Auto-generated method stub
		circuit_DB temp_DB = topo_list.get(own);
		circuit_DB temp_DB_b = topo_list.get(b);
		for(int i = 0; i < temp_DB.nbr_link;i++){
			for(int j = 0; j <temp_DB_b.nbr_link;j++){
				if(temp_DB.linkcost[i].link == temp_DB_b.linkcost[j].link){
					return temp_DB.linkcost[i].cost;
				}
			}
			
		}
		return Integer.MAX_VALUE;
	}

	private static int findneighbour(int curr_id, int link_id){
		for(int i = 1; i <= 5;i++){
			if(i == curr_id) continue;
			circuit_DB temp_DB = topo_list.get(i);
			for(int j = 0; j < temp_DB.nbr_link;j++){
				if(link_id == temp_DB.linkcost[j].link) return i;
			}
		}
		return -1;
	}

	private static void process_hello(ByteBuffer buffer) throws IOException {
		// TODO Auto-generated method stub
		log_file.newLine();
		int hello_router_id = buffer.getInt();
		int hello_link_id = buffer.getInt();
		log_file.write("R"+router_id+" receives Hello from R"+hello_router_id);
		log_file.newLine();
		aL[hello_link_id] = 1;
		for(int i = 0; i < topo_list.get(router_id).nbr_link;i++){
			int temp_id = topo_list.get(router_id).linkcost[i].link;
			int temp_cost = topo_list.get(router_id).linkcost[i].cost;
			ByteBuffer init_buffer = ByteBuffer.allocate(20);
	        init_buffer.order(ByteOrder.LITTLE_ENDIAN);
	        init_buffer.putInt(router_id);
	        init_buffer.putInt(router_id);
	        init_buffer.putInt(temp_id);
	        init_buffer.putInt(temp_cost);
	        init_buffer.putInt(hello_link_id);
	        byte[] data = init_buffer.array();
	        DatagramPacket packet = new DatagramPacket(data, data.length, nse_host, nse_port);
	        socket.send(packet);
	        log_file.write("R"+router_id+" sends the  LSPDU packet.");
	        log_file.newLine();
		}
	}

	public static void main(String args[]) throws Exception{
		if(args.length != 4){
			System.exit(1);
		}
		router inst_router = new router(Integer.parseInt(args[0]), Integer.parseInt(args[2]),
								Integer.parseInt(args[3]), InetAddress.getByName(args[1]));
		String log_file_name = new String("router");
		log_file_name = log_file_name + inst_router.router_id+".log";
		inst_router.log_file = new BufferedWriter(new FileWriter(log_file_name));
		router.start();

	}

}
