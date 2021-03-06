// Common tracker behaviour

/* Initial beliefs*/

//TODO these should be perceived from environment or set on agent creation
//auctionNumber(1).
//numberOfAgents(2).
progressiveNo(1).

//From source(percept) to source(self)
+tracking(Ag,Tid, X, Y)[source(percept)] <- +tracking(Ag, Tid, X, Y).

//Update tracked target position
+tracking(Ag,Tid, X, Y)[source(self)] 
    : tracking(Ag, Tid, XPrev, YPrev) & (XPrev\==X | YPrev\==Y)
    <- -tracking(Ag, Tid, Xprev, YPrev).

//Environment says my tracked target is out my viewZone 
+lost[source(percept)]
    <- -tracking(_, _, _, _).

+numberOfAgents(N)[source(percept)] <- +numberOfAgents(N).
+noNeighbors(N)[source(percept)]<- +noNeighbors(N).
+myPosition(X,Y)[source(percept)]<- +myPosition(X,Y).
+canSee(A,B,C,D)[source(percept)]<- +canSee(A,B,C,D).



/* RULES */

//losingTarget(X) :- ; //TODO
//observeTarget(Name,Tid,X,Y) :-
  //  tracking(Name,Tid,X,Y).

amInterested(X,Y) :- 
    canSee(X1, Y1, X2, Y2) &
    X>=X1 & X<=X2 & Y>=Y2 & Y<=Y1. //TODO inverted Y-axis

//isFree :- not tracking(_,_,_,_).

/* Initial goals */

/* Plans */


/*------------------------ Auctioneer ------------------------*/
/* 
* Behaviour of the tracker who has to lose his target.
*
*/

// When losing the target start an auction.
+losingTarget(Ag, Tid, X, Y)[source(percept)]
    <- // -losingTarget(Ag, Tid, X, Y);
        //.print("I'm losing my target (",X,",",Y,"), let's start an auction! Target ID: ",Ag,"-",Tid);
        .broadcast(achieve, cfp(Ag, Tid, X, Y));
        +auctionOngoing(Ag,Tid, X, Y);
        //TODO solo per statistiche
        .print("ASTA").
        //!auction(Ag, Tid, X,Y, <parameters that indicates the chain of auctions>)

//Wait for all agents to tell whether they want to partecipate or not.
//Tell the interested ones to place their bid.
+partecipate(Ag, Tid, V)[source(S)]
    :   //.print(S," wants to partecipate to the auction for ", Ag,"-",Tid, " ? ", V) &
        .findall(PAg, partecipate(Ag, Tid, _)[source(PAg)], ListOfAnswerers) &
        numberOfAgents(N) &
        .length(ListOfAnswerers,N-1) &
        auctionOngoing(Ag, Tid, X, Y) &
        .findall(Partecipant, partecipate(Ag, Tid ,true)[source(Partecipant)], ListOfPartecipants) &
        .length(ListOfPartecipants, NumberOfPartecipants) 
    <-  +numberOfPartecipants(Ag, Tid, NumberOfPartecipants);
        //.print("Partecipants individuated, let's get their bid");
        .abolish(partecipate(Ag, Tid, _));
        .send(ListOfPartecipants, achieve, placeBid(Ag, Tid, X, Y)).


//If there are no partecipants in the auction, cancel it.
+numberOfPartecipants(Ag, Tid, 0)
    :   not winner(_,_,_,_)[source(S)]
    <-  .print("ASTA FALLITA");
        !clearAuction(Ag, Tid).

//If there are no partecipants in my auction and I am the winner 
//of a previous auction, cancel the current and tell the previous
//auctioneer that I can't confirm my win.
+numberOfPartecipants(Ag, Tid, 0)
    :   winner(PrevAg, PrevTid, X, Y)[source(S)]
    <-  .print("ASTA FALLITA");
        !clearAuction(Ag, Tid);
        -winner(PrevAg, PrevTid, _, _)[source(S)];
        .send(S, achieve, confirm(PrevAg, PrevTid, false)).

