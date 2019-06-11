Akka-WhatsApp

Omer Porzecanski - 208892992 Toren Danino - 203831714

Client actor order-
It contains the LookupActor as main actor.
This actor is used as the play framework actor and as client actor.
It has 2 behaviors- 
1. trying to connect to a server.
2. acts as a client.
once it finds the server it becomes a client.
It is being initialized by the play frameworks and gets the JS WebSocket interface as an ActorRef.
As a client it offers all the client functionality and acts as a front end-
Once ot recieves data to be sent to the user fromm another users\groups it send to the Websocket.
And also once it recieves data  from the websocket, it parases the data and transfers it to the right place.
The main login is in parseCommands- the function that parses the commands and then creates the relevant messages.

Remote service (the server)-
1. It contains the ServerActor
this actor hendling the connection to server and pass message from the user to the groupManager actor ,also contains all connecting user and there data.
2. It contains the GroupManager actor-
this actor mange all the groups logic and contains data for every group.

actor ops - is assistance class for client app and the remote service(the server)
1. Action class contains all the action class for actorRef behavior.
2. Errors class contains all the kind of errors.
3. Util holdong some assistance function.
