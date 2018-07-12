// Common tracker behaviour

/* Initial beliefs*/

auctionNumber(1).
numberOfAgents(3).

/* RULES */

//losingTarget(X) :- ; //TODO

amInterested(X,Y) :- 
    canSee(X1, Y1, X2, Y2) &
    X>=X1 & X<=X2 & Y>=Y1 & Y<=Y2.


/* Initial goals */

/* Plans */

/* Auctioneer */

+losingTarget(X, Y) : auctionNumber(N)
    <-  .print("I'm losing my target (",X,",",Y,"), let's start an auction! ID=",N);
        .broadcast(achieve, cfp(N, X, Y));
        -auctionNumber(N);
        +auctionNumber(N+1).

+partecipate(N,V)[source(S)] 
    :   .print(S," wants to partecipate to the auction ", N, " ? ", V) &
        .findall(Ag, partecipate(N,P)[source(Ag)], ListOfAnswerer) &
        numberOfAgents(NumberOfAgents) &
        .length(ListOfAnswerer,NumberOfAgents) &
        target(X,Y) 
    <-  .findall(Partecipant, partecipate(N,true)[source(Partecipant)], ListOfPartecipants);
        .length(ListOfPartecipants, NumberOfPartecipants);
        +numberOfPartecipants(N, NumberOfPartecipants);
        .print("Partecipants individuated, let's get their bid");
        .send(ListOfPartecipants, achieve, placeBid(N, X, Y)).

+bid(N,V)[source(S)]
    :   .print(S, "'s bid for auction ", N, " is: ", V) &
        .findall(bid(B, Ag), bid(N,B)[source(Ag)], ListOfBids) &
        numberOfPartecipants(N, NumberOfPartecipants) &
        .length(ListOfBids, NumberOfPartecipants) &
        target(X,Y)
    <-  .max(ListOfBids, bid(B, Winner));
        .print("The winner of auction ", N, " is ", Winner, " with a bid of ", B);
        //TODO get confirm
        .send(Winner, tell, winner(N, X, Y));
        //TODO get confirm!
        -target(X,Y).

/* Bidder */

+!cfp(N, X, Y)[source(S)] : amInterested(X, Y) <- .send(S, tell, partecipate(N,true)).
+!cfp(N, X, Y)[source(S)] : not amInterested(X, Y) <- .send(S, tell, partecipate(N,false)).

+!placeBid(N, X, Y)[source(S)] : true
    <-  //TODO .calculateBid(X, Y, B)
        .random(B);
        .print("My bid for auction ",N," is: ", B);
        .send(S, tell, bid(N,B)).

+winner(N, X, Y) : true <- +target(X, Y). //TODO confirm

//TODO
//+sprayed[source(Strunz)] : hasWeapon <- .kill(S);