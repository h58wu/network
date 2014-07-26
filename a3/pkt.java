class pkt_HELLO{
	 int router_id; /* id of the router who sends the HELLO PDU */
	 int link_id; /* id of the link through which it is sent */
}



class pkt_LSPDU{
	 int sender; /* sender of the LS PDU */
	 int router_id; /* router id */
	 int link_id; /* link id */
	 int cost; /* cost of the link */
	 int via; /* id of the link through which the LS PDU is sent */
}

class pkt_INIT{
	 int router_id; /* id of the router that send the INIT PDU */
}

class link_cost{
	  int link; /* link id */
	  int cost; /* associated cost */
	  public link_cost(int a, int b){
	  	link = a;
	  	cost = b;
	  }
	  public link_cost(){
		  link =0;
		  cost = -1;
	  }

}

class circuit_DB{
	int nbr_link; /* number of links attached to a router */
	link_cost[] linkcost = new link_cost[5];
	/* we assume that at most NBR_ROUTER links are attached to each router */
	public circuit_DB(int link_num, link_cost[] linkcost){
		nbr_link = link_num;
		this.linkcost = linkcost;  
	}
	
	public circuit_DB(){
		nbr_link = 0;
		for(int i = 0; i < 5;i++){
			linkcost[i] = new link_cost();
		}
	}
}


class RIB_MAP{
	int cost;
	int start_point;
	public RIB_MAP(){
		cost = Integer.MAX_VALUE;
		start_point = -1;
	}
}