//If all agents have told me they don't want to partecipate and I am the winner 
//of a previous auction, cancel the current and tell the previous
//auctioneer that I can't confirm my win.
+partecipate(Ag, Tid, V)[source(S)]
    :   .findall(PAg, partecipate(Ag, Tid, _)[source(PAg)], ListOfAnswerers) &
        numberOfAgents(N) &
        .length(ListOfAnswerers,N-1) &
        not partecipate(Ag, Tid, true) &
        winner(PrevAg, PrevTid, _, _)[source(S)]
    <-  !clearAuction(Ag, Tid);
        -winner(PrevAg, PrevTid, _, _)[source(S)];
        .send(S, achieve, confirm(PrevAg, PrevTid, false)).

//If all agents have told me they don't want to partecipate and I'm not the winner 
//of a previous auction, just cancel the current.
+partecipate(Ag, Tid, V)[source(S)]
    :   .findall(PAg, partecipate(Ag, Tid, _)[source(PAg)], ListOfAnswerers) &
        numberOfAgents(N) &
        .length(ListOfAnswerers,N-1) &
        not partecipate(Ag, Tid, true) &
        not winner(PrevAg, PrevTid, _, _)[source(S)]
    <-  !clearAuction(Ag, Tid).

//After all of the agents interested in my target have placed their bid,
//find the winner.
+bid(Ag, Tid, V)[source(S)]
    :   //.print(S, "'s bid for auction ", Ag,"-",Tid, " is: ", V) &
        .findall(bid(B, PAg), bid(Ag, Tid ,B)[source(PAg)], ListOfBids) &
        numberOfPartecipants(Ag, Tid, NumberOfPartecipants) &
        .length(ListOfBids, NumberOfPartecipants) &
        auctionOngoing(Ag, Tid, X, Y)
    <-  !findWinner(Ag, Tid).

//Communicate the win to the winner (only).
+!findWinner(Ag, Tid)
    :   .findall(bid(B, PAg), bid(Ag, Tid ,B)[source(PAg)], ListOfBids) &
        auctionOngoing(Ag, Tid, X, Y)
    <-  .max(ListOfBids, bid(B, Winner));
        //.print("The winner of auction ", Ag, "-",Tid, " is ", Winner, " with a bid of ", B);
        //.print("The winner of auction: ", Winner, ".");
        //TODO get confirm
        .send(Winner, tell, winner(Ag, Tid, X, Y)).

//If the winner of my auction confirms his win and I'm not the winner
//of a previous auction, lose the current target.
+!confirm(Ag, Tid, Confirmation)[source(S)]
    :   Confirmation=true &
        not winner(_,_,_,_)[source(_)]
    <-  .abolish(tracking(Ag,Tid,_,_));
        //-tracking(Ag,Tid,_,_)[source(self)];
        !clearAuction(Ag, Tid).

//If the winner of my auction confirms his win and I am the winner
//of a previous auction, lose the current target and confirm my win
//to the previous auctioneer. 
+!confirm(Ag, Tid, Confirmation)[source(S)]
    :   Confirmation=true &
        winner(PrevAg, PrevTid, X ,Y)[source(S)]
        & .print("Auction for ",Ag,"-",Tid," has ended2")
    <-  +tracking(PrevAg, PrevTid, X, Y);
        !clearAuction(Ag,Tid);
        -winner(PrevAg,PrevTid,_,_)[source(S)];
        -tracking(Ag, Tid, _,_)[source(self)];
        .send(S, achieve, confirm(PrevAg, PrevTid, true)).

//TODO what if the winner does not confirm?

//If the winner of my auction refuses his win, calculate a new winner
//if there are enough partecipants left.
+!confirm(Ag, Tid, Confirmation)[source(S)]
    :   Confirmation=false & .count(bid(Ag,Tid,_),N) & N>1
    <-  -bid(Ag, Tid,_)[source(S)];
        !findWinner(Ag, Tid).

