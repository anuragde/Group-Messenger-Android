# Group-Messenger-with-Total-and-FIFO-Ordering-Guarantees (Distributed Systems PA-2B)
Group messenger, providing the Total and FIFO ordering guarantees to address the multicast ordering problem and fault tolerance by handling node failures.

PA Specification : https://docs.google.com/document/d/1xgXwZ6GYA152WT3K0B1MPP7F0mf0sPCPzfqr528pO5Y/edit#

Testing scripts and instructions are provided in the specification document.


#### Algorithm to implement Total and FIFO ordering of messages under a crash-stop failure of at most one-node:

Totally Ordered Multicast can be achieved in two ways:
* Using a sequencer
  - One dedicated “sequencer” that orders all messages
  - Everyone else follows.
* ISIS system
  - Similar to having a sequencer, but the responsibility is
distributed to each sender.

ISIS system is implemented in this assignment.

#### ISIS algorithm for total ordering:

* Sender multicasts message to everyone.
* Nodes reply with proposed priority(sequence no.) by appending process id.
  - Larger than all observed agreed priorities
  - Larger than any previously proposed (by self) priority
* Nodes store message in priority queue
  - Ordered by priority (proposed or agreed)
  - Mark message as undeliverable
* Sender chooses agreed priority, re-multicasts message
with agreed priority
  - Maximum of all proposed priorities
* Nodes upon receiving agreed (final) priority
  - Mark message as deliverable
  - Deliver any deliverable messages at the front of priority queue 
  
Each message sent to a node is used to detect node failure upon socket read timeout.

![](https://github.com/anuragde/Group-Messenger-with-Total-and-FIFO-Ordering-Guarantees/blob/master/GroupMessenger2/images/ISIS_algorithm.png)
  
##### Background theory:

##### FIFO Ordering
* Preserving the process order
* The message delivery order at each process should
preserve the message sending order from every
process. But each process can deliver in a different
order.
* For example,
  - P1: m0, m1, m2
  - P2: m3, m4, m5
  - P3: m6, m7, m8

FIFO Order:
  - P1: m0, m3, m6, m1, m4, m7, m2, m5, m8
  - P2: m0, m4, m6, m1, m3, m7, m2, m5, m8
  - P3: m6, m7, m8, m0, m1, m2, m3, m4, m5


##### Causal Ordering
* Preserving the happened-before relations
* The message delivery order at each process should
preserve the happened-before relations across all
processes. But each process can deliver in a
different order.
* For example,
  - P1: m0, m1, m2
  - P2: m3, m4, m5
  - P3: m6, m7, m8
  - Cross-process happened-before: m0 -> m4, m5 -> m8

Causal order:
 - P1: m0, m3, m6, m1, m4, m7, m2, m5, m8
 - P2: m0, m1, m3, m6, m4, m7, m2, m5, m8
 - P3: m0, m1, m2, m3, m4, m5, m6, m7, m8


##### Total Ordering 
* Every process delivers all messages in the same
order.
* For example,
  - P1: m0, m1, m2
  - P2: m3, m4, m5
  - P3: m6, m7, m8

Total order:
P1: m7, m1, m2, m4, m5, m3, m6, m0, m8
P2: m7, m1, m2, m4, m5, m3, m6, m0, m8
P3: m7, m1, m2, m4, m5, m3, m6, m0, m8









