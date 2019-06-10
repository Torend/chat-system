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

Also CSP has to be disabled for the Play framework to work because of JS issues