//If the winner of my auction refuses his win and there are no 
//partecipants left and I am the winner of a previous auction,
//cancel my auction and refuse the previous win. 
+!confirm(Ag, Tid, Confirmation) 
    :   Confirmation=false & .count(bid(Ag,Tid,_),1) &
        winner(PrevAg, PrevTid, _, _)[source(S)]
    <-  !clearAuction(Ag,Tid);
        -winner(PrevAg, PrevTid, _, _)[source(S)];
        .send(S, achieve, confirm(PrevAg, PrevTid, false)).

//If the winner of my auction refuses his win and there are no 
//partecipants left and I'm not the winner of a previous auction,
//just cancel my auction. 
+!confirm(Ag, Tid, Confirmation) 
    :   Confirmation=false & .count(bid(Ag,Tid,_),1) &
        not winner(PrevAg, PrevTid, _, _)[source(S)]
    <-  .print("ASTA FALLITA");
        !clearAuction(Ag,Tid).
        //TODO implement
        //.print("not implemented yet").

//Clear the beliefs used to manage an auction
+!clearAuction(Ag, Tid)
    <-  -auctionOngoing(Ag,Tid, _, _);
        .abolish(bid(Ag,Tid,_));
        -numberOfPartecipants(Ag, Tid,_);
        .abolish(partecipate(Ag,Tid,_)).


/*------------------------ Bidder ------------------------*/
/**
* Behaviour of the agent when he is asked whether he wants to
* take care of a target tracked by another agent.
**/

//Tell an auctioneer whether I want to partecipate to his auction or not.
+!cfp(Ag, Tid, X, Y)[source(S)] : amInterested(X, Y) <- .send(S, tell, partecipate(Ag, Tid ,true)).
+!cfp(Ag, Tid, X, Y)[source(S)] : not amInterested(X, Y) <- .send(S, tell, partecipate(Ag, Tid,false)).

// Place a bid for a target.
+!placeBid(Ag, Tid, X, Y)[source(S)]
    <-  camera.calculateBid(X, Y, B);
        .send(S, tell, bid(Ag, Tid,B)).

//When I'm told I'm the winner and I'm free,
//confirm the win to the auctioneer.
+winner(Ag, Tid, X, Y)[source(S)] 
    :   not tracking(_,_,_,_) //TODO check correctness
        //& .print("I'm tracking no one, I confirm my win.") //TODO remove this
    <-  +tracking(Ag, Tid, X, Y);
        .send(S, achieve, confirm(Ag, Tid, true));
        -winner(Ag, Tid, X, Y)[source(S)].

//When I'm told I'm the winner of an auction but I'm also the auctioneer
//for another auction, lose the current target and confirm the win.
+winner(Ag, Tid, X, Y)[source(S)] 
    :   auctionOngoing(AuctionAg, AuctionTid,_,_) //TODO check correctness
        //& .print("Auction ongoing, I lose my target and I confirm my win.") //TODO remove this
    <-  +tracking(Ag, Tid, X, Y);
        .abolish(tracking(AuctionAg, AuctionTid, _, _));
        .send(S, achieve, confirm(Ag, Tid, true));
        -winner(Ag, Tid, X, Y)[source(S)];.

//When I'm told I'm the winner of an auction but I'm already tracking
//a target, start a new auction before I confirm my win.
+winner(Ag, Tid, X, Y)[source(S)]
    :   tracking(TrackedAg,TrackedTid, TrackedX, TrackedY) & 
        not auctionOngoing(TrackedAg,TrackedTid , _, _)
    <-  //.print("I'm the winner but I'm tracking someone. I'll start the auction for ",TrackedAg,"-", TrackedTid);
        +auctionOngoing(TrackedAg,TrackedTid , TrackedX, TrackedY);
        //TODO solo per statistiche
        .print("ASTA");
        .broadcast(achieve, cfp(TrackedAg, TrackedTid, TrackedX, TrackedY)).

/*------------------------ A Wild Target Appears ------------------------*/
/*
+target(X,Y)[source(S)]
    :   S\==self & tracking(Ag,Tid,X,Y)
    <-  -target(X,Y)[source(S)];
        .send(S, tell, tracking(Ag,Tid,X,Y)).
*/

