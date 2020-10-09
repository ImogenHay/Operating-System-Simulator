# Operating-System-Simulator
Simulates booting, multuprocessing, buffering, synchronisation and deadlock prevention in an OS. Includes process scheduling and memory management unit subsystems. 

Running ‘MainBooting.java’ will boot
the OS simulation and output ‘OS OPTIONS’ in the console and prompt further user input

You will be prompted to enter a configuration file.
There are currently two options ‘config1.txt’ or ‘config2.txt’. Each is a test case containing
information on the hardware of the OS. 

You will then be prompted to choose a page
replacement algorithm for my memory management unit which serves as a virtual memory
manager. You can enter either ‘lru’ (lest recently used) or ‘fifo’ (first in first out).

You can then choose a process scheduling algorithm by entering a number from 0-6. The algorithms these
numbers represent are listed here:

0 = FCFS (first come first serve)
1 = SJF (shortest job first)
2 = RR (round robin)
3 = HPFSN (highest priority first static priority, non-preemptive)
4 = HPFSP (highest priority first static priority preemptive)
5 = HPFD (highest priority first dynamic priority)
6 = WRR (weighted round robin)

Instructions Set:
EXIT - ends simulation
X ADD Y - where x and why are both integers and the sum of both is produced.
X SUB Y - where x and why are both integers and the y is subtracted from x.
RQ <customer> <amount of R1> <amount of R2> ect. - requests to obtain resources for customer (process)
RL <customer> <amount of R1> <amount of R2> ect.- requests to release resources for customer (process)
* - displays the current resource state.
READ x - reads physical address of virtual address x.
NEW <ID> <arrival time> <CPU burst> <priority> <program size> - creates a new PCB for this process using values inputted.
