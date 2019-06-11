Akka-WhatsApp

Omer Porzecanski - 208892992 
Toren Danino - 203831714

Client actor order-
Each client is implemented as an actor.
It contains the LookupActor as main actor.
This actor is used as the play framework actor and as client actor.
The play framework web is in index.scala.main and it has the Chat GUI.
HomeController is responsible over doing the connection between the web and the Actors-
it connects the Websocket to the client (LookupActor) and renders the webpage.
It has 2 behaviors- 
1. trying to connect to a server.
2. acts as a client.
once it finds the server it becomes a client.
It is being initialized by the play frameworks and gets the JS WebSocket interface as an ActorRef.
As a client it offers all the client functionality and acts as a front end-
Once it receives data to be sent to the user from another users\groups it send to the Websocket.
And also once it receives data  from the websocket, it parses the data and transfers it to the right place-
if it is a user it uses the server to get its ActorRef and send messages directly to it 
and if it is a group it will use the server to get the relevant group actor and send the message to it.
Each action in this system is implements using a specially crafted message that using 'match' function we can identify.
The main login is in parseCommands- the function that parses the commands and then creates the relevant messages.

Remote service (the server)-
1. It contains the ServerActor
this actor is handling the connection to the server and pass messages from the user to the GroupManager actor,
also contains all connected user and their relevant data in a Map of UserData class.
2. It contains the GroupManager actor-
this actor manages all the groups logic and contains data for every group in a Map that contains GroupData class as items.
At its base each group is implemented as a router and clients are added and removed when needed.
To implement the administrating mechanism we used an internal state that is saved for each user on the GroupData Map.
For muting- we used scheduling.

Actor ops - is where we store the messages that represent the different actions and error codes implemented on our project. 
1. Action class contains all the action class for actorRef behavior.
2. Errors class contains all the kind of errors.
3. Util holding some assistance function.

To run:
1. sbt akka-remote-service/run
2. sbt play-client-app/run
3. Use browser to enter localhost:9000
