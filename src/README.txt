1. Compile everything with 
			javac *.java
			
2. Choose a machine to act as bootstrap server and record its IP.
	
2. Run BootStrap Server in any machine using command
			java BootStrapServer <port number of bootstrap>
 	Enter port number of bootstrap server without the <>
 
3. Run Peer in different machines using command
			java Peer IP  <port number of bootstrap>
	IP is from step 1.  <port number of bootstrap> is entered in step 2
	
4. In peer program, keywords are
	join, insert, search and leave.
	 
5. Test leave at the very end !!!
6. Leave where a valid CAN Zone is created seems to work very well. Files transferred, zones merged and neighbors updated correctly.
7. Leave with irregular zone created assigns Peer to temporary hold vacated zone sucessfully and updates neighbors.
8. Handing over said temporary zone to new Peer will give results that I cannot guarantee.
9. AGAIN, test leave AFTER testing joins, inserts and searches.
10. If CAN appears to be broken, kill all processes and try afresh from step 1 with a fresh port.