// When I perceive a new target, ask the other agents whether
// it's tracked by one of them.

//TODO aggiungere qualcosa da fare per quando metteremo più target
+target(X, Y)[source(percept)]
    :   tracking(Ag, Tid, X2, Y2)[source(_)]
    <-  .print("Sto gia' tracciando ",Ag, "-",Tid, "-",X2,"-",Y2," !");
        .print("Per il momento non facciamo niente.").

+target(X,Y)[source(percept)]  
    :   not tracking(_, _, _, _)[source(_)] //else?
    <-  //.print("aggiunto target da jason1 (",X," ",Y,")");
        .broadcast(achieve, tellMeTracking(X,Y)).


// Tell another agent if I'm already tracking a target at the specified position
// and whether I'm interested in it or not.
+!tellMeTracking(X,Y)[source(S)] 
    :  not amInterested(X,Y) 
    <- .send(S, tell, alreadyTracking(X,Y,false, false)).

+!tellMeTracking(X,Y)[source(S)] 
    :  amInterested(X,Y) & tracking(_,_,X,Y)[source(_)]
    <- .send(S, tell, alreadyTracking(X,Y,true, true)).

+!tellMeTracking(X,Y)[source(S)] 
    :  amInterested(X,Y) & not tracking(_,_,X,Y)[source(_)]
    <- .send(S, tell, alreadyTracking(X,Y,false, true)).

//When one of the agents tells me he is tracking the target
//I've found, I ignore it. 
+alreadyTracking(X,Y,V,_)[source(S)]
    :   numberOfAgents(N) &
        .findall(Ag, alreadyTracking(X,Y,_,_)[source(Ag)], ListOfAnswerers) &
        .length(ListOfAnswerers,N-1) &
        alreadyTracking(X,Y,true,_)[source(_)]
    <-  //.print("Non me lo prendoooo"); //TODO
       // -target(X,Y)[source(_)];
        .abolish(alreadyTracking(X,Y,_,_)).

//If all of the agents tell me they don't know the target at the
//specified position, I take the task only if my name is lexicographically
//bigger then the agents interested in that position. (This heuristic
//lets only one agent take the task) 
+alreadyTracking(X,Y,V,_)[source(S)]
    :   numberOfAgents(N) &
        .findall(Ag, alreadyTracking(X,Y,_,_)[source(Ag)], ListOfAnswerers) &
        .length(ListOfAnswerers,N-1) &
        not alreadyTracking(X,Y,true,_)[source(_)] &
        .findall(I, alreadyTracking(X,Y,_,true)[source(I)], ListOfInterested) &
        .length(ListOfInterested, 0) &      // se nessun altro è interessato
        progressiveNo(Tid) & .my_name(Name)
    <-  .abolish(target(X, Y)); //TODO
        .abolish(alreadyTracking(X,Y,_,_));
        //track(Name, Tid, X, Y); //TODO
        +tracking(Name, Tid, X, Y);
        //.print("No one is interested about the target, I start to track the target!");
        -progressiveNo(Tid);
        +progressiveNo(Tid+1).

+alreadyTracking(X,Y,V,_)[source(S)]
    :   numberOfAgents(N) &
        .findall(Ag, alreadyTracking(X,Y,_,_)[source(Ag)], ListOfAnswerers) &
        .length(ListOfAnswerers,N-1) &
        not alreadyTracking(X,Y,true,_)[source(_)] &
        .findall(I, alreadyTracking(X,Y,_,true)[source(I)], ListOfInterested) &
        progressiveNo(Tid) & .my_name(Name) &
        .max(ListOfInterested, Max) &
        Max<=Name 
    <-  //-target(X, Y)[source(percept)];
        +tracking(Name, Tid, X, Y);
        .abolish(alreadyTracking(X, Y, _, _));
        //.print("Someone is interested about the target, but I win and I start to track the target!");
        -progressiveNo(Tid);
        +progressiveNo(Tid+1).

//TODO Jolly
//+sprayed[source(Strunz)] : hasWeapon <- .kill(S);