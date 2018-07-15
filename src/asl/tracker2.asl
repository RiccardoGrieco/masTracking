// Common tracker behaviour

/* Initial beliefs*/

//TODO these should be perceived from environment or set on agent creation
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

+losingTarget(Ag, Tid, X, Y) //: true
    <-  .print("I'm losing my target (",X,",",Y,"), let's start an auction! Target ID: ",Ag,"-",Tid);
        .broadcast(achieve, cfp(Ag, Tid, X, Y));
        +auctionOngoing(Ag,Tid).
        //!auction(Ag, Tid, X,Y, <parameters that indicates the chain of auctions>)

+partecipate(Ag, Tid, V)[source(S)] 
    :   .print(S," wants to partecipate to the auction for ", Ag,"-",Tid, " ? ", V) &
        .findall(PAg, partecipate(Ag, Tid, _)[source(PAg)], ListOfAnswerer) &
        numberOfAgents(NumberOfAgents) &
        .length(ListOfAnswerer,NumberOfAgents) &
        //TODO tracking(Ag, Tid, X,Y) &
        tracking(Ag, Tid, X, Y)
    <-  .findall(Partecipant, partecipate(Ag, Tid ,true)[source(Partecipant)], ListOfPartecipants);
        .length(ListOfPartecipants, NumberOfPartecipants);
        +numberOfPartecipants(Ag, Tid, NumberOfPartecipants);
        .print("Partecipants individuated, let's get their bid");
        -partecipate(Ag, Tid, _);
        .send(ListOfPartecipants, achieve, placeBid(Ag, Tid, X, Y)).

+bid(Ag, Tid, V)[source(S)]
    :   .print(S, "'s bid for auction ", Ag,"-",Tid, " is: ", V) &
        .findall(bid(B, PAg), bid(Ag, Tid ,B)[source(PAg)], ListOfBids) &
        numberOfPartecipants(Ag, Tid, NumberOfPartecipants) &
        .length(ListOfBids, NumberOfPartecipants) &
        //TODO tracking(Tid, X,Y) &
        tracking(Ag, Tid, X, Y)
    <-  !findWinner(Ag, Tid).

+!findWinner(Ag, Tid)
    :   .findall(bid(B, PAg), bid(Ag, Tid ,B)[source(PAg)], ListOfBids) &
        tracking(Ag, Tid, X, Y)
    <-  .max(ListOfBids, bid(B, Winner));
        .print("The winner of auction ", Ag, "-",Tid, " is ", Winner, " with a bid of ", B);
        //TODO get confirm
        .send(Winner, tell, winner(Ag, Tid, X, Y));
        //TODO get confirm!
        .

+!confirm(Ag, Tid, Confirmation) : Confirmation=true & .print("Auction has ended") //TODO remove this
    <-  //clear belief base regarding the auction)
        !clearAuction(Ag, Tid);
        -tracking(Ag,Tid,_,_).


//TODO what if the winner does not confirm?

+!confirm(Ag, Tid, Confirmation) 
    :   Confirmation=false & .count(bid(Ag,Tid,_),1) //no one left
    <-  !clearAuction(Ag,Tid)
        //TODO implement
        .send("not implemented yet").

+!confirm(Ag, Tid, Confirmation)[source(S)]
    :   Confirmation=false & .count(bid(Ag,Tid,_),N) & N>1 //recursive winner chain
    <-  -bid(Ag, Tid,_)[source(S)];
        !findWinner(Ag, Tid).


// clear beliefs for the auction
+!clearAuction(Ag, Tid)
    <-  .abolish(bid(Ag,Tid,_));
        .abolish(numberOfPartecipants(Ag, Tid,_));
        .abolish(partecipate(Ag,Tid,_));
        .abolish(tracking(Ag,Tid,_,_));
        -auctionOngoing(Ag,Tid).


/* Bidder */

+!cfp(Ag, Tid, X, Y)[source(S)] : amInterested(X, Y) <- .send(S, tell, partecipate(Ag, Tid ,true)).
+!cfp(Ag, Tid, X, Y)[source(S)] : not amInterested(X, Y) <- .send(S, tell, partecipate(Ag, Tid,false)).

+!placeBid(Ag, Tid, X, Y)[source(S)] : true
    <-  //TODO .calculateBid(X, Y, B)
        .random(B); //TODO remove this line
        .print("My bid for auction ",Ag,"-",Tid," is: ", B);
        .send(S, tell, bid(Ag, Tid,B)).

+winner(Ag, Tid, X, Y)[source(S)] 
    :   (not tracking(_,_,_,_) | auctionOngoing(AuctionAg, AuctionTid)) //TODO check correctness
        & .print("Not tracking anything, i confirm") //TODO remove this
    <-  .send(S, achieve, confirm(Ag, Tid, true));
        +tracking(Ag, Tid, X, Y).


//TODO what to do??
+winner(Ag, Tid, X, Y)[source(S)]
    :   tracking(TrackedAg,TrackedTid, _, _) & not auctionOngoing(TrackedAg,TrackedTid)
    <-  .broadcast(achieve, cfp(TrackedAg, TrackedTid, X, Y));
        +auctionOngoing(Ag,Tid).

/*  */

+target(X,Y)[source(S)]
    : S\==self & tracking(Ag,Tid,X,Y)
    <-  .send(S, tell, tracking(Ag,Tid,X,Y)).

/*+target(X,Y)[source(S)]
    : S\==self & not tracking(Ag, Tid, X, Y) & amInterested(X,Y)
    <-
*/
/*+target(X,Y)[source(self)]
    : target(X,Y)[source(S)] & S\==self
  <-
*/

/*+tracking(_,_,X,Y)[source(S)]
    :   S\==self & target(X,Y)
    <-  
*/
//TODO
//+sprayed[source(Strunz)] : hasWeapon <- .kill(